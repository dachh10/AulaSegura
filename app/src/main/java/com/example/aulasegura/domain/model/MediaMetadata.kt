package com.example.aulasegura.domain.model

/**
 * Metadata of one file living in Cloudinary.
 *
 * This is the only thing that travels to Firestore: the image or video itself
 * never does, and neither does a base64 copy (CONTEXT §7.3).
 *
 * [assetFolder] is stored next to the rest on purpose. The upload preset does
 * not prefix the folder into the public id, so the folder is the only record of
 * where the asset lives; without it, a future cleanup task would have to guess.
 */
data class MediaMetadata(
    val publicId: String,
    val url: String,
    val thumbnailUrl: String,
    val assetFolder: String,
    val type: String,
    val format: String,
    val bytes: Long,
    val width: Int,
    val height: Int,
    val durationSec: Double? = null,
) {

    /** Firestore-shaped map, ready for `set()` or `update()`. */
    fun toFirestoreMap(): Map<String, Any?> = buildMap {
        put("publicId", publicId)
        put("url", url)
        put("thumbnailUrl", thumbnailUrl)
        put("assetFolder", assetFolder)
        put("type", type)
        put("format", format)
        put("bytes", bytes)
        put("width", width)
        put("height", height)
        durationSec?.let { put("durationSec", it) }
    }
}
