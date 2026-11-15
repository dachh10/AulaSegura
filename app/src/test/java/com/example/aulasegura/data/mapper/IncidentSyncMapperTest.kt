package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The incident document is what the tutor and the admin read, and the rules only
 * accept it with `reportedBy == uid` and `status == 'nuevo'`. Both are pinned here
 * so a refactor of the entity cannot quietly break every write.
 */
class IncidentSyncMapperTest {

    @Test
    fun `the document carries the denormalised names the tutor needs`() {
        val map = draft().toFirestoreMap()

        assertEquals("uid-docente", map["reportedBy"])
        assertEquals("Diego Hernández", map["reportedByName"])
        assertEquals("Mariana Gómez Ruiz", map["studentName"])
        assertEquals("stu-1", map["studentId"])
        assertEquals("nuevo", map["status"])
        assertEquals("grave", map["severity"])
        assertEquals("caida", map["category"])
    }

    @Test
    fun `the document never leaks local state and omits media until bloque 7`() {
        val map = draft().toFirestoreMap()

        assertFalse("syncState is a Room concept", map.containsKey("syncState"))
        assertFalse(
            "an empty array would say the incident has no evidence",
            map.containsKey("media"),
        )
    }

    @Test
    fun `timestamps go as Timestamps and the location as a nested map`() {
        val map = draft().toFirestoreMap()

        assertTrue(map["occurredAt"] is Timestamp)
        assertTrue(map["createdAt"] is Timestamp)
        assertTrue(map["updatedAt"] is Timestamp)

        val location = map["location"] as Map<*, *>
        assertEquals(19.2926, location["lat"])
        assertEquals(-99.6557, location["lng"])
        assertEquals(true, location["verified"])
        assertFalse("no accuracy was captured", location.containsKey("accuracy"))
    }

    @Test
    fun `a draft without a pin has no location map at all`() {
        val map = draft().copy(locationLat = null, locationLng = null).toFirestoreMap()

        assertFalse(map.containsKey("location"))
        assertEquals(emptyList<String>(), map["seenByTutorIds"])
    }

    private fun draft() = IncidentDraftEntity(
        incidentId = "inc-uuid",
        schoolId = "sch-benito-juarez",
        groupId = "grp-3b",
        studentId = "stu-1",
        studentName = "Mariana Gómez Ruiz",
        title = "Se cayó en el patio",
        description = "Se cayó en el patio y se raspó la rodilla.",
        severity = "grave",
        category = "caida",
        reportedBy = "uid-docente",
        reportedByName = "Diego Hernández",
        occurredAt = 1_763_136_000_000L,
        locationLat = 19.2926,
        locationLng = -99.6557,
        locationDistanceMeters = 12.0,
        locationVerified = true,
        status = "nuevo",
        syncState = SyncState.PENDING,
        createdAt = 1_763_136_000_000L,
        updatedAt = 1_763_136_000_000L,
    )
}
