package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.TaskDao
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.mapper.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Writes one cached task to `tasks/{taskId}`.
 *
 * The outbox row's `entityLocalId` **is** the task id, which is also the
 * Firestore document id, so a replay overwrites the same document instead of
 * publishing the task twice.
 *
 * Unlike the incident, no denied-replay reconciliation is needed: `tasks` has an
 * update clause for the docente that created the document, so a second `set()`
 * on the same id is accepted by the rules. A `PERMISSION_DENIED` here means the
 * create rule genuinely refused it — usually because the teacher no longer
 * teaches one of the groups — and retrying cannot fix that.
 *
 * There is no local sync column to update: the cache row is also the read model,
 * and the outbox row itself is what the history reads to show "pending".
 */
class TaskSyncHandler(
    private val taskDao: TaskDao,
    private val firestore: FirebaseFirestore,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val task = taskDao.findById(entry.entityLocalId)
            ?: return SyncOutcome.Permanent(
                "Task ${entry.entityLocalId} is no longer stored locally",
            )

        return try {
            firestore.collection(COLLECTION)
                .document(task.taskId)
                .set(task.toFirestoreMap())
                .await()
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
            if (denied.isRetryable) {
                SyncOutcome.Retry(
                    "Firestore unavailable for task ${task.taskId} (${denied.code})",
                    denied,
                )
            } else {
                SyncOutcome.Permanent(
                    "Firestore rejected task ${task.taskId} (${denied.code})",
                    denied,
                )
            }
        }
    }

    private companion object {
        const val COLLECTION = "tasks"
    }
}
