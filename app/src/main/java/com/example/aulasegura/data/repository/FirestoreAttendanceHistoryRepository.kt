package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.AttendanceDao
import com.example.aulasegura.data.local.dao.GroupDao
import com.example.aulasegura.data.local.dao.StudentDao
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.mapper.attendanceRecordEntityFrom
import com.example.aulasegura.data.mapper.attendanceSessionEntityFrom
import com.example.aulasegura.data.mapper.toRollCallSync
import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionRecord
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.repository.AttendanceHistoryRepository
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.HistoryRefreshResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Cache-first history of the teacher's roll calls.
 *
 * The same rule as the roster applies, for the same reason: `Source.SERVER` on
 * every refresh. A cached Firestore read can come back empty instead of failing,
 * and a history that resolves to "no sessions" because the phone has no signal is
 * worse than an error — the teacher would think their attendance was lost.
 *
 * Records are pulled only for the session being expanded. Fetching 28 records for
 * every row of the list would turn opening the history into hundreds of reads.
 */
class FirestoreAttendanceHistoryRepository(
    private val firestore: FirebaseFirestore,
    private val attendanceDao: AttendanceDao,
    private val groupDao: GroupDao,
    private val studentDao: StudentDao,
    private val auth: AuthRepository,
) : AttendanceHistoryRepository {

    override fun observeHistory(): Flow<List<AttendanceSessionSummary>> {
        val user = auth.currentUser ?: return flowOf(emptyList())
        return combine(
            attendanceDao.observeSessionsByTeacher(user.uid),
            groupDao.observeBySchool(user.schoolId),
        ) { sessions, groups ->
            val labels = groups.associate { it.groupId to it.name }
            sessions.map { it.toSummary(labels) }
        }
    }

    override suspend fun refreshHistory(): HistoryRefreshResult {
        val user = auth.currentUser ?: return HistoryRefreshResult.SignedOut

        // `teacherId == uid` is exactly what the read rule checks, so the query
        // carries the same filter; the composite index (teacherId + date desc)
        // is already published.
        val snapshot = try {
            withTimeout(REFRESH_TIMEOUT_MILLIS) {
                firestore.collection(SESSIONS)
                    .whereEqualTo(FIELD_TEACHER_ID, user.uid)
                    .orderBy(FIELD_DATE, Query.Direction.DESCENDING)
                    .limit(HISTORY_LIMIT)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            return HistoryRefreshResult.Failed(error.toRosterFailure())
        }

        val sessions = snapshot.documents.mapNotNull { document ->
            document.data?.let { attendanceSessionEntityFrom(document.id, it) }
        }
        if (sessions.isNotEmpty()) attendanceDao.upsertSessions(sessions)

        return HistoryRefreshResult.Updated(sessions.size)
    }

    override fun observeDetail(sessionId: String): Flow<AttendanceSessionDetail?> {
        val user = auth.currentUser ?: return flowOf(null)
        return combine(
            attendanceDao.observeSession(sessionId),
            attendanceDao.observeRecords(sessionId),
            groupDao.observeBySchool(user.schoolId),
            studentDao.observeBySchool(user.schoolId),
        ) { session, records, groups, students ->
            if (session == null) {
                null
            } else {
                val labels = groups.associate { it.groupId to it.name }
                val names = students.associate { it.studentId to it.fullName }

                AttendanceSessionDetail(
                    summary = session.toSummary(labels),
                    records = records.sortedBy { it.listNumber }.map { record ->
                        AttendanceSessionRecord(
                            studentId = record.studentId,
                            listNumber = record.listNumber,
                            fullName = names[record.studentId]
                                ?: "Alumno ${record.listNumber.toString().padStart(2, '0')}",
                            mark = AttendanceMark.fromWire(record.status),
                            time = record.time,
                        )
                    },
                )
            }
        }
    }

    override suspend fun refreshRecords(sessionId: String): HistoryRefreshResult {
        auth.currentUser ?: return HistoryRefreshResult.SignedOut

        val snapshot = try {
            withTimeout(REFRESH_TIMEOUT_MILLIS) {
                firestore.collection(SESSIONS)
                    .document(sessionId)
                    .collection(RECORDS)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            // Best effort by design: the cache may already hold the records (a
            // session taken on this phone), and failing the expansion would hide
            // what is stored locally.
            return HistoryRefreshResult.Failed(error.toRosterFailure())
        }

        val records = snapshot.documents.mapNotNull { document ->
            document.data?.let { attendanceRecordEntityFrom(sessionId, document.id, it) }
        }
        if (records.isNotEmpty()) attendanceDao.upsertRecords(records)

        return HistoryRefreshResult.Updated(records.size)
    }

    private fun AttendanceSessionEntity.toSummary(labels: Map<String, String>) =
        AttendanceSessionSummary(
            sessionId = sessionId,
            groupId = groupId,
            groupLabel = labels[groupId] ?: groupId,
            subject = subject,
            date = date,
            startedAt = startedAt,
            counts = AttendanceCounts(
                present = presentCount,
                absent = absentCount,
                late = lateCount,
                justified = justifiedCount,
            ),
            verified = locationVerified,
            sync = syncState.toRollCallSync(),
        )

    private companion object {
        const val SESSIONS = "attendanceSessions"
        const val RECORDS = "records"
        const val FIELD_TEACHER_ID = "teacherId"
        const val FIELD_DATE = "date"

        /** Enough history for a term without turning the refresh into a download. */
        const val HISTORY_LIMIT = 120L

        /** Same ceiling as the roster refresh: a school Wi-Fi hiccup vs. no signal. */
        const val REFRESH_TIMEOUT_MILLIS = 12_000L
    }
}
