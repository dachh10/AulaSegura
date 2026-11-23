package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.ClassSlot
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.domain.model.Student
import com.example.aulasegura.domain.model.SyncPhase
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The group detail is arithmetic over cached data, so it is tested without
 * Android: a month grid that quietly moves a day, or an incident leaking from
 * another group, would be a real answer shown to the wrong class.
 */
class TeacherGroupDetailUiTest {

    private val zone: TimeZone = TimeZone.getTimeZone("UTC")

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis

    private fun session(
        id: String,
        isoDate: String,
        present: Int = 27,
        absent: Int = 1,
        late: Int = 0,
        justified: Int = 0,
        sync: RollCallSync = RollCallSync.SYNCED,
        startedAt: Long = date(
            isoDate.substring(0, 4).toInt(),
            isoDate.substring(5, 7).toInt() - 1,
            isoDate.substring(8, 10).toInt(),
        ),
    ) = AttendanceSessionSummary(
        sessionId = id,
        groupId = "3B",
        groupLabel = "3° B",
        subject = "Matemáticas",
        date = isoDate,
        startedAt = startedAt,
        counts = AttendanceCounts(
            present = present,
            absent = absent,
            late = late,
            justified = justified,
        ),
        verified = true,
        sync = sync,
    )

    private fun student(id: String, number: Int, first: String, last: String) = Student(
        id = id,
        firstName = first,
        lastName = last,
        fullName = "$first $last",
        listNumber = number,
        groupId = "3B",
        schoolId = "school-1",
        active = true,
        photoUrl = null,
    )

    private fun incident(
        id: String,
        groupId: String = "3B",
        title: String = "Caída en el recreo",
        description: String = "Se cayó en el patio y se raspó la rodilla.",
        severity: IncidentSeverity? = IncidentSeverity.LEVE,
        occurredAt: Long = date(2026, Calendar.OCTOBER, 30),
    ) = IncidentSummary(
        incidentId = id,
        groupId = groupId,
        studentName = "Mariana Gómez Ruiz",
        title = title,
        description = description,
        severity = severity,
        category = IncidentCategory.CAIDA,
        status = IncidentStatus.NUEVO,
        occurredAt = occurredAt,
        locationVerified = true,
        locationDistanceMeters = 12.4,
        sync = SyncPhase.SYNCED,
        mediaCount = 1,
    )

    @Test
    fun `the calendar paints only weekdays and the attendance of that day`() {
        val now = date(2026, Calendar.NOVEMBER, 17)
        val calendar = groupCalendar(
            sessions = listOf(session("s1", "2026-11-04", present = 18, absent = 1, late = 1)),
            now = now,
            monthOffset = 0,
            todayIso = "2026-11-17",
            zone = zone,
        )

        assertEquals("Noviembre 2026", calendar.monthLabel)
        assertEquals(0, calendar.leadingBlanks)
        // Noviembre 2026 empieza en domingo: la rejilla arranca el lunes 2.
        assertEquals(2, calendar.days.first().day)
        assertTrue(calendar.days.none { it.day == 7 || it.day == 8 })

        val fourth = calendar.days.first { it.day == 4 }
        // 19 de 20 asistieron (un retardo cuenta como asistencia).
        assertEquals(95, requireNotNull(fourth.percent))
        assertEquals(1, calendar.days.count { it.isToday })
        assertEquals(17, calendar.days.first { it.isToday }.day)
    }

    @Test
    fun `a month that starts on Wednesday pushes two empty cells`() {
        val calendar = groupCalendar(
            sessions = emptyList(),
            now = date(2026, Calendar.JULY, 15),
            monthOffset = 0,
            todayIso = "2026-07-15",
            zone = zone,
        )

        // 1 de julio de 2026 es miércoles: lunes y martes quedan en blanco.
        assertEquals(2, calendar.leadingBlanks)
        assertEquals(1, calendar.days.first().day)
    }

    @Test
    fun `a day without a session prints no percentage instead of zero`() {
        val calendar = groupCalendar(
            sessions = listOf(session("s1", "2026-11-04")),
            now = date(2026, Calendar.NOVEMBER, 17),
            monthOffset = 0,
            todayIso = "2026-11-17",
            zone = zone,
        )

        assertNull(calendar.days.first { it.day == 5 }.percent)
    }

    @Test
    fun `attendance counts late arrivals but not justified absences`() {
        val counts = AttendanceCounts(present = 10, absent = 6, late = 2, justified = 2)

        assertEquals(60, requireNotNull(attendancePercentOf(counts)))
        assertNull(attendancePercentOf(AttendanceCounts()))
        assertNull(groupAttendancePercent(emptyList()))
    }

    @Test
    fun `the card average is the mean of the sessions`() {
        val sessions = listOf(
            session("s1", "2026-11-04", present = 19, absent = 1), // 95%
            session("s2", "2026-11-05", present = 15, absent = 5), // 75%
        )

        assertEquals(85, requireNotNull(groupAttendancePercent(sessions)))
    }

