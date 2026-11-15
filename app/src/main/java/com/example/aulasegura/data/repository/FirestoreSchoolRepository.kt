package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.SchoolDao
import com.example.aulasegura.data.local.mapper.schoolEntityFrom
import com.example.aulasegura.data.local.mapper.toSchoolSite
import com.example.aulasegura.domain.model.SchoolSite
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.SchoolRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Cache-first school site.
 *
 * The read is a single document (`schools/{schoolId}`), not a query, because the
 * rule is `schoolId == mySchool()`: a document read carries that predicate in its
 * path, so there is no filter to forget.
 *
 * The refresh asks for `Source.SERVER` for the same reason as the roster: a
 * cached Firestore read is not a refresh, and with the persistent cache off a
 * "cached" read is just a miss. A failed refresh leaves Room untouched, which is
 * what keeps a cached geofence working in airplane mode.
 */
class FirestoreSchoolRepository(
    private val firestore: FirebaseFirestore,
    private val schoolDao: SchoolDao,
    private val auth: AuthRepository,
    private val defaultRadiusMeters: () -> Double,
) : SchoolRepository {

    override fun observeSchool(): Flow<SchoolSite?> {
        val user = auth.currentUser ?: return flowOf(null)
        val fallbackRadius = defaultRadiusMeters()
        return schoolDao.observeById(user.schoolId).map { entity ->
            entity?.toSchoolSite(fallbackRadius)
        }
    }

    override suspend fun refreshSchool() {
        val user = auth.currentUser ?: return

        val snapshot = try {
            withTimeout(REFRESH_TIMEOUT_MILLIS) {
                firestore.collection(SCHOOLS)
                    .document(user.schoolId)
                    .get(Source.SERVER)
                    .await()
            }
        } catch (error: Exception) {
            // Best effort: the cache decides. A school document that cannot be
            // refreshed is not worth a screen message — the teacher would be
            // told about a failure they cannot act on, in the middle of a class.
            return
        }

        val entity = snapshot.data?.let { schoolEntityFrom(snapshot.id, it) } ?: return
        schoolDao.upsert(entity)
    }

    private companion object {
        const val SCHOOLS = "schools"

        /** Same ceiling as the roster refresh: one server read, not a hang. */
        const val REFRESH_TIMEOUT_MILLIS = 12_000L
    }
}
