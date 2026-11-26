package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.model.ParentTaskBrief
import com.example.aulasegura.domain.repository.RosterFailure
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home card renders what the repository returned and never a design-time
 * constant: these tests pin the mappers that stand between the two.
 */
class ParentHomeUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `a child without a classroom is not reported as a permission problem`() {
        assertEquals(
            "Tu hijo aún no tiene grupo asignado.",
            homeFailureMessage(RosterFailure.NO_GROUP),
        )
    }

    @Test
    fun `initialsOf takes the first letter of the first two words`() {
        assertEquals("CG", initialsOf("Carlos Gómez"))
        assertEquals("M", initialsOf("Mariana"))
        assertEquals("AM", initialsOf("  ana   maría  lópez "))
        assertEquals("", initialsOf("   "))
    }

    @Test
    fun `headlineFor names the mark and never invents one`() {
        assertEquals("Carlos asistió a clase", headlineFor("Carlos", AttendanceMark.PRESENT))
        assertEquals("Carlos llegó tarde", headlineFor("Carlos", AttendanceMark.LATE))
        assertEquals("Carlos faltó hoy", headlineFor("Carlos", AttendanceMark.ABSENT))
        assertEquals(
            "Carlos tiene falta justificada",
            headlineFor("Carlos", AttendanceMark.JUSTIFIED),
        )
        assertEquals("Sin pase de lista hoy", headlineFor("Carlos", null))
    }

    @Test
    fun `statusFor prints the arrival time only when there is one`() {
        assertEquals("Presente · 07:52", statusFor(AttendanceMark.PRESENT, "07:52"))
        assertEquals("Presente", statusFor(AttendanceMark.PRESENT, null))
        assertEquals("Retardo · 08:21", statusFor(AttendanceMark.LATE, "08:21"))
        assertEquals("Retardo", statusFor(AttendanceMark.LATE, null))
        assertEquals("Falta", statusFor(AttendanceMark.ABSENT, null))
        assertEquals("Justificada", statusFor(AttendanceMark.JUSTIFIED, null))
        assertEquals("Sin registro", statusFor(null, null))
    }

    @Test
    fun `percent counts late arrivals as attendance and is null with no records`() {
        val counts = ParentAttendanceCounts(present = 18, late = 1, absent = 1, justified = 0)

        assertEquals(20, counts.total)
        assertEquals(19, counts.attended)
        assertEquals(95, counts.percent)

        assertEquals(0, ParentAttendanceCounts().total)
        assertNull(ParentAttendanceCounts().percent)
    }

    @Test
    fun `a justified absence stays out of the attendance percentage`() {
        val counts = ParentAttendanceCounts(present = 9, late = 0, absent = 0, justified = 1)

        assertEquals(10, counts.total)
        assertEquals(90, counts.percent)
    }

    @Test
    fun `toChildUi maps the loaded overview`() {
        val ui = overview(
            today = ParentDayAttendance(
                date = "2026-11-18",
                subject = "Matemáticas",
                startedAt = 0L,
                mark = AttendanceMark.PRESENT,
                time = "07:52",
                note = null,
            ),
        ).toChildUi()

        assertEquals("Carlos", ui.name)
        assertEquals("4° D", ui.grade)
        assertEquals("CG", ui.initials)
        assertEquals(95, ui.percent)
        assertEquals(AttendanceMark.PRESENT, ui.mark)
        assertEquals("Carlos asistió a clase", ui.headline)
        assertEquals("Presente · 07:52", ui.status)
    }

    @Test
    fun `toChildUi says there was no roll call instead of guessing`() {
        val ui = overview(today = null).toChildUi()

        assertNull(ui.mark)
        assertEquals("Sin pase de lista hoy", ui.headline)
        assertEquals("Sin registro", ui.status)
    }

    @Test
    fun `task due today and tomorrow is marked as soon`() {
        val now = millisUtc(2026, 11, 18)

        val today = task(due = millisUtc(2026, 11, 18)).toUi(now, utc)
        val tomorrow = task(due = millisUtc(2026, 11, 19)).toUi(now, utc)
        val later = task(due = millisUtc(2026, 11, 23)).toUi(now, utc)

        assertEquals("Hoy", today.dueLabel)
        assertTrue(today.dueSoon)
        assertEquals("Mañana", tomorrow.dueLabel)
        assertTrue(tomorrow.dueSoon)
        assertEquals("Lun 23 nov", later.dueLabel.replace(".", ""))
        assertFalse(later.dueSoon)
    }

    @Test
    fun `todayHeader asks for today in Spanish`() {
        val header = todayHeader(millisUtc(2026, 11, 18), utc).replace(".", "")

        assertEquals("HOY · MIÉ 18 NOV", header)
    }

    @Test
    fun `failure messages name the reason`() {
        assertEquals(
            "Sin conexión. Revisa tu red e intenta de nuevo.",
            homeFailureMessage(RosterFailure.OFFLINE),
        )
        assertEquals(
            "No tienes permiso para ver estos datos.",
            homeFailureMessage(RosterFailure.DENIED),
        )
        assertEquals(
            "No se pudieron cargar los datos.",
            homeFailureMessage(RosterFailure.UNKNOWN),
        )
    }

    private fun overview(today: ParentDayAttendance?) = ParentChildOverview(
        child = ParentChild(
            studentId = "stu-1",
            fullName = "Carlos Gómez",
            firstName = "Carlos",
            listNumber = 7,
            groupId = "grp-4d",
            groupLabel = "4° D",
        ),
        today = today,
        month = ParentMonthAttendance(
            year = 2026,
            month = 11,
            counts = ParentAttendanceCounts(present = 18, late = 1, absent = 1),
            days = emptyList(),
        ),
        tasks = emptyList(),
    )

    private fun task(due: Long) = ParentTaskBrief(
        taskId = "task-1",
        subject = "Matemáticas",
        title = "Fracciones",
        dueDateMillis = due,
    )

    private fun millisUtc(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
        }.timeInMillis
}
