package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.AttendanceDao
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.mapper.AttendanceSyncMapper.toFirestoreMap
import com.example.aulasegura.data.mapper.AttendanceSyncMapper.withCountsFrom
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Writes one attendance session, plus its per-student records, to Firestore.
 *
 * The outbox row's `entityLocalId` **is** the session id, which is also the
 * Firestore document id: replaying the same row overwrites the same document
 * instead of creating a duplicate (`CONTEXT.md` §8.3).
 *
 * Session and records go in a single `WriteBatch` so a half-written session
 * (counters without records) cannot exist after a partial failure.
 */
class AttendanceSyncHandler(
    private val attendanceDao: AttendanceDao,
    private val firestore: FirebaseFirestore,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val session = attendanceDao.findSessionById(entry.entityLocalId)
            ?: return SyncOutcome.Permanent("Session ${entry.entityLocalId} is no longer stored locally")

        val records = attendanceDao.records(session.sessionId)
        val withCounts = session.withCountsFrom(records)

        // Painted while the batch is in flight, and written before the network
        // call on purpose: if the process dies mid-commit the row stays SYNCING
        // until the next pass rewrites it, which is exactly the truth.
        markSession(withCounts.sessionId, SyncState.SYNCING)

        return try {
            val batch = firestore.batch()
            val sessionRef = firestore
                .collection(COLLECTION_SESSIONS)
                .document(session.sessionId)

            batch.set(sessionRef, withCounts.toFirestoreMap())
            records.forEach { record ->
                batch.set(
                    sessionRef.collection(COLLECTION_RECORDS).document(record.studentId),
                    record.toFirestoreMap(),
                )
            }
            batch.commit().await()

            markSession(withCounts.sessionId, SyncState.SYNCED)
            // Fired only after Firestore accepted the batch: this is the number
            // that tells whether offline capture is actually reaching the cloud,
            // so counting the local save instead would hide every outage.
            analytics.log(AnalyticsEvent.RollCallSaved(synced = true))
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
            val retryable = denied.isRetryable
            markSession(withCounts.sessionId, SyncState.FAILED)
            // The group travels in the message on purpose: "no guardó" without
            // the group turns a permissions bug into an unsolvable support ticket.
            if (retryable) {
                SyncOutcome.Retry(
                    "Firestore unavailable for group ${session.groupId} (${denied.code})",
                    denied,
                )
            } else {
                SyncOutcome.Permanent(
                    "Firestore rejected the session of group ${session.groupId} (${denied.code})",
                    denied,
                )
            }
        }
    }

    private suspend fun markSession(
        sessionId: String,
        state: SyncState,
    ) {
        // Local bookkeeping only; if this fails the next pass rewrites the
        // session anyway, so there is nothing to recover here.
        runCatching {
            attendanceDao.findSessionById(sessionId)?.let {
                attendanceDao.upsertSession(it.copy(syncState = state))
            }
            attendanceDao.records(sessionId).forEach { record ->
                attendanceDao.upsertRecord(record.copy(syncState = state))
            }
        }
    }

    private companion object {
        const val COLLECTION_SESSIONS = "attendanceSessions"
        const val COLLECTION_RECORDS = "records"
    }
}
