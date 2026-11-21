package com.example.aulasegura.ui.screens.admin

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The camera decoder turns ARGB pixels into the single luminance plane ZXing
 * reads. Pure so the conversion is verified without a device.
 */
class QrLuminanceTest {

    @Test
    fun `white and black map to the luminance extremes`() {
        val luma = luminanceFromArgb(
            intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt()),
        )

        assertEquals(255, luma[0].toInt() and 0xFF)
        assertEquals(0, luma[1].toInt() and 0xFF)
    }

    @Test
    fun `one byte is produced per pixel`() {
        assertEquals(6, luminanceFromArgb(IntArray(6)).size)
    }
}
