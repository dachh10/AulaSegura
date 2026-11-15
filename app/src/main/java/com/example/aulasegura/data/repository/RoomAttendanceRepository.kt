package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.AttendanceDao
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.mapper.AttendanceSyncMapper.withCountsFrom
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.RollCallDraft
import com.example.aulasegura.domain.model.RollCallEntry
import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.AttendanceStart
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.SaveRollCallOutcome
import com.example.aulasegura.sync.OutboxRepository
import com.example.aulasegura.util.AttendanceTimes
import java.util.UUID
import kotlinx.coroutines.flow.first

/**
 * The roll call write path: Room first, Firestore later.
 *
 * Nothing here talks to the network. A session is written to the cache and to
 * the outbox in one transaction and the sync worker is what reaches Firestore,
 * so a teacher in airplane mode gets exactly the same behaviour as one online —
 * the only difference is how long the `PENDING` state lasts.
 *
 * The session id is minted here, on the client, before the first write. That is
 * the whole idempotency story: the retry sends the same document id, so it
 * overwrites instead of duplicating (`CONTEXT.md` §8.3).
 */
class RoomAttendanceRepository(
    private val attendanceDao: AttendanceDao,
    private val outbox: OutboxRepository,
    private val groups: GroupRepository,
    private val auth: AuthRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newSessionId: () -> String = { UUID.randomUUID().toString() },
) : AttendanceRepository {

    override suspend fun startDraft(groupId: String): AttendanceStart {
        val teacher = auth.currentUser ?: return AttendanceStart.SignedOut
        val group = groups.observeMyGroups().first().firstOrNull { it.id == groupId }
            ?: return AttendanceStart.NoGroup(groupId)
        val now = clock()

        return AttendanceStart.Ready(
            RollCallDraft(
                sessionId = newSessionId(),
                groupId = group.id,
                groupLabel = group.label,
                schoolId = group.schoolId,
                teacherId = teacher.uid,
                // The profile carries the subjects the teacher declared; an empty
                // list stays an empty string rather than an invented label in
                // Firestore. The screen prints "Sin materia asignada" for it.
                subject = teacher.subjects.firstOrNull().orEmpty(),
                date = AttendanceTimes.isoDate(now),
                startedAt = now,
            ),
        )
    }

    override suspend fun save(
        draft: RollCallDraft,
        entries: List<RollCallEntry>,
    ): SaveRollCallOutcome {
        val teacher = auth.currentUser ?: return SaveRollCallOutcome.SignedOut

        // The rules authorize the session by `teacherId == uid`. Catching a
        // session that was opened by another account here turns a confusing
        // PERMISSION_DENIED in the worker into an immediate screen message.
        if (teacher.uid != draft.teacherId) {
            return SaveRollCallOutcome.Failed(draft.groupId)
        }

        val now = clock()
        val arrivalTime = AttendanceTimes.clockTime(now)

        val records = entries.map { entry ->
            AttendanceRecordEntity(
                sessionId = draft.sessionId,
                studentId = entry.studentId,
                listNumber = entry.listNumber,
                status = entry.mark.wire,
                // Only an arrival has a time. An absence with "08:12" on it would
                // read like the student showed up, which is the one thing the
                // tutor must be able to trust.
                time = entry.mark.arrivalTimeOrNull(arrivalTime),
                updatedAt = now,
                updatedBy = teacher.uid,
            )
        }

        val session = AttendanceSessionEntity(
            sessionId = draft.sessionId,
            groupId = draft.groupId,
            schoolId = draft.schoolId,
            teacherId = draft.teacherId,
            subject = draft.subject,
            date = draft.date,
            startedAt = draft.startedAt,
            closedAt = now,
            status = STATUS_CLOSED,
            createdAt = now,
            updatedAt = now,
        )
            // Counters are derived from the records being written, not from the
            // chips the teacher saw: those are display state, this is the data.
            .withCountsFrom(records)

        return try {
            outbox.enqueueAttendanceSession(session, records)
            SaveRollCallOutcome.Queued
        } catch (failed: Exception) {
            // Room is the local source of truth: if the transaction did not
            // commit, nothing changed and the teacher can retry the same screen.
            SaveRollCallOutcome.Failed(draft.groupId)
        }
    }

    private fun AttendanceMark.arrivalTimeOrNull(clockTime: String): String? = when (this) {
        AttendanceMark.PRESENT, AttendanceMark.LATE -> clockTime
        AttendanceMark.ABSENT, AttendanceMark.JUSTIFIED -> null
    }

    private companion object {
        const val STATUS_CLOSED = "closed"
    }
}
