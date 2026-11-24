package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.JustificationDecision
import com.example.aulasegura.domain.model.JustificationEvidence
import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationReview
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.JustificationInboxRepository
import com.example.aulasegura.domain.repository.JustificationInboxResult
import com.example.aulasegura.domain.repository.JustificationReviewFailure
import com.example.aulasegura.domain.repository.JustificationReviewResult
import com.example.aulasegura.domain.repository.RosterFailure
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.FirebaseNetworkException
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Online inbox of the tutor's justifications.
 *
 * Reads `Source.SERVER` on purpose: an empty cached snapshot would read as "no
 * justifications", the one answer that would hide a family's report.
 *
 * The rules authorize a teacher by `groupId in myGroups()` and the principal by
 * `schoolId == mySchool()`. Every query here uses exactly one of those clauses,
 * which keeps it provable without a composite index.
 */
class FirestoreJustificationInboxRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : JustificationInboxRepository {

    override suspend fun loadForGroups(groupIds: List<String>): JustificationInboxResult {
        auth.currentUser ?: return JustificationInboxResult.SignedOut
        val distinct = groupIds.filter { it.isNotBlank() }.distinct()
        if (distinct.isEmpty()) return JustificationInboxResult.Loaded(emptyList())

        val outcomes = coroutineScope {
            distinct.map { groupId ->
                async(Dispatchers.IO) { runCatching { loadGroup(groupId) } }
            }.awaitAll()
        }

        // A denied group is a stale id in the profile, not a broken inbox: the
        // groups the rules do accept still answer. Only "every group failed"
        // becomes a screen error, and it keeps the reason of the first failure.
        val documents = outcomes.flatMap { it.getOrDefault(emptyList()) }
        if (documents.isEmpty() && outcomes.all { it.isFailure }) {
            val error = outcomes.firstNotNullOfOrNull { it.exceptionOrNull() }
            return JustificationInboxResult.Failed(
                (error as? Exception)?.toRosterFailure()
                    ?: com.example.aulasegura.domain.repository.RosterFailure.UNKNOWN,
            )
        }

        val records = documents
            .distinctBy { it.id }
            .mapNotNull { document ->
                document.data?.let { justificationFrom(document.id, it) }
            }
            .sortedByDescending { it.createdAtMillis }

        return JustificationInboxResult.Loaded(records)
    }

    override suspend fun loadForSchool(): JustificationInboxResult {
        val user = auth.currentUser ?: return JustificationInboxResult.SignedOut
        // The rule grants the school-wide read to a principal only. Asking as
        // anyone else would be denied by the backend, but saying it here keeps
        // the screen message honest and saves a round trip.
        if (user.role != UserRole.ADMIN) {
            return JustificationInboxResult.Failed(RosterFailure.DENIED)
        }

        val documents = try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(COLLECTION)
                    .whereEqualTo(FIELD_SCHOOL_ID, user.schoolId)
                    .limit(QUERY_LIMIT)
                    .get(Source.SERVER)
                    .await()
                    .documents
            }
        } catch (error: Exception) {
            return JustificationInboxResult.Failed(error.toRosterFailure())
        }

        val records = documents
            .mapNotNull { document -> document.data?.let { justificationFrom(document.id, it) } }
            .sortedByDescending { it.createdAtMillis }
        return JustificationInboxResult.Loaded(records)
    }

    override suspend fun review(request: JustificationReview): JustificationReviewResult {
        val user = auth.currentUser ?: return JustificationReviewResult.SignedOut
        // Same guard as the read: the rules are the authority, this is the
        // explanation the screen can print without waiting for a denial.
        if (user.role != UserRole.ADMIN) {
            return JustificationReviewResult.Failed(JustificationReviewFailure.DENIED)
        }
        val id = request.justificationId
        if (id.isBlank()) {
            return JustificationReviewResult.Failed(JustificationReviewFailure.NOT_FOUND)
        }

        val status = when (request.decision) {
            JustificationDecision.APPROVE -> JustificationStatus.APPROVED
            JustificationDecision.REJECT -> JustificationStatus.REJECTED
        }.wire

        // Exactly the four fields firestore.rules allows in changedKeys(). A
        // `set()` here would overwrite the tutor's report, so `update()` is the
        // only safe write.
        val changes = mapOf<String, Any>(
            FIELD_STATUS to status,
            FIELD_REVIEWED_BY to user.uid,
            FIELD_REVIEWED_AT to FieldValue.serverTimestamp(),
            FIELD_REVIEW_NOTE to request.note.trim(),
        )
        return try {
            withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.collection(COLLECTION).document(id).update(changes).await()
            }
            JustificationReviewResult.Success
        } catch (error: Exception) {
            JustificationReviewResult.Failed(error.toReviewFailure())
        }
    }

    private suspend fun loadGroup(groupId: String): List<DocumentSnapshot> =
        withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(COLLECTION)
                .whereEqualTo(FIELD_GROUP_ID, groupId)
                .limit(QUERY_LIMIT)
                .get(Source.SERVER)
                .await()
                .documents
        }

    private companion object {
        const val COLLECTION = "justifications"
        const val FIELD_GROUP_ID = "groupId"
        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_STATUS = "status"
        const val FIELD_REVIEWED_BY = "reviewedBy"
        const val FIELD_REVIEWED_AT = "reviewedAt"
        const val FIELD_REVIEW_NOTE = "reviewNote"
        const val READ_TIMEOUT_MILLIS = 15_000L
        const val WRITE_TIMEOUT_MILLIS = 15_000L

        /** A group's term, not a download. */
        const val QUERY_LIMIT = 100L
    }
}

