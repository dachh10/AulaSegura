package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.JustificationInboxRepository
import com.example.aulasegura.domain.repository.JustificationInboxResult
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.domain.repository.RosterRefreshResult
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The teacher's inbox asks for the groups the teacher actually teaches and never
 * turns a failed count into a failed screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherJustificationsViewModelTest {

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
    fun `the inbox asks for the groups the teacher teaches`() = runTest(dispatcher) {
        val inbox = FakeInbox(
            result = JustificationInboxResult.Loaded(listOf(record("just-1", JustificationStatus.PENDING))),
        )
        val groups = FakeGroups(listOf(group("3B"), group("4A")))

        val viewModel = TeacherJustificationsViewModel(groups, inbox, zone)
        advanceUntilIdle()

        assertEquals(listOf("3B", "4A"), inbox.requestedGroups)
        val state = viewModel.state.value as TeacherJustificationsUiState.Loaded
        assertEquals(1, state.pending.size)
        assertEquals(0, state.reviewed.size)
    }

    @Test
    fun `a reviewed report lands in the second section`() = runTest(dispatcher) {
        val inbox = FakeInbox(
            result = JustificationInboxResult.Loaded(
                listOf(
                    record("just-1", JustificationStatus.PENDING),
                    record("just-2", JustificationStatus.APPROVED),
                ),
            ),
        )

        val viewModel = TeacherJustificationsViewModel(FakeGroups(listOf(group("3B"))), inbox, zone)
        advanceUntilIdle()

        val state = viewModel.state.value as TeacherJustificationsUiState.Loaded
        assertEquals(listOf("just-1"), state.pending.map { it.id })
        assertEquals(listOf("just-2"), state.reviewed.map { it.id })
    }

    @Test
    fun `a denied read explains the permission instead of showing an empty inbox`() =
        runTest(dispatcher) {
            val inbox = FakeInbox(
                result = JustificationInboxResult.Failed(RosterFailure.DENIED),
            )

            val viewModel =
                TeacherJustificationsViewModel(FakeGroups(listOf(group("3B"))), inbox, zone)
            advanceUntilIdle()

            val state = viewModel.state.value as TeacherJustificationsUiState.Failed
            assertEquals("No tienes permiso para ver estas justificaciones.", state.message)
        }

    @Test
    fun `a lost session sends the teacher back to login`() = runTest(dispatcher) {
        val inbox = FakeInbox(result = JustificationInboxResult.SignedOut)

        val viewModel = TeacherJustificationsViewModel(FakeGroups(emptyList()), inbox, zone)
        advanceUntilIdle()

        val state = viewModel.state.value as TeacherJustificationsUiState.Failed
        assertEquals("Vuelve a iniciar sesión.", state.message)
    }

    @Test
    fun `a broken roster still lets the inbox answer`() = runTest(dispatcher) {
        val inbox = FakeInbox(
            result = JustificationInboxResult.Loaded(listOf(record("just-1", JustificationStatus.PENDING))),
        )
        val groups = FakeGroups(emptyList(), failure = IllegalStateException("room closed"))

        val viewModel = TeacherJustificationsViewModel(groups, inbox, zone)
        advanceUntilIdle()

        assertEquals(emptyList<String>(), inbox.requestedGroups)
        val state = viewModel.state.value as TeacherJustificationsUiState.Loaded
        assertTrue(state.pending.isNotEmpty())
    }

    private fun group(id: String) = Group(
        id = id,
        name = id,
        grade = 3,
        section = "B",
        schoolId = "school-1",
        titularTeacherId = "teacher-1",
        teacherIds = listOf("teacher-1"),
        studentCount = 20,
        cycle = "2026-2027",
    )

    private fun record(id: String, status: JustificationStatus) = JustificationRecord(
        justificationId = id,
        studentId = "alumno-1",
        studentName = "Mariana Gómez Ruiz",
        groupId = "3B",
        groupLabel = "3° B",
        dateFromMillis = 1_763_100_000_000L,
        reason = "enfermedad",
        description = "Fiebre",
        status = status,
        evidence = null,
        createdAtMillis = 1_763_100_000_000L,
        reviewedAtMillis = null,
        reviewNote = null,
        sessionId = "sesion-1",
    )
}

private class FakeGroups(
    private val groups: List<Group>,
    private val failure: Throwable? = null,
) : GroupRepository {

    override fun observeMyGroups(): Flow<List<Group>> =
        if (failure == null) flowOf(groups) else flow { throw failure }

    override suspend fun refreshMyGroups(): RosterRefreshResult =
        RosterRefreshResult.Updated(groups.size)
}

private class FakeInbox(
    private val result: JustificationInboxResult,
) : JustificationInboxRepository {

    var requestedGroups: List<String> = emptyList()
        private set

    override suspend fun loadForGroups(groupIds: List<String>): JustificationInboxResult {
        requestedGroups = groupIds
        return result
    }
}
