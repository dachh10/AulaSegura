package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.ClassSlot
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.PublishedTask
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.domain.model.minutesOf
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The home used to print fixed numbers; now it prints the teacher's day, so the
 * parts that can silently lie are the weekday conversion, the "which class is
 * next" pick and the summary arithmetic.
 */
class TeacherHomeUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    // 2026-11-16 is a Monday and 2026-11-17 a Tuesday.
    private val monday = at(2026, Calendar.NOVEMBER, 16, 8, 0)
    private val tuesday = at(2026, Calendar.NOVEMBER, 17, 8, 0)

    @Test
    fun `the schedule weekday is monday first, not calendar first`() {
        assertEquals("lunes", 1, scheduleWeekday(monday, utc))
        assertEquals("martes", 2, scheduleWeekday(tuesday, utc))
        assertEquals("domingo", 7, scheduleWeekday(at(2026, Calendar.NOVEMBER, 15, 9, 0), utc))
    }

    @Test
    fun `a time becomes minutes of the day`() {
        assertEquals(480, minutesOfDay(at(2026, Calendar.NOVEMBER, 17, 8, 0), utc))
        assertEquals(510, minutesOf("08:30"))
        assertNull(minutesOf("8:30 pm"))
        assertNull(minutesOf("25:00"))
        assertNull(minutesOf(null))
    }

    @Test
    fun `a class in progress wins over the next one`() {
        val slots = listOf(
            slot("s1", weekday = 2, start = "08:00", end = "08:45"),
            slot("s2", weekday = 2, start = "10:00", end = "10:45"),
        )

        val next = nextClassOf(slots, groups(), weekday = 2, nowMinutes = 10 * 60 + 10)

        assertEquals(ClassStatus.IN_PROGRESS, next?.status)
        assertEquals("10:00 – 10:45", next?.timeLabel)
        assertEquals("10:00", nextClassOf(slots, groups(), 2, 9 * 60)?.timeLabel)
        assertEquals(ClassStatus.NEXT, nextClassOf(slots, groups(), 2, 9 * 60)?.status)
    }

    @Test
    fun `another day's slots are ignored`() {
        val slots = listOf(slot("s1", weekday = 1, start = "08:00", end = "08:45"))

        assertNull(nextClassOf(slots, groups(), weekday = 2, nowMinutes = 0))
    }

    @Test
    fun `a day with nothing left has no next class`() {
        val slots = listOf(slot("s1", weekday = 2, start = "08:00", end = "08:45"))

        assertNull("17:00 is after the last bell", nextClassOf(slots, groups(), 2, 17 * 60))
        assertNull(nextClassOf(emptyList(), groups(), 2, 8 * 60))
    }

    @Test
    fun `a slot with an unreadable time is dropped instead of hiding the real one`() {
        val slots = listOf(
            slot("roto", weekday = 2, start = "", end = "09:00"),
            slot("bueno", weekday = 2, start = "10:00", end = "10:45"),
        )

        assertEquals("10:00", nextClassOf(slots, groups(), 2, 8 * 60)?.timeLabel)
    }

    @Test
    fun `the card carries the real group and its roster size`() {
        val next = nextClassOf(
            slots = listOf(slot("s1", weekday = 2, start = "10:00", end = "10:45", subject = "Matemáticas")),
            groups = groups(),
            weekday = 2,
            nowMinutes = 9 * 60,
        )

        assertEquals("3° B", next?.groupLabel)
        assertEquals(28, next?.studentCount)
        assertEquals("Matemáticas · 3° B", next?.headline)
        assertEquals("grp-3b", next?.groupId)
        assertEquals("g3-matematicas-iii", next?.subjectId)
    }

    @Test
    fun `summary counts groups once, sums absences and passes the queue through`() {
        val summary = todaySummary(
            sessions = listOf(
                session("a", groupId = "grp-3b", date = "2026-11-17", absent = 2),
                session("b", groupId = "grp-3b", date = "2026-11-17", absent = 1),
                session("c", groupId = "grp-4a", date = "2026-11-17", absent = 0),
                session("d", groupId = "grp-2c", date = "2026-11-16", absent = 5),
            ),
            todayIso = "2026-11-17",
            groupsTotal = 5,
            pendingSync = 1,
        )

        assertEquals(2, summary.listsPassed)
        assertEquals(5, summary.groupsTotal)
        assertEquals(3, summary.absences)
        assertEquals(1, summary.pendingSync)
    }

    @Test
    fun `the denominator never becomes smaller than the lists passed`() {
        val summary = todaySummary(
            sessions = listOf(session("a", "grp-3b", "2026-11-17", absent = 0)),
            todayIso = "2026-11-17",
            groupsTotal = 0,
            pendingSync = 0,
        )

        assertEquals(1, summary.listsPassed)
        assertEquals(1, summary.groupsTotal)
    }

    @Test
    fun `recent activity mixes the four sources newest first and keeps four`() {
        val now = tuesday + 5 * 3_600_000L

        val activity = recentActivity(
            sessions = listOf(session("a", "grp-3b", "2026-11-17", absent = 0, startedAt = tuesday + 60_000)),
            incidents = listOf(incident("inc-1", at = tuesday + 120_000)),
            tasks = listOf(task("t-1", at = tuesday + 180_000)),
            notices = listOf(notice("n-1", at = tuesday + 240_000), notice("n-0", at = monday)),
            now = now,
            zone = utc,
        )

        assertEquals(4, activity.size)
        assertEquals(listOf("n-1", "t-1", "inc-1", "a"), activity.map { it.id.removePrefix("notice-").removePrefix("task-").removePrefix("incident-").removePrefix("roll-") })
        assertEquals("08:04", activity.first().timeLabel)
        assertEquals("08:01", activity.last().timeLabel)
        assertEquals(ActivityKind.NOTICE, activity.first().kind)
        // The fifth item (yesterday's notice) is the one the limit drops.
        assertEquals("Ayer", activityTimeLabel(monday, now, utc))
    }

    @Test
    fun `activity titles say what happened, not just the name`() {
        val activity = recentActivity(
            sessions = listOf(session("a", "grp-3b", "2026-11-17", absent = 0, startedAt = tuesday)),
            incidents = listOf(incident("inc-1", at = tuesday)),
            tasks = listOf(task("t-1", at = tuesday)),
            notices = listOf(notice("n-1", at = tuesday)),
            now = tuesday,
            zone = utc,
        ).map { it.title }

        assertTrue(activity.contains("Lista de 3° B guardada"))
        assertTrue(activity.contains("Incidente de Benjamín Trejo"))
        assertTrue(activity.contains("Tarea «Fracciones» publicada"))
        assertTrue(activity.contains("Aviso «Suspensión de clases» publicado"))
    }

    @Test
    fun `an empty day renders no activity at all`() {
        assertTrue(recentActivity(emptyList(), emptyList(), emptyList(), emptyList(), tuesday, utc).isEmpty())
    }

    private fun groups(): Map<String, Group> = mapOf(
        "grp-3b" to Group(
            id = "grp-3b",
            name = "3° B",
            grade = 3,
            section = "B",
            schoolId = "sch-benito-juarez",
            titularTeacherId = "uid-docente",
            teacherIds = listOf("uid-docente"),
            studentCount = 28,
            cycle = "2026-2027",
        ),
    )

    @Test
    fun `the groups card counts the real groups with correct singular`() {
        assertEquals("Sin grupos", groupCountLabel(0))
        assertEquals("1 grupo", groupCountLabel(1))
        assertEquals("3 grupos", groupCountLabel(3))
    }

    private fun slot(
        id: String,
        weekday: Int,
        start: String,
        end: String,
        subject: String = "Matemáticas",
    ) = ClassSlot(
        slotId = id,
        groupId = "grp-3b",
        weekday = weekday,
        startTime = start,
        endTime = end,
        subject = subject,
        teacherId = "uid-docente",
    )

    private fun session(
        id: String,
        groupId: String,
        date: String,
        absent: Int,
        startedAt: Long = at(2026, Calendar.NOVEMBER, 17, 8, 0),
    ) = AttendanceSessionSummary(
        sessionId = id,
        groupId = groupId,
        groupLabel = "3° B",
        subject = "Matemáticas",
        date = date,
        startedAt = startedAt,
        counts = AttendanceCounts(present = 27, absent = absent),
        verified = true,
        sync = RollCallSync.SYNCED,
    )

    private fun incident(id: String, at: Long) = IncidentSummary(
        incidentId = id,
        studentName = "Benjamín Trejo",
        title = "Caída en el patio",
        description = "Se cayó",
        severity = IncidentSeverity.LEVE,
        category = IncidentCategory.CAIDA,
        status = IncidentStatus.NUEVO,
        occurredAt = at,
        locationVerified = true,
        locationDistanceMeters = 12.0,
        sync = SyncPhase.SYNCED,
        mediaCount = 1,
    )

    private fun task(id: String, at: Long) = PublishedTask(
        taskId = id,
        title = "Fracciones",
        subject = "Matemáticas",
        description = "Ejercicios",
        groupNames = listOf("3° B"),
        dueDate = at + 86_400_000L,
        createdAt = at,
        pending = false,
    )

    private fun notice(id: String, at: Long) = PublishedNotice(
        noticeId = id,
        title = "Suspensión de clases",
        body = "El viernes no hay clases.",
        groupNames = listOf("3° B"),
        urgent = true,
        createdAt = at,
        pending = false,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
