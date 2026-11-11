package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One evidence file of an incident, plus where it stands in the Cloudinary
 * upload.
 *
 * Cloudinary replaced Firebase Storage, so [localPath] is the only thing that
 * exists until the upload finishes; afterwards [publicId] and [url] are what
 * gets written into the Firestore `media` array. [incidentId] points at
 * [IncidentDraftEntity] but is not a foreign key: a media row must survive long
 * enough to be retried even if the draft is rewritten.
 */
@Entity(
    tableName = "incident_media",
    indices = [Index("incidentId"), Index("uploadState")],
)
data class IncidentMediaEntity(
    @PrimaryKey val mediaId: String,
    val incidentId: String,
    /** `"image"` or `"video"`. */
    val type: String,
    /** Absolute path of the file on this device, until it is uploaded. */
    val localPath: String? = null,
    val publicId: String? = null,
    val url: String? = null,
    val thumbnailUrl: String? = null,
    val format: String? = null,
    val bytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val durationSec: Double? = null,
    val uploadState: UploadState = UploadState.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
