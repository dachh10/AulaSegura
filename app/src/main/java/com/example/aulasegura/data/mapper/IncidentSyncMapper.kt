package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.data.local.mapper.toTimestamp
import com.example.aulasegura.domain.model.MediaUploadPhase

/**
 * Room draft to the Firestore document of `incidents/{incidentId}`.
 *
 * Written with `set()` on a document whose id is the local UUID, so a replay is
 * idempotent: the same incident sent twice leaves one document, not two.
 *
 * Two fields exist only here and not in the entity's schema notes:
 *  - `studentName` and `reportedByName` travel **inside** the document because the
 *    tutor can read their own incidents but not the school roster; without the
 *    denormalised name, the tutor's screen would need a read the rules deny.
 *  - `media` carries one map per uploaded file (`CONTEXT.md` §6). [media] is
 *    required, not defaulted: a forgotten argument would publish "no evidence"
 *    for an incident that has a photo, which is the one lie this document cannot
 *    afford.
 */
fun IncidentDraftEntity.toFirestoreMap(media: List<IncidentMediaEntity>): Map<String, Any> {
    val data = mutableMapOf<String, Any>(
        "incidentId" to incidentId,
        "schoolId" to schoolId,
        "groupId" to groupId,
        "studentName" to studentName,
        "title" to title,
        "description" to description,
        "severity" to severity,
        "category" to category,
        "reportedBy" to reportedBy,
        "reportedByName" to reportedByName,
        "occurredAt" to occurredAt.toTimestamp(),
        "status" to status,
        "seenByTutorIds" to seenByTutorIds,
        "createdAt" to createdAt.toTimestamp(),
        "updatedAt" to updatedAt.toTimestamp(),
    )

    studentId?.let { data["studentId"] = it }

    // A half-written pin (lat without lng) is not a location: the map is only
    // emitted when both coordinates exist, so a reader never gets a point it
    // cannot place.
    val lat = locationLat
    val lng = locationLng
    if (lat != null && lng != null) {
        val location = mutableMapOf<String, Any>("lat" to lat, "lng" to lng)
        locationAccuracy?.let { location["accuracy"] = it }
        locationDistanceMeters?.let { location["distanceMeters"] = it }
        locationVerified?.let { location["verified"] = it }
        data["location"] = location
    }

    // Only files that actually reached Cloudinary: a `PENDING` row has no public
    // id or url yet, so writing it would publish an evidence entry that nobody
    // can open.
    data["media"] = media
        .filter { it.uploadState == UploadState.UPLOADED }
        .map { it.toFirestoreMap() }

    return data
}

/**
 * One `media` array entry (`CONTEXT.md` §6).
 *
 * `id` is the local media id and not Cloudinary's `publicId`: the array needs a
 * stable key the client controls, and the upload response is the only place the
 * public id is authoritative.
 *
 * The uploaded file's own bytes/width/height come from Cloudinary's answer, never
 * from what the phone guessed before uploading: a transformation preset can
 * change both.
 */
private fun IncidentMediaEntity.toFirestoreMap(): Map<String, Any> = buildMap {
    put("id", mediaId)
    put("type", type)
    publicId?.let { put("publicId", it) }
    url?.let { put("url", it) }
    thumbnailUrl?.let { put("thumbnailUrl", it) }
    format?.let { put("format", it) }
    put("bytes", bytes)
    put("width", width)
    put("height", height)
    durationSec?.let { put("durationSec", it) }
}

/** Room upload state to the phase the screen prints next to each file. */
fun UploadState.toMediaUploadPhase(): MediaUploadPhase = when (this) {
    UploadState.PENDING -> MediaUploadPhase.PENDING
    UploadState.UPLOADING -> MediaUploadPhase.UPLOADING
    UploadState.UPLOADED -> MediaUploadPhase.UPLOADED
    UploadState.FAILED -> MediaUploadPhase.FAILED
}
