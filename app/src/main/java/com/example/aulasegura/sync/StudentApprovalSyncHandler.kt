package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.StudentDao
import com.example.aulasegura.data.remote.tutorApprovalFirestoreUpdate
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.domain.repository.approvedStudentDocument
import com.example.aulasegura.domain.repository.tutorProfileOnApproval
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Replays a QR approval that was queued with no signal. It reproduces exactly
 * the same two-document batch as the online path: the student document and the
 * tutor profile links. Idempotent: if the document is already there (a retry
 * after a killed process), success is reported instead of a duplicate write.
 */
class StudentApprovalSyncHandler(
    private val studentDao: StudentDao,
    private val firestore: FirebaseFirestore,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val approval = decodeStudentApproval(entry.payloadJson)
            ?: return SyncOutcome.Permanent("Corrupt student approval payload")
        val studentId = approval.studentId

        mark(studentId, SyncState.SYNCING)

        val input = approval.toInput()
        val document = approvedStudentDocument(input, approval.schoolId).toMutableMap().apply {
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }

        return try {
            val batch = firestore.batch()
            if (approval.duplicateStudentId.isNullOrBlank()) {
                batch.set(firestore.collection(COLLECTION).document(studentId), document)
            } else {
                val existing = firestore.collection(COLLECTION).document(approval.duplicateStudentId)
                batch.update(
                    existing,
                    mapOf(
                        "tutorIds" to FieldValue.arrayUnion(input.tutorUid),
                        "tutores" to FieldValue.arrayUnion(input.tutorUid),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            // Same additive links as the online path: a replacement list here
            // would silently drop the first child's group on replay too.
            val tutorUpdate = tutorApprovalFirestoreUpdate(
                tutorProfileOnApproval(
                    studentId = approval.duplicateStudentId ?: studentId,
                    groupId = input.groupId,
                ),
            )
            tutorUpdate["updatedAt"] = FieldValue.serverTimestamp()
            batch.update(firestore.collection("users").document(input.tutorUid), tutorUpdate)
            batch.commit().await()

            mark(studentId, SyncState.SYNCED)
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
            if (denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED &&
                alreadyWritten(approval.duplicateStudentId ?: studentId, input.tutorUid)
            ) {
                mark(studentId, SyncState.SYNCED)
                return SyncOutcome.Success
            }
            mark(studentId, SyncState.FAILED)
            if (denied.isRetryable) {
                SyncOutcome.Retry("Firestore unavailable for student approval (${denied.code})", denied)
            } else {
                SyncOutcome.Permanent("Firestore rejected the student approval (${denied.code})", denied)
            }
        } catch (error: Exception) {
            mark(studentId, SyncState.FAILED)
            SyncOutcome.Retry("Student approval failed: ${error.message}", error)
        }
    }

    /** A retry after a crash may find the document already written. */
    private suspend fun alreadyWritten(studentId: String, tutorUid: String): Boolean = runCatching {
        val snapshot = firestore.collection(COLLECTION).document(studentId)
            .get(Source.SERVER).await()
        snapshot.exists() &&
            (snapshot.get("tutorIds") as? List<*>)?.contains(tutorUid) == true
    }.getOrDefault(false)

    private suspend fun mark(studentId: String, state: SyncState) {
        runCatching { studentDao.updateSyncStatus(studentId, state) }
    }

    private companion object {
        const val COLLECTION = "students"
    }
}
