package com.example.aulasegura.data.remote.cloudinary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These two rules are pure string work, which is exactly why they are tested
 * here: a doubly applied transformation or an escaped folder path fails silently
 * in production and costs money or breaks a listing.
 */
class CloudinaryUrlTest {

    private val original = "https://res.cloudinary.com/demo/image/upload/v1712/aulasegura/incidents/abc/photo.jpg"

    @Test
    fun `inserts the transformation before the version segment`() {
        val result = CloudinaryTransformations.apply(original, "w_96,h_96,c_fill,g_face")

        assertEquals(
            "https://res.cloudinary.com/demo/image/upload/w_96,h_96,c_fill,g_face/v1712/aulasegura/incidents/abc/photo.jpg",
            result,
        )
    }

    @Test
    fun `works without a version segment`() {
        val noVersion = "https://res.cloudinary.com/demo/image/upload/aulasegura/incidents/abc/photo.jpg"

        val result = CloudinaryTransformations.apply(noVersion, "w_400,c_limit")

        assertEquals(
            "https://res.cloudinary.com/demo/image/upload/w_400,c_limit/aulasegura/incidents/abc/photo.jpg",
            result,
        )
    }

    @Test
    fun `does not stack a second transformation`() {
        val already = "https://res.cloudinary.com/demo/image/upload/w_400,c_limit/v1712/aulasegura/photo.jpg"

        assertEquals(already, CloudinaryTransformations.apply(already, "w_96,h_96"))
    }

    @Test
    fun `leaves a non-cloudinary url untouched`() {
        assertEquals("https://example.com/photo.jpg", CloudinaryTransformations.apply("https://example.com/photo.jpg", "w_96"))
    }

    @Test
    fun `an empty transformation is a no-op`() {
        assertEquals(original, CloudinaryTransformations.apply(original, " "))
    }

    @Test
    fun `folder path uses the documented layout`() {
        assertEquals("aulasegura/profiles/uid-1", CloudinaryFolder.PROFILES.path("uid-1"))
        assertEquals("aulasegura/incidents/inc-9", CloudinaryFolder.INCIDENTS.path("inc-9"))
    }

    @Test
    fun `folder path cannot escape its prefix`() {
        val escaped = CloudinaryFolder.PROFILES.path("../etc")

        assertTrue(escaped.startsWith("aulasegura/profiles/"))
        assertFalse(escaped.contains(".."))
        assertFalse(escaped.substringAfter("aulasegura/profiles/").contains("/"))
        assertEquals("aulasegura/profiles/sin-id", CloudinaryFolder.PROFILES.path(""))
        assertEquals("aulasegura/profiles/sin-id", CloudinaryFolder.PROFILES.path("///"))
    }

    @Test
    fun `every folder keeps the root prefix`() {
        CloudinaryFolder.entries.forEach { folder ->
            assertTrue(folder.path("x").startsWith("aulasegura/"))
        }
    }
}
