package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentChildOverview
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.model.ParentNoticeItem
import com.example.aulasegura.domain.model.ParentTaskBrief
import com.example.aulasegura.domain.repository.IncidentSeenResult
import com.example.aulasegura.domain.repository.NoticeReadResult
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentIncidentResult
import com.example.aulasegura.domain.repository.ParentIncidentsResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentNoticesResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.ParentTaskResult
import com.example.aulasegura.domain.repository.ParentTasksResult
import com.example.aulasegura.domain.repository.RosterFailure
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The loader is the only place that decides what the assistant may know, so the
 * tests pin the two decisions that are easy to get silently wrong: the 30-day
 * window crossing into the previous month, and a failed read closing the whole
 * context instead of producing a half-truth.
 */
class LoadAssistantContextUseCaseTest {

    private val zone: TimeZone = TimeZone.getTimeZone("America/Mexico_City")

    @Test
    fun `the window crosses into the previous month when it has to`() = runTest {
        val repository = FakeParents(
            monthDays = listOf(
                day("2026-11-19", AttendanceMark.PRESENT),
                day("2026-11-18", AttendanceMark.LATE),
                day("2026-11-02", AttendanceMark.ABSENT),
            ),
            previousMonthDays = listOf(
                day("2026-10-31", AttendanceMark.PRESENT),
                // Older than the window even though it is in the previous month.
                day("2026-10-02", AttendanceMark.ABSENT),
            ),
        )

        val result = LoadAssistantContextUseCase(
            parents = repository,
            now = { at(2026, 11, 19) },
            zone = zone,
        )()

        assertTrue(result is AssistantContextResult.Loaded)
        val child = (result as AssistantContextResult.Loaded).context.children.single()
        assertEquals("both months were read", listOf(2026 to 10), repository.monthsRead)
        assertEquals(2, child.attendance.present)
        assertEquals(1, child.attendance.late)
        assertEquals(1, child.attendance.absent)
        assertEquals(4, child.attendance.total)
    }

    @Test
    fun `when the window starts on the first of the month nothing extra is read`() = runTest {
        val repository = FakeParents(
            monthDays = listOf(day("2026-12-31", AttendanceMark.PRESENT)),
        )

        LoadAssistantContextUseCase(
            parents = repository,
            now = { at(2026, 12, 31) },
            zone = zone,
        )()

        assertEquals(emptyList<Pair<Int, Int>>(), repository.monthsRead)
    }

    @Test
    fun `a failed notices read closes the context`() = runTest {
        val repository = FakeParents(failNotices = true)

        val result = LoadAssistantContextUseCase(
            parents = repository,
            now = { at(2026, 11, 19) },
            zone = zone,
        )()

        assertEquals(AssistantContextResult.Failed(RosterFailure.OFFLINE), result)
    }

    @Test
    fun `a tutor without children is told so, not given an empty assistant`() = runTest {
        val repository = FakeParents(children = emptyList())

        val result = LoadAssistantContextUseCase(
            parents = repository,
            now = { at(2026, 11, 19) },
            zone = zone,
        )()

        assertEquals(AssistantContextResult.NoChildren, result)
    }

    @Test
    fun `the context keeps notices and incidents trimmed to a bounded prompt`() = runTest {
        val repository = FakeParents(
            notices = (1..12).map { notice("n$it") },
            incidents = (1..12).map { incident("i$it") },
        )

        val result = LoadAssistantContextUseCase(
            parents = repository,
            now = { at(2026, 11, 19) },
            zone = zone,
        )() as AssistantContextResult.Loaded

        assertEquals(8, result.context.notices.size)
        assertEquals(8, result.context.incidents.size)
    }

    private fun at(year: Int, month: Int, day: Int): Long = Calendar.getInstance(zone).apply {
        clear()
        set(year, month - 1, day, 12, 0)
    }.timeInMillis

    private fun day(date: String, mark: AttendanceMark) = ParentDayAttendance(
        date = date,
        subject = "Matemáticas",
        startedAt = 0L,
        mark = mark,
        time = null,
        note = null,
    )

    private fun notice(id: String) = ParentNoticeItem(
        noticeId = id,
        title = "Aviso $id",
        body = "Cuerpo $id",
        category = "grupo",
        urgent = false,
        createdByName = "Prof. Ricardo Salinas",
        createdAtMillis = at(2026, 11, 18),
    )

    private fun incident(id: String) = ParentIncident(
        incidentId = id,
        studentId = "std-1",
        studentName = "Mariana Gómez Ruiz",
        groupId = "grp-4d",
        title = "Incidente $id",
        description = "Descripción $id",
        severity = "leve",
        category = "otro",
        reportedByName = "Prof. Ricardo Salinas",
        occurredAtMillis = at(2026, 11, 17),
        status = "nuevo",
        location = null,
    )

    private inner class FakeParents(
        private val children: List<ParentChildOverview>? = null,
        private val monthDays: List<ParentDayAttendance> = emptyList(),
        private val previousMonthDays: List<ParentDayAttendance> = emptyList(),
        private val notices: List<ParentNoticeItem> = listOf(notice("n1")),
        private val incidents: List<ParentIncident> = listOf(incident("i1")),
        private val failNotices: Boolean = false,
    ) : ParentRepository {

        val monthsRead = mutableListOf<Pair<Int, Int>>()

        private val overviews: List<ParentChildOverview> = children ?: listOf(
            ParentChildOverview(
                child = ParentChild(
                    studentId = "std-1",
                    fullName = "Mariana Gómez Ruiz",
                    firstName = "Mariana",
                    listNumber = 1,
                    groupId = "grp-4d",
                    groupLabel = "4° D",
                ),
                today = day("2026-11-19", AttendanceMark.PRESENT),
                month = ParentMonthAttendance(2026, 11, ParentAttendanceCounts(), monthDays),
                tasks = listOf(
                    ParentTaskBrief(
                        taskId = "task-1",
                        subject = "Matemáticas",
                        title = "Fracciones",
                        dueDateMillis = at(2026, 11, 20),
                    ),
                ),
            ),
        )

        override suspend fun loadChildren(): ParentChildrenResult =
            ParentChildrenResult.Loaded(overviews.map { it.child })

        override suspend fun loadHome(subjectId: String?): ParentHomeResult = ParentHomeResult.Loaded(overviews)

        override suspend fun loadMonth(
            child: ParentChild,
            year: Int,
            month: Int,
            subjectId: String?,
        ): ParentMonthResult {
            monthsRead += year to month
            return ParentMonthResult.Loaded(
                ParentMonthAttendance(year, month, ParentAttendanceCounts(), previousMonthDays),
            )
        }

        override suspend fun loadNotices(): ParentNoticesResult =
            if (failNotices) {
                ParentNoticesResult.Failed(RosterFailure.OFFLINE)
            } else {
                ParentNoticesResult.Loaded(notices)
            }

        override suspend fun loadIncidents(): ParentIncidentsResult =
            ParentIncidentsResult.Loaded(incidents)

        override suspend fun loadTasks(child: ParentChild): ParentTasksResult =
            ParentTasksResult.Loaded(emptyList())

        override suspend fun loadTask(taskId: String): ParentTaskResult = ParentTaskResult.NotFound

        override suspend fun markNoticeRead(noticeId: String): NoticeReadResult =
            NoticeReadResult.Marked

        override suspend fun loadIncident(incidentId: String): ParentIncidentResult =
            ParentIncidentResult.NotFound

        override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult =
            IncidentSeenResult.Marked
    }
}
