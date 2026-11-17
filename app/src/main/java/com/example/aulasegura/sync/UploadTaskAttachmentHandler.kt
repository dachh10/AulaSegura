package com.example.aulasegura.sync

import com.example.aulasegura.data.local.dao.TaskDao
import com.example.aulasegura.data.local.entity.OutboxEntity
import com.example.aulasegura.data.mapper.StoredTaskAttachment
import com.example.aulasegura.data.mapper.attachments
import com.example.aulasegura.data.mapper.withAttachment
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import java.io.File

/**
 * Uploads the single PDF of a task to Cloudinary (`/raw/upload`).
 *
 * Runs before the task's own outbox row, which is why the document can be written
 * with the final URL instead of an empty array.
 *
 * Failure split, same rule as the incident media:
 *  - network or Cloudinary having a bad day → the row stays queued (`Retry`), the
 *    file is still on disk and nothing is lost.
 *  - the file is gone, the preset refuses PDFs, or Cloudinary is not configured →
 *    the attachment is marked `FAILED` and the task still publishes **without**
 *    it, because losing the homework announcement over an unreadable attachment
 *    would be worse. The history row keeps the file name so the teacher knows.
 */
class UploadTaskAttachmentHandler(
    private val taskDao: TaskDao,
    private val uploader: CloudinaryUploader,
) : SyncOperationHandler {

    override suspend fun handle(entry: OutboxEntity): SyncOutcome {
        val task = taskDao.findById(entry.entityLocalId)
            ?: return SyncOutcome.Permanent(
                "Task ${entry.entityLocalId} is no longer stored locally",
            )

        val attachment = task.attachments().firstOrNull() ?: return SyncOutcome.Success
        if (attachment.isUploaded) return SyncOutcome.Success

        val path = attachment.localPath
            ?: return fail(task.taskId, attachment, "el archivo ya no está en el teléfono")

        val file = File(path)
        if (!file.exists()) {
            return fail(task.taskId, attachment, "el archivo ya no está en el teléfono")
        }

        val bytes = runCatching { file.readBytes() }.getOrNull()
            ?: return fail(task.taskId, attachment, "no se pudo leer el archivo")

        val result = uploader.uploadDocument(
            bytes = bytes,
            fileName = attachment.fileName,
            folder = CloudinaryFolder.TASKS,
            scopeId = task.taskId,
            mimeType = attachment.mimeType,
        )

        return when (result) {
            is CloudinaryResult.Success -> {
                taskDao.upsert(
                    task.withAttachment(
                        attachment.copy(
                            state = StoredTaskAttachment.UPLOADED,
                            publicId = result.asset.publicId,
                            url = result.asset.url,
                            bytes = result.asset.bytes,
                            // The copy on the phone is no longer the source of
                            // truth: Cloudinary has the file now.
                            localPath = null,
                        ),
                    ),
                )
                runCatching { file.delete() }
                SyncOutcome.Success
            }

            CloudinaryResult.NetworkError -> SyncOutcome.Retry(
                "Network error uploading the attachment of task ${task.taskId}",
            )

            is CloudinaryResult.ServerError -> SyncOutcome.Retry(
                "Cloudinary failed with ${result.httpCode} for task ${task.taskId}",
            )

            CloudinaryResult.NotConfigured -> fail(
                task.taskId,
                attachment,
                "el build no tiene credenciales de Cloudinary",
            )

            is CloudinaryResult.Rejected -> fail(
                task.taskId,
                attachment,
                "Cloudinary rechazó el PDF (${result.httpCode}: ${result.message ?: "sin detalle"})",
            )
        }
    }

    /**
     * Marks the attachment as failed and lets the queue move on.
     *
     * The local file is deleted: keeping a PDF nobody will upload only fills the
     * phone, and the teacher can attach it again from the task form.
     */
    private suspend fun fail(
        taskId: String,
        attachment: StoredTaskAttachment,
        reason: String,
    ): SyncOutcome {
        runCatching {
            taskDao.findById(taskId)?.let { task ->
                taskDao.upsert(
                    task.withAttachment(
                        attachment.copy(state = StoredTaskAttachment.FAILED, localPath = null),
                    ),
                )
            }
            attachment.localPath?.let { path -> runCatching { File(path).delete() } }
        }
        return SyncOutcome.Permanent("Attachment of task $taskId dropped: $reason")
    }
}
