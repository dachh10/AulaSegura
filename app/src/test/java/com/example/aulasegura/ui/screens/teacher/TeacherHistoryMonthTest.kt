package com.example.aulasegura.ui.screens.teacher

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The history header used to say "Noviembre 2026" whatever the month was. These
 * are the pieces that decide what the header prints and which rows belong to it;
 * a month filter that is off by one hides a teacher's whole week.
 */
class TeacherHistoryMonthTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val november: Long = at(2026, Calendar.NOVEMBER, 17, 9, 0)

    @Test
    fun `the label is the month of the date, in spanish and capitalised`() {
        assertEquals("Noviembre 2026", monthLabel(november, 0, utc))
        assertEquals("Octubre 2026", monthLabel(november, -1, utc))
        assertEquals("Diciembre 2025", monthLabel(november, -11, utc))
    }

    @Test
    fun `the offset crosses the year correctly`() {
        val january = at(2027, Calendar.JANUARY, 5, 8, 0)

        assertEquals("Diciembre 2026", monthLabel(january, -1, utc))
        assertEquals("Enero 2027", monthLabel(january, 0, utc))
    }

    @Test
    fun `every day of a month shares its key`() {
        val first = monthKey(at(2026, Calendar.NOVEMBER, 1, 0, 0), utc)
        val last = monthKey(at(2026, Calendar.NOVEMBER, 30, 23, 59), utc)
        val december = monthKey(at(2026, Calendar.DECEMBER, 1, 0, 0), utc)

        assertEquals(first, last)
        assertTrue(december > last)
        assertEquals(first, monthKeyOfIso("2026-11-17", utc))
    }

    @Test
    fun `the previous month is the one the arrow asks for`() {
        val target = monthKeyBefore(november, -1, utc)

        assertEquals(monthKeyOfIso("2026-10-31", utc), target)
        assertFalse("una lista de noviembre no es de octubre", monthKeyOfIso("2026-11-01", utc) == target)
    }

    @Test
    fun `an unreadable date belongs to no month instead of crashing`() {
        assertNull(monthKeyOfIso(null, utc))
        assertNull(monthKeyOfIso("17/11/2026", utc))
    }

    @Test
    fun `the header never goes past the current month`() {
        assertEquals(0, nextMonthOffset(0))
        assertEquals(0, nextMonthOffset(-1))
        assertEquals(-3, nextMonthOffset(-4))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
