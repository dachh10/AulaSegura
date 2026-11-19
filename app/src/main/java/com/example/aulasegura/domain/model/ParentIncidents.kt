package com.example.aulasegura.domain.model

/**
 * A school incident about one of the tutor's children, in the schema of
 * `CONTEXT.md` §`incidents/{incidentId}`.
 *
 * Everything here was written by the teacher who reported it: the tutor reads it
 * and only ever writes `seenByTutorIds`. The place is a coordinate, not a name:
 * the schema carries `location { lat, lng, accuracy, distanceMeters, verified }`
 * and nothing that says "Patio Central", so the UI does not print one.
 */
data class ParentIncident(
    val incidentId: String,
    val studentId: String?,
    val studentName: String,
    val groupId: String,
    val title: String,
    val description: String,
    /** `leve`, `moderado` or `grave`, as reported. */
    val severity: String,
    /** `discusion`, `caida`, `danio`, `desacato` or `otro`. */
    val category: String,
    val reportedByName: String,
    val occurredAtMillis: Long,
    /** `nuevo`, `revisado` or `cerrado`. */
    val status: String,
    val location: ParentIncidentLocation?,
    val media: List<ParentIncidentMedia> = emptyList(),
    /** The signed-in tutor is already in `seenByTutorIds`. */
    val seenByMe: Boolean = false,
)

data class ParentIncidentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double?,
    /** Distance to the school centre at the moment it was reported. */
    val distanceMeters: Double?,
    /** The teacher's position fell inside the geofence when reporting. */
    val verified: Boolean,
)

data class ParentIncidentMedia(
    val mediaId: String?,
    /** `image` or `video`. */
    val type: String,
    val url: String,
    val thumbnailUrl: String?,
    val durationSec: Double?,
) {
    val isVideo: Boolean get() = type == "video"
}
