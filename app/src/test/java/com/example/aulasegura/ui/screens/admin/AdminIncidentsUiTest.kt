package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminIncident
import com.example.aulasegura.domain.model.AdminIncidentLocation
import com.example.aulasegura.domain.model.AdminIncidentMedia
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.repository.AdminIncidentError
import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure helpers only: no Firebase, no ViewModel, no Compose. */
class AdminIncidentsUiTest {

    private val knownIncident = AdminIncident(
        id = "inc-1",
        studentName = "Mariana Gómez",
        title = "Discusión con compañero",
        description = "Se registró una discusión en el recreo.",
        severity = IncidentSeverity.MODERADO,
        category = IncidentCategory.DISCUSION,
        reportedByName = "Prof. Salinas",
        occurredAt = millisOf(2026, Calendar.OCTOBER, 21, 13, 5),
        location = AdminIncidentLocation(
            latitude = -34.60372,
            longitude = -58.38159,
            verified = true,
        ),
        media = listOf(
            AdminIncidentMedia(
                id = "m1",
                type = "image",
                url = "https://res.cloudinary.com/demo/image/upload/evidencia-1.jpg",
            ),
            AdminIncidentMedia(
                id = "m2",
                type = "video",
                url = "https://res.cloudinary.com/demo/video/upload/evidencia-2.mp4",
            ),
        ),
        status = IncidentStatus.NUEVO,
        createdAt = millisOf(2026, Calendar.OCTOBER, 21, 13, 10),
    )

    private fun millisOf(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = Calendar.getInstance(Locale.getDefault()).apply {
        clear()
        set(year, month, day, hour, minute, 0)
    }.timeInMillis

    @Test
    fun `severity filter keeps only the chosen severity and null keeps every incident`() {
        val list = listOf(
            knownIncident.copy(id = "a", severity = IncidentSeverity.LEVE),
            knownIncident.copy(id = "b", severity = IncidentSeverity.MODERADO),
            knownIncident.copy(id = "c", severity = IncidentSeverity.GRAVE),
        )

        assertEquals(
            listOf("a"),
            filterBySeverity(list, IncidentSeverity.LEVE).map { it.id },
        )
        assertEquals(
            listOf("c"),
            filterBySeverity(list, IncidentSeverity.GRAVE).map { it.id },
        )
        assertEquals(list, filterBySeverity(list, null))
    }

    @Test
    fun `status filter keeps only the chosen status and null keeps every incident`() {
        val list = listOf(
            knownIncident.copy(id = "a", status = IncidentStatus.NUEVO),
            knownIncident.copy(id = "b", status = IncidentStatus.REVISADO),
            knownIncident.copy(id = "c", status = IncidentStatus.CERRADO),
        )

        assertEquals(
            listOf("a"),
            filterByStatus(list, IncidentStatus.NUEVO).map { it.id },
        )
        assertEquals(
            listOf("b"),
            filterByStatus(list, IncidentStatus.REVISADO).map { it.id },
        )
        assertEquals(
            listOf("c"),
            filterByStatus(list, IncidentStatus.CERRADO).map { it.id },
        )
        assertEquals(list, filterByStatus(list, null))
    }

    @Test
    fun `unknown severity and status wire values fall back to safe defaults`() {
        assertEquals(IncidentSeverity.LEVE, AdminIncident.severityFromWire(null))
        assertEquals(IncidentSeverity.LEVE, AdminIncident.severityFromWire("desconocido"))
        assertEquals(IncidentSeverity.GRAVE, AdminIncident.severityFromWire("grave"))
        assertEquals(IncidentStatus.NUEVO, AdminIncident.statusFromWire(null))
        assertEquals(IncidentStatus.NUEVO, AdminIncident.statusFromWire("archivado"))
        assertEquals(IncidentStatus.CERRADO, AdminIncident.statusFromWire("cerrado"))
        assertEquals(IncidentCategory.OTRO, AdminIncident.categoryFromWire("otro"))

        val defaulted = AdminIncident(severity = AdminIncident.severityFromWire("?"))
        assertEquals(IncidentSeverity.LEVE, defaulted.severity)
        assertEquals(IncidentStatus.NUEVO, defaulted.status)
    }

    @Test
    fun `date label renders epoch millis and keeps a missing date explicit`() {
        assertEquals("21 oct 2026 · 13:05", formatIncidentDate(knownIncident.occurredAt))
        assertEquals("—", formatIncidentDate(0L))
        assertEquals("—", formatIncidentDate(-1L))
    }

    @Test
    fun `detail rows come from the incident and carry every evidence URL`() {
        val rows = incidentDetailRows(knownIncident)
        val values = rows.map { it.value }

        assertTrue(values.contains("Mariana Gómez"))
        assertTrue(values.contains("Prof. Salinas"))
        assertTrue(values.contains("Se registró una discusión en el recreo."))
        assertTrue(values.contains("Discusión"))
        assertTrue(values.contains("21 oct 2026 · 13:05"))
        assertTrue(values.contains("-34.60372, -58.38159 · verificada"))
        assertTrue(values.contains("https://res.cloudinary.com/demo/image/upload/evidencia-1.jpg"))
        assertTrue(values.contains("https://res.cloudinary.com/demo/video/upload/evidencia-2.mp4"))

        val mediaRows = rows.filter { it.isMedia }
        assertEquals(2, mediaRows.size)
        assertEquals(
            listOf(
                "https://res.cloudinary.com/demo/image/upload/evidencia-1.jpg",
                "https://res.cloudinary.com/demo/video/upload/evidencia-2.mp4",
            ),
            mediaRows.map { it.mediaUrl },
        )
        assertEquals("Evidencia 1 · foto", mediaRows[0].label)
        assertEquals("Evidencia 2 · video", mediaRows[1].label)

        assertNull(rows.first { it.label == "Estudiante" }.mediaUrl)
    }

    @Test
    fun `sort is newest first with an id tie break and stats come from the real list`() {
        val list = listOf(
            knownIncident.copy(id = "old", createdAt = 100L),
            knownIncident.copy(id = "b", createdAt = 200L, status = IncidentStatus.CERRADO),
            knownIncident.copy(
                id = "a",
                createdAt = 200L,
                severity = IncidentSeverity.GRAVE,
                status = IncidentStatus.REVISADO,
            ),
        )

        assertEquals(listOf("a", "b", "old"), sortedIncidents(list).map { it.id })
        val stats = incidentStats(list)
        assertEquals(3, stats.total)
        assertEquals(1, stats.news)
        assertEquals(1, stats.grave)
    }

    @Test
    fun `status label and photo count describe the transition and the real evidence`() {
        assertEquals("Nuevo → Revisado", statusChangeLabel(IncidentStatus.NUEVO, IncidentStatus.REVISADO))
        assertEquals("Revisado → Cerrado", statusChangeLabel(IncidentStatus.REVISADO, IncidentStatus.CERRADO))
        assertEquals(1, knownIncident.photoCount)
        assertEquals(2, knownIncident.media.size)
        assertEquals("Moderado · Discusión con compañero", knownIncident.displayLabel)
        assertEquals(2, knownIncident.severityRank)
        AdminIncidentError.entries.forEach { assertTrue(it.toMessage().isNotBlank()) }
    }
}
