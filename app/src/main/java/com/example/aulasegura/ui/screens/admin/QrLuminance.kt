package com.example.aulasegura.ui.screens.admin

/**
 * Converts ARGB pixels into the single Y plane ZXing reads. Kept pure so the
 * camera decoding path is verifiable on the JVM without a device.
 */
internal fun luminanceFromArgb(pixels: IntArray): ByteArray {
    val out = ByteArray(pixels.size)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        out[i] = ((r * 299 + g * 587 + b * 114) / 1000).toByte()
    }
    return out
}
