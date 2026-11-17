package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.data.mapper.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Writes one incident draft to `incidents/{incidentId}`.
 *
 * The outbox row's `entityLocalId` **is** the incident id, which is also the
 * Firestore document id: replaying the row overwrites the same document instead
 * of creating a second incident (`CONTEXT.md` §8.3).
 *
 * One `set()` and not a batch, unlike the roll call: an incident is a single
 * document. Its evidence is uploaded to Cloudinary by
 * [UploadIncidentMediaHandler] before this handler runs, so the `media` array is
 * complete by the time the document exists (`FASE1.md` 7.6).
 *
 * The rules only accept `status == 'nuevo'` on create, which is what the draft
 * carries. A later status change is the admin's job, not this handler's.
 */
class IncidentSyncHandler(
    private val incidentDao: IncidentDao,
    private val firestore: FirebaseFirestore,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val draft = incidentDao.findDraft(entry.entityLocalId)
            ?: return SyncOutcome.Permanent(
                "Incident ${entry.entityLocalId} is no longer stored locally",
            )

        // The queue order is what makes this true — the file rows are inserted
        // before this one — but it is re-checked here because a document written
        // while a file is still uploading would publish an incident whose `media`
        // array lost that photo without anybody noticing.
        val media = incidentDao.media(draft.incidentId)
        val uploading = media.filter {
            it.uploadState == UploadState.PENDING || it.uploadState == UploadState.UPLOADING
        }
        if (uploading.isNotEmpty()) {
            return SyncOutcome.Retry(
                "Incident ${draft.incidentId} still has ${uploading.size} evidence file(s) uploading",
            )
        }

        // Before the network call: if the process dies mid-write the row keeps
        // saying what actually happened instead of looking untouched.
        mark(draft.incidentId, SyncState.SYNCING)

        return try {
            firestore.collection(COLLECTION)
                .document(draft.incidentId)
                .set(draft.toFirestoreMap(media))
                .await()

            mark(draft.incidentId, SyncState.SYNCED)
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
            // A replay of a document that is already there is denied by the rules,
            // not accepted as an overwrite. Reading it back is the only way to
            // tell "already written" from "definitely rejected".
            if (denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                alreadyWritten(draft)
            ) {
                mark(draft.incidentId, SyncState.SYNCED)
                return SyncOutcome.Success
            }

            mark(draft.incidentId, SyncState.FAILED)
            val group = "grupo ${draft.groupId}"
            if (denied.isRetryable) {
                SyncOutcome.Retry(
                    "Firestore unavailable for the incident of $group (${denied.code})",
                    denied,
                )
            } else {
                SyncOutcome.Permanent(
                    "Firestore rejected the incident of $group (${denied.code})",
                    denied,
                )
            }
        }
    }

    /**
     * True when a previous pass already wrote this document and died before it
     * could clear the outbox row.
     *
     * `docs/firestore.rules` has no docente `update` clause, so that replay comes
     * back as `PERMISSION_DENIED`. Without this check the teacher would see a
     * failed incident that is in fact already in Firestore.
     */
    private suspend fun alreadyWritten(draft: IncidentDraftEntity): Boolean = runCatching {
        val snapshot = firestore.collection(COLLECTION)
            .document(draft.incidentId)
            .get(Source.SERVER)
            .await()
        snapshot.exists() && snapshot.getString("reportedBy") == draft.reportedBy
    }.getOrDefault(false)

    private suspend fun mark(incidentId: String, state: SyncState) {
        // Local bookkeeping only: the next pass rewrites the document anyway, so
        // a failure here has nothing to recover.
        runCatching { incidentDao.updateSyncState(incidentId, state) }
    }

    private companion object {
        const val COLLECTION = "incidents"
    }
}
