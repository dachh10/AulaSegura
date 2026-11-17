package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.domain.model.IncidentMediaStatus
import com.example.aulasegura.domain.repository.IncidentRepository
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `incident_created` is the only number the school uses to know whether teachers
 * report at all, so it must fire once per queued incident — not on a rejected one
 * and not on a local write that failed.
 */
class CreateIncidentUseCaseTest {

    private val incident = NewIncident(
        groupId = "grp-3b",
        studentId = "stu-1",
        studentName = "Mariana Gómez Ruiz",
        title = "Se cayó en el patio",
        description = "Se cayó en el patio",
        severity = IncidentSeverity.GRAVE,
        occurredAt = 1_763_136_000_000L,
    )

    private class FakeIncidentRepository(
        private val outcome: IncidentSaveOutcome,
    ) : IncidentRepository {

        var calls = 0
            private set

        override suspend fun create(incident: NewIncident): IncidentSaveOutcome {
            calls++
            return outcome
        }

        override fun observeSync(incidentId: String): Flow<SyncPhase> = emptyFlow()

    override fun observeMedia(incidentId: String): Flow<List<IncidentMediaStatus>> = emptyFlow()
    }

    @Test
    fun `a queued incident logs the event with its severity`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val repository = FakeIncidentRepository(IncidentSaveOutcome.Queued("inc-1"))
        val useCase = CreateIncidentUseCase(incidents = repository, analytics = { events += it })

        val outcome = useCase(incident)

        assertEquals(IncidentSaveOutcome.Queued("inc-1"), outcome)
        assertEquals(
            listOf<AnalyticsEvent>(AnalyticsEvent.IncidentCreated(severity = "grave", mediaCount = 0)),
            events,
        )
    }

    @Test
    fun `a failed local write logs nothing`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val repository = FakeIncidentRepository(IncidentSaveOutcome.Failed("grp-3b"))
        val useCase = CreateIncidentUseCase(incidents = repository, analytics = { events += it })

        useCase(incident)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `an incident without a student is rejected before touching storage`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val repository = FakeIncidentRepository(IncidentSaveOutcome.Queued("inc-1"))
        val useCase = CreateIncidentUseCase(incidents = repository, analytics = { events += it })

        val outcome = useCase(incident.copy(studentId = ""))

        assertEquals(IncidentSaveOutcome.NoStudent, outcome)
        assertEquals(0, repository.calls)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `an incident without a description is rejected before touching storage`() = runBlocking {
        val events = mutableListOf<AnalyticsEvent>()
        val repository = FakeIncidentRepository(IncidentSaveOutcome.Queued("inc-1"))
        val useCase = CreateIncidentUseCase(incidents = repository, analytics = { events += it })

        val outcome = useCase(incident.copy(description = "   "))

        assertEquals(IncidentSaveOutcome.EmptyDescription, outcome)
        assertEquals(0, repository.calls)
        assertTrue(events.isEmpty())
    }
}
