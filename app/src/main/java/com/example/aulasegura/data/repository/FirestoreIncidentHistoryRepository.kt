package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.mapper.incidentEntityFrom
import com.example.aulasegura.data.mapper.incidentMediaFrom
import com.example.aulasegura.data.mapper.toIncidentEvidence
import com.example.aulasegura.data.mapper.toIncidentSummary
import com.example.aulasegura.domain.model.IncidentEvidence
import com.example.aulasegura.domain.model.IncidentSummary
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.HistoryRefreshResult
import com.example.aulasegura.domain.repository.IncidentHistoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Cache-first history of the teacher's own incidents.
 *
 * Same contract as the roll call history: the list is read from Room, so opening
 * the screen costs no reads and works offline; [refresh] is what downloads the
 * incidents this phone never saw.
 *
 * The query filters `reportedBy == uid` with no `orderBy`. That is deliberate:
 * it is exactly the clause the rules check, so Firestore can prove the query is
 * safe, and it needs no composite index to be deployed — the list is sorted
 * locally by `occurredAt`.
 */
class FirestoreIncidentHistoryRepository(
    private val firestore: FirebaseFirestore,
    private val incidentDao: IncidentDao,
    private val auth: AuthRepository,
) : IncidentHistoryRepository {

    override fun observeIncidents(): Flow<List<IncidentSummary>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.emptyFlow()
        return incidentDao.observeHistory(uid).map { rows ->
            rows.map { row -> row.draft.toIncidentSummary(row.mediaCount) }
        }
    }

    override suspend fun refresh(): HistoryRefreshResult {
        val user = auth.currentUser ?: return HistoryRefreshResult.SignedOut

        val snapshot = try {
            withTimeout(REFRESH_TIMEOUT_MILLIS) {
                firestore.collection(COLLECTION)
                    .whereEqualTo(FIELD_REPORTED_BY, user.uid)
                    .limit(HISTORY_LIMIT)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            // `Source.SERVER` and not the default cache: an empty snapshot from a
            // stale cache reads as "no incidents", which is the one answer that
            // would make a teacher think their report was lost.
            return HistoryRefreshResult.Failed(error.toRosterFailure())
        }

        val incidents = snapshot.documents.mapNotNull { document ->
            document.data?.let { incidentEntityFrom(document.id, it) }
        }
        if (incidents.isNotEmpty()) incidentDao.upsertDrafts(incidents)

        val media = snapshot.documents.flatMap { document ->
            val entries = document.data?.get(FIELD_MEDIA) as? List<*> ?: return@flatMap emptyList()
            entries.mapNotNull { entry ->
                (entry as? Map<*, *>)?.let { incidentMediaFrom(document.id, it) }
            }
        }
        if (media.isNotEmpty()) incidentDao.upsertMediaList(media)

        return HistoryRefreshResult.Updated(incidents.size)
    }

    override fun observeEvidence(incidentId: String): Flow<List<IncidentEvidence>> =
        incidentDao.observeMedia(incidentId).map { rows -> rows.map { it.toIncidentEvidence() } }

    private companion object {
        const val COLLECTION = "incidents"
        const val FIELD_REPORTED_BY = "reportedBy"
        const val FIELD_MEDIA = "media"
        const val REFRESH_TIMEOUT_MILLIS = 15_000L

        /** Same ceiling as the roll call history: a term, not a download. */
        const val HISTORY_LIMIT = 120L
    }
}
