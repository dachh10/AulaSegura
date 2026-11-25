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
        subjectId = "g3-matematicas-iii",
        date = "2026-11-14",
        startedAt = 1_763_136_000_000L,
    )

    private class FakeAttendanceRepository(
        private val start: AttendanceStart,
        private val onStart: (String, String) -> Unit = { _, _ -> },
        private val alreadyTakenId: String? = null,
        private val onLookup: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    ) : AttendanceRepository {

        override suspend fun startDraft(groupId: String, subjectId: String): AttendanceStart {
            onStart(groupId, subjectId)
            return start
        }

        override suspend fun existingSessionId(
            groupId: String,
            date: String,
            teacherId: String,
            subjectKey: String,
        ): String? {
            onLookup(groupId, date, teacherId, subjectKey)
            return alreadyTakenId
        }

        override suspend fun save(
            draft: RollCallDraft,
            entries: List<com.example.aulasegura.domain.model.RollCallEntry>,
        ): SaveRollCallOutcome = SaveRollCallOutcome.Queued

        override fun observeSync(
            sessionId: String,
        ): kotlinx.coroutines.flow.Flow<com.example.aulasegura.domain.model.RollCallSync> =
            kotlinx.coroutines.flow.emptyFlow()
    }

    @Test
    fun `a ready draft logs the start event`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(AttendanceStart.Ready(draft)),
            analytics = { events += it },
        )

        val start = useCase("grp-3b", "g3-matematicas-iii")

        assertEquals(AttendanceStart.Ready(draft), start)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.RollCallStarted), events)
    }

    @Test
    fun `the canonical subject id reaches the repository`() = runBlocking {
        var seen: Pair<String, String>? = null
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(
                AttendanceStart.Ready(draft),
                onStart = { group, subject -> seen = group to subject },
            ),
        )

        useCase("grp-3b", "g3-matematicas-iii")

        assertEquals("grp-3b" to "g3-matematicas-iii", seen)
    }

    @Test
    fun `a group missing from the cache logs nothing`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(AttendanceStart.NoGroup("grp-3b")),
            analytics = { events += it },
        )

        useCase("grp-3b", "g3-matematicas-iii")

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a session already closed for this slot blocks a new start`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(
                AttendanceStart.Ready(draft),
                alreadyTakenId = "session-closed",
            ),
            analytics = { events += it },
        )

        val start = useCase("grp-3b", "g3-matematicas-iii")

        assertEquals(
            AttendanceStart.AlreadyTaken("session-closed", "3° B", "Matemáticas"),
            start,
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun `the slot check uses group date teacher and subject label`() = runBlocking {
        var seen: List<String>? = null
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(
                AttendanceStart.Ready(draft),
                onLookup = { group, date, teacher, subject ->
                    seen = listOf(group, date, teacher, subject)
                },
            ),
        )

        useCase("grp-3b", "g3-matematicas-iii")

        assertEquals(
            listOf("grp-3b", "2026-11-14", "uid-docente", "Matemáticas"),
            seen,
        )
    }

    @Test
    fun `a legacy draft checks the same durable subject label`() = runBlocking {
        val legacy = draft.copy(subjectId = "")
        var seenSubjectKey: String? = null
        val useCase = StartRollCallUseCase(
            attendance = FakeAttendanceRepository(
                AttendanceStart.Ready(legacy),
                onLookup = { _, _, _, subject -> seenSubjectKey = subject },
            ),
        )

        useCase("grp-3b", "")

        assertEquals("Matemáticas", seenSubjectKey)
    }
}
