package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AdminIncident
import com.example.aulasegura.domain.model.AdminIncidentLocation
import com.example.aulasegura.domain.model.AdminIncidentMedia
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.repository.AdminIncidentError
import com.example.aulasegura.domain.repository.AdminIncidentRepository
import com.example.aulasegura.domain.repository.AdminIncidentResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Incident inbox over `incidents/{id}`.
 *
 * The read query always carries the `schoolId == mySchool()` equality the rules
 * check, plus `createdAt DESC` so it uses the deployed
 * `schoolId ASC + createdAt DESC` composite index. Firestore does not filter
 * results: dropping the equality would make rules deny the read, not hide other
 * schools' rows.
 *
 * Both calls are one-shot with a single 12 s deadline. A live listener would bill
 * a snapshot on every change while the screen stays open (Spark read budget).
 * Status is the only mutable field here; there is no delete path.
 */
class FirestoreAdminIncidentRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : AdminIncidentRepository {

    override suspend fun getIncidents(): AdminIncidentResult<List<AdminIncident>> =
        incidentCall { schoolId ->
            firestore.collection(COLLECTION_INCIDENTS)
                .whereEqualTo(FIELD_SCHOOL_ID, schoolId)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .limit(INBOX_LIMIT)
                .get(Source.SERVER)
                .await()
                .documents
                .map { document -> documentToIncident(document.id, document.data.orEmpty()) }
        }

    override suspend fun setStatus(
        incidentId: String,
        status: IncidentStatus,
    ): AdminIncidentResult<Unit> = incidentCall {
        firestore.collection(COLLECTION_INCIDENTS)
            .document(incidentId)
            .update(
                mapOf(
                    FIELD_STATUS to status.wire,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    /** One deadline and one error mapping for every public call. */
    private suspend fun <T> incidentCall(
        block: suspend (schoolId: String) -> T,
    ): AdminIncidentResult<T> {
        val schoolId = auth.currentUser?.schoolId
            ?: return AdminIncidentResult.Failure(AdminIncidentError.NO_SESSION)

        return try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                AdminIncidentResult.Success(block(schoolId))
            }
        } catch (error: TimeoutCancellationException) {
            AdminIncidentResult.Failure(AdminIncidentError.TIMEOUT)
        } catch (error: FirebaseFirestoreException) {
            AdminIncidentResult.Failure(error.toDomainError())
        } catch (error: Exception) {
            AdminIncidentResult.Failure(AdminIncidentError.UNKNOWN)
        }
    }

    private fun documentToIncident(
        id: String,
        data: Map<String, Any?>,
    ): AdminIncident {
        val location = data[FIELD_LOCATION] as? Map<*, *>
        val media = (data[FIELD_MEDIA] as? List<*>).orEmpty().mapNotNull { entry ->
            (entry as? Map<*, *>)?.let { item ->
                AdminIncidentMedia(
                    id = item[MEDIA_ID] as? String ?: "",
                    type = item[MEDIA_TYPE] as? String ?: DEFAULT_MEDIA_TYPE,
                    publicId = item[MEDIA_PUBLIC_ID] as? String,
                    url = item[MEDIA_URL] as? String ?: "",
                    thumbnailUrl = item[MEDIA_THUMBNAIL] as? String,
                    format = item[MEDIA_FORMAT] as? String,
                )
            }
        }

        return AdminIncident(
            id = id,
            schoolId = data[FIELD_SCHOOL_ID] as? String ?: "",
            groupId = data[FIELD_GROUP_ID] as? String ?: "",
            studentId = data[FIELD_STUDENT_ID] as? String,
            studentName = data[FIELD_STUDENT_NAME] as? String ?: "",
            title = data[FIELD_TITLE] as? String ?: "",
            description = data[FIELD_DESCRIPTION] as? String ?: "",
            severity = AdminIncident.severityFromWire(data[FIELD_SEVERITY] as? String),
            category = AdminIncident.categoryFromWire(data[FIELD_CATEGORY] as? String),
            reportedBy = data[FIELD_REPORTED_BY] as? String ?: "",
            reportedByName = data[FIELD_REPORTED_BY_NAME] as? String ?: "",
            occurredAt = timestampMillis(data[FIELD_OCCURRED_AT]),
            location = location?.let { pin ->
                AdminIncidentLocation(
                    latitude = number(pin[LOCATION_LAT]),
                    longitude = number(pin[LOCATION_LNG]),
                    accuracyMeters = number(pin[LOCATION_ACCURACY]),
                    distanceMeters = number(pin[LOCATION_DISTANCE]),
                    verified = pin[LOCATION_VERIFIED] as? Boolean ?: false,
                )
            },
            media = media,
            status = AdminIncident.statusFromWire(data[FIELD_STATUS] as? String),
            seenByTutorIds = (data[FIELD_SEEN_BY] as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty(),
            createdAt = timestampMillis(data[FIELD_CREATED_AT]),
        )
    }

    /** Firestore writes a `Timestamp`; hand-edited test rows may hold a `Number`. */
    private fun timestampMillis(value: Any?): Long = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> 0L
    }

    private fun number(value: Any?): Double? = (value as? Number)?.toDouble()

    private fun FirebaseFirestoreException.toDomainError(): AdminIncidentError = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        -> AdminIncidentError.DENIED

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> AdminIncidentError.NETWORK

        else -> AdminIncidentError.UNKNOWN
    }

    private companion object {
        const val COLLECTION_INCIDENTS = "incidents"

        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_GROUP_ID = "groupId"
        const val FIELD_STUDENT_ID = "studentId"
        const val FIELD_STUDENT_NAME = "studentName"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_SEVERITY = "severity"
        const val FIELD_CATEGORY = "category"
        const val FIELD_REPORTED_BY = "reportedBy"
        const val FIELD_REPORTED_BY_NAME = "reportedByName"
        const val FIELD_OCCURRED_AT = "occurredAt"
        const val FIELD_LOCATION = "location"
        const val FIELD_MEDIA = "media"
        const val FIELD_STATUS = "status"
        const val FIELD_SEEN_BY = "seenByTutorIds"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"

        const val LOCATION_LAT = "lat"
        const val LOCATION_LNG = "lng"
        const val LOCATION_ACCURACY = "accuracy"
        const val LOCATION_DISTANCE = "distanceMeters"
        const val LOCATION_VERIFIED = "verified"

        const val MEDIA_ID = "id"
        const val MEDIA_TYPE = "type"
        const val MEDIA_PUBLIC_ID = "publicId"
        const val MEDIA_URL = "url"
        const val MEDIA_THUMBNAIL = "thumbnailUrl"
        const val MEDIA_FORMAT = "format"

        const val DEFAULT_MEDIA_TYPE = "image"

        /** A term of incidents, not an unbounded download on a metered plan. */
        const val INBOX_LIMIT = 300L

        const val READ_TIMEOUT_MILLIS = 12_000L
    }
}
