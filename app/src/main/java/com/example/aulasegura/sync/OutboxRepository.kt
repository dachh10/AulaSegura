package com.example.aulasegura.sync

import androidx.room.withTransaction
import com.example.aulasegura.data.local.AulaSeguraDatabase
import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState

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

        scheduling.schedule()
    }
}
