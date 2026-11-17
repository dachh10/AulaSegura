package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.IncidentDao
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import java.io.File
import java.io.IOException

/**
 * Uploads one evidence file to Cloudinary and records where it landed.
 *
 * One outbox row per file, not one per incident (`FASE1.md` 7.4 and 7.5): a photo
 * that fails stops only its own row, and the files already uploaded are never
 * sent again. The row's `entityLocalId` is the media id.
 *
 * The file was compressed when the teacher took the photo, so it is sent exactly
 * as it sits on disk — re-encoding here would spend CPU on every retry.
 *
 * A transient failure leaves the file `PENDING` and asks the worker to come back;
 * a permanent one marks it `FAILED` and lets the queue continue, which is what
 * keeps one unreadable photo from costing the whole incident.
 */
class UploadIncidentMediaHandler(
    private val incidentDao: IncidentDao,
    private val uploader: CloudinaryUploader,
    private val clock: () -> Long = System::currentTimeMillis,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val media = incidentDao.findMedia(entry.entityLocalId)
            ?: return SyncOutcome.Permanent(
                "Evidence ${entry.entityLocalId} is no longer stored locally",
            )

        // A replay after a crash between the upload and the row delete. The file
        // is already in Cloudinary, so uploading it again would pay for the same
        // bytes twice. `FAILED` rows are skipped for the same reason: the decision
        // was already taken and retrying it would only replay a permanent error.
        if (media.uploadState == UploadState.UPLOADED ||
            media.uploadState == UploadState.FAILED
        ) {
            return SyncOutcome.Success
        }

        val file = media.localPath?.let(::File)
        if (file == null || !file.isFile) {
            return bury(media, "The evidence file is gone from this device")
        }

        mark(media, UploadState.UPLOADING, media.attempts + 1, lastError = null)

        val bytes = try {
            file.readBytes()
        } catch (e: IOException) {
            return bury(media, "The evidence file could not be read")
        }

        return when (
            val result = uploader.uploadImage(
                bytes = bytes,
                fileName = file.name,
                folder = CloudinaryFolder.INCIDENTS,
                // The incident id, not the school id: `CONTEXT.md` §7.1 groups the
                // evidence of one incident so a future cleanup can delete it in a
                // single call, and mixing every school photo in one folder would
                // make that impossible.
                scopeId = media.incidentId,
            )
        ) {
            is CloudinaryResult.Success -> {
                val asset = result.asset
                mark(
                    media.copy(
                        publicId = asset.publicId,
                        url = asset.url,
                        thumbnailUrl = asset.thumbnailUrl,
                        format = asset.format,
                        bytes = asset.bytes,
                        width = asset.width,
                        height = asset.height,
                        durationSec = asset.durationSec,
                    ),
                    UploadState.UPLOADED,
                    media.attempts + 1,
                    lastError = null,
                )
                // The bytes live in Cloudinary now. Keeping the copy would grow
                // the app's storage for a file no screen reads any more.
                runCatching { file.delete() }
                SyncOutcome.Success
            }

            CloudinaryResult.NetworkError, is CloudinaryResult.ServerError -> {
                mark(
                    media,
                    UploadState.PENDING,
                    media.attempts + 1,
                    lastError = result.describe(),
                )
                SyncOutcome.Retry(
                    "Cloudinary unavailable for evidence ${media.mediaId} (${result.describe()})",
                )
            }

            is CloudinaryResult.Rejected ->
                bury(media, "Cloudinary rejected the file (${result.httpCode})")

            CloudinaryResult.NotConfigured ->
                bury(media, "This build has no Cloudinary credentials")
        }
    }

    /**
     * Gives up on one file and lets the queue continue.
     *
     * `Success` and not `Permanent`: the row's work is over — this file will never
     * upload — so leaving it in the outbox would mean nine more pointless attempts
     * and a list that keeps saying "pending" about a photo that is not going
     * anywhere. The incident is written without it and the screen shows it failed.
     */
    private suspend fun bury(media: IncidentMediaEntity, reason: String): SyncOutcome {
        mark(media, UploadState.FAILED, media.attempts + 1, reason)
        return SyncOutcome.Success
    }

    private suspend fun mark(
        media: IncidentMediaEntity,
        state: UploadState,
        attempts: Int,
        lastError: String?,
    ) {
        runCatching {
            incidentDao.upsertMedia(
                media.copy(
                    uploadState = state,
                    attempts = attempts,
                    lastError = lastError,
                    updatedAt = clock(),
                ),
            )
        }
    }

    private fun CloudinaryResult.describe(): String = when (this) {
        is CloudinaryResult.Success -> "ok"
        CloudinaryResult.NetworkError -> "network"
        CloudinaryResult.NotConfigured -> "not configured"
        is CloudinaryResult.Rejected -> "rejected $httpCode"
        is CloudinaryResult.ServerError -> "server $httpCode"
    }
}
