package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.repository.IncidentRepository
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger

/**
 * Validates an incident before anything is written, then queues it.
 *
 * The two checks are not UI niceties: the schema has no anonymous incident and no
 * incident without a description, and a document that violates that is one the
 * tutor cannot understand and the admin cannot triage. Returning `NoStudent` or
 * `EmptyDescription` keeps the reason in the domain instead of in the screen.
 *
 * `incident_created` fires only when the row is actually queued, and only once:
 * a retry of the outbox does not create a new incident, so it must not count as
 * a second creation either.
 */
class CreateIncidentUseCase(
    private val incidents: IncidentRepository,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) {

    suspend operator fun invoke(
        incident: NewIncident,
        mediaCount: Int = 0,
    ): IncidentSaveOutcome {
        if (incident.studentId.isBlank()) return IncidentSaveOutcome.NoStudent
        if (incident.description.isBlank()) return IncidentSaveOutcome.EmptyDescription

        val outcome = incidents.create(incident)
        if (outcome is IncidentSaveOutcome.Queued) {
            analytics.log(AnalyticsEvent.IncidentCreated(incident.severity.wire, mediaCount))
        }
        return outcome
    }
}
