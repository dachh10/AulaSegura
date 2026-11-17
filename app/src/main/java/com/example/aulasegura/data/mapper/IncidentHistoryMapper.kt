package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentEvidence
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.domain.model.IncidentSummary
import com.google.firebase.Timestamp

/**
 * Room and Firestore both feed the teacher's history, so the two directions live
 * next to each other.
 *
 * The wire values are the same ones the write path sends (`CONTEXT.md` §6.2), and
 * nothing here invents a field: a document that is missing one is skipped instead
 * of being cached as an incident with an empty name that the list would print.
 */

/** A cached draft, as the history row shows it. */
internal fun IncidentDraftEntity.toIncidentSummary(mediaCount: Int): IncidentSummary =
    IncidentSummary(
        incidentId = incidentId,
        groupId = groupId,
        studentName = studentName,
        title = title,
        description = description,
        severity = IncidentSeverity.entries.firstOrNull { it.wire == severity },
        category = IncidentCategory.entries.firstOrNull { it.wire == category },
        status = IncidentStatus.fromWire(status),
        occurredAt = occurredAt,
        locationVerified = locationVerified,
        locationDistanceMeters = locationDistanceMeters,
        sync = syncState.toRollCallSync(),
        mediaCount = mediaCount,
    )

/** A cached media row, as the expanded incident shows it. */
internal fun IncidentMediaEntity.toIncidentEvidence(): IncidentEvidence = IncidentEvidence(
    mediaId = mediaId,
    url = url,
    thumbnailUrl = thumbnailUrl,
    format = format,
    bytes = bytes,
    phase = uploadState.toMediaUploadPhase(),
    localPath = localPath,
)

/**
 * `incidents/{incidentId}` back into the cache row.
 *
 * Returns `null` when the document cannot stand on its own (no school, group or
 * reporter): caching half an incident would put a nameless row in the teacher's
 * history, and the next refresh would hide the fact that the document is broken.
 *
 * The result is always `SYNCED`: it came from the server, so there is nothing
 * left to push. A local draft that was still `FAILED` for the same id gets
 * repaired by the same upsert, which is correct — the document exists.
 */
internal fun incidentEntityFrom(
    incidentId: String,
    data: Map<String, Any?>,
): IncidentDraftEntity? {
    val schoolId = data["schoolId"] as? String ?: return null
    val groupId = data["groupId"] as? String ?: return null
    val reportedBy = data["reportedBy"] as? String ?: return null
    val location = data["location"] as? Map<*, *>

    return IncidentDraftEntity(
        incidentId = incidentId,
        schoolId = schoolId,
        groupId = groupId,
        studentId = data["studentId"] as? String,
        studentName = data["studentName"] as? String ?: "Sin nombre",
        title = data["title"] as? String ?: "",
        description = data["description"] as? String ?: "",
        severity = data["severity"] as? String ?: "",
        category = data["category"] as? String ?: "",
        reportedBy = reportedBy,
        reportedByName = data["reportedByName"] as? String ?: "",
        occurredAt = (data["occurredAt"] as? Timestamp)?.toDate()?.time ?: 0L,
        locationLat = (location?.get("lat") as? Number)?.toDouble(),
        locationLng = (location?.get("lng") as? Number)?.toDouble(),
        locationAccuracy = (location?.get("accuracy") as? Number)?.toDouble(),
        locationDistanceMeters = (location?.get("distanceMeters") as? Number)?.toDouble(),
        locationVerified = location?.get("verified") as? Boolean,
        status = data["status"] as? String ?: IncidentStatus.NUEVO.wire,
        seenByTutorIds = (data["seenByTutorIds"] as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty(),
        syncState = SyncState.SYNCED,
        createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L,
        updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: 0L,
    )
}

/**
 * One `media` array entry back into a cache row.
 *
 * The id is the one the document carries, which is the local media id the phone
 * minted. Anything already uploaded, so the row is `UPLOADED` with no file on
 * disk: the bytes live in Cloudinary now, and the detail opens the URL.
 */
internal fun incidentMediaFrom(
    incidentId: String,
    entry: Map<*, *>,
): IncidentMediaEntity? {
    val mediaId = entry["id"] as? String ?: return null

    return IncidentMediaEntity(
        mediaId = mediaId,
        incidentId = incidentId,
        type = entry["type"] as? String ?: "image",
        publicId = entry["publicId"] as? String,
        url = entry["url"] as? String,
        thumbnailUrl = entry["thumbnailUrl"] as? String,
        format = entry["format"] as? String,
        bytes = (entry["bytes"] as? Number)?.toLong() ?: 0L,
        width = (entry["width"] as? Number)?.toInt() ?: 0,
        height = (entry["height"] as? Number)?.toInt() ?: 0,
        durationSec = (entry["durationSec"] as? Number)?.toDouble(),
        uploadState = UploadState.UPLOADED,
        // It came from the server: there is no local copy, and inventing one
        // would make the tile try to open a path that does not exist.
        localPath = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
