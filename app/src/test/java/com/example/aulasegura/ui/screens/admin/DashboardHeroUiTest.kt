package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminReportRangeSummary
import org.junit.Assert.assertEquals
import org.junit.Test

/** The hero ring and legend are driven by the real day summary, never mockups. */
class DashboardHeroUiTest {

    @Test
    fun `the ring offset shrinks as attendance grows`() {
        val circumference = 2 * Math.PI * 36.0

        val high = ringOffset(93)
        val low = ringOffset(25)

        assertEquals(circumference - circumference * 0.93, high, 0.5)
        assertEquals(circumference - circumference * 0.25, low, 0.5)
        assert(low > high)
    }

    @Test
    fun `the ring is fully empty when there are no marks`() {
        assertEquals(2 * Math.PI * 36.0, ringOffset(0), 0.5)
        assertEquals(0.0, ringOffset(100), 0.5)
    }

    @Test
    fun `the legend shows the real counts`() {
        val summary = AdminReportRangeSummary(
            sessionCount = 2,
            presentCount = 79,
            absentCount = 4,
            lateCount = 2,
        )

        val legend = attendanceLegend(summary)

        assertEquals(listOf("79 presentes", "4 faltas", "2 retardos"), legend)
        assertEquals(93, summary.attendancePercentage)
    }
}
