package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.NoticeDao
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.mapper.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Writes one cached notice to `notices/{noticeId}`.
 *
 * Same idempotence argument as [TaskSyncHandler]: the cached notice id is the
 * document id and `notices` has an update clause for its author, so replaying the
 * row rewrites the same document instead of publishing a second notice.
 *
 * What the rules check on create is the shape this handler sends: `schoolId`,
 * `createdBy`, and an `audience.groupIds` that is a non-empty subset of the
 * teacher's groups. A denial is a permanent failure, not something a retry fixes.
 */
class NoticeSyncHandler(
    private val noticeDao: NoticeDao,
    private val firestore: FirebaseFirestore,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val notice = noticeDao.findById(entry.entityLocalId)
            ?: return SyncOutcome.Permanent(
                "Notice ${entry.entityLocalId} is no longer stored locally",
            )

        return try {
            firestore.collection(COLLECTION)
                .document(notice.noticeId)
                .set(notice.toFirestoreMap())
                .await()
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
            if (denied.isRetryable) {
                SyncOutcome.Retry(
                    "Firestore unavailable for notice ${notice.noticeId} (${denied.code})",
                    denied,
                )
            } else {
                SyncOutcome.Permanent(
                    "Firestore rejected notice ${notice.noticeId} (${denied.code})",
                    denied,
                )
            }
        }
    }

    private companion object {
        const val COLLECTION = "notices"
    }
}
