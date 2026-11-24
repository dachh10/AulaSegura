package com.example.aulasegura.data.mapper

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A justification is write-only, so the only thing that can be checked without a
 * device is the document it produces. The two halves that matter: the absence day
 * is the school's, and evidence only travels when Cloudinary already has it.
 */
class JustificationPayloadTest {

    @Test
    fun `the payload survives the JSON column`() {
        val decoded = decodeJustificationPayload(encodeJustificationPayload(payload()))

        assertEquals(payload(), decoded)
    }

    @Test
    fun `an unreadable payload is reported instead of crashing the worker`() {
        assertNull(decodeJustificationPayload("{ esto no es json"))
        assertNull(decodeJustificationPayload(""))
    }

    @Test
    fun `the document carries exactly the schema keys`() {
        val document = payload().toDocument()

        assertEquals("std-1", document["studentId"])
        assertEquals("uid-tutor", document["tutorId"])
        assertEquals("sch-benito", document["schoolId"])
        assertEquals("grp-3b", document["groupId"])
        assertEquals("enfermedad", document["reason"])
        assertEquals("Fiebre de 38 grados", document["description"])
        assertEquals("the tutor only creates; the school decides", "pendiente", document["status"])
        assertEquals("sesion-1", document["sessionId"])
        assertEquals("Mariana Gómez Ruiz", document["studentName"])
        assertEquals("3° B", document["groupLabel"])
    }

    @Test
    fun `a legacy draft without session or display data does not write empty keys`() {
        val document = payload()
            .copy(sessionId = "", studentName = "", groupLabel = "")
            .toDocument()

        assertFalse(document.containsKey("sessionId"))
        assertFalse(document.containsKey("studentName"))
        assertFalse(document.containsKey("groupLabel"))
    }

    @Test
    fun `the absence dates are timestamps of the day the school marked`() {
        val document = payload().toDocument()

        assertEquals(Timestamp(ABSENCE_MILLIS / 1000, 0), document["dateFrom"])
        assertEquals(Timestamp(ABSENCE_MILLIS / 1000, 0), document["dateTo"])
    }

    @Test
    fun `an uploaded receipt travels with its url`() {
        val document = payload(attachment = uploaded()).toDocument()

        @Suppress("UNCHECKED_CAST")
        val map = document["attachment"] as Map<String, Any?>

        assertEquals("aulasegura/justifications/jst-1/receta", map["publicId"])
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/receta.jpg", map["url"])
        assertEquals("image", map["type"])
    }

    @Test
    fun `a receipt still on the phone is not written into the document`() {
        val document = payload(attachment = pending()).toDocument()

        assertNull(
            "a document without evidence is honest; a placeholder URL is not",
            document["attachment"],
        )
    }

    @Test
    fun `a receipt the upload could not deliver is not written either`() {
        val failed = pending().copy(state = StoredJustificationAttachment.FAILED, localPath = null)
        val document = payload(attachment = failed).toDocument()

        assertNull(document["attachment"])
        assertEquals("the report still reaches the school", "pendiente", document["status"])
    }

    @Test
    fun `a PDF receipt is typed as a document`() {
        val pdf = pending().copy(fileName = "receta.pdf", mimeType = "application/pdf")

        assertEquals("pdf", pdf.type)
    }

    @Test
    fun `an upload that finished forgets the local path`() {
        val stored = uploaded()

        assertTrue(stored.isUploaded)
        assertNull(stored.localPath)
    }

    @Test
    fun `a pending upload is not uploaded`() {
        assertFalse(pending().isUploaded)
    }

    private fun payload(
        attachment: StoredJustificationAttachment? = null,
    ) = JustificationPayload(
        studentId = "std-1",
        tutorId = "uid-tutor",
        schoolId = "sch-benito",
        groupId = "grp-3b",
        dateFromMillis = ABSENCE_MILLIS,
        dateToMillis = ABSENCE_MILLIS,
        reason = "enfermedad",
        description = "Fiebre de 38 grados",
        createdAtMillis = 1_763_100_000_000L,
        attachment = attachment,
        sessionId = "sesion-1",
        studentName = "Mariana Gómez Ruiz",
        groupLabel = "3° B",
    )

    private fun pending() = StoredJustificationAttachment(
        fileName = "receta.jpg",
        bytes = 204_800L,
        mimeType = "image/jpeg",
        localPath = "/data/app/justification-attachments/JUSTIFICATION_1.jpg",
    )

    private fun uploaded() = StoredJustificationAttachment(
        fileName = "receta.jpg",
        bytes = 204_800L,
        mimeType = "image/jpeg",
        publicId = "aulasegura/justifications/jst-1/receta",
        url = "https://res.cloudinary.com/demo/image/upload/v1/receta.jpg",
        state = StoredJustificationAttachment.UPLOADED,
    )

    private companion object {
        /** 2026-10-31, local midnight in Mexico City, rounded to whole seconds. */
        const val ABSENCE_MILLIS = 1_793_437_200_000L
    }
}
