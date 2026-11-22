package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.model.ParentNoticeItem
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The inbox reads the school's notices and writes only the tutor's read mark.
 *
 * The two rules pinned here: the badge counts what the cloud says, and a mark the
 * cloud refused is taken back instead of left as a silent lie.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentNoticesViewModelTest {

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
    fun `the badge counts the notices the repository marked as unread`() = runTest(dispatcher) {
        val viewModel = viewModel(
            notices = listOf(
                notice("hoy", at(2026, 11, 18, 9)),
                notice("ayer", at(2026, 11, 17, 9), read = true),
                notice("semana", at(2026, 11, 14, 9)),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.state.value as ParentNoticesUiState.Loaded
        assertEquals(2, state.unreadCount)
        assertEquals("Último: Hoy, 09:00", state.latestLabel)
        assertEquals(listOf("HOY", "ESTA SEMANA"), state.sections.map { it.label })
    }

    @Test
    fun `the inbox names the child whose notices are shown`() = runTest(dispatcher) {
        val viewModel = viewModel(notices = listOf(notice("hoy", at(2026, 11, 18, 9))))
        advanceUntilIdle()

        val state = viewModel.state.value as ParentNoticesUiState.Loaded
        assertEquals("Mariana Gómez Ruiz", state.childLabel)
    }

    @Test
    fun `a refresh reads the roster only once`() = runTest(dispatcher) {
        val parents = FakeNotices(notices = listOf(notice("hoy", at(2026, 11, 18, 9))))
        val viewModel = ParentNoticesViewModel(
            parents = parents,
            selection = TestChildSelection(),
            now = { at(2026, 11, 18, 12) },
            zone = zone,
        )
        advanceUntilIdle()

        assertEquals(1, parents.childrenLoads)
    }

    @Test
    fun `opening a notice writes the mark once and lowers the badge`() = runTest(dispatcher) {
        val parents = FakeNotices(
            notices = listOf(notice("hoy", at(2026, 11, 18, 9))),
        )
        val viewModel = ParentNoticesViewModel(
            selection = TestChildSelection(),
            parents = parents,
            now = { at(2026, 11, 18, 12) },
            zone = zone,
        )
        advanceUntilIdle()

        viewModel.markRead("hoy")
        advanceUntilIdle()
        viewModel.markRead("hoy")
        advanceUntilIdle()

        val state = viewModel.state.value as ParentNoticesUiState.Loaded
        assertEquals(0, state.unreadCount)
        assertEquals(listOf("hoy"), parents.readMarks)
        assertEquals("Estás al día", state.latestLabel)
    }

    @Test
    fun `a refused mark is reverted and explained`() = runTest(dispatcher) {
        val parents = FakeNotices(
            notices = listOf(notice("hoy", at(2026, 11, 18, 9))),
            readResult = NoticeReadResult.Failed(RosterFailure.OFFLINE),
        )
        val viewModel = ParentNoticesViewModel(
            selection = TestChildSelection(),
            parents = parents,
            now = { at(2026, 11, 18, 12) },
            zone = zone,
        )
        advanceUntilIdle()

        viewModel.markRead("hoy")
        advanceUntilIdle()

        val state = viewModel.state.value as ParentNoticesUiState.Loaded
        assertEquals("the badge goes back to what the cloud still holds", 1, state.unreadCount)
        assertNotNull(state.error)
        assertFalse(state.sections.isEmpty())
    }

    @Test
    fun `the filters keep school and group notices apart`() = runTest(dispatcher) {
        val viewModel = viewModel(
            notices = listOf(
                notice("escuela", at(2026, 11, 18, 9), category = "escuela"),
                notice("grupo", at(2026, 11, 18, 8), category = "grupo"),
                notice("urgente", at(2026, 11, 18, 7), category = "urgente", urgent = true),
            ),
        )
        advanceUntilIdle()

        viewModel.selectFilter(ParentNoticeFilter.Urgent)

        val urgent = viewModel.state.value as ParentNoticesUiState.Loaded
        assertEquals(listOf("urgente"), urgent.sections.single().notices.map { it.noticeId })

        viewModel.selectFilter(ParentNoticeFilter.School)

        val school = viewModel.state.value as ParentNoticesUiState.Loaded
        assertEquals(listOf("escuela"), school.sections.single().notices.map { it.noticeId })
    }

    @Test
    fun `an empty inbox says where the next notice will appear`() = runTest(dispatcher) {
        val viewModel = viewModel(notices = emptyList())
        advanceUntilIdle()

        val state = viewModel.state.value as ParentNoticesUiState.Loaded
        assertTrue(state.sections.isEmpty())
        assertEquals("Estás al día", state.latestLabel)
        assertTrue(state.emptyMessage.isNotBlank())
    }

    @Test
    fun `a failed read is an error, not an empty inbox`() = runTest(dispatcher) {
        val viewModel = viewModel(notices = emptyList(), failure = RosterFailure.DENIED)
        advanceUntilIdle()

        assertEquals(
            ParentNoticesUiState.Failed("No tienes permiso para ver los avisos."),
            viewModel.state.value,
        )
    }

    private fun viewModel(
        notices: List<ParentNoticeItem>,
        failure: RosterFailure? = null,
    ) = ParentNoticesViewModel(
        selection = TestChildSelection(),
        parents = FakeNotices(notices = notices, failure = failure),
        now = { at(2026, 11, 18, 12) },
        zone = zone,
    )

    private fun notice(
        id: String,
        createdAt: Long,
        category: String = "grupo",
        urgent: Boolean = false,
        read: Boolean = false,
    ) = ParentNoticeItem(
        noticeId = id,
        title = "Aviso $id",
        body = "Cuerpo del aviso $id",
        category = category,
        urgent = urgent,
        createdByName = "Prof. Ricardo Salinas",
        createdAtMillis = createdAt,
        read = read,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, 0)
        }.timeInMillis
}

private class FakeNotices(
    private val notices: List<ParentNoticeItem>,
    private val failure: RosterFailure? = null,
    private val readResult: NoticeReadResult = NoticeReadResult.Marked,
) : ParentRepository {

    val readMarks = mutableListOf<String>()
    var childrenLoads = 0

    override suspend fun loadChildren(): ParentChildrenResult {
        childrenLoads++
        return ParentChildrenResult.Loaded(
            listOf(
                ParentChild(
                    studentId = "std-1",
                    fullName = "Mariana Gómez Ruiz",
                    firstName = "Mariana",
                    listNumber = 1,
                    groupId = "grp-3b",
                    groupLabel = "3° B",
                ),
            ),
        )
    }

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

    override suspend fun loadNotices(): ParentNoticesResult {
        failure?.let { return ParentNoticesResult.Failed(it) }
        val children = (loadChildren() as? ParentChildrenResult.Loaded)?.children.orEmpty()
        return ParentNoticesResult.Loaded(notices, children)
    }

    override suspend fun markNoticeRead(noticeId: String): NoticeReadResult {
        readMarks += noticeId
        return readResult
    }

    override suspend fun loadIncidents(): ParentIncidentsResult =
        ParentIncidentsResult.Loaded(emptyList())

    override suspend fun loadIncident(incidentId: String): ParentIncidentResult =
        ParentIncidentResult.NotFound

    override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult =
        IncidentSeenResult.Marked
}
