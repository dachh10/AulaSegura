package com.example.aulasegura.data.remote.cloudinary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Shrinks an image before it leaves the phone.
 *
 * Kept as an interface for the same reason [CloudinaryUploader] is one: the
 * profile ViewModel has to be testable without an Android [Context], and the
 * compression is the one step of the avatar flow that needs one.
 */
interface PhotoCompressor {

    /** Compresses the image behind [uri]. Returns `null` if it cannot be read. */
    suspend fun compress(uri: Uri): ByteArray?
}

/**
 * Shrinks an image before it leaves the phone.
 *
 * Cloudinary's free plan is charged by stored bytes and traffic (CONTEXT §7.2),
 * so a 4 MB camera photo is money spent for nothing: the detail screen never
 * shows more than 1080 px anyway.
 *
 * The pipeline is deliberately boring:
 * 1. read the bounds without decoding the pixels,
 * 2. decode at a power-of-two sample so the bitmap never enters memory at full
 *    size,
 * 3. apply the EXIF rotation, because re-encoding drops the tag and a portrait
 *    photo would otherwise arrive sideways,
 * 4. scale to [MAX_DIMENSION] on the long side,
 * 5. re-encode as JPEG at [JPEG_QUALITY].
 */
class ImageCompressor(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PhotoCompressor {

    private val resolver = context.applicationContext.contentResolver

    /** Compresses the image behind [uri]. Returns `null` if it cannot be read. */
    override suspend fun compress(uri: Uri): ByteArray? = withContext(ioDispatcher) {
        val raw = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: IOException) {
            null
        }
        raw?.let { compress(it) }
    }

    /** Compresses raw image bytes. Returns the input untouched if it is not an image. */
    fun compress(source: ByteArray): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return source

        val decoded = BitmapFactory.decodeByteArray(
            source,
            0,
            source.size,
            BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            },
        ) ?: return source

        // Every bitmap this method creates is recycled before returning; a
        // camera photo is tens of megabytes and low-end devices notice.
        val created = mutableListOf(decoded)

        val oriented = applyExifRotation(source, decoded).also {
            if (it !== decoded) created += it
        }
        val scaled = scaleDown(oriented).also {
            if (it !== oriented) created += it
        }

        val jpeg = scaled.toJpeg()
        created.forEach { it.recycle() }
        return jpeg
    }

    /** Compresses an in-memory bitmap. The caller keeps ownership of [source]. */
    fun compress(source: Bitmap): ByteArray {
        val scaled = scaleDown(source)
        val jpeg = scaled.toJpeg()
        if (scaled !== source) scaled.recycle()
        return jpeg
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sample = 1
        // Halve while the decoded image would still be at least twice the target,
        // so the bitmap never lands in memory far above what we need.
        while (max(width, height) / sample >= MAX_DIMENSION * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(source: Bitmap): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= MAX_DIMENSION) return source

        val ratio = MAX_DIMENSION.toFloat() / longest
        val targetWidth = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val targetHeight = (source.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun applyExifRotation(source: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = try {
            ExifInterface(ByteArrayInputStream(source))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
    }

    private fun Bitmap.toJpeg(): ByteArray = ByteArrayOutputStream().use { out ->
        compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        out.toByteArray()
    }

    companion object {
        /** Longest side kept, in pixels. Above this the free plan pays for nothing. */
        const val MAX_DIMENSION: Int = 1080

        /** JPEG quality. 80 is the point where the file shrinks without visible loss. */
        const val JPEG_QUALITY: Int = 80
    }
}
