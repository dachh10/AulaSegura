package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.data.mapper.toMediaUploadPhase
import com.example.aulasegura.data.mapper.toRollCallSync
import com.example.aulasegura.domain.model.IncidentMediaStatus
import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.IncidentRepository
import com.example.aulasegura.sync.OutboxRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The incident write path: Room first, Firestore later.
 *
 * Nothing here talks to the network. The draft and its outbox row are written in
 * one transaction, so an incident the teacher sees as saved on screen is
 * guaranteed to be queued — and one saved in airplane mode reaches the cloud when
 * the worker runs, once, under the client UUID.
 *
 * The names that the tutor and the admin will read travel denormalised
 * (`studentName`, `reportedByName`): the tutor cannot read the school roster, so
 * a document that only carries ids would render as a blank row for them.
 */
class RoomIncidentRepository(
    private val incidentDao: IncidentDao,
    private val outbox: OutboxRepository,
    private val groups: GroupRepository,
    private val auth: AuthRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newIncidentId: () -> String = { UUID.randomUUID().toString() },
    private val newMediaId: () -> String = { UUID.randomUUID().toString() },
) : IncidentRepository {

    override suspend fun create(incident: NewIncident): IncidentSaveOutcome {
        val teacher = auth.currentUser ?: return IncidentSaveOutcome.SignedOut

        // Resolved from the cache, not from the caller: `schoolId` has to be the
        // one the rules expect, and the group proves the teacher teaches it.
        val group = groups.observeMyGroups().first()
            .firstOrNull { it.id == incident.groupId }
            ?: return IncidentSaveOutcome.NoGroup(incident.groupId)

        val now = clock()
        val draft = IncidentDraftEntity(
            incidentId = newIncidentId(),
            schoolId = group.schoolId,
            groupId = group.id,
            studentId = incident.studentId,
            studentName = incident.studentName,
            title = incident.title,
            description = incident.description,
            severity = incident.severity.wire,
            category = incident.category.wire,
            reportedBy = teacher.uid,
            reportedByName = teacher.fullName,
            occurredAt = incident.occurredAt,
            locationLat = incident.location?.latitude,
            locationLng = incident.location?.longitude,
            locationAccuracy = incident.location?.accuracyMeters,
            locationDistanceMeters = incident.location?.distanceMeters,
            locationVerified = incident.location?.verified,
            status = STATUS_NEW,
            syncState = SyncState.PENDING,
            createdAt = now,
            updatedAt = now,
        )

        return try {
            // The evidence rows are built here and not in the use case: the
            // incident id they hang from is minted two lines above.
            val media = incident.media.map { file ->
                IncidentMediaEntity(
                    mediaId = newMediaId(),
                    incidentId = draft.incidentId,
                    type = file.type,
                    localPath = file.localPath,
                    uploadState = UploadState.PENDING,
                    createdAt = now,
                    updatedAt = now,
                )
            }
            outbox.enqueueIncident(draft, media)
            IncidentSaveOutcome.Queued(draft.incidentId)
        } catch (failed: Exception) {
            // Room is the local source of truth: if the transaction did not
            // commit, nothing changed and the teacher can retry the same screen.
            IncidentSaveOutcome.Failed(group.id)
        }
    }

    override fun observeSync(incidentId: String): Flow<SyncPhase> =
        incidentDao.observeDraft(incidentId)
            .filterNotNull()
            .map { it.syncState.toRollCallSync() }

    override fun observeMedia(incidentId: String): Flow<List<IncidentMediaStatus>> =
        incidentDao.observeMedia(incidentId).map { files ->
            files.map { file ->
                IncidentMediaStatus(
                    mediaId = file.mediaId,
                    localPath = file.localPath,
                    phase = file.uploadState.toMediaUploadPhase(),
                )
            }
        }

    private companion object {
        const val STATUS_NEW = "nuevo"
    }
}
