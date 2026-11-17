package com.example.aulasegura.data.local.media

import android.content.Context
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps the task's PDF on this device until the worker uploads it.
 *
 * Same reasoning as `IncidentMediaStore`: `filesDir` and not the cache, because a
 * task can be published on Friday and reach Cloudinary on Monday, and the system
 * is free to delete cached files long before that.
 *
 * It is a store and not a column: the path travels inside the task's
 * `attachmentsJson`, so persisting the file does not need a Room migration for a
 * single field.
 */
interface TaskAttachmentStore {

    /** Copies [bytes] into private storage; `null` when the file cannot be written. */
    suspend fun keep(bytes: ByteArray, fileName: String): KeptTaskAttachment?

    /** Deletes a file the teacher removed, or one that already reached Cloudinary. */
    fun discard(localPath: String)
}

/** One PDF on disk, plus what the form prints about it. */
data class KeptTaskAttachment(
    val fileName: String,
    val localPath: String,
    val bytes: Long,
)

/** The real store: one directory in `filesDir`. */
class FileTaskAttachmentStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TaskAttachmentStore {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    override suspend fun keep(bytes: ByteArray, fileName: String): KeptTaskAttachment? =
        withContext(ioDispatcher) {
            if (bytes.isEmpty()) return@withContext null
            try {
                directory.mkdirs()
                // The stored name is ours, not the teacher's: a file called
                // "../../algo.pdf" must not decide where it lands.
                val target = File(directory, "TASK_${UUID.randomUUID()}.pdf")
                target.writeBytes(bytes)
                KeptTaskAttachment(
                    fileName = fileName,
                    localPath = target.absolutePath,
                    bytes = target.length(),
                )
            } catch (failed: Exception) {
                null
            }
        }

    override fun discard(localPath: String) {
        runCatching { File(localPath).delete() }
    }

    private companion object {
        const val DIRECTORY = "task-attachments"
    }
}
