package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentEvidence
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.MediaUploadPhase
import com.example.aulasegura.domain.model.SyncPhase
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The incident segment of the history is a pure function of the cached rows, so
 * it is checked here: the screen only paints what this returns.
 *
 * The buckets and the labels are shared with the roll call history on purpose —
 * a teacher reading "HOY" in one segment expects the same word in the other.
 */
class IncidentHistoryUiTest {

    private val zone = TimeZone.getTimeZone("America/Mexico_City")

    @Test
    fun `the incidents land in the same buckets as the roll calls`() {
        val today = instant(2026, Calendar.NOVEMBER, 16, 9, 0)
        val yesterday = instant(2026, Calendar.NOVEMBER, 15, 18, 30)
        val lastMonth = instant(2026, Calendar.OCTOBER, 2, 8, 15)

        val sections = incidentSections(
            incidents = listOf(
                incident(id = "a", occurredAt = today),
                incident(id = "b", occurredAt = yesterday),
                incident(id = "c", occurredAt = lastMonth),
            ),
            todayIso = "2026-11-16",
            zone = zone,
        )

        assertEquals(listOf("HOY", "ESTA SEMANA", "ANTERIORES"), sections.map { it.label })
        assertEquals("a", sections[0].incidents.single().id)
        assertEquals("b", sections[1].incidents.single().id)
        assertEquals("c", sections[2].incidents.single().id)
    }

    @Test
    fun `the date block prints the day of the incident`() {
        val sections = incidentSections(
            incidents = listOf(incident(occurredAt = instant(2026, Calendar.NOVEMBER, 16, 9, 0))),
            todayIso = "2026-11-16",
            zone = zone,
        )

        val row = sections.single().incidents.single()
        assertEquals("16", row.day)
        assertEquals("LUN", row.weekday)
        assertEquals("Lun 16 nov 2026 · 09:00", row.occurredLabel)
    }

    @Test
    fun `only the expanded row receives the files`() {
        val evidence = listOf(
            IncidentEvidence(
                mediaId = "m-1",
                url = "https://res.cloudinary.com/demo/photo.jpg",
                thumbnailUrl = null,
                format = "jpg",
                bytes = 204_800L,
                phase = MediaUploadPhase.UPLOADED,
                localPath = null,
            ),
        )
        val incidents = listOf(incident(id = "a"), incident(id = "b"))

        val collapsed = incidentSections(incidents, todayIso = "2026-11-16", zone = zone)
        assertTrue(collapsed.flatMap { it.incidents }.all { it.evidence.isEmpty() })

        val open = incidentSections(
            incidents = incidents,
            todayIso = "2026-11-16",
            expandedId = "b",
            evidence = evidence,
            zone = zone,
        ).flatMap { it.incidents }

        assertEquals(1, open.single { it.id == "b" }.evidence.size)
        assertEquals("JPG · 200 KB", open.single { it.id == "b" }.evidence.single().label)
    }

    @Test
    fun `a severity this build does not know hides the chip instead of printing it`() {
        val row = incidentSections(
            incidents = listOf(incident(severity = null)),
            todayIso = "2026-11-16",
            zone = zone,
        ).single().incidents.single()

        assertNull("an unknown wire value must not reach the screen", row.severity)
        assertEquals("Nuevo", row.status)
        assertEquals("Caída", row.category)
    }

    @Test
    fun `the location line says whether the pin fell inside the school`() {
        assertEquals("Dentro de la escuela · 12 m", incidentLocationLabel(true, 12.4))
        assertEquals("Fuera del radio · 40 m", incidentLocationLabel(false, 39.6))
        assertEquals("Dentro de la escuela", incidentLocationLabel(true, null))
        assertNull("no pin, nothing to say", incidentLocationLabel(null, null))
    }

    @Test
    fun `an incident without a title falls back to its description`() {
        val row = incidentSections(
            incidents = listOf(incident(title = "", description = "Se cayó en el patio")),
            todayIso = "2026-11-16",
            zone = zone,
        ).single().incidents.single()

        assertEquals("Se cayó en el patio", row.title)
    }

    @Test
    fun `the file size reads in the unit a teacher understands`() {
        assertEquals("900 B", evidenceSizeLabel(900L))
        assertEquals("2 KB", evidenceSizeLabel(2_048L))
        assertEquals("1.5 MB", evidenceSizeLabel(1_572_864L))
        assertEquals("tamaño desconocido", evidenceSizeLabel(0L))
    }

    private fun incident(
        id: String = "inc-1",
        occurredAt: Long = instant(2026, Calendar.NOVEMBER, 16, 9, 0),
        title: String = "Se cayó en el patio",
        description: String = "Se cayó en el patio y se raspó la rodilla.",
        severity: IncidentSeverity? = IncidentSeverity.GRAVE,
    ) = IncidentSummary(
        incidentId = id,
        studentName = "Mariana Gómez Ruiz",
        title = title,
        description = description,
        severity = severity,
        category = IncidentCategory.CAIDA,
        status = IncidentStatus.NUEVO,
        occurredAt = occurredAt,
        locationVerified = true,
        locationDistanceMeters = 12.0,
        sync = SyncPhase.SYNCED,
        mediaCount = 1,
    )

    private fun instant(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(zone, Locale.US).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
