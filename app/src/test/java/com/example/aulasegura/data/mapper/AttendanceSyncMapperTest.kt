package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.AttendanceRecordEntity
import com.example.aulasegura.data.local.entity.AttendanceSessionEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.mapper.AttendanceSyncMapper.toFirestoreMap
import com.example.aulasegura.data.mapper.AttendanceSyncMapper.withCountsFrom
import com.example.aulasegura.domain.model.AttendanceCounts
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The map produced here is what actually lands in Firestore, so it is worth
 * pinning without a device: wrong types (an `Int` instead of a `Long`) or a
 * leaked local column are exactly the failures that only show up in production.
 */
class AttendanceSyncMapperTest {

    @Test
    fun derivesCountersFromRecordsInsteadOfTrustingTheEntity() {
        val session = session(present = 0, absent = 0, late = 0, justified = 0)
        val records = listOf(
            record("stu-1", "present"),
            record("stu-2", "present"),
            record("stu-3", "absent"),
            record("stu-4", "late"),
            record("stu-5", "justified"),
        )

        val withCounts = session.withCountsFrom(records)

        assertEquals(2, withCounts.presentCount)
        assertEquals(1, withCounts.absentCount)
        assertEquals(1, withCounts.lateCount)
        assertEquals(1, withCounts.justifiedCount)
    }

    @Test
    fun countsMapStoresIntegersAsLongForFirestore() {
        val map = AttendanceCounts(present = 2, absent = 0, late = 1, justified = 3)
            .toFirestoreMap()

        assertEquals(2L, map["present"] as Long)
        assertEquals(0L, map["absent"] as Long)
        assertEquals(1L, map["late"] as Long)
        assertEquals(3L, map["justified"] as Long)
    }

    @Test
    fun sessionMapNeverLeaksLocalStateAndOmitsEmptyOptionalFields() {
        val map = session(present = 3).toFirestoreMap()

        assertFalse("syncState is a Room concept", map.containsKey("syncState"))
        assertFalse("no closedAt means the session is still open", map.containsKey("closedAt"))
        assertFalse("no location captured", map.containsKey("location"))
        assertTrue(map["startedAt"] is Timestamp)
        assertEquals("open", map["status"])
    }

    @Test
    fun sessionMapIncludesClosedAtAndLocationWhenPresent() {
        val session = session().copy(
            closedAt = 1_700_000_900_000L,
            locationLat = 19.4326,
            locationLng = -99.1332,
            locationAccuracy = 8.5,
            locationDistanceMeters = 12.0,
            locationVerified = true,
        )

        val map = session.toFirestoreMap()

        assertTrue(map["closedAt"] is Timestamp)
        val location = map["location"] as Map<*, *>
        assertEquals(19.4326, location["lat"])
        assertEquals(true, location["verified"])
    }

    @Test
    fun sessionMapCarriesTheCanonicalSubjectId() {
        val map = session().toFirestoreMap()

        assertEquals("g3-matematicas-iii", map["subjectId"])
        assertEquals("Matemáticas", map["subject"])
    }

    @Test
    fun recordMapSendsNumbersAsLongAndOnlySetOptionals() {
        val base = record("stu-1", "present").toFirestoreMap()

        assertTrue("Firestore stores integers as Long", base["listNumber"] is Long)
        assertTrue(base["updatedAt"] is Timestamp)
        assertFalse(base.containsKey("note"))

        val annotated = record("stu-1", "present")
            .copy(time = "08:12", note = "llegó con recado")
            .toFirestoreMap()
        assertEquals("08:12", annotated["time"])
        assertEquals("llegó con recado", annotated["note"])
    }

    private fun session(
        present: Int = 0,
        absent: Int = 0,
        late: Int = 0,
        justified: Int = 0,
    ) = AttendanceSessionEntity(
        sessionId = "ses-1",
        groupId = "grp-3b",
        schoolId = "sch-benito-juarez",
        teacherId = "uid-docente",
        subject = "Matemáticas",
        subjectId = "g3-matematicas-iii",
        date = "2026-11-03",
        startedAt = 1_700_000_000_000L,
        status = "open",
        presentCount = present,
        absentCount = absent,
        lateCount = late,
        justifiedCount = justified,
        syncState = SyncState.PENDING,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L,
    )

    private fun record(studentId: String, status: String) = AttendanceRecordEntity(
        sessionId = "ses-1",
        studentId = studentId,
        listNumber = 1,
        status = status,
        updatedAt = 1_700_000_000_000L,
        updatedBy = "uid-docente",
        syncState = SyncState.PENDING,
    )
}
