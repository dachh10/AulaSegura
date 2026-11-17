package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.IncidentEvidence
import com.example.aulasegura.domain.model.IncidentSummary
import kotlinx.coroutines.flow.Flow

/**
 * The teacher's own incident history: cache first, like the roll call history.
 *
 * [observeIncidents] answers from Room, so the list opens with no signal and
 * shows the drafts this phone has not managed to push yet. [refresh] is what
 * brings the rest from Firestore, and its failure never clears the cache.
 *
 * [observeEvidence] is separate on purpose: the list row does not need the files,
 * and reading them for every row would turn opening the screen into one query per
 * incident.
 */
interface IncidentHistoryRepository {

    fun observeIncidents(): Flow<List<IncidentSummary>>

    suspend fun refresh(): HistoryRefreshResult

    fun observeEvidence(incidentId: String): Flow<List<IncidentEvidence>>
}