    @Test
    fun `absences today only exist when today has a roll call`() {
        val sessions = listOf(session("s1", "2026-11-17", absent = 3))

        assertEquals(3, requireNotNull(absencesOn(sessions, "2026-11-17")))
        assertNull(absencesOn(sessions, "2026-11-16"))
    }

    @Test
    fun `incidents of another group never reach this tab`() {
        val rows = groupIncidentRows(
            incidents = listOf(
                incident("a", groupId = "3B"),
                incident("b", groupId = "4A"),
            ),
            groupId = "3B",
            zone = zone,
        )

        assertEquals(1, rows.size)
        assertEquals("a", rows.first().id)
        assertEquals("Leve", rows.first().severityLabel)
        assertEquals("Dentro de la escuela · 12 m", rows.first().locationLabel)
    }

    @Test
    fun `an incident without a title falls back to its description`() {
        val rows = groupIncidentRows(
            incidents = listOf(
                incident("a", title = "", description = "Se lastimó jugando.\nNo fue grave."),
            ),
            groupId = "3B",
            zone = zone,
        )

        assertEquals("Se lastimó jugando.", rows.first().title)
    }

    @Test
    fun `only the open incident is expanded`() {
        val rows = groupIncidentRows(
            incidents = listOf(incident("a"), incident("b")),
            groupId = "3B",
            expandedId = "b",
            zone = zone,
        )

        assertTrue(rows.first { it.id == "b" }.expanded)
        assertFalse(rows.first { it.id == "a" }.expanded)
    }

    @Test
    fun `the search finds a child by name or by list number`() {
        val students = groupStudents(
            listOf(
                student("1", 1, "Ana Sofía", "Aguilar"),
                student("3", 3, "Camila", "Domínguez"),
            ),
        )

        assertEquals("01", students.first().number)
        assertEquals("AA", students.first().initials)
        assertEquals(listOf("Camila Domínguez"), searchGroupStudents(students, "camila").map { it.fullName })
        // "3" finds "N° 03": nadie teclea el cero de relleno.
        assertEquals(listOf("Camila Domínguez"), searchGroupStudents(students, "3").map { it.fullName })
        assertTrue(searchGroupStudents(students, "zzz").isEmpty())
        assertEquals(2, searchGroupStudents(students, "  ").size)
    }

    @Test
    fun `the session list keeps only the month, newest first`() {
        val now = date(2026, Calendar.NOVEMBER, 17)
        val rows = groupSessionRows(
            sessions = listOf(
                session("old", "2026-10-30", startedAt = date(2026, Calendar.OCTOBER, 30)),
                session("mid", "2026-11-03", startedAt = date(2026, Calendar.NOVEMBER, 3)),
                session("new", "2026-11-17", startedAt = date(2026, Calendar.NOVEMBER, 17)),
            ),
            now = now,
            monthOffset = 0,
            zone = zone,
        )

        assertEquals(listOf("new", "mid"), rows.map { it.id })
    }

    @Test
    fun `the hero shows the first slot of the group today`() {
        val slots = listOf(
            ClassSlot("s1", "4A", weekday = 2, startTime = "07:00", endTime = "07:45", subject = "Español", teacherId = null),
            ClassSlot("s2", "3B", weekday = 2, startTime = "10:00", endTime = "10:45", subject = "Matemáticas", teacherId = null),
            ClassSlot("s3", "3B", weekday = 2, startTime = "08:00", endTime = "09:00", subject = "Matemáticas", teacherId = null),
        )

        assertEquals("Hoy · 08:00 – 09:00", groupSlotLabel(slots, groupId = "3B", weekday = 2))
        assertNull(groupSlotLabel(slots, groupId = "3B", weekday = 3))
    }

    @Test
    fun `only this teacher's subjects are offered for the roll call`() {
        val slots = listOf(
            ClassSlot("s1", "grp-3b", 2, "08:00", "09:00", "Matemáticas", "uid-docente"),
            ClassSlot("s2", "grp-3b", 3, "10:00", "11:00", "Español III", "uid-docente"),
            ClassSlot("s3", "grp-3b", 4, "12:00", "13:00", "Ciencias Naturales", "otro-docente"),
            ClassSlot("s4", "grp-4a", 2, "08:00", "09:00", "Historia de México I", "uid-docente"),
        )

        val subjects = rollCallSubjectsOf(slots, "grp-3b", 3, "uid-docente")

        assertEquals(
            listOf("g3-matematicas-iii", "g3-espanol-iii"),
            subjects.map { it.id },
        )
        assertEquals(listOf("Matemáticas III", "Español III"), subjects.map { it.label })
    }

    @Test
    fun `an unsynced session never reads as synced`() {
        assertEquals("Sincronizado", sessionSyncLabel(RollCallSync.SYNCED))
        assertEquals("Pendiente", sessionSyncLabel(RollCallSync.PENDING))
        assertEquals("Sin sincronizar", sessionSyncLabel(RollCallSync.FAILED))
    }
}