/**
 * A failed decision has to say whether retrying could help. Kept as a pure
 * mapping so the message a principal sees is pinned by a test, not guessed from
 * an exception type at the call site.
 */
internal fun Exception.toReviewFailure(): JustificationReviewFailure = when {
    this is TimeoutCancellationException -> JustificationReviewFailure.OFFLINE
    this is FirebaseNetworkException -> JustificationReviewFailure.OFFLINE
    this is FirebaseFirestoreException && (
        code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        ) -> JustificationReviewFailure.OFFLINE

    this is FirebaseFirestoreException && (
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            code == FirebaseFirestoreException.Code.UNAUTHENTICATED
        ) -> JustificationReviewFailure.DENIED

    this is FirebaseFirestoreException &&
        code == FirebaseFirestoreException.Code.NOT_FOUND -> JustificationReviewFailure.NOT_FOUND

    else -> JustificationReviewFailure.UNKNOWN
}

/**
 * Maps `justifications/{id}` to the inbox model.
 *
 * Tolerant by design: a malformed document is skipped, and a missing status is
 * treated as pending so an old report never becomes invisible.
 */
internal fun justificationFrom(
    id: String,
    data: Map<String, Any?>,
): JustificationRecord? {
    val studentId = data["studentId"] as? String ?: return null
    val groupId = data["groupId"] as? String ?: return null

    return JustificationRecord(
        justificationId = id,
        studentId = studentId,
        studentName = data["studentName"] as? String ?: "",
        groupId = groupId,
        groupLabel = data["groupLabel"] as? String ?: "",
        dateFromMillis = epochMillis(data["dateFrom"]) ?: 0L,
        reason = data["reason"] as? String ?: "",
        description = data["description"] as? String ?: "",
        status = JustificationStatus.fromWire(data["status"] as? String),
        evidence = evidenceFrom(data["attachment"]),
        createdAtMillis = epochMillis(data["createdAt"]) ?: 0L,
        reviewedAtMillis = epochMillis(data["reviewedAt"]),
        reviewNote = data["reviewNote"] as? String,
        sessionId = data["sessionId"] as? String ?: "",
    )
}

private fun evidenceFrom(value: Any?): JustificationEvidence? {
    val map = value as? Map<*, *> ?: return null
    val url = (map["url"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    return JustificationEvidence(
        publicId = map["publicId"] as? String,
        url = url,
        type = map["type"] as? String ?: "",
    )
}

private fun epochMillis(value: Any?): Long? = when (value) {
    is Timestamp -> value.toDate().time
    is Date -> value.time
    is Number -> value.toLong()
    else -> null
}
