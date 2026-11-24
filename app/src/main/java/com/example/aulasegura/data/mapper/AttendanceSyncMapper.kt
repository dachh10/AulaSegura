package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.mapper.toTimestamp
import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark

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
     * The `counts` sub-document of a session. Shared by the roll-call write and
     * by the justification flip so both paths store the same types and keys.
     */
    fun AttendanceCounts.toFirestoreMap(): Map<String, Any> = mapOf(
        "present" to present.toLong(),
        "absent" to absent.toLong(),
        "late" to late.toLong(),
        "justified" to justified.toLong(),
    )

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
        val counts = AttendanceCounts.of(records.map { AttendanceMark.fromWire(it.status) })
        return copy(
            presentCount = counts.present,
            absentCount = counts.absent,
            lateCount = counts.late,
            justifiedCount = counts.justified,
        )
    }

    fun AttendanceSessionEntity.toFirestoreMap(): Map<String, Any> {
        val data = mutableMapOf<String, Any>(
            "sessionId" to sessionId,
            "groupId" to groupId,
            "schoolId" to schoolId,
            "teacherId" to teacherId,
            "subject" to subject,
            "subjectId" to subjectId,
            "date" to date,
            "startedAt" to startedAt.toTimestamp(),
            "status" to status,
            "counts" to AttendanceCounts(
                present = presentCount,
                absent = absentCount,
                late = lateCount,
                justified = justifiedCount,
            ).toFirestoreMap(),
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
}
