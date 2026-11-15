package com.example.aulasegura.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The `date` column is compared lexicographically by Room, so the ISO shape and
 * the numbering system are part of the contract, not cosmetics.
 */
class AttendanceTimesTest {

    private val utc = TimeZone.getTimeZone("UTC")

    /** 2026-11-14 15:04 UTC — a Saturday, one day after the demo session. */
    private val saturdayAfternoon: Long = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        .apply { timeZone = utc }
        .parse("2026-11-14 15:04")!!
        .time

    @Test
    fun `the stored date is the sortable ISO shape`() {
        assertEquals("2026-11-14", AttendanceTimes.isoDate(saturdayAfternoon, utc))
    }

    @Test
    fun `a record keeps the clock time of the arrival`() {
        assertEquals("15:04", AttendanceTimes.clockTime(saturdayAfternoon, utc))
    }

    @Test
    fun `the header prints the Spanish day label`() {
        assertEquals("Sáb 14 nov", AttendanceTimes.dayLabel(saturdayAfternoon, utc))
    }
}
