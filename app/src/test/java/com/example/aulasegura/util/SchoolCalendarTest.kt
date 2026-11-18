package com.example.aulasegura.util

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The roll call is blocked on weekends, so "which day is it" decides whether a
 * teacher can work. A wrong answer either locks them out on a Tuesday or records
 * attendance on a Sunday, and neither looks wrong on a screenshot.
 */
class SchoolCalendarTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `monday to friday are class days`() {
        // 2026-11-16 is a Monday.
        (16..20).forEach { day ->
            assertTrue("2026-11-$day", SchoolCalendar.isClassDay(at(day, 8), utc))
        }
    }

    @Test
    fun `saturday and sunday are not`() {
        // 2026-11-14 is a Saturday, 2026-11-15 a Sunday.
        assertFalse("sábado", SchoolCalendar.isClassDay(at(14, 8), utc))
        assertFalse("domingo", SchoolCalendar.isClassDay(at(15, 8), utc))
    }

    @Test
    fun `the answer does not depend on the hour`() {
        assertTrue(SchoolCalendar.isClassDay(at(17, 0), utc))
        assertTrue(SchoolCalendar.isClassDay(at(17, 23), utc))
        assertFalse(SchoolCalendar.isClassDay(at(14, 23), utc))
        assertFalse(SchoolCalendar.isClassDay(at(15, 0), utc))
    }

    private fun at(day: Int, hour: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, 0, 0)
    }.timeInMillis
}
