package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.JustificationDecision
import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationReview
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.repository.JustificationInboxRepository
import com.example.aulasegura.domain.repository.JustificationInboxResult
import com.example.aulasegura.domain.repository.JustificationReviewFailure
import com.example.aulasegura.domain.repository.JustificationReviewResult
import com.example.aulasegura.domain.repository.RosterFailure
import java.util.TimeZone
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * The principal's inbox reads the whole school and writes back only a decision.
 * The write must never leave a stale card on screen or a double-tap in the queue.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminJustificationsViewModelTest {

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
    fun `the principal reads the school and splits pending from reviewed`() =
        runTest(dispatcher) {
            val inbox = FakeInbox(
                JustificationInboxResult.Loaded(
                    listOf(
                        record("just-1", JustificationStatus.PENDING),
                        record("just-2", JustificationStatus.APPROVED),
                    ),
                ),
            )

            val viewModel = AdminJustificationsViewModel(inbox, zone)
            advanceUntilIdle()

            assertEquals(1, inbox.schoolReads)
            val state = viewModel.state.value
            assertEquals(listOf("just-1"), state.pending.map { it.id })
            assertEquals(listOf("just-2"), state.reviewed.map { it.id })
            assertEquals(1, state.pendingCount)
        }

    @Test
    fun `a denied read explains the permission`() = runTest(dispatcher) {
        val inbox = FakeInbox(JustificationInboxResult.Failed(RosterFailure.DENIED))

        val viewModel = AdminJustificationsViewModel(inbox, zone)
        advanceUntilIdle()

        assertEquals(
            "No tienes permiso para ver estas justificaciones.",
            viewModel.state.value.error,
        )
    }

    // Decision tests live in TeacherJustificationsViewModelTest: the principal
    // keeps a read-only inbox and no longer writes a review.

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

private class FakeInbox(
    private val result: JustificationInboxResult,
    private val reviewResult: JustificationReviewResult = JustificationReviewResult.Success,
    private val reviewGate: CompletableDeferred<Unit>? = null,
) : JustificationInboxRepository {

    var schoolReads: Int = 0
        private set
    val requests = mutableListOf<JustificationReview>()

    override suspend fun loadForGroups(groupIds: List<String>): JustificationInboxResult =
        throw UnsupportedOperationException("group reads belong to the teacher")

    override suspend fun loadForSchool(): JustificationInboxResult {
        schoolReads++
        return result
    }

    override suspend fun review(request: JustificationReview): JustificationReviewResult {
        reviewGate?.await()
        requests += request
        return reviewResult
    }
}
