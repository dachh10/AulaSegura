package com.example.aulasegura.data.local.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.aulasegura.data.remote.cloudinary.ImageCompressor
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps an evidence photo on this device until the sync worker uploads it.
 *
 * The file is **compressed here and not in the worker** (`FASE1.md` 7.2): the
 * teacher can see the photo shrink while the form is still on screen, and a retry
 * uploads the same bytes instead of paying the compression cost again.
 *
 * It lives in `filesDir` and not in the cache: an incident can wait days for a
 * signal, and the system is free to delete a cached photo long before that.
 */
interface IncidentMediaStore {

    /**
     * Compresses [source], stores the result and returns what the form needs to
     * show it. The source file is deleted either way: it was only the camera's
     * temporary output.
     */
    suspend fun keep(source: File): KeptEvidence?

    /** Deletes a file the teacher removed from the form before saving. */
    fun discard(localPath: String)
}

/** One compressed photo on disk, plus the thumbnail the form prints. */
data class KeptEvidence(
    val localPath: String,
    val bytes: Long,
    val thumbnail: Bitmap?,
)

/** The real store: [ImageCompressor] plus a private directory in `filesDir`. */
class FileIncidentMediaStore(
    context: Context,
    private val compressor: ImageCompressor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IncidentMediaStore {

    private val directory = File(context.applicationContext.filesDir, DIRECTORY)

    override suspend fun keep(source: File): KeptEvidence? = withContext(ioDispatcher) {
        try {
            val raw = source.readBytes()
            val compressed = compressor.compress(raw)
            if (compressed.isEmpty()) return@withContext null

            directory.mkdirs()
            val target = File(directory, "IMG_${UUID.randomUUID()}.jpg")
            target.writeBytes(compressed)

            KeptEvidence(
                localPath = target.absolutePath,
                bytes = target.length(),
                thumbnail = thumbnailOf(target),
            )
        } catch (failed: IOException) {
            null
        } finally {
            // The camera's file has no reader once the compressed copy exists.
            runCatching { source.delete() }
        }
    }

    override fun discard(localPath: String) {
        runCatching { File(localPath).delete() }
    }

    /**
     * A tile-sized bitmap, not the full photo: three 1080 px images decoded at
     * full size is memory the form does not need for a 96 dp square.
     */
    private fun thumbnailOf(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= THUMBNAIL_PX && height / (sample * 2) >= THUMBNAIL_PX) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val DIRECTORY = "incident-media"
        const val THUMBNAIL_PX = 320
    }
}
