package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import com.example.aulasegura.util.SchoolCalendar
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The calendar is built from the sessions the school produced. These tests pin
 * the two things that could silently lie: which day carries which mark, and how
 * the grid aligns with the real weekday of the month.
 */
class ParentAttendanceUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `marksByDay keeps the most severe mark of a day`() {
        val marks = marksByDay(
            listOf(
                day("2026-11-11", AttendanceMark.PRESENT),
                day("2026-11-11", AttendanceMark.ABSENT),
                day("2026-11-12", AttendanceMark.LATE),
            ),
        )

        assertEquals(AttendanceMark.ABSENT, marks[11])
        assertEquals(AttendanceMark.LATE, marks[12])
        assertNull(marks[13])
    }

    @Test
    fun `worstMark ignores days without a record`() {
        assertEquals(AttendanceMark.PRESENT, worstMark(listOf(null, AttendanceMark.PRESENT)))
        assertEquals(AttendanceMark.JUSTIFIED, worstMark(listOf(AttendanceMark.JUSTIFIED)))
        assertNull(worstMark(listOf(null, null)))
        assertNull(worstMark(emptyList()))
    }

    @Test
    fun `monthWeeks aligns November 2026 to Monday-first rows`() {
        val weeks = monthWeeks(
            year = 2026,
            month = 11,
            marks = mapOf(11 to AttendanceMark.ABSENT),
            todayYear = 2026,
            todayMonth = 11,
            todayDay = 18,
            zone = utc,
        )

        // November 2026 starts on a Sunday, so the first class day is Monday 2.
        assertEquals(listOf(2, 3, 4, 5, 6), weeks[0].map { it?.day })
        assertEquals(listOf(9, 10, 11, 12, 13), weeks[1].map { it?.day })
        assertEquals(listOf(16, 17, 18, 19, 20), weeks[2].map { it?.day })
        assertEquals(listOf(23, 24, 25, 26, 27), weeks[3].map { it?.day })
        // Monday 30 is the only class day of the last row.
        assertEquals(listOf(30, null, null, null, null), weeks[4].map { it?.day })
    }

    @Test
    fun `monthWeeks keeps every class day once and in order`() {
        val weeks = monthWeeks(
            year = 2026,
            month = 11,
            marks = emptyMap(),
            todayYear = 2026,
            todayMonth = 11,
            todayDay = 18,
            zone = utc,
        )

        val days = weeks.flatten().mapNotNull { it?.day }

        assertEquals(days.distinct(), days)
        assertEquals(days.sorted(), days)
        // November 2026 has 21 Monday-to-Friday days.
        assertEquals(21, days.size)
        assertTrue(weeks.all { it.size == 5 })
    }

    @Test
    fun `monthWeeks flags today and past days without inventing marks`() {
        val weeks = monthWeeks(
            year = 2026,
            month = 11,
            marks = mapOf(18 to AttendanceMark.LATE),
            todayYear = 2026,
            todayMonth = 11,
            todayDay = 18,
            zone = utc,
        )
        val cells = weeks.flatten().filterNotNull()

        val today = cells.single { it.isCurrent }
        assertEquals(18, today.day)
        assertEquals(AttendanceMark.LATE, today.mark)
        assertTrue(cells.single { it.day == 17 }.isPast)
        assertTrue(cells.single { it.day == 18 }.isPast.not())
        assertTrue(cells.single { it.day == 19 }.isPast.not())
        assertNull(cells.single { it.day == 19 }.mark)
    }

    @Test
    fun `a later month is not marked as past`() {
        val weeks = monthWeeks(
            year = 2026,
            month = 12,
            marks = emptyMap(),
            todayYear = 2026,
            todayMonth = 11,
            todayDay = 18,
            zone = utc,
        )

        assertTrue(weeks.flatten().filterNotNull().none { it.isPast })
        assertTrue(weeks.flatten().filterNotNull().none { it.isCurrent })
    }

    @Test
    fun `toRecentUi reads the record of one session`() {
        val present = day("2026-11-18", AttendanceMark.PRESENT, time = "07:52").toRecentUi(utc)

        assertEquals("MIÉ", present.weekday)
        assertEquals("18", present.day)
        assertEquals("Matemáticas", present.title)
        assertEquals("Entrada 07:52", present.subtitle)
        assertEquals("Presente", present.pillText)

        val absent = day("2026-11-17", AttendanceMark.ABSENT, note = "Cita médica")
            .toRecentUi(utc)

        assertEquals("Cita médica", absent.subtitle)
        assertEquals("Falta", absent.pillText)

        val missing = day("2026-11-16", null).toRecentUi(utc)

        assertEquals("Sin registro", missing.subtitle)
        assertEquals("Sin registro", missing.pillText)
        assertNull(missing.mark)
    }

    @Test
    fun `toRecentUi falls back to a neutral title when the subject is empty`() {
        val record = ParentDayAttendance(
            date = "2026-11-18",
            subject = "",
            startedAt = 0L,
            mark = AttendanceMark.LATE,
            time = null,
            note = null,
        ).toRecentUi(utc)

        assertEquals("Día completo", record.title)
        assertEquals("Sin hora registrada", record.subtitle)
        assertEquals("Retardo", record.pillText)
    }

    @Test
    fun `monthLabel speaks Spanish and keeps the year`() {
        assertEquals("Noviembre 2026", AttendanceTimes.monthLabel(2026, 11, utc))
        assertEquals("Enero 2027", AttendanceTimes.monthLabel(2027, 1, utc))
    }

    @Test
    fun `classDaysInMonth counts Monday-to-Friday only`() {
        // November 2026: 30 days, 9 weekend days.
        assertEquals(21, SchoolCalendar.classDaysInMonth(2026, 11, utc))
        // February 2027 starts on a Monday: 28 days, 8 weekend days.
        assertEquals(20, SchoolCalendar.classDaysInMonth(2027, 2, utc))
    }

    @Test
    fun `failure messages name the reason`() {
        assertEquals(
            "Sin conexión. Revisa tu red e intenta de nuevo.",
            attendanceFailureMessage(RosterFailure.OFFLINE),
        )
        assertEquals(
            "No tienes permiso para ver la asistencia.",
            attendanceFailureMessage(RosterFailure.DENIED),
        )
        assertEquals(
            "No se pudo cargar la asistencia.",
            attendanceFailureMessage(RosterFailure.UNKNOWN),
        )
    }

    @Test
    fun `subject options exclude unknown legacy labels`() {
        val days = listOf(
            day("2026-11-03", AttendanceMark.PRESENT).copy(
                subjectId = "g3-matematicas-iii",
                subject = "Matemáticas III",
            ),
            day("2026-11-04", AttendanceMark.ABSENT).copy(
                subjectId = "g3-espanol-iii",
                subject = "Español III",
            ),
            day("2026-11-05", AttendanceMark.PRESENT).copy(
                subjectId = "",
                subject = "Materia desconocida",
            ),
        )

        assertEquals(
            listOf(
                ParentSubjectUi("g3-espanol-iii", "Español III"),
                ParentSubjectUi("g3-matematicas-iii", "Matemáticas III"),
            ),
            subjectOptionsOf(days),
        )
    }

    private fun day(
        date: String,
        mark: AttendanceMark?,
        time: String? = null,
        note: String? = null,
    ) = ParentDayAttendance(
        date = date,
        subject = "Matemáticas",
        startedAt = 0L,
        mark = mark,
        time = time,
        note = note,
    )
}
