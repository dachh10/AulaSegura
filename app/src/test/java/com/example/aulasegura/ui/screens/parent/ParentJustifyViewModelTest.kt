package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.JustificationDraft
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.repository.IncidentSeenResult
import com.example.aulasegura.domain.repository.JustificationRepository
import com.example.aulasegura.domain.repository.JustificationResult
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
import com.example.aulasegura.util.AttendanceTimes
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
 * The justification form has one rule worth pinning: it never invents a date.
 * Everything here runs without Firebase — the ViewModel only sees the domain
 * contracts, which is exactly why the absence can be a fixed October day.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentJustifyViewModelTest {

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
    fun `without an absence in two months the form cannot be sent`() = runTest(dispatcher) {
        val parents = FakeParentRepository(
            months = mapOf(
                "2026-11" to month(2026, 11, day("2026-11-18", AttendanceMark.PRESENT)),
                "2026-10" to month(2026, 10, day("2026-10-15", AttendanceMark.PRESENT)),
            ),
        )

        val viewModel = viewModel(parents)
        advanceUntilIdle()

        val state = viewModel.state.value as ParentJustifyUiState.Loaded
        assertNull(state.absence)
        assertNotNull("the card has to explain why there is nothing to send", state.emptyMessage)
        assertFalse(state.canSend)
    }

    @Test
    fun `the newest absence of the month is the one justified`() = runTest(dispatcher) {
        val parents = FakeParentRepository(
            months = mapOf(
                "2026-11" to month(
                    2026,
                    11,
                    day("2026-11-19", AttendanceMark.ABSENT),
                    day("2026-11-12", AttendanceMark.ABSENT),
                ),
            ),
        )

        val viewModel = viewModel(parents)
        advanceUntilIdle()

        val state = viewModel.state.value as ParentJustifyUiState.Loaded
        assertEquals("2026-11-19", state.absence?.isoDate)
        assertEquals("Jueves 19 de noviembre", state.absence?.dateLabel)
        assertTrue(state.canSend)
    }

    @Test
    fun `the absence touched in the attendance detail wins over the newest one`() =
        runTest(dispatcher) {
            val parents = FakeParentRepository(
                months = mapOf(
                    "2026-11" to month(
                        2026,
                        11,
                        day("2026-11-19", AttendanceMark.ABSENT),
                        day("2026-11-12", AttendanceMark.ABSENT),
                    ),
                ),
            )

            val viewModel = viewModel(parents, absenceIso = "2026-11-12")
            advanceUntilIdle()

            val state = viewModel.state.value as ParentJustifyUiState.Loaded
            assertEquals("2026-11-12", state.absence?.isoDate)
        }

    @Test
    fun `a touched day that is not absent does not become a justification`() =
        runTest(dispatcher) {
            val parents = FakeParentRepository(
                months = mapOf(
                    "2026-11" to month(
                        2026,
                        11,
                        day("2026-11-18", AttendanceMark.PRESENT),
                    ),
                ),
            )

            val viewModel = viewModel(parents, absenceIso = "2026-11-18")
            advanceUntilIdle()

            val state = viewModel.state.value as ParentJustifyUiState.Loaded
            assertNull(state.absence)
            assertEquals(
                "Esa falta ya no está registrada en la asistencia.",
                state.emptyMessage,
            )
            assertFalse(state.canSend)
        }

    @Test
    fun `the previous month is searched when this one has no absence`() = runTest(dispatcher) {
        val parents = FakeParentRepository(
            months = mapOf(
                "2026-11" to month(2026, 11, day("2026-11-18", AttendanceMark.PRESENT)),
                "2026-10" to month(2026, 10, day("2026-10-30", AttendanceMark.ABSENT)),
            ),
        )

        val viewModel = viewModel(parents)
        advanceUntilIdle()

        val state = viewModel.state.value as ParentJustifyUiState.Loaded
        assertEquals("2026-10-30", state.absence?.isoDate)
    }

    @Test
    fun `sending queues the draft with the real date and the schema reason`() =
        runTest(dispatcher) {
            val parents = FakeParentRepository(
                months = mapOf(
                    "2026-11" to month(2026, 11, day("2026-11-19", AttendanceMark.ABSENT)),
                ),
            )
            val justifications = FakeJustificationRepository()

            val viewModel = viewModel(parents, justifications = justifications)
            advanceUntilIdle()

            viewModel.selectReason(JustificationReason.APPOINTMENT)
            viewModel.updateDetails("  Cita en el IMSS  ")
            viewModel.attach("receta.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
            viewModel.send()
            advanceUntilIdle()

            val draft = justifications.lastDraft
            assertNotNull(draft)
            assertEquals("std-1", draft?.studentId)
            assertEquals("grp-3b", draft?.groupId)
            assertEquals("cita", draft?.reasonCode)
            assertEquals("Cita en el IMSS", draft?.description)
            assertEquals(
                "the school receives the day it marked absent, not the day the tutor sent",
                AttendanceTimes.isoDateToMillis("2026-11-19", zone),
                draft?.absenceDateMillis,
            )
            assertEquals("receta.jpg", draft?.attachment?.fileName)
            assertEquals(3, draft?.attachment?.bytes?.size ?: 0)

            val state = viewModel.state.value as ParentJustifyUiState.Loaded
            assertTrue(state.sent)
            assertFalse("a queued report cannot be sent twice", state.canSend)
        }

    @Test
    fun `a receipt that cannot be stored keeps the form open`() = runTest(dispatcher) {
        val parents = FakeParentRepository(
            months = mapOf(
                "2026-11" to month(2026, 11, day("2026-11-19", AttendanceMark.ABSENT)),
            ),
        )
        val justifications = FakeJustificationRepository(
            result = JustificationResult.StorageFailed,
        )

        val viewModel = viewModel(parents, justifications = justifications)
        advanceUntilIdle()
        viewModel.attach("receta.jpg", "image/jpeg", byteArrayOf(1))
        viewModel.send()
        advanceUntilIdle()

        val state = viewModel.state.value as ParentJustifyUiState.Loaded
        assertNotNull(state.error)
        assertFalse(state.sent)
        assertTrue("nothing was queued, so the tutor can try again", state.canSend)
    }

    @Test
    fun `a read failure offers a retry instead of an invented date`() = runTest(dispatcher) {
        val parents = FakeParentRepository(
            months = emptyMap(),
            failure = RosterFailure.OFFLINE,
        )

        val viewModel = viewModel(parents)
        advanceUntilIdle()

        assertEquals(
            ParentJustifyUiState.Failed("Sin conexión. Revisa tu red e intenta de nuevo."),
            viewModel.state.value,
        )
    }

    @Test
    fun `family reasons travel as otro because the schema has no such value`() {
        assertEquals("enfermedad", JustificationReason.ILLNESS.code)
        assertEquals("cita", JustificationReason.APPOINTMENT.code)
        assertEquals("otro", JustificationReason.FAMILY.code)
        assertEquals("otro", JustificationReason.OTHER.code)
        assertEquals("Asunto familiar", JustificationReason.FAMILY.label)
    }

    private fun viewModel(
        parents: ParentRepository,
        justifications: JustificationRepository = FakeJustificationRepository(),
        absenceIso: String? = null,
    ) = ParentJustifyViewModel(
        parents = parents,
        justifications = justifications,
        studentId = "std-1",
        absenceIso = absenceIso,
        now = { at(2026, 11, 20) },
        zone = zone,
    )

    private fun at(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(zone).apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}

private class FakeJustificationRepository(
    private val result: JustificationResult = JustificationResult.Queued,
) : JustificationRepository {

    var lastDraft: JustificationDraft? = null

    override suspend fun submit(draft: JustificationDraft): JustificationResult {
        lastDraft = draft
        return result
    }
}

private class FakeParentRepository(
    private val months: Map<String, ParentMonthAttendance>,
    private val failure: RosterFailure? = null,
) : ParentRepository {

    override suspend fun loadChildren(): ParentChildrenResult =
        ParentChildrenResult.Loaded(listOf(CHILD))

    override suspend fun loadMonth(
        child: ParentChild,
        year: Int,
        month: Int,
    ): ParentMonthResult {
        failure?.let { return ParentMonthResult.Failed(it) }
        val found = months["$year-$month"]
            ?: ParentMonthAttendance(year, month, ParentAttendanceCounts(), emptyList())
        return ParentMonthResult.Loaded(found)
    }

    override suspend fun loadHome(): ParentHomeResult = ParentHomeResult.Loaded(emptyList())

    override suspend fun loadTasks(child: ParentChild): ParentTasksResult =
        ParentTasksResult.Loaded(emptyList())

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

private fun day(date: String, mark: AttendanceMark?) = ParentDayAttendance(
    date = date,
    subject = "Matemáticas",
    startedAt = 0L,
    mark = mark,
    time = null,
    note = null,
)

private fun month(year: Int, month: Int, vararg days: ParentDayAttendance) =
    ParentMonthAttendance(
        year = year,
        month = month,
        counts = ParentAttendanceCounts(),
        days = days.toList(),
    )
