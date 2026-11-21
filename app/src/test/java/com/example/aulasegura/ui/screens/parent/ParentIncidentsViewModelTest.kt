package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentIncident
import com.example.aulasegura.domain.model.ParentIncidentLocation
import com.example.aulasegura.domain.model.ParentMonthAttendance
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

/**
 * The list shows one child at a time, with that child's counters.
 *
 * A sibling's incident must not appear behind the other's name: that would be a
 * privacy bug and a false report at the same time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentIncidentsViewModelTest {

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
    fun `counters and rows belong to the child on screen`() = runTest(dispatcher) {
        val viewModel = viewModel(
            incidents = listOf(
                incident("a", studentId = "std-1", seen = false),
                incident("b", studentId = "std-1", seen = true, severity = "grave"),
                incident("c", studentId = "std-2", seen = false, severity = "grave"),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.state.value as ParentIncidentsUiState.Loaded
        assertEquals("Mariana", state.childName)
        assertEquals(2, state.total)
        assertEquals(1, state.newCount)
        assertEquals(1, state.graveCount)
        assertEquals(listOf("a", "b"), state.incidents.map { it.incidentId })
    }

    @Test
    fun `the pill switches child without reading again`() = runTest(dispatcher) {
        val parents = FakeIncidentParents(
            incidents = listOf(
                incident("a", studentId = "std-1"),
                incident("c", studentId = "std-2"),
            ),
        )
        val viewModel = ParentIncidentsViewModel(parents, TestChildSelection(), { "tutor-1" }, zone)
        advanceUntilIdle()

        viewModel.cycleChild()
        advanceUntilIdle()

        val state = viewModel.state.value as ParentIncidentsUiState.Loaded
        assertEquals("Diego", state.childName)
        assertEquals(listOf("c"), state.incidents.map { it.incidentId })
        assertEquals(1, parents.incidentReads)
    }

    @Test
    fun `an empty list is not an error`() = runTest(dispatcher) {
        val viewModel = viewModel(incidents = emptyList())
        advanceUntilIdle()

        val state = viewModel.state.value as ParentIncidentsUiState.Loaded
        assertEquals(0, state.total)
        assertTrue(state.incidents.isEmpty())
    }

    @Test
    fun `an account without a linked child is told so`() = runTest(dispatcher) {
        val viewModel = viewModel(children = emptyList())
        advanceUntilIdle()

        assertEquals(ParentIncidentsUiState.NoChildren, viewModel.state.value)
    }

    @Test
    fun `a failed read offers a retry and the second attempt can succeed`() =
        runTest(dispatcher) {
            val parents = FakeIncidentParents(
                incidents = listOf(incident("a", studentId = "std-1")),
                failure = RosterFailure.OFFLINE,
            )
            val viewModel = ParentIncidentsViewModel(parents, TestChildSelection(), { "tutor-1" }, zone)
            advanceUntilIdle()

            assertEquals(
                ParentIncidentsUiState.Failed("Sin conexión. Revisa tu red e intenta de nuevo."),
                viewModel.state.value,
            )

            parents.failure = null
            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.state.value as ParentIncidentsUiState.Loaded
            assertFalse(state.incidents.isEmpty())
        }

    private fun viewModel(
        children: List<ParentChild> = listOf(INCIDENT_CHILD, INCIDENT_SIBLING),
        incidents: List<ParentIncident> = emptyList(),
    ) = ParentIncidentsViewModel(
        selection = TestChildSelection(),
        parents = FakeIncidentParents(children = children, incidents = incidents),
        zone = zone,
    )

    private fun incident(
        id: String,
        studentId: String,
        seen: Boolean = false,
        severity: String = "leve",
    ) = ParentIncident(
        incidentId = id,
        studentId = studentId,
        studentName = "Mariana Gómez Ruiz",
        groupId = "grp-3b",
        title = "Incidente $id",
        description = "Descripción",
        severity = severity,
        category = "caida",
        reportedByName = "Prof. Ricardo Salinas",
        occurredAtMillis = Calendar.getInstance(zone).apply {
            clear()
            set(2026, 9, 30, 11, 20)
        }.timeInMillis,
        status = "nuevo",
        location = ParentIncidentLocation(19.2926, -99.6557, 5.0, 12.4, true),
        seenByMe = seen,
    )
}

private class FakeIncidentParents(
    private val children: List<ParentChild> = listOf(INCIDENT_CHILD, INCIDENT_SIBLING),
    private val incidents: List<ParentIncident> = emptyList(),
    var failure: RosterFailure? = null,
) : ParentRepository {

    var incidentReads = 0

    override suspend fun loadChildren(): ParentChildrenResult =
        ParentChildrenResult.Loaded(children)

    override suspend fun loadMonth(
        child: ParentChild,
        year: Int,
        month: Int,
    ): ParentMonthResult = ParentMonthResult.Loaded(
        ParentMonthAttendance(year, month, ParentAttendanceCounts(), emptyList()),
    )

    override suspend fun loadHome(): ParentHomeResult = ParentHomeResult.Loaded(emptyList())

    override suspend fun loadTasks(child: ParentChild): ParentTasksResult =
        ParentTasksResult.Loaded(emptyList())

    override suspend fun loadTask(taskId: String): ParentTaskResult = ParentTaskResult.NotFound

    override suspend fun loadNotices(): ParentNoticesResult = ParentNoticesResult.Loaded(emptyList())

    override suspend fun markNoticeRead(noticeId: String): NoticeReadResult = NoticeReadResult.Marked

    override suspend fun loadIncidents(): ParentIncidentsResult {
        incidentReads++
        failure?.let { return ParentIncidentsResult.Failed(it) }
        return ParentIncidentsResult.Loaded(incidents)
    }

    override suspend fun loadIncident(incidentId: String): ParentIncidentResult =
        ParentIncidentResult.NotFound

    override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult =
        IncidentSeenResult.Marked
}

private val INCIDENT_CHILD = ParentChild(
    studentId = "std-1",
    fullName = "Mariana Gómez Ruiz",
    firstName = "Mariana",
    listNumber = 1,
    groupId = "grp-3b",
    groupLabel = "3° B",
)

private val INCIDENT_SIBLING = ParentChild(
    studentId = "std-2",
    fullName = "Diego Gómez Ruiz",
    firstName = "Diego",
    listNumber = 1,
    groupId = "grp-1a",
    groupLabel = "1° A",
)
