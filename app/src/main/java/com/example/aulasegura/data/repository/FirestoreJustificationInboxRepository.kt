package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.JustificationEvidence
import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.JustificationInboxRepository
import com.example.aulasegura.domain.repository.JustificationInboxResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.Date
import kotlinx.coroutines.Dispatchers
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
        const val READ_TIMEOUT_MILLIS = 15_000L

        /** A group's term, not a download. */
        const val QUERY_LIMIT = 100L
    }
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
