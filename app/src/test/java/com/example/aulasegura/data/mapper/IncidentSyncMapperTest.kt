package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.IncidentDraftEntity
import com.example.aulasegura.data.local.entity.IncidentMediaEntity
import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.domain.model.MediaUploadPhase
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The incident document is what the tutor and the admin read, and the rules only
 * accept it with `reportedBy == uid` and `status == 'nuevo'`. Both are pinned here
 * so a refactor of the entity cannot quietly break every write.
 *
 * Bloque 7 adds the `media` array, so the tests that used to assert its absence
 * now pin what each entry carries.
 */
class IncidentSyncMapperTest {

    @Test
    fun `the document carries the denormalised names the tutor needs`() {
        val map = draft().toFirestoreMap(emptyList())

        assertEquals("uid-docente", map["reportedBy"])
        assertEquals("Diego Hernández", map["reportedByName"])
        assertEquals("Mariana Gómez Ruiz", map["studentName"])
        assertEquals("stu-1", map["studentId"])
        assertEquals("nuevo", map["status"])
        assertEquals("grave", map["severity"])
        assertEquals("caida", map["category"])
    }

    @Test
    fun `the document never leaks local state`() {
        val map = draft().toFirestoreMap(emptyList())

        assertFalse("syncState is a Room concept", map.containsKey("syncState"))
        assertFalse("localPath is a path on this phone", map.toString().contains("localPath"))
        assertFalse("uploadState is a Room concept", map.toString().contains("uploadState"))
    }

    @Test
    fun `an incident without evidence publishes an empty media array`() {
        val map = draft().toFirestoreMap(emptyList())

        assertEquals(emptyList<Any>(), map["media"])
    }

    @Test
    fun `only uploaded files reach the media array`() {
        val map = draft().toFirestoreMap(
            listOf(
                uploaded(),
                uploaded().copy(mediaId = "media-2", url = null, uploadState = UploadState.PENDING),
                uploaded().copy(mediaId = "media-3", uploadState = UploadState.FAILED),
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val media = map["media"] as List<Map<String, Any?>>

        // A pending or failed file has no public id or url yet, so publishing it
        // would be an evidence entry nobody can open.
        assertEquals(1, media.size)
        assertEquals("media-1", media.single()["id"])
    }

    @Test
    fun `a media entry carries what a reader needs to render it`() {
        val map = draft().toFirestoreMap(listOf(uploaded()))

        @Suppress("UNCHECKED_CAST")
        val entry = (map["media"] as List<Map<String, Any?>>).single()

        assertEquals("media-1", entry["id"])
        assertEquals("image", entry["type"])
        assertEquals("aulasegura/incidents/inc-uuid/photo", entry["publicId"])
        assertEquals("https://res.cloudinary.com/demo/photo.jpg", entry["url"])
        assertEquals("https://res.cloudinary.com/demo/w_400/photo.jpg", entry["thumbnailUrl"])
        assertEquals("jpg", entry["format"])
        assertEquals(204_800L, entry["bytes"])
        assertEquals(1080, entry["width"])
        assertEquals(810, entry["height"])
        assertFalse("images have no duration", entry.containsKey("durationSec"))
        assertFalse("the folder is not part of the schema", entry.containsKey("assetFolder"))
    }

    @Test
    fun `timestamps go as Timestamps and the location as a nested map`() {
        val map = draft().toFirestoreMap(emptyList())

        assertTrue(map["occurredAt"] is Timestamp)
        assertTrue(map["createdAt"] is Timestamp)
        assertTrue(map["updatedAt"] is Timestamp)

        val location = map["location"] as Map<*, *>
        assertEquals(19.2926, location["lat"])
        assertEquals(-99.6557, location["lng"])
        assertEquals(true, location["verified"])
        assertFalse("no accuracy was captured", location.containsKey("accuracy"))
    }

    @Test
    fun `a draft without a pin has no location map at all`() {
        val map = draft().copy(locationLat = null, locationLng = null)
            .toFirestoreMap(emptyList())

        assertFalse(map.containsKey("location"))
        assertEquals(emptyList<String>(), map["seenByTutorIds"])
    }

    @Test
    fun `every upload state maps to the phase the screen prints`() {
        assertEquals(MediaUploadPhase.PENDING, UploadState.PENDING.toMediaUploadPhase())
        assertEquals(MediaUploadPhase.UPLOADING, UploadState.UPLOADING.toMediaUploadPhase())
        assertEquals(MediaUploadPhase.UPLOADED, UploadState.UPLOADED.toMediaUploadPhase())
        assertEquals(MediaUploadPhase.FAILED, UploadState.FAILED.toMediaUploadPhase())
    }

    private fun uploaded() = IncidentMediaEntity(
        mediaId = "media-1",
        incidentId = "inc-uuid",
        type = "image",
        localPath = "/data/user/0/com.example.aulasegura/files/incident-media/IMG_1.jpg",
        publicId = "aulasegura/incidents/inc-uuid/photo",
        url = "https://res.cloudinary.com/demo/photo.jpg",
        thumbnailUrl = "https://res.cloudinary.com/demo/w_400/photo.jpg",
        format = "jpg",
        bytes = 204_800L,
        width = 1080,
        height = 810,
        uploadState = UploadState.UPLOADED,
        createdAt = 1_763_136_000_000L,
        updatedAt = 1_763_136_000_000L,
    )

    private fun draft() = IncidentDraftEntity(
        incidentId = "inc-uuid",
        schoolId = "sch-benito-juarez",
        groupId = "grp-3b",
        studentId = "stu-1",
        studentName = "Mariana Gómez Ruiz",
        title = "Se cayó en el patio",
        description = "Se cayó en el patio y se raspó la rodilla.",
        severity = "grave",
        category = "caida",
        reportedBy = "uid-docente",
        reportedByName = "Diego Hernández",
        occurredAt = 1_763_136_000_000L,
        locationLat = 19.2926,
        locationLng = -99.6557,
        locationDistanceMeters = 12.0,
        locationVerified = true,
        status = "nuevo",
        syncState = SyncState.PENDING,
        createdAt = 1_763_136_000_000L,
        updatedAt = 1_763_136_000_000L,
    )
}
