package com.example.aulasegura.data.repository

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mapping `incidents/{id}` into the family's view.
 *
 * The regression this pins: the tutor's "Nuevo" badge is the tutor's own presence
 * in `seenByTutorIds`, never the incident's `status`; and a coordinate that is not
 * complete is dropped instead of pinned to the school.
 */
class ParentIncidentMappingTest {

    @Test
    fun `an incident keeps the fields the detail and the map print`() {
        val incident = parentIncidentFrom(
            id = "inc-1",
            data = incidentData(),
            uid = "uid-tutor",
        )

        val found = requireNotNull(incident)
        assertEquals("inc-1", found.incidentId)
        assertEquals("std-1", found.studentId)
        assertEquals("Mariana Gómez Ruiz", found.studentName)
        assertEquals("Caída en el recreo", found.title)
        assertEquals("leve", found.severity)
        assertEquals("caida", found.category)
        assertEquals("Prof. Ricardo Salinas", found.reportedByName)
        assertEquals("nuevo", found.status)
        assertEquals(19.2926, found.location!!.latitude, 0.000001)
        assertEquals(-99.6557, found.location!!.longitude, 0.000001)
        assertTrue(found.location!!.verified)
    }

    @Test
    fun `the read mark belongs to the signed-in tutor`() {
        val seen = parentIncidentFrom("inc-1", incidentData(), uid = "uid-tutor")
        val otherTutor = parentIncidentFrom("inc-1", incidentData(), uid = "uid-other")

        assertTrue(seen?.seenByMe == true)
        assertFalse(otherTutor?.seenByMe == true)
    }

    @Test
    fun `a location without both coordinates is dropped`() {
        assertNull(parentIncidentLocationFrom(mapOf("lat" to 19.29)))
        assertNull(parentIncidentLocationFrom(mapOf("lng" to -99.65)))
        assertNull(parentIncidentLocationFrom(null))
        assertNull(parentIncidentLocationFrom("no es un mapa"))
    }

    @Test
    fun `an incomplete measurement stays null instead of becoming zero`() {
        val location = parentIncidentLocationFrom(mapOf("lat" to 19.29, "lng" to -99.65))

        assertNull(location?.accuracyMeters)
        assertNull(location?.distanceMeters)
        assertFalse(location?.verified == true)
    }

    @Test
    fun `media without a URL is dropped and the rest keeps its order`() {
        val media = parentIncidentMediaFrom(
            listOf(
                mapOf("id" to "m1", "type" to "image", "url" to ""),
                mapOf("id" to "m2", "type" to "video", "url" to "https://cdn/v.mp4", "durationSec" to 24.0),
            ),
        )

        assertEquals(1, media.size)
        assertEquals("m2", media.first().mediaId)
        assertTrue(media.first().isVideo)
        assertEquals(24.0, media.first().durationSec!!, 0.0001)
    }

    @Test
    fun `an incident without a title is not rendered as an empty card`() {
        assertNull(parentIncidentFrom("inc-2", emptyMap(), "uid-tutor"))
        assertNull(parentIncidentFrom("inc-3", mapOf("title" to "   "), "uid-tutor"))
    }

    private fun incidentData(): Map<String, Any?> = mapOf(
        "schoolId" to "sch-benito-juarez",
        "groupId" to "grp-3b",
        "studentId" to "std-1",
        "studentName" to "Mariana Gómez Ruiz",
        "title" to "Caída en el recreo",
        "description" to "Se cayó en el patio.",
        "severity" to "leve",
        "category" to "caida",
        "reportedBy" to "uid-docente",
        "reportedByName" to "Prof. Ricardo Salinas",
        "occurredAt" to timestamp(2026, 10, 30, 11, 20),
        "status" to "nuevo",
        "seenByTutorIds" to listOf("uid-tutor"),
        "location" to mapOf(
            "lat" to 19.2926,
            "lng" to -99.6557,
            "accuracy" to 5.0,
            "distanceMeters" to 12.4,
            "verified" to true,
        ),
        "media" to emptyList<Map<String, Any?>>(),
    )

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Timestamp {
        val calendar = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }
        return Timestamp(Date(calendar.timeInMillis))
    }
}
