package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.JustificationStatus
import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The inbox reads documents the app does not control: an old report queued by a
 * previous build, or one edited by hand in the console. The mapping is the only
 * place that decides whether that document stays visible, so it is pinned here.
 */
class JustificationInboxMappingTest {

    @Test
    fun `a complete report keeps every field the inbox renders`() {
        val record = justificationFrom("just-1", document())!!

        assertEquals("just-1", record.justificationId)
        assertEquals("alumno-1", record.studentId)
        assertEquals("Mariana Gómez Ruiz", record.studentName)
        assertEquals("3B", record.groupId)
        assertEquals("3° B", record.groupLabel)
        assertEquals("sesion-1", record.sessionId)
        assertEquals("doc-mates", record.teacherId)
        assertEquals("Matemáticas", record.subject)
        assertEquals("enfermedad", record.reason)
        assertEquals("Fiebre de 38 grados", record.description)
        assertEquals(JustificationStatus.APPROVED, record.status)
        assertEquals(1_763_100_000_000L, record.dateFromMillis)
        assertEquals("https://res.cloudinary.com/demo/receta.jpg", record.evidence?.url)
        assertEquals("image", record.evidence?.type)
        assertEquals("nota", record.reviewNote)
    }

    @Test
    fun `a document written before the inbox fields existed stays readable`() {
        val legacy = document() - "studentName" - "groupLabel" - "sessionId" -
            "teacherId" - "subjectId" - "subject" - "status"

        val record = justificationFrom("just-legacy", legacy)!!

        assertEquals("", record.studentName)
        assertEquals("", record.groupLabel)
        assertEquals("", record.sessionId)
        assertEquals("", record.teacherId)
        assertEquals("", record.subject)
        assertEquals(
            "a missing status must stay actionable, never disappear",
            JustificationStatus.PENDING,
            record.status,
        )
    }

    @Test
    fun `the old accepted status still means approved`() {
        val record = justificationFrom("just-2", document() + ("status" to "aceptada"))!!

        assertEquals(JustificationStatus.APPROVED, record.status)
    }

    @Test
    fun `an unknown status is pending instead of hidden`() {
        val record = justificationFrom("just-3", document() + ("status" to "en-revision"))!!

        assertEquals(JustificationStatus.PENDING, record.status)
    }

    @Test
    fun `a document without the keys that identify it is skipped`() {
        assertNull(justificationFrom("just-4", document() - "studentId"))
        assertNull(justificationFrom("just-5", document() - "groupId"))
    }

    @Test
    fun `an attachment without a url is not offered to the reader`() {
        val record = justificationFrom(
            "just-6",
            document() + ("attachment" to mapOf("publicId" to "x", "type" to "image")),
        )!!

        assertNull(record.evidence)
    }

    private fun document(): Map<String, Any?> = mapOf(
        "studentId" to "alumno-1",
        "studentName" to "Mariana Gómez Ruiz",
        "groupId" to "3B",
        "groupLabel" to "3° B",
        "sessionId" to "sesion-1",
        "teacherId" to "doc-mates",
        "subjectId" to "g3-matematicas-iii",
        "subject" to "Matemáticas",
        "reason" to "enfermedad",
        "description" to "Fiebre de 38 grados",
        "status" to "aprobada",
        "dateFrom" to Timestamp(Date(1_763_100_000_000L)),
        "createdAt" to Timestamp(Date(1_763_000_000_000L)),
        "reviewedAt" to Timestamp(Date(1_763_200_000_000L)),
        "reviewNote" to "nota",
        "attachment" to mapOf(
            "publicId" to "demo/receta",
            "url" to "https://res.cloudinary.com/demo/receta.jpg",
            "type" to "image",
        ),
    )
}
