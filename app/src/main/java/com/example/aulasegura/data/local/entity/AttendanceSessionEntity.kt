package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local cache of `attendanceSessions/{sessionId}`.
 *
 * A session can be created offline, so the primary key is the client-generated
 * UUID and [syncState] tracks how far the write got. The Firestore `counts` and
 * `location` maps are flattened into columns: Room has no embedded-map type and
 * flattening keeps the queries simple.
 */
@Entity(
    tableName = "attendance_sessions",
    indices = [Index("groupId"), Index("date"), Index("teacherId"), Index("syncState")],
)
data class AttendanceSessionEntity(
    @PrimaryKey val sessionId: String,
    val groupId: String,
    val schoolId: String,
    val teacherId: String,
    val subject: String,
    /** ISO date, `"2026-11-03"`, kept as text so range queries stay lexicographic. */
    val date: String,
    val startedAt: Long,
    val closedAt: Long? = null,
    /** `"open"` or `"closed"`. */
    val status: String,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val justifiedCount: Int = 0,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationAccuracy: Double? = null,
    val locationDistanceMeters: Double? = null,
    val locationVerified: Boolean? = null,
    val syncState: SyncState = SyncState.SYNCED,
    val createdAt: Long,
    val updatedAt: Long,
)
