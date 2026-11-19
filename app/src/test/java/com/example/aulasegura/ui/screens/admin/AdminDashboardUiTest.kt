package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminDashboard
import com.example.aulasegura.domain.model.AdminDashboardStatus
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.repository.AdminDashboardError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dashboard's formatting, summary line and card mapping are pure functions.
 * These tests pin the block's invariant: the screen renders the numbers it was
 * handed and never a design-time constant (85 students / 4 teachers / 3 groups).
 */
class AdminDashboardUiTest {

    @Test
    fun `formatCount renders plain decimal counts`() {
        assertEquals("0", formatCount(0))
        assertEquals("7", formatCount(7))
        assertEquals("111", formatCount(111))
    }

    @Test
    fun `summaryLabel uses the real counts for every collection`() {
        val dashboard = AdminDashboard.loaded(
            studentCount = 111,
            teacherCount = 7,
            groupCount = 9,
            incidentCount = 2,
        )
        assertEquals("111 alumnos · 7 docentes · 9 grupos · 2 incidentes", summaryLabel(dashboard))
    }

    @Test
    fun `dashboardCards map each real count to its own card`() {
        val dashboard = AdminDashboard.loaded(
            studentCount = 111,
            teacherCount = 7,
            groupCount = 9,
            incidentCount = 2,
        )
        val cards = dashboardCards(dashboard)

        assertEquals(listOf("111", "7", "9", "2"), cards.map { it.value })
        assertEquals(
            listOf("Alumnos", "Docentes", "Grupos", "Incidentes nuevos"),
            cards.map { it.label },
        )
        assertEquals(
            listOf(
                DashboardCardKey.STUDENTS,
                DashboardCardKey.TEACHERS,
                DashboardCardKey.GROUPS,
                DashboardCardKey.INCIDENTS,
            ),
            cards.map { it.key },
        )
    }

    @Test
    fun `no design-time mock constant leaks into produced labels`() {
        // Counts differ from all three mock values, so any appearance of a mock
        // constant in the output proves a helper hardcoded it.
        val dashboard = AdminDashboard.loaded(
            studentCount = 111,
            teacherCount = 7,
            groupCount = 9,
            incidentCount = 2,
        )
        val produced = buildList {
            add(summaryLabel(dashboard))
            dashboardCards(dashboard).forEach { add(it.value); add(it.label) }
            loadingCards().forEach { add(it.value); add(it.label) }
        }.joinToString(" ")

        listOf("85", "4", "3").forEach { mock ->
            assertFalse("Mock constant $mock leaked into dashboard labels", produced.contains(mock))
        }
    }

    @Test
    fun `load status distinguishes not loaded, empty and loaded`() {
        assertEquals(AdminDashboardStatus.NOT_LOADED, AdminDashboard().status)
        assertTrue(AdminDashboard.loaded(0, 0, 0, 0).empty)
        assertFalse(AdminDashboard.loaded(1, 0, 0, 0).empty)
        assertFalse(AdminDashboard().empty)
    }

    @Test
    fun `today attendance copy prints the count and never the model's toString`() {
        // Regression: `"$summary.sessionCount"` interpolates the whole object and
        // appends ".sessionCount" literally, so the card printed the data class.
        val one = AdminReportRangeSummary(
            sessionCount = 1,
            presentCount = 0,
            absentCount = 2,
            lateCount = 0,
            justifiedCount = 0,
        )

        val message = todayAttendanceMessage(summary = one, loading = false, error = null)

        assertEquals("1 sesión · 0% de asistencia", message)
        assertFalse(message.contains("AdminReportRangeSummary"))
        assertEquals(
            "2 sesiones · 0% de asistencia",
            todayAttendanceMessage(summary = one.copy(sessionCount = 2), loading = false, error = null),
        )
        assertEquals(
            "Aún no hay sesiones registradas hoy.",
            todayAttendanceMessage(
                summary = one.copy(sessionCount = 0),
                loading = false,
                error = null,
            ),
        )
    }

    @Test
    fun `loading cards show no number and every typed error has copy`() {
        assertEquals(4, loadingCards().size)
        assertTrue(loadingCards().all { it.value == "…" })

        AdminDashboardError.entries.forEach { error ->
            assertTrue(error.toMessage().isNotBlank())
        }
    }
}
