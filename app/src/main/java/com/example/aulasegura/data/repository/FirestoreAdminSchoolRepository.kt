package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AdminCampusFields
import com.example.aulasegura.domain.model.AdminCampusConfig
import com.example.aulasegura.domain.model.AdminSchool
import com.example.aulasegura.domain.model.SERVER_TIMESTAMP_SENTINEL
import com.example.aulasegura.domain.model.toUpdateMap
import com.example.aulasegura.domain.repository.AdminSchoolError
import com.example.aulasegura.domain.repository.AdminSchoolRepository
import com.example.aulasegura.domain.repository.AdminSchoolResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Campus configuration over `schools/{schoolId}`.
 *
 * The document read is a direct `get(Source.SERVER)` on the path the rule checks
 * (`schoolId == mySchool()`), so there is no query filter to forget and no stale
 * cache to mistake for a refresh. One 12 s deadline covers both the read and the
 * update; an offline principal gets an explicit error, never a plausible empty form.
 *
 * The update is a partial `update(map)` built from [AdminCampusConfig.toUpdateMap].
 * That builder is the only place the writable keys are named, which is what keeps
 * `key` and `createdAt` out of the payload. `updatedAt` uses the server clock.
 *
 * This repository is separate from the teacher's `FirestoreSchoolRepository`:
 * that one is a cache-first geofence read tied to Room and the Remote Config
 * fallback, while this one is an online-only principal read/write with no cache.
 */
class FirestoreAdminSchoolRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : AdminSchoolRepository {

    override suspend fun getSchool(): AdminSchoolResult<AdminSchool> =
        schoolCall { schoolId ->
            val snapshot = firestore.collection(COLLECTION_SCHOOLS)
                .document(schoolId)
                .get(Source.SERVER)
                .await()

            if (!snapshot.exists()) throw SchoolNotFound

            documentToSchool(snapshot.data.orEmpty())
        }

    override suspend fun updateCampus(config: AdminCampusConfig): AdminSchoolResult<Unit> =
        schoolCall { schoolId ->
            val fields = config.toUpdateMap().toMutableMap()
            fields[AdminCampusFields.UPDATED_AT] = FieldValue.serverTimestamp()

            firestore.collection(COLLECTION_SCHOOLS)
                .document(schoolId)
                .update(fields)
                .await()
        }

    /** One deadline and one error mapping for every public call. */
    private suspend fun <T> schoolCall(
        block: suspend (schoolId: String) -> T,
    ): AdminSchoolResult<T> {
        val schoolId = auth.currentUser?.schoolId
            ?: return AdminSchoolResult.Failure(AdminSchoolError.NO_SESSION)

        return try {
            withTimeout(CALL_TIMEOUT_MILLIS) {
                AdminSchoolResult.Success(block(schoolId))
            }
        } catch (error: SchoolNotFound) {
            AdminSchoolResult.Failure(AdminSchoolError.NOT_FOUND)
        } catch (error: TimeoutCancellationException) {
            AdminSchoolResult.Failure(AdminSchoolError.TIMEOUT)
        } catch (error: FirebaseFirestoreException) {
            AdminSchoolResult.Failure(error.toDomainError())
        } catch (error: Exception) {
            AdminSchoolResult.Failure(AdminSchoolError.UNKNOWN)
        }
    }

    /** Every field degrades to a safe value; the form validates what it can edit. */
    private fun documentToSchool(data: Map<String, Any?>): AdminSchool {
        val location = data[AdminCampusFields.LOCATION] as? Map<*, *>
        val schedule = data[AdminCampusFields.SCHEDULE] as? Map<*, *>

        return AdminSchool(
            name = data[FIELD_NAME] as? String ?: "",
            key = data[FIELD_KEY] as? String ?: "",
            address = data[FIELD_ADDRESS] as? String ?: "",
            cycle = data[FIELD_CYCLE] as? String ?: "",
            latitude = number(location?.get(AdminCampusFields.LOCATION_LAT)),
            longitude = number(location?.get(AdminCampusFields.LOCATION_LNG)),
            radiusMeters = (data[AdminCampusFields.RADIUS_METERS] as? Number)?.toInt()
                ?: AdminSchool.DEFAULT_RADIUS_METERS,
            entryTime = schedule?.get(AdminCampusFields.ENTRY_TIME) as? String ?: "",
            exitTime = schedule?.get(AdminCampusFields.EXIT_TIME) as? String ?: "",
            requireLocation = data[AdminCampusFields.REQUIRE_LOCATION] as? Boolean ?: true,
            updatedAtMillis = timestampMillis(data[AdminCampusFields.UPDATED_AT]),
        )
    }

    /** Firestore writes a `Timestamp`; hand-edited test rows may hold a `Number`. */
    private fun timestampMillis(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> null
    }

    private fun number(value: Any?): Double = (value as? Number)?.toDouble() ?: 0.0

    private fun FirebaseFirestoreException.toDomainError(): AdminSchoolError = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        -> AdminSchoolError.DENIED

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> AdminSchoolError.NETWORK

        else -> AdminSchoolError.UNKNOWN
    }

    /** Local signal for a missing document, mapped without exposing Firestore types. */
    private object SchoolNotFound : Exception()

    private companion object {
        const val COLLECTION_SCHOOLS = "schools"

        const val FIELD_NAME = "name"
        const val FIELD_KEY = "key"
        const val FIELD_ADDRESS = "address"
        const val FIELD_CYCLE = "cycle"

        const val CALL_TIMEOUT_MILLIS = 12_000L
    }
}
