package com.example.aulasegura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The Firestore map is the contract with the rules and with the screens, so its
 * key names are pinned here: a renamed field would be accepted by Firestore and
 * silently break every reader.
 */
class MediaMetadataTest {

    private val asset = MediaMetadata(
        publicId = "aulasegura/profiles/uid-1/abc123",
        url = "https://res.cloudinary.com/demo/image/upload/v1/abc123.jpg",
        thumbnailUrl = "https://res.cloudinary.com/demo/image/upload/w_96,h_96/v1/abc123.jpg",
        assetFolder = "aulasegura/profiles/uid-1",
        type = "image",
        format = "jpg",
        bytes = 48_213,
        width = 720,
        height = 1080,
    )

    @Test
    fun `image metadata maps to the documented keys`() {
        val map = asset.toFirestoreMap()

        assertEquals("aulasegura/profiles/uid-1/abc123", map["publicId"])
        assertEquals(asset.url, map["url"])
        assertEquals(asset.thumbnailUrl, map["thumbnailUrl"])
        assertEquals("aulasegura/profiles/uid-1", map["assetFolder"])
        assertEquals("image", map["type"])
        assertEquals("jpg", map["format"])
        assertEquals(48_213L, map["bytes"])
        assertEquals(720, map["width"])
        assertEquals(1080, map["height"])
    }

    @Test
    fun `duration is omitted for images and present for video`() {
        assertFalse(asset.toFirestoreMap().containsKey("durationSec"))

        val video = asset.copy(type = "video", format = "mp4", durationSec = 12.4)
        assertEquals(12.4, video.toFirestoreMap()["durationSec"])
    }
}
