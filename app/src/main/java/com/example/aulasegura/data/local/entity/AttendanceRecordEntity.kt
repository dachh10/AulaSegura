package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Local cache of `attendanceSessions/{sessionId}/records/{studentId}`.
 *
 * The Firestore document id is the student id, so the composite primary key is
 * `(sessionId, studentId)`: records are always merged per student, never by
 * replacing the whole group, which is the conflict rule in `CONTEXT.md` §8.4.
 *
 * No foreign key to [AttendanceSessionEntity] on purpose: a record may reach
 * Room before its session when a queued write is replayed, and losing a record
 * because of ordering would be worse than an orphan row.
 */
@Entity(
    tableName = "attendance_records",
    primaryKeys = ["sessionId", "studentId"],
    indices = [Index("sessionId"), Index("syncState")],
)
data class AttendanceRecordEntity(
    val sessionId: String,
    val studentId: String,
    val listNumber: Int,
    /** `"present"`, `"absent"`, `"late"` or `"justified"`. */
    val status: String,
    /** Local time of the mark, `"08:12"`. */
    val time: String? = null,
    val note: String? = null,
    val updatedAt: Long,
    val updatedBy: String,
    val syncState: SyncState = SyncState.SYNCED,
)
