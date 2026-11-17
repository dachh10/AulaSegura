package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.IncidentMediaStatus
import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.model.SyncPhase
import kotlinx.coroutines.flow.Flow

/**
 * Write side of the incidents.
 *
 * Same shape as [AttendanceRepository]: nothing here talks to the network. An
 * incident becomes a Room draft plus an outbox row in one transaction, and
 * `IncidentSyncHandler` is what puts it in Firestore later — which is what makes
 * "created in airplane mode" a normal case instead of an error path.
 *
 * The incident id is minted on the client (`UUID`) so a retry overwrites the same
 * document instead of creating a second incident (`CONTEXT.md` §8.3).
 */
interface IncidentRepository {

    /** Persists [incident] locally and queues it. */
    suspend fun create(incident: NewIncident): IncidentSaveOutcome

    /** Follows one draft while it waits for Firestore. */
    fun observeSync(incidentId: String): Flow<SyncPhase>

    /**
     * Follows the evidence files of one incident while they wait for Cloudinary.
     *
     * Separate from [observeSync] because the two finish at different times: the
     * files upload first and the document is written afterwards, so a screen that
     * only watched the draft would show "syncing" with no idea which photo failed.
     */
    fun observeMedia(incidentId: String): Flow<List<IncidentMediaStatus>>
}
