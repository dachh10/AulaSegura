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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * "Enterado" is the only write the tutor has on an incident.
 *
 * These tests pin the contract the rules enforce: the button cannot be undone, so
 * the screen only shows `seen` once the cloud accepted the mark.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentIncidentDetailViewModelTest {

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
    fun `the incident carries the real coordinate to the map`() = runTest(dispatcher) {
        val viewModel = viewModel(parents = FakeIncidentDetailParents())

        advanceUntilIdle()

        val state = viewModel.state.value as ParentIncidentDetailUiState.Loaded
        assertEquals("Incidente de prueba", state.title)
        assertEquals(19.2926, state.latitude!!, 0.000001)
        assertEquals(-99.6557, state.longitude!!, 0.000001)
        assertTrue(state.locationVerified)
        assertFalse(state.seen)
    }

    @Test
    fun `acknowledging writes once and then the button is settled`() = runTest(dispatcher) {
        val parents = FakeIncidentDetailParents()
        val viewModel = viewModel(parents)
        advanceUntilIdle()

        viewModel.acknowledge()
        advanceUntilIdle()

        val seen = viewModel.state.value as ParentIncidentDetailUiState.Loaded
        assertTrue(seen.seen)
        assertFalse(seen.marking)
        assertEquals(1, parents.seenCalls)

        viewModel.acknowledge()
        advanceUntilIdle()

        assertEquals(
            "the rules cannot remove a tutor, so a second press must not even try",
            1,
            parents.seenCalls,
        )
    }

    @Test
    fun `a refused mark leaves the incident unseen and explains why`() = runTest(dispatcher) {
        val parents = FakeIncidentDetailParents(seenFailure = RosterFailure.OFFLINE)
        val viewModel = viewModel(parents)
        advanceUntilIdle()

        viewModel.acknowledge()
        advanceUntilIdle()

        val state = viewModel.state.value as ParentIncidentDetailUiState.Loaded
        assertFalse("nothing was stored, so the badge must stay New", state.seen)
        assertFalse(state.marking)
        assertNotNull(state.error)
    }

    @Test
    fun `an incident that no longer exists is not an error`() = runTest(dispatcher) {
        val viewModel = viewModel(parents = FakeIncidentDetailParents(incident = null))
        advanceUntilIdle()

        assertEquals(ParentIncidentDetailUiState.NotFound, viewModel.state.value)
    }

    @Test
    fun `a read failure offers a retry`() = runTest(dispatcher) {
        val parents = FakeIncidentDetailParents(readFailure = RosterFailure.OFFLINE)
        val viewModel = viewModel(parents)
        advanceUntilIdle()

        assertEquals(
            ParentIncidentDetailUiState.Failed("Sin conexión. Revisa tu red e intenta de nuevo."),
            viewModel.state.value,
        )

        parents.readFailure = null
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is ParentIncidentDetailUiState.Loaded)
    }

    @Test
    fun `an incident without a coordinate has no pin`() = runTest(dispatcher) {
        val viewModel = viewModel(
            parents = FakeIncidentDetailParents(
                incident = INCIDENT.copy(location = null),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.state.value as ParentIncidentDetailUiState.Loaded
        assertNull(state.latitude)
        assertNull(state.longitude)
        assertEquals("Sin ubicación", state.locationLabel)
    }

    private fun viewModel(parents: ParentRepository) = ParentIncidentDetailViewModel(
        parents = parents,
        incidentId = "inc-1",
        zone = zone,
    )
}

private class FakeIncidentDetailParents(
    private val incident: ParentIncident? = INCIDENT,
    var readFailure: RosterFailure? = null,
    var seenFailure: RosterFailure? = null,
) : ParentRepository {

    var seenCalls = 0

    override suspend fun loadChildren(): ParentChildrenResult =
        ParentChildrenResult.Loaded(emptyList())

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

    override suspend fun loadIncidents(): ParentIncidentsResult =
        ParentIncidentsResult.Loaded(emptyList())

    override suspend fun loadIncident(incidentId: String): ParentIncidentResult {
        readFailure?.let { return ParentIncidentResult.Failed(it) }
        val found = incident ?: return ParentIncidentResult.NotFound
        return ParentIncidentResult.Loaded(found)
    }

    override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult {
        seenCalls++
        seenFailure?.let { return IncidentSeenResult.Failed(it) }
        return IncidentSeenResult.Marked
    }
}

private val INCIDENT = ParentIncident(
    incidentId = "inc-1",
    studentId = "std-1",
    studentName = "Mariana Gómez Ruiz",
    groupId = "grp-3b",
    title = "Incidente de prueba",
    description = "Descripción del incidente.",
    severity = "moderado",
    category = "discusion",
    reportedByName = "Prof. Ricardo Salinas",
    occurredAtMillis = Calendar.getInstance().apply {
        clear()
        set(2026, 9, 21, 13, 5)
    }.timeInMillis,
    status = "nuevo",
    location = ParentIncidentLocation(19.2926, -99.6557, 5.0, 12.4, true),
)
