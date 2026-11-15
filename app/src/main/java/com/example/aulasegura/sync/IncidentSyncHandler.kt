package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.mapper.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Writes one incident draft to `incidents/{incidentId}`.
 *
 * The outbox row's `entityLocalId` **is** the incident id, which is also the
 * Firestore document id: replaying the row overwrites the same document instead
 * of creating a second incident (`CONTEXT.md` §8.3).
 *
 * One `set()` and not a batch, unlike the roll call: an incident is a single
 * document, and its evidence (bloque 7) is uploaded to Cloudinary before this
 * write, so there is no second collection to keep consistent here.
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

        // Before the network call: if the process dies mid-write the row keeps
        // saying what actually happened instead of looking untouched.
        mark(draft.incidentId, SyncState.SYNCING)

        return try {
            firestore.collection(COLLECTION)
                .document(draft.incidentId)
                .set(draft.toFirestoreMap())
                .await()

            mark(draft.incidentId, SyncState.SYNCED)
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
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

    private suspend fun mark(incidentId: String, state: SyncState) {
        // Local bookkeeping only: the next pass rewrites the document anyway, so
        // a failure here has nothing to recover.
        runCatching { incidentDao.updateSyncState(incidentId, state) }
    }

    private companion object {
        const val COLLECTION = "incidents"
    }
}
