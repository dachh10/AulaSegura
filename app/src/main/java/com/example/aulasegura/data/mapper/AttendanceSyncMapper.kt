package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.mapper.toTimestamp

/**
 * Room entity to Firestore document maps for the attendance flow.
 *
 * These maps are written with `set(document)`, not `update()`, on a document
 * whose id is the local UUID. That makes a replay idempotent: sending the same
 * session twice leaves one document, not two (`CONTEXT.md` §8.3).
 *
 * Local-only columns (`syncState`) never leave the device.
 */
object AttendanceSyncMapper {

    /**
     * Recomputes the four counters from the actual records.
     *
     * Counters are denormalised in Firestore for list screens, so they must be
     * derived from the rows being written instead of trusted from a stale
     * entity: two devices marking the same group would otherwise persist
     * counters that contradict the records.
     */
    fun AttendanceSessionEntity.withCountsFrom(
        records: List<AttendanceRecordEntity>,
    ): AttendanceSessionEntity {
        val byStatus = records.groupingBy { it.status }.eachCount()
        return copy(
            presentCount = byStatus[STATUS_PRESENT] ?: 0,
            absentCount = byStatus[STATUS_ABSENT] ?: 0,
            lateCount = byStatus[STATUS_LATE] ?: 0,
            justifiedCount = byStatus[STATUS_JUSTIFIED] ?: 0,
        )
    }

    fun AttendanceSessionEntity.toFirestoreMap(): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "sessionId" to sessionId,
            "groupId" to groupId,
            "schoolId" to schoolId,
            "teacherId" to teacherId,
            "subject" to subject,
            "date" to date,
            "startedAt" to startedAt.toTimestamp(),
            "status" to status,
            "counts" to mapOf(
                "present" to presentCount.toLong(),
                "absent" to absentCount.toLong(),
                "late" to lateCount.toLong(),
                "justified" to justifiedCount.toLong(),
            ),
            "createdAt" to createdAt.toTimestamp(),
            "updatedAt" to updatedAt.toTimestamp(),
        )
        closedAt?.let { data["closedAt"] = it.toTimestamp() }

        val location = mutableMapOf<String, Any>()
        locationLat?.let { location["lat"] = it }
        locationLng?.let { location["lng"] = it }
        locationAccuracy?.let { location["accuracy"] = it }
        locationDistanceMeters?.let { location["distanceMeters"] = it }
        locationVerified?.let { location["verified"] = it }
        if (location.isNotEmpty()) data["location"] = location

        return data
    }

    fun AttendanceRecordEntity.toFirestoreMap(): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "studentId" to studentId,
            "listNumber" to listNumber.toLong(),
            "status" to status,
            "updatedAt" to updatedAt.toTimestamp(),
            "updatedBy" to updatedBy,
        )
        time?.let { data["time"] = it }
        note?.let { data["note"] = it }
        return data
    }

    private const val STATUS_PRESENT = "present"
    private const val STATUS_ABSENT = "absent"
    private const val STATUS_LATE = "late"
    private const val STATUS_JUSTIFIED = "justified"
}
