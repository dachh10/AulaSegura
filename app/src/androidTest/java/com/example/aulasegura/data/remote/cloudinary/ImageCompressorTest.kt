package com.example.aulasegura.data.remote.cloudinary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Bitmaps cannot be exercised on the JVM, so the compressor is verified on a
 * device: it needs the real decoder to prove that the long side lands on 1080
 * and that the re-encode is actually a JPEG.
 */
@RunWith(AndroidJUnit4::class)
class ImageCompressorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val compressor = ImageCompressor(context)

    @Test
    fun scalesCameraSizedPhotoDownTo1080() {
        val source = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888)

        val jpeg = compressor.compress(source)
        val decoded = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

        assertEquals(1080, max(decoded.width, decoded.height))
        assertEquals(1080, decoded.width)
        assertEquals(720, decoded.height)
    }

    @Test
    fun keepsTheAspectRatio() {
        val source = Bitmap.createBitmap(2000, 3000, Bitmap.Config.ARGB_8888)

        val decoded = decode(compressor.compress(source))

        val expected = 2000f / 3000f
        assertTrue(abs(decoded.width.toFloat() / decoded.height - expected) < 0.01f)
        assertEquals(1080, max(decoded.width, decoded.height))
    }

    @Test
    fun neverUpscalesASmallImage() {
        val source = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)

        val decoded = decode(compressor.compress(source))

        assertEquals(400, decoded.width)
        assertEquals(300, decoded.height)
    }

    @Test
    fun writesARealJpeg() {
        val source = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)

        val jpeg = compressor.compress(source)

        assertEquals(0xFF.toByte(), jpeg[0])
        assertEquals(0xD8.toByte(), jpeg[1])
        assertEquals(0xFF.toByte(), jpeg[2])
    }

    @Test
    fun doesNotRecycleTheCallersBitmap() {
        val source = Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888)

        compressor.compress(source)

        assertFalse(source.isRecycled)
    }

    @Test
    fun compressesRawBytesThroughTheSamePipeline() {
        val png = ByteArrayOutputStream().use { out ->
            Bitmap.createBitmap(2400, 1600, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }

        val jpeg = compressor.compress(png)
        val decoded = decode(jpeg)

        assertEquals(1080, max(decoded.width, decoded.height))
        assertEquals(0xFF.toByte(), jpeg[0])
        assertEquals(0xD8.toByte(), jpeg[1])
    }

    @Test
    fun returnsNonImageBytesUntouched() {
        val notAnImage = "no soy una imagen".toByteArray()

        assertSame(notAnImage, compressor.compress(notAnImage))
    }

    private fun decode(jpeg: ByteArray): Bitmap =
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
}
