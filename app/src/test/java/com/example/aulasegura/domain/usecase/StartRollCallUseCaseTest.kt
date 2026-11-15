package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.RollCallDraft
import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.AttendanceStart
import com.example.aulasegura.domain.repository.SaveRollCallOutcome
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `roll_call_started` must fire when a session is actually opened, and only
 * then: logging it on a group that could not be resolved would inflate the
 * metric that decides whether teachers use the feature at all.
 */
class StartRollCallUseCaseTest {

    private val draft = RollCallDraft(
        sessionId = "session-uuid",
        groupId = "grp-3b",
        groupLabel = "3° B",
        schoolId = "sch-benito-juarez",
        teacherId = "uid-docente",
        subject = "Matemáticas",
        date = "2026-11-14",
        startedAt = 1_763_136_000_000L,
    )

    private class FakeAttendanceRepository(
        private val start: AttendanceStart,
    ) : AttendanceRepository {

        override suspend fun startDraft(groupId: String): AttendanceStart = start

        override suspend fun save(
            draft: RollCallDraft,
            entries: List<com.example.aulasegura.domain.model.RollCallEntry>,
        ): SaveRollCallOutcome = SaveRollCallOutcome.Queued
    }

    @Test
    fun `a ready draft logs the start event`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(AttendanceStart.Ready(draft)),
            analytics = { events += it },
        )

        val start = useCase("grp-3b")

        assertEquals(AttendanceStart.Ready(draft), start)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.RollCallStarted), events)
    }

    @Test
    fun `a group missing from the cache logs nothing`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(AttendanceStart.NoGroup("grp-3b")),
            analytics = { events += it },
        )

        useCase("grp-3b")

        assertTrue(events.isEmpty())
    }
}
