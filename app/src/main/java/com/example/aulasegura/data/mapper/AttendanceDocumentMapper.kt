package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.mapper.toEpochMillis
import com.example.aulasegura.domain.model.RollCallSync
import com.google.firebase.Timestamp

/**
 * Firestore documents to Room entities, for the read side of attendance.
 *
 * The write path already had [AttendanceSyncMapper]; this is its inverse, and it
 * takes a raw map instead of a `DocumentSnapshot` for the same reason the roster
 * mappers do: a snapshot cannot be built in a unit test, and the bugs live in the
 * reading (`counts` arriving as `Long`, a missing `closedAt`, a session created
 * before `location` existed).
 *
 * `syncState` is always `SYNCED`: a document that came from the server is, by
 * definition, already there.
 */

fun attendanceSessionEntityFrom(id: String, data: Map<String, Any?>): AttendanceSessionEntity? {
    val groupId = data["groupId"] as? String ?: return null
    val schoolId = data["schoolId"] as? String ?: return null
    val teacherId = data["teacherId"] as? String ?: return null
    val date = data["date"] as? String ?: return null
    val counts = data["counts"] as? Map<*, *>
    val location = data["location"] as? Map<*, *>

    return AttendanceSessionEntity(
        sessionId = id,
        groupId = groupId,
        schoolId = schoolId,
        teacherId = teacherId,
        subject = data["subject"] as? String ?: "",
        date = date,
        startedAt = millis(data["startedAt"]),
        closedAt = (data["closedAt"] as? Timestamp)?.toEpochMillis(),
        status = data["status"] as? String ?: STATUS_CLOSED,
        presentCount = count(counts, "present"),
        absentCount = count(counts, "absent"),
        lateCount = count(counts, "late"),
        justifiedCount = count(counts, "justified"),
        locationLat = number(location?.get("lat")),
        locationLng = number(location?.get("lng")),
        locationAccuracy = number(location?.get("accuracy")),
        locationDistanceMeters = number(location?.get("distanceMeters")),
        locationVerified = location?.get("verified") as? Boolean,
        syncState = SyncState.SYNCED,
        createdAt = millis(data["createdAt"]),
        updatedAt = millis(data["updatedAt"]),
    )
}

fun attendanceRecordEntityFrom(
    sessionId: String,
    studentId: String,
    data: Map<String, Any?>,
): AttendanceRecordEntity = AttendanceRecordEntity(
    sessionId = sessionId,
    studentId = studentId,
    listNumber = (data["listNumber"] as? Number)?.toInt() ?: 0,
    // No invented default: an unreadable status becomes an empty string and the
    // UI shows "sin registrar" instead of claiming the student was present.
    status = data["status"] as? String ?: "",
    time = data["time"] as? String,
    note = data["note"] as? String,
    updatedAt = millis(data["updatedAt"]),
    updatedBy = data["updatedBy"] as? String ?: "",
    syncState = SyncState.SYNCED,
)

/**
 * Room's sync enum to the domain enum the UI follows.
 *
 * Shared by the write path and the history so the two cannot disagree about what
 * `FAILED` means.
 */
fun SyncState.toRollCallSync(): RollCallSync = when (this) {
    SyncState.PENDING -> RollCallSync.PENDING
    SyncState.SYNCING -> RollCallSync.SYNCING
    SyncState.SYNCED -> RollCallSync.SYNCED
    SyncState.FAILED -> RollCallSync.FAILED
}

/**
 * A missing timestamp maps to `0`, never to "now": the history sorts by date, and
 * a document without `startedAt` would otherwise jump to the top of the list.
 */
private fun millis(value: Any?): Long = (value as? Timestamp)?.toEpochMillis() ?: 0L

private fun count(counts: Map<*, *>?, key: String): Int =
    (counts?.get(key) as? Number)?.toInt() ?: 0

private fun number(value: Any?): Double? = (value as? Number)?.toDouble()

private const val STATUS_CLOSED = "closed"
