package com.example.aulasegura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState
import kotlinx.coroutines.flow.Flow

/**
 * Incident drafts and their evidence files.
 *
 * Drafts are written here first and pushed to Firestore by the sync worker, so
 * the pending reads are what the outbox ultimately walks. Media uploads go to
 * Cloudinary, so their state machine is separate from the draft's.
 */
@Dao
interface IncidentDao {

    @Query("SELECT * FROM incident_drafts WHERE groupId = :groupId ORDER BY occurredAt DESC")
    fun observeDraftsByGroup(groupId: String): Flow<List<IncidentDraftEntity>>

    @Query(
        "SELECT * FROM incident_drafts WHERE schoolId = :schoolId ORDER BY occurredAt DESC",
    )
    fun observeDraftsBySchool(schoolId: String): Flow<List<IncidentDraftEntity>>

    @Query("SELECT * FROM incident_drafts WHERE incidentId = :incidentId LIMIT 1")
    suspend fun findDraft(incidentId: String): IncidentDraftEntity?

    /**
     * The one draft the screen follows after saving, so the strip can show its
     * real phase instead of promising a sync in the background.
     */
    @Query("SELECT * FROM incident_drafts WHERE incidentId = :incidentId LIMIT 1")
    fun observeDraft(incidentId: String): Flow<IncidentDraftEntity?>

    @Query("SELECT * FROM incident_drafts WHERE syncState = :state")
    suspend fun draftsWithState(state: SyncState): List<IncidentDraftEntity>

    @Upsert
    suspend fun upsertDraft(draft: IncidentDraftEntity)

    /** The handler's one-column bookkeeping; no need to rewrite the whole draft. */
    @Query("UPDATE incident_drafts SET syncState = :state WHERE incidentId = :incidentId")
    suspend fun updateSyncState(incidentId: String, state: SyncState)

    @Query("SELECT * FROM incident_media WHERE incidentId = :incidentId")
    fun observeMedia(incidentId: String): Flow<List<IncidentMediaEntity>>

    @Query("SELECT * FROM incident_media WHERE incidentId = :incidentId")
    suspend fun media(incidentId: String): List<IncidentMediaEntity>

    @Query("SELECT * FROM incident_media WHERE uploadState = :state ORDER BY createdAt")
    suspend fun mediaWithState(state: UploadState): List<IncidentMediaEntity>

    /**
     * The upload handler's entry point: one outbox row carries the media id, so
     * the row itself is the only thing the handler needs to look up.
     */
    @Query("SELECT * FROM incident_media WHERE mediaId = :mediaId LIMIT 1")
    suspend fun findMedia(mediaId: String): IncidentMediaEntity?

    @Upsert
    suspend fun upsertMedia(media: IncidentMediaEntity)

    @Upsert
    suspend fun upsertMediaList(media: List<IncidentMediaEntity>)

    @Query("DELETE FROM incident_media WHERE mediaId = :mediaId")
    suspend fun deleteMedia(mediaId: String)

    @Query("DELETE FROM incident_drafts WHERE incidentId = :incidentId")
    suspend fun deleteDraft(incidentId: String)

    @Query("DELETE FROM incident_drafts")
    suspend fun clearDrafts()

    @Query("DELETE FROM incident_media")
    suspend fun clearMedia()
}
