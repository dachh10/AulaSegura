package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.OutboxDao
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.mapper.StoredJustificationAttachment
import com.example.aulasegura.data.mapper.decodeJustificationPayload
import com.example.aulasegura.data.mapper.encodeJustificationPayload
import com.example.aulasegura.data.mapper.toDocument
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.File
import kotlinx.coroutines.tasks.await

/**
 * Uploads the receipt and writes `justifications/{id}` in one replayable step.
 *
 * Why one row and not two: the tutor's rule only allows `create`, never `update`,
 * so the document cannot exist before the URL does. The handler therefore
 * uploads first, stores `publicId`/`url` back into the row's `payloadJson`, and
 * only then writes the document. A retried Firestore write reads the URL from the
 * payload and does not upload the photo twice.
 *
 * Failure split, same rule as the task attachment:
 *  - network or Cloudinary having a bad day → `Retry`, the file is still on disk;
 *  - the file is gone, the preset refuses the format, or the build has no
 *    credentials → the attachment is marked `FAILED` and the report is still
 *    filed without evidence. Losing the tutor's report over an unreadable photo
 *    would be worse, and the principal can ask for it again.
 *
 * `entityLocalId` **is** the document id, so a replay overwrites the same
 * document instead of filing the absence twice.
 */
class JustificationSyncHandler(
    private val firestore: FirebaseFirestore,
    private val uploader: CloudinaryUploader,
    private val outboxDao: OutboxDao,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        var payload = decodeJustificationPayload(entry.payloadJson)
            ?: return SyncOutcome.Permanent(
                "Justification ${entry.entityLocalId} has an unreadable payload",
            )

        val attachment = payload.attachment
        if (attachment != null && !attachment.isUploaded) {
            // Done and Dropped both settle the attachment (with a URL or as a
            // failed file); Later leaves the row untouched so the phone copy is
            // still there on the next run.
            val settled = when (val step = upload(attachment, entry.entityLocalId)) {
                is UploadStep.Done -> step.attachment
                is UploadStep.Dropped -> step.attachment
                is UploadStep.Later -> return SyncOutcome.Retry(step.message)
            }
            payload = payload.copy(attachment = settled)
            // Persist before the document: if `set()` fails, the next attempt
            // must not upload the same photo again.
            outboxDao.updatePayload(entry.id, encodeJustificationPayload(payload))
        }

        return try {
            firestore.collection(COLLECTION)
                .document(entry.entityLocalId)
                .set(payload.toDocument())
                .await()
            SyncOutcome.Success
        } catch (denied: FirebaseFirestoreException) {
            if (denied.isRetryable) {
                SyncOutcome.Retry(
                    "Firestore unavailable for justification ${entry.entityLocalId} (${denied.code})",
                    denied,
                )
            } else {
                SyncOutcome.Permanent(
                    "Firestore refused justification ${entry.entityLocalId} (${denied.code})",
                    denied,
                )
            }
        } catch (error: Exception) {
            SyncOutcome.Retry(
                "Could not write justification ${entry.entityLocalId}: ${error.message}",
                error,
            )
        }
    }

    private suspend fun upload(
        attachment: StoredJustificationAttachment,
        justificationId: String,
    ): UploadStep {
        val path = attachment.localPath
            ?: return drop(attachment, "el archivo ya no está en el teléfono")

        val file = File(path)
        if (!file.exists()) return drop(attachment, "el archivo ya no está en el teléfono")

        val bytes = runCatching { file.readBytes() }.getOrNull()
            ?: return drop(attachment, "no se pudo leer el archivo")

        val result = if (attachment.type == "image") {
            uploader.uploadImage(
                bytes = bytes,
                fileName = attachment.fileName,
                folder = CloudinaryFolder.JUSTIFICATIONS,
                scopeId = justificationId,
            )
        } else {
            uploader.uploadDocument(
                bytes = bytes,
                fileName = attachment.fileName,
                folder = CloudinaryFolder.JUSTIFICATIONS,
                scopeId = justificationId,
                mimeType = attachment.mimeType,
            )
        }

        return when (result) {
            is CloudinaryResult.Success -> {
                runCatching { file.delete() }
                UploadStep.Done(
                    attachment.copy(
                        state = StoredJustificationAttachment.UPLOADED,
                        publicId = result.asset.publicId,
                        url = result.asset.url,
                        bytes = result.asset.bytes,
                        // The phone copy is no longer the source of truth.
                        localPath = null,
                    ),
                )
            }

            CloudinaryResult.NetworkError -> UploadStep.Later(
                "Network error uploading the receipt of justification $justificationId",
            )

            is CloudinaryResult.ServerError -> UploadStep.Later(
                "Cloudinary failed with ${result.httpCode} for justification $justificationId",
            )

            CloudinaryResult.NotConfigured -> drop(
                attachment,
                "el build no tiene credenciales de Cloudinary",
            )

            is CloudinaryResult.Rejected -> drop(
                attachment,
                "Cloudinary rechazó el comprobante " +
                    "(${result.httpCode}: ${result.message ?: "sin detalle"})",
            )
        }
    }

    /**
     * Marks the attachment as failed, frees the phone copy and lets the document
     * be filed without it. Never `Permanent`: the report itself still matters.
     */
    private fun drop(
        attachment: StoredJustificationAttachment,
        reason: String,
    ): UploadStep {
        attachment.localPath?.let { path -> runCatching { File(path).delete() } }
        return UploadStep.Dropped(
            attachment.copy(
                state = StoredJustificationAttachment.FAILED,
                localPath = null,
                url = null,
                publicId = null,
            ),
            reason,
        )
    }

    private sealed interface UploadStep {

        data class Done(val attachment: StoredJustificationAttachment) : UploadStep

        data class Dropped(
            val attachment: StoredJustificationAttachment,
            val reason: String,
        ) : UploadStep

        data class Later(val message: String) : UploadStep
    }

    private companion object {
        const val COLLECTION = "justifications"
    }
}
