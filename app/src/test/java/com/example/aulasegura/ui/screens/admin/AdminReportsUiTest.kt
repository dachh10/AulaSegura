package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroupBreakdown
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.model.AdminTopAbsenceDay
import com.example.aulasegura.domain.model.attendancePercent
import com.example.aulasegura.domain.repository.AdminReportError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure helpers only: no Firebase, no ViewModel, no Compose. */
class AdminReportsUiTest {

    private val knownSummary = AdminReportRangeSummary(
        sessionCount = 4,
        presentCount = 100,
        absentCount = 10,
        lateCount = 5,
        justifiedCount = 3,
    )

    @Test
    fun `zero sessions produce a guarded zero percent and an explicit empty state`() {
        val empty = AdminReportRangeSummary()

        assertEquals(0, attendancePercent(0, 0))
        assertEquals(0, attendancePercent(1, 0))
        assertEquals("0%", formatAttendancePercent(0, 0))
        assertEquals("0%", formatAttendancePercent(0, empty.totalMarks))
        assertEquals(0, empty.attendancePercentage)
        assertTrue(empty.empty)
        assertEquals("85%", formatAttendancePercent(knownSummary.presentCount, knownSummary.totalMarks))
    }

    @Test
    fun `range label renders real ISO dates and keeps unreadable input`() {
        assertEquals("1 nov 2026 — 17 nov 2026", rangeLabel("2026-11-01", "2026-11-17"))
        assertEquals("17 nov 2026", rangeLabel("2026-11-17", "2026-11-17"))
        assertEquals("3 ene 2026", formatIsoDate("2026-01-03"))
        assertEquals("no-date", rangeLabel("no-date", "no-date"))
    }

    @Test
    fun `top days and groups are sorted by absences with deterministic ties`() {
        val days = listOf(
            AdminTopAbsenceDay(date = "2026-11-05", absentCount = 2),
            AdminTopAbsenceDay(date = "2026-11-17", absentCount = 7),
            AdminTopAbsenceDay(date = "2026-11-03", absentCount = 7),
        )
        assertEquals(
            listOf("2026-11-03", "2026-11-17", "2026-11-05"),
            sortedTopDays(days).map { it.date },
        )

        val groups = listOf(
            AdminGroupBreakdown(groupId = "a", groupLabel = "1° A", absentCount = 1, presentCount = 20),
            AdminGroupBreakdown(groupId = "b", groupLabel = "3° B", absentCount = 6, presentCount = 15),
            AdminGroupBreakdown(groupId = "c", groupLabel = "2° C", absentCount = 3, presentCount = 18),
        )
        assertEquals(
            listOf("3° B", "2° C", "1° A"),
            sortedGroups(groups).map { it.groupLabel },
        )
    }

    @Test
    fun `chart values come from the known summary and never from a mock constant`() {
        val values = chartValues(knownSummary)

        assertEquals(100, values.present)
        assertEquals(10, values.absent)
        assertEquals(5, values.late)
        assertEquals(3, values.justified)
        assertEquals(118, values.total)

        val rendered = listOf(
            values.present, values.absent, values.late, values.justified,
        ).joinToString(" ")
        assertTrue(!rendered.contains("1420") && !rendered.contains("93"))
    }

    @Test
    fun `pdf content includes the period, the summary and the per-group breakdown`() {
        val state = AdminReportsUiState(
            fromDate = "2026-11-01",
            toDate = "2026-11-17",
            summary = knownSummary,
            groups = listOf(
                AdminGroupBreakdown(
                    groupId = "g-3b",
                    groupLabel = "3° B",
                    sessionCount = 4,
                    presentCount = 60,
                    absentCount = 6,
                ),
            ),
            topDays = listOf(AdminTopAbsenceDay(date = "2026-11-17", sessionCount = 2, absentCount = 7)),
        )

        val content = reportPdfLines(state)

        assertEquals("Reporte de asistencia", content.first())
        assertTrue(content.any { it.contains("1 nov 2026 — 17 nov 2026") })
        assertTrue(content.any { it.contains("Asistencia: 85%") })
        assertTrue(content.any { it.contains("Sesiones: 4") })
        assertTrue(content.any { it.contains("Total de registros: 118") })
        assertTrue(content.any { it.contains("3° B") && it.contains("faltas") })
        assertTrue(content.any { it.contains("17 nov 2026") && it.contains("7 faltas") })
    }

    @Test
    fun `pdf content says so when the range has no sessions`() {
        val content = reportPdfLines(AdminReportsUiState(fromDate = "2026-11-01", toDate = "2026-11-01"))

        assertTrue(content.any { it.contains("1 nov 2026") })
        assertTrue(content.any { it.contains("Sin datos de asistencia") })
    }

    @Test
    fun `month range starts on day one and every error has user-facing copy`() {
        assertEquals("2026-11-01" to "2026-11-17", monthRange("2026-11-17"))
        assertEquals("bad" to "bad", monthRange("bad"))
        AdminReportError.entries.forEach { assertTrue(it.toMessage().isNotBlank()) }
    }
}
