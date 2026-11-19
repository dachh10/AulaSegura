package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentIncidentLocation
import com.example.aulasegura.domain.model.ParentIncidentMedia
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How incident data becomes the strings on the family's screens.
 *
 * The two rules pinned here: the place is described only in terms the document
 * supports (verified / not / no coordinate), and the counters are the child's
 * own, not the family's.
 */
class ParentIncidentsUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `severity maps to the three wire values plus a neutral fallback`() {
        assertEquals(ParentIncidentSeverity.Leve, incidentSeverityOf("leve"))
        assertEquals(ParentIncidentSeverity.Moderado, incidentSeverityOf("moderado"))
        assertEquals(ParentIncidentSeverity.Grave, incidentSeverityOf("grave"))
        assertEquals(ParentIncidentSeverity.Other, incidentSeverityOf(""))
        assertEquals("Grave", incidentSeverityOf("grave").label)
    }

    @Test
    fun `the date label carries the day and the clock the school reported`() {
        val label = incidentDateLabel(at(2026, 10, 30, 11, 20), utc)

        assertTrue(label, label.contains("30 oct"))
        assertTrue(label, label.endsWith("· 11:20"))
        assertEquals("Sin fecha", incidentDateLabel(0L, utc))
    }

    @Test
    fun `the long date is the one the detail and the map print`() {
        val label = incidentLongDateLabel(at(2026, 10, 21, 13, 5), utc)

        assertTrue(label, label.contains("de octubre"))
        assertTrue(label, label.endsWith("· 13:05"))
        assertEquals("Sin fecha registrada", incidentLongDateLabel(0L, utc))
    }

    @Test
    fun `the place says verified, unverified or missing, never a made-up name`() {
        assertEquals("Sin ubicación", incidentLocationLabel(null))
        assertEquals(
            "Ubicación verificada",
            incidentLocationLabel(location(verified = true)),
        )
        assertEquals("Sin verificar", incidentLocationLabel(location(verified = false)))
        assertEquals("Sin ubicación", incidentCoordinatesLabel(null))
        assertEquals("19.292600, -99.655700", incidentCoordinatesLabel(location()))
    }

    @Test
    fun `precision and distance are printed only when measured`() {
        assertEquals(
            "± 12 m · a 8 m del plantel",
            incidentLocationDetail(location(accuracy = 12.4, distance = 8.2)),
        )
        assertEquals("± 5 m", incidentLocationDetail(location(accuracy = 5.0, distance = null)))
        assertNull(incidentLocationDetail(location(accuracy = null, distance = null)))
    }

    @Test
    fun `video duration is m ss and zero is not printed`() {
        assertEquals("0:24", mediaDurationLabel(24.0))
        assertEquals("1:05", mediaDurationLabel(65.0))
        assertNull(mediaDurationLabel(null))
        assertNull(mediaDurationLabel(0.0))
    }

    @Test
    fun `a video row uses its thumbnail and keeps the duration chip`() {
        val row = incident(
            media = listOf(
                ParentIncidentMedia(
                    mediaId = "m1",
                    type = "video",
                    url = "https://cdn/v.mp4",
                    thumbnailUrl = "https://cdn/v.jpg",
                    durationSec = 24.0,
                ),
            ),
        ).toRowUi(utc)

        assertEquals("https://cdn/v.jpg", row.thumbnailUrl)
        assertTrue(row.isVideo)
        assertEquals("0:24", row.videoDuration)
    }

    @Test
    fun `an image row falls back to its own URL for the thumbnail`() {
        val row = incident(
            media = listOf(
                ParentIncidentMedia("m1", "image", "https://cdn/foto.jpg", null, null),
            ),
        ).toRowUi(utc)

        assertEquals("https://cdn/foto.jpg", row.thumbnailUrl)
        assertNull(row.videoDuration)
        assertEquals(false, row.isVideo)
    }

    @Test
    fun `counters belong to one child`() {
        val incidents = listOf(
            incident(id = "a", studentId = "std-1", seen = false),
            incident(id = "b", studentId = "std-1", seen = true, severity = "grave"),
            incident(id = "c", studentId = "std-2", seen = false, severity = "grave"),
        )
        val mine = incidentsOf(incidents, "std-1")

        assertEquals(listOf("a", "b"), mine.map { it.incidentId })
        assertEquals(1, newIncidents(mine))
        assertEquals(1, graveIncidents(mine))
        assertEquals(listOf("c"), incidentsOf(incidents, "std-2").map { it.incidentId })
    }

    @Test
    fun `the detail carries the real coordinate and the cloud read mark`() {
        val ui = incident(
            id = "abc12345-rest",
            seen = true,
            media = listOf(ParentIncidentMedia("m1", "image", "https://cdn/foto.jpg", null, null)),
        ).toDetailUi(marking = false, error = null, zone = utc)

        assertEquals(19.2926, ui.latitude!!, 0.000001)
        assertEquals(-99.6557, ui.longitude!!, 0.000001)
        assertTrue(ui.seen)
        assertEquals("Evidencia 1", ui.media.single().label)
        assertEquals("#INC-ABC12345", incidentFolio(ui.incidentId))
    }

    private fun incident(
        id: String = "inc-1",
        studentId: String = "std-1",
        seen: Boolean = false,
        severity: String = "leve",
        media: List<ParentIncidentMedia> = emptyList(),
    ) = ParentIncident(
        incidentId = id,
        studentId = studentId,
        studentName = "Mariana Gómez Ruiz",
        groupId = "grp-3b",
        title = "Caída en el recreo",
        description = "Se cayó en el patio.",
        severity = severity,
        category = "caida",
        reportedByName = "Prof. Ricardo Salinas",
        occurredAtMillis = at(2026, 10, 30, 11, 20),
        status = "nuevo",
        location = location(),
        media = media,
        seenByMe = seen,
    )

    private fun location(
        accuracy: Double? = 5.0,
        distance: Double? = 12.4,
        verified: Boolean = true,
    ) = ParentIncidentLocation(
        latitude = 19.2926,
        longitude = -99.6557,
        accuracyMeters = accuracy,
        distanceMeters = distance,
        verified = verified,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis
}
