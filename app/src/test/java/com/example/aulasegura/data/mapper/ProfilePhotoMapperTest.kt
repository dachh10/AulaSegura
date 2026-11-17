package com.example.aulasegura.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * An avatar update must not touch the rest of `users/{uid}`: role, school and
 * active are written by other flows, and a whole-document `set()` from the
 * teacher's phone would race with them.
 */
class ProfilePhotoMapperTest {

    @Test
    fun `an avatar update sends only the two profile fields`() {
        val update = profilePhotoUpdate(
            photoUrl = "https://res.cloudinary.com/demo/image/upload/aulasegura/profiles/uid-1.jpg",
            photoPublicId = "aulasegura/profiles/uid-1",
        )

        assertEquals(
            setOf("photoUrl", "photoPublicId"),
            update.keys,
        )
        assertEquals("aulasegura/profiles/uid-1", update["photoPublicId"])
        assertFalse(update.containsKey("role"))
        assertFalse(update.containsKey("schoolId"))
        assertFalse(update.containsKey("active"))
    }

    @Test
    fun `an empty public id is still sent so the old one is not kept`() {
        val update = profilePhotoUpdate(photoUrl = "https://example.com/a.jpg", photoPublicId = "")

        assertEquals("", update["photoPublicId"])
        assertEquals("https://example.com/a.jpg", update["photoUrl"])
    }
}
