package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.JustificationEvidence
import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.ui.screens.justifications.justificationFailureMessage
import com.example.aulasegura.ui.screens.justifications.justificationSections
import com.example.aulasegura.ui.screens.justifications.toCardUi
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The inbox has two jobs: put what needs a decision first, and never show a raw
 * document id where a name belongs. Both are pure functions, so both are pinned
 * without Compose.
 */
class TeacherJustificationsUiTest {

    private val zone: TimeZone = TimeZone.getTimeZone("America/Mexico_City")

    @Test
    fun `pending reports come first and each section is newest absence first`() {
        val sections = justificationSections(
            listOf(
                record(id = "old-pending", date = 1_000L, status = JustificationStatus.PENDING),
                record(id = "new-pending", date = 3_000L, status = JustificationStatus.PENDING),
                record(id = "old-approved", date = 2_000L, status = JustificationStatus.APPROVED),
            ),
        )

        assertEquals(listOf("new-pending", "old-pending"), sections.pending.map { it.justificationId })
        assertEquals(listOf("old-approved"), sections.reviewed.map { it.justificationId })
    }

    @Test
    fun `a report with no denormalised name falls back to something readable`() {
        val card = record(id = "just-1", date = 1_000L, status = JustificationStatus.PENDING)
            .copy(studentName = "", groupLabel = "")
            .toCardUi(zone)

        assertEquals("Alumno", card.studentName)
        assertEquals("3B", card.groupLabel)
        assertEquals("Pendiente", card.statusLabel)
        assertEquals("Cita médica", card.reasonLabel)
    }

    @Test
    fun `a reviewed report carries the school's note and evidence`() {
        val card = record(id = "just-2", date = 1_000L, status = JustificationStatus.REJECTED)
            .copy(
                reviewNote = "Falta el comprobante",
                evidence = JustificationEvidence(
                    publicId = "demo/receta",
                    url = "https://res.cloudinary.com/demo/receta.pdf",
                    type = "pdf",
                ),
            )
            .toCardUi(zone)

        assertEquals("Rechazada", card.statusLabel)
        assertEquals("Falta el comprobante", card.reviewNote)
        assertEquals("https://res.cloudinary.com/demo/receta.pdf", card.evidenceUrl)
    }

    @Test
    fun `a blank review note is not rendered as an empty label`() {
        val card = record(id = "just-3", date = 1_000L, status = JustificationStatus.APPROVED)
            .copy(reviewNote = "   ")
            .toCardUi(zone)

        assertNull(card.reviewNote)
    }

    @Test
    fun `every failure reason becomes a sentence the tutor can act on`() {
        assertEquals(
            "Sin conexión. Revisa tu red e intenta de nuevo.",
            justificationFailureMessage(RosterFailure.OFFLINE),
        )
        assertEquals(
            "No tienes permiso para ver estas justificaciones.",
            justificationFailureMessage(RosterFailure.DENIED),
        )
        assertEquals(
            "No se pudieron cargar las justificaciones.",
            justificationFailureMessage(RosterFailure.UNKNOWN),
        )
    }

    private fun record(
        id: String,
        date: Long,
        status: JustificationStatus,
    ) = JustificationRecord(
        justificationId = id,
        studentId = "alumno-1",
        studentName = "Mariana Gómez Ruiz",
        groupId = "3B",
        groupLabel = "3° B",
        dateFromMillis = date,
        reason = "cita",
        description = "Consulta dental",
        status = status,
        evidence = null,
        createdAtMillis = date,
        reviewedAtMillis = null,
        reviewNote = null,
        sessionId = "sesion-1",
    )
}
