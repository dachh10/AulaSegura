package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminIncident
import com.example.aulasegura.domain.model.IncidentStatus

/**
 * One-shot incident inbox reads and status edits for the signed-in principal's school.
 *
 * No listener: on Spark a live snapshot would be billed for as long as the inbox is
 * open. The list is a snapshot; the only write is the status transition the
 * principal makes from the detail. Incidents are evidence and are never deleted.
 */
interface AdminIncidentRepository {
    suspend fun getIncidents(): AdminIncidentResult<List<AdminIncident>>

    suspend fun setStatus(
        incidentId: String,
        status: IncidentStatus,
    ): AdminIncidentResult<Unit>
}

/** Typed outcome, so the ViewModel never inspects an exception. */
sealed interface AdminIncidentResult<out T> {
    data class Success<T>(val data: T) : AdminIncidentResult<T>
    data class Failure(val reason: AdminIncidentError) : AdminIncidentResult<Nothing>
}

enum class AdminIncidentError {
    NO_SESSION,
    NETWORK,
    TIMEOUT,
    DENIED,
    UNKNOWN,
}
