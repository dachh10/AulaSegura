package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.entity.UploadState
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The history caches what Firestore already holds, so the reverse mapper is the
 * only thing standing between a document and a row the teacher can read.
 *
 * A document that cannot stand on its own is skipped instead of cached: a
 * nameless row in the history looks like a bug, not like a broken document.
 */
class IncidentHistoryMapperTest {

    @Test
    fun `a document becomes a synced cache row`() {
        val entity = incidentEntityFrom("inc-1", document()) ?: error("the document is complete")

        assertEquals("inc-1", entity.incidentId)
        assertEquals("sch-benito-juarez", entity.schoolId)
        assertEquals("grp-3b", entity.groupId)
        assertEquals("Mariana Gómez Ruiz", entity.studentName)
        assertEquals("uid-docente", entity.reportedBy)
        assertEquals(12.4, entity.locationDistanceMeters!!, 0.0001)
        assertEquals(true, entity.locationVerified)
        assertEquals(listOf("uid-tutor"), entity.seenByTutorIds)
        assertEquals(
            "it came from the server: there is nothing left to push",
            SyncState.SYNCED,
            entity.syncState,
        )
    }

    @Test
    fun `the summary resolves the wire values the screen prints`() {
        val summary = incidentEntityFrom("inc-1", document())!!.toIncidentSummary(mediaCount = 2)

        assertEquals(IncidentSeverity.GRAVE, summary.severity)
        assertEquals(IncidentStatus.NUEVO, summary.status)
        assertEquals(2, summary.mediaCount)
        assertEquals(1_763_136_000_000L, summary.occurredAt)
    }

    @Test
    fun `a document without a group is skipped instead of cached half written`() {
        assertNull(incidentEntityFrom("inc-1", document() - "groupId"))
        assertNull(incidentEntityFrom("inc-1", document() - "reportedBy"))
        assertNull(incidentEntityFrom("inc-1", document() - "schoolId"))
    }

    @Test
    fun `a media entry becomes an uploaded row with no local file`() {
        val media = incidentMediaFrom("inc-1", mediaEntry()) ?: error("the entry has an id")

        assertEquals("m-1", media.mediaId)
        assertEquals("inc-1", media.incidentId)
        assertEquals("https://res.cloudinary.com/demo/photo.jpg", media.url)
        assertEquals(204_800L, media.bytes)
        assertEquals(1080, media.width)
        assertEquals(UploadState.UPLOADED, media.uploadState)
        assertNull("the local copy was deleted after the upload", media.localPath)
    }

    @Test
    fun `a media entry without an id is skipped`() {
        assertNull(incidentMediaFrom("inc-1", mediaEntry() - "id"))
    }

    @Test
    fun `the evidence row keeps the url the teacher will open`() {
        val evidence = incidentMediaFrom("inc-1", mediaEntry())!!.toIncidentEvidence()

        assertNotNull(evidence.url)
        assertEquals("jpg", evidence.format)
    }

    private fun document(): Map<String, Any?> = mapOf(
        "schoolId" to "sch-benito-juarez",
        "groupId" to "grp-3b",
        "studentId" to "stu-1",
        "studentName" to "Mariana Gómez Ruiz",
        "title" to "Se cayó en el patio",
        "description" to "Se cayó en el patio y se raspó la rodilla.",
        "severity" to "grave",
        "category" to "caida",
        "reportedBy" to "uid-docente",
        "reportedByName" to "Diego Hernández",
        "occurredAt" to Timestamp(1_763_136_000, 0),
        "status" to "nuevo",
        "seenByTutorIds" to listOf("uid-tutor"),
        "location" to mapOf(
            "lat" to 19.2926,
            "lng" to -99.6557,
            "distanceMeters" to 12.4,
            "verified" to true,
        ),
        "createdAt" to Timestamp(1_763_136_000, 0),
        "updatedAt" to Timestamp(1_763_136_000, 0),
    )

    private fun mediaEntry(): Map<String, Any?> = mapOf(
        "id" to "m-1",
        "type" to "image",
        "publicId" to "aulasegura/incidents/inc-1/photo",
        "url" to "https://res.cloudinary.com/demo/photo.jpg",
        "thumbnailUrl" to "https://res.cloudinary.com/demo/w_400/photo.jpg",
        "format" to "jpg",
        "bytes" to 204_800L,
        "width" to 1080,
        "height" to 810,
    )
}
