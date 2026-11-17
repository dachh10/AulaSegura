package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.TaskEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The task's attachment, stored as JSON inside `TaskEntity.attachmentsJson`.
 *
 * Why JSON and not a table: a task has exactly one PDF, so a second table plus a
 * Room migration (and its test) would buy nothing. The column already existed for
 * the `attachments` array of the document, and it is the same list of maps, just
 * with the local file added while the upload is still pending.
 *
 * The row is a small state machine, not a cache: `PENDING` means the worker still
 * has to upload the file, `UPLOADED` means the URL is final and the task document
 * can be written, `FAILED` means the file is gone for good and the task publishes
 * without it.
 */
@Serializable
data class StoredTaskAttachment(
    val attachmentId: String,
    val fileName: String,
    val bytes: Long,
    val mimeType: String = PDF_MIME,
    /** Local copy; only while the upload has not finished. */
    val localPath: String? = null,
    val publicId: String? = null,
    val url: String? = null,
    val state: String = PENDING,
) {

    val isUploaded: Boolean get() = state == UPLOADED && !url.isNullOrBlank()

    companion object {
        const val PDF_MIME = "application/pdf"
        const val PENDING = "PENDING"
        const val UPLOADED = "UPLOADED"
        const val FAILED = "FAILED"
    }
}

/**
 * Tolerant on purpose: a row written by a future build (or a corrupted one) must
 * not crash the form or the worker. An unreadable column reads as "no
 * attachment", which publishes the task without the file instead of losing it.
 */
private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun encodeTaskAttachments(attachments: List<StoredTaskAttachment>): String =
    runCatching { json.encodeToString(attachments) }.getOrDefault("[]")

fun decodeTaskAttachments(raw: String): List<StoredTaskAttachment> =
    runCatching { json.decodeFromString<List<StoredTaskAttachment>>(raw) }.getOrDefault(emptyList())

/** The attachment of this task, if it has one. */
fun TaskEntity.attachments(): List<StoredTaskAttachment> = decodeTaskAttachments(attachmentsJson)

/** Replaces the attachment of this task; a task never holds more than one. */
fun TaskEntity.withAttachment(attachment: StoredTaskAttachment): TaskEntity =
    copy(
        attachmentsJson = encodeTaskAttachments(listOf(attachment)),
        updatedAt = System.currentTimeMillis(),
    )

/** The `attachments` array of the Firestore document: only what has a URL. */
fun StoredTaskAttachment.toFirestoreMap(): Map<String, Any?> = buildMap {
    put("attachmentId", attachmentId)
    put("fileName", fileName)
    put("bytes", bytes)
    put("mimeType", mimeType)
    // The field the reader uses to decide how to open it, without parsing the name.
    put("type", "pdf")
    url?.let { put("url", it) }
    publicId?.let { put("publicId", it) }
}
