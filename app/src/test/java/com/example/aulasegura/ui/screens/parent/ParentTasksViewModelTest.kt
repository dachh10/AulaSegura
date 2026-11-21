package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentTaskItem
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The list shows what the group published, and nothing the school did not write. */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentTasksViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val zone: TimeZone = TimeZone.getTimeZone("America/Mexico_City")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pending and overdue are split by the due date`() = runTest(dispatcher) {
        val viewModel = viewModel(
            tasks = mapOf(
                "grp-3b" to listOf(
                    task("ayer", at(2026, 11, 17, 8)),
                    task("hoy", at(2026, 11, 18, 8)),
                ),
            ),
        )
        advanceUntilIdle()

        val pending = viewModel.state.value as ParentTasksUiState.Loaded
        assertEquals(ParentTaskFilter.Pending, pending.filter)
        assertEquals(listOf("HOY"), pending.sections.map { it.label })
        assertEquals(listOf("hoy"), pending.sections[0].tasks.map { it.title })

        viewModel.selectFilter(ParentTaskFilter.Overdue)

        val overdue = viewModel.state.value as ParentTasksUiState.Loaded
        assertEquals(listOf("VENCIDAS"), overdue.sections.map { it.label })
        assertEquals(listOf("ayer"), overdue.sections[0].tasks.map { it.title })
    }

    @Test
    fun `the delivered filter is empty on purpose`() = runTest(dispatcher) {
        val viewModel = viewModel(tasks = mapOf("grp-3b" to listOf(task("hoy", at(2026, 11, 18, 8)))))
        advanceUntilIdle()

        viewModel.selectFilter(ParentTaskFilter.Delivered)

        val state = viewModel.state.value as ParentTasksUiState.Loaded
        assertTrue(state.sections.isEmpty())
        assertTrue(state.emptyMessage.contains("no registra entregas"))
    }

    @Test
    fun `changing the filter does not read the tasks again`() = runTest(dispatcher) {
        val parents = FakeParents(tasks = mapOf("grp-3b" to listOf(task("hoy", at(2026, 11, 18, 8)))))
        val viewModel = ParentTasksViewModel(parents, selection = TestChildSelection(), now = { at(2026, 11, 18, 12) }, zone = zone)
        advanceUntilIdle()

        viewModel.selectFilter(ParentTaskFilter.All)
        viewModel.selectFilter(ParentTaskFilter.Overdue)
        advanceUntilIdle()

        assertEquals(1, parents.taskReads)
    }

    @Test
    fun `the pill switches child and reloads that group`() = runTest(dispatcher) {
        val viewModel = viewModel(
            children = listOf(CHILD, SIBLING),
            tasks = mapOf(
                "grp-3b" to listOf(task("de mariana", at(2026, 11, 18, 8))),
                "grp-1a" to listOf(task("de diego", at(2026, 11, 19, 8))),
            ),
        )
        advanceUntilIdle()

        assertEquals("Mariana", (viewModel.state.value as ParentTasksUiState.Loaded).childName)

        viewModel.cycleChild()
        advanceUntilIdle()

        val state = viewModel.state.value as ParentTasksUiState.Loaded
        assertEquals("Diego", state.childName)
        assertEquals(listOf("de diego"), state.sections.single().tasks.map { it.title })
    }

    @Test
    fun `a single child cannot be switched`() = runTest(dispatcher) {
        val parents = FakeParents(tasks = mapOf("grp-3b" to emptyList()))
        val viewModel = ParentTasksViewModel(parents, selection = TestChildSelection(), now = { at(2026, 11, 18, 12) }, zone = zone)
        advanceUntilIdle()

        viewModel.cycleChild()
        advanceUntilIdle()

        assertEquals(1, parents.taskReads)
    }

    @Test
    fun `an account without a linked child is told so`() = runTest(dispatcher) {
        val viewModel = viewModel(children = emptyList())
        advanceUntilIdle()

        assertEquals(ParentTasksUiState.NoChildren, viewModel.state.value)
    }

    @Test
    fun `a failed read offers a retry and a second attempt can succeed`() = runTest(dispatcher) {
        val parents = FakeParents(
            tasks = mapOf("grp-3b" to listOf(task("hoy", at(2026, 11, 18, 8)))),
            failure = RosterFailure.OFFLINE,
        )
        val viewModel = ParentTasksViewModel(parents, selection = TestChildSelection(), now = { at(2026, 11, 18, 12) }, zone = zone)
        advanceUntilIdle()

        assertEquals(
            ParentTasksUiState.Failed("Sin conexión. Revisa tu red e intenta de nuevo."),
            viewModel.state.value,
        )

        parents.failure = null
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value as ParentTasksUiState.Loaded
        assertFalse(state.sections.isEmpty())
    }

    private fun viewModel(
        children: List<ParentChild> = listOf(CHILD),
        tasks: Map<String, List<ParentTaskItem>> = emptyMap(),
    ) = ParentTasksViewModel(
        selection = TestChildSelection(),
        parents = FakeParents(children = children, tasks = tasks),
        now = { at(2026, 11, 18, 12) },
        zone = zone,
    )

    private fun task(title: String, due: Long) = ParentTaskItem(
        taskId = "tsk-$title",
        groupIds = listOf("grp-3b"),
        subject = "Matemáticas",
        title = title,
        description = "",
        dueDateMillis = due,
        createdAtMillis = at(2026, 11, 10, 10),
        createdByName = "Prof. Ricardo Salinas",
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, 0)
        }.timeInMillis
}

private class FakeParents(
    private val children: List<ParentChild> = listOf(CHILD),
    private val tasks: Map<String, List<ParentTaskItem>> = emptyMap(),
    var failure: RosterFailure? = null,
) : ParentRepository {

    var taskReads = 0

    override suspend fun loadChildren(): ParentChildrenResult =
        ParentChildrenResult.Loaded(children)

    override suspend fun loadMonth(
        child: ParentChild,
        year: Int,
        month: Int,
    ): ParentMonthResult = ParentMonthResult.Loaded(
        com.example.aulasegura.domain.model.ParentMonthAttendance(
            year = year,
            month = month,
            counts = com.example.aulasegura.domain.model.ParentAttendanceCounts(),
            days = emptyList(),
        ),
    )

    override suspend fun loadHome(): ParentHomeResult = ParentHomeResult.Loaded(emptyList())

    override suspend fun loadTasks(child: ParentChild): ParentTasksResult {
        taskReads++
        failure?.let { return ParentTasksResult.Failed(it) }
        return ParentTasksResult.Loaded(tasks[child.groupId].orEmpty())
    }

    override suspend fun loadTask(taskId: String): ParentTaskResult = ParentTaskResult.NotFound

    override suspend fun loadNotices(): ParentNoticesResult = ParentNoticesResult.Loaded(emptyList())

    override suspend fun markNoticeRead(noticeId: String): NoticeReadResult = NoticeReadResult.Marked

    override suspend fun loadIncidents(): ParentIncidentsResult =
        ParentIncidentsResult.Loaded(emptyList())

    override suspend fun loadIncident(incidentId: String): ParentIncidentResult =
        ParentIncidentResult.NotFound

    override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult =
        IncidentSeenResult.Marked
}

private val CHILD = ParentChild(
    studentId = "std-1",
    fullName = "Mariana Gómez Ruiz",
    firstName = "Mariana",
    listNumber = 1,
    groupId = "grp-3b",
    groupLabel = "3° B",
)

private val SIBLING = ParentChild(
    studentId = "std-2",
    fullName = "Diego Gómez Ruiz",
    firstName = "Diego",
    listNumber = 1,
    groupId = "grp-1a",
    groupLabel = "1° A",
)
