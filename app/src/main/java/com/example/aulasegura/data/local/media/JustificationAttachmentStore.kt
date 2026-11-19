package com.example.aulasegura.data.local.media

import android.content.Context
import com.example.aulasegura.data.remote.cloudinary.ImageCompressor
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps the justification's receipt on this device until the worker uploads it.
 *
 * Same reasoning as `TaskAttachmentStore`: `filesDir` and not the cache, because
 * a justification sent on Sunday can reach Cloudinary on Tuesday, and the system
 * is free to delete cached files long before that.
 *
 * It is a store and not a column: the path travels inside the outbox row's
 * `payloadJson`, so keeping the file does not need a Room migration.
 */
interface JustificationAttachmentStore {

    /** Copies [bytes] into private storage; `null` when the file cannot be written. */
    suspend fun keep(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): KeptJustificationAttachment?

    /** Deletes a file the tutor removed, or one that already reached Cloudinary. */
    fun discard(localPath: String)
}

/** One receipt on disk, plus what the form prints about it. */
data class KeptJustificationAttachment(
    val fileName: String,
    val localPath: String,
    val bytes: Long,
    val mimeType: String,
)

/** The real store: one directory in `filesDir`. */
class FileJustificationAttachmentStore(
    context: Context,
    /**
     * A phone photo is tens of megabytes and the free Cloudinary plan is charged
     * by stored bytes, so the copy that travels is the compressed one
     * (`CONTEXT.md` §7.2). PDFs are never re-encoded.
     */
    private val compressor: ImageCompressor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : JustificationAttachmentStore {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    override suspend fun keep(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): KeptJustificationAttachment? = withContext(ioDispatcher) {
        if (bytes.isEmpty()) return@withContext null
        try {
            directory.mkdirs()
            val stored = if (mimeType.startsWith(IMAGE_PREFIX)) {
                runCatching { compressor.compress(bytes) }.getOrDefault(bytes)
            } else {
                bytes
            }
            // The stored name is ours, not the tutor's: a file called
            // "../../algo.jpg" must not decide where it lands.
            val target = File(directory, "JUSTIFICATION_${UUID.randomUUID()}.${extensionFor(mimeType)}")
            target.writeBytes(stored)
            KeptJustificationAttachment(
                fileName = fileName.takeIf { it.isNotBlank() } ?: target.name,
                localPath = target.absolutePath,
                bytes = target.length(),
                mimeType = mimeType,
            )
        } catch (failed: Exception) {
            null
        }
    }

    override fun discard(localPath: String) {
        runCatching { File(localPath).delete() }
    }

    /**
     * Only the extension the upload needs. A PDF must reach Cloudinary as a raw
     * document; everything else travels as a compressed JPEG.
     */
    private fun extensionFor(mimeType: String): String =
        if (mimeType == PDF_MIME) "pdf" else "jpg"

    private companion object {
        const val DIRECTORY = "justification-attachments"
        const val PDF_MIME = "application/pdf"
        const val IMAGE_PREFIX = "image/"
    }
}
