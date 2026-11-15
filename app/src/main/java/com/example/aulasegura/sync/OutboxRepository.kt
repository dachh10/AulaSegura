package com.example.aulasegura.sync

import androidx.room.withTransaction
import com.example.aulasegura.data.local.AulaSeguraDatabase
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger

/**
 * The write path the UI uses when a teacher closes a roll call.
 *
 * The session, its records and the outbox entry are written in **one database
 * transaction**: either the teacher sees a `PENDING` session that is guaranteed
 * to be queued, or nothing changes. A session marked `PENDING` with no outbox
 * row would be data that never leaves the device.
 *
 * `payloadJson` is `{}` because for attendance the target is identified by
 * `entityLocalId` alone (it is the session id, which is also the Firestore
 * document id). Operations that need more than an id, such as a media upload,
 * put it in the payload.
 */
class OutboxRepository(
    private val database: AulaSeguraDatabase,
    private val scheduling: SyncScheduling,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) {

    suspend fun enqueueAttendanceSession(
        session: AttendanceSessionEntity,
        records: List<AttendanceRecordEntity>,
    ) {
        database.withTransaction {
            database.attendanceDao().upsertSession(session.copy(syncState = SyncState.PENDING))
            database.attendanceDao().upsertRecords(
                records.map { it.copy(syncState = SyncState.PENDING) },
            )
            database.outboxDao().insert(
                OutboxEntity(
                    opType = SyncOperation.UPSERT_ATTENDANCE_SESSION.name,
                    entityLocalId = session.sessionId,
                    payloadJson = "{}",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        // Saved on the device, not yet in the cloud. Both moments fire this event
        // with different `synced` values: logging only the sync success would make
        // every session stuck in the outbox invisible on the dashboard, which is
        // exactly the failure this counter exists to catch. Outside the
        // transaction so telemetry never holds a database lock.
        analytics.log(AnalyticsEvent.RollCallSaved(synced = false))

        scheduling.schedule()
    }

    /**
     * Queues a new incident: the draft and its outbox row in one transaction.
     *
     * Same rule as attendance — a `PENDING` draft is always backed by a row that
     * will be replayed, never a document that only exists on this phone. No
     * Analytics event here: `incident_created` counts incidents the teacher
     * created, and it is the use case that knows a creation actually happened.
     */
    suspend fun enqueueIncident(draft: IncidentDraftEntity) {
        database.withTransaction {
            database.incidentDao().upsertDraft(draft.copy(syncState = SyncState.PENDING))
            database.outboxDao().insert(
                OutboxEntity(
                    opType = SyncOperation.UPSERT_INCIDENT.name,
                    entityLocalId = draft.incidentId,
                    payloadJson = "{}",
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        scheduling.schedule()
    }
}
