package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.mapper.toTimestamp

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
 *  - `media` is deliberately absent until bloque 7: writing an empty array would
 *    say "this incident has no evidence" while the teacher did attach a photo.
 */
fun IncidentDraftEntity.toFirestoreMap(): Map<String, Any> {
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

    return data
}
