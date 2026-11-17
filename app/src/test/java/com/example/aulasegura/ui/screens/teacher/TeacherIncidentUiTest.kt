package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.MediaUploadPhase
import com.example.aulasegura.domain.model.SyncPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * The footer's enabled state is the screen's copy of the domain rules, so it is
 * checked here: a CTA that stays enabled without a student would send the teacher
 * into a rejection they cannot see.
 */
class TeacherIncidentUiTest {

    private val student = IncidentStudentUi(
        id = "stu-1",
        fullName = "Mariana Gómez Ruiz",
        initials = "MG",
        groupId = "grp-3b",
        groupLabel = "3° B",
    )

    @Test
    fun `every sync phase has a message and no phase has none`() {
        SyncPhase.entries.forEach { phase ->
            assertTrue("missing message for $phase", incidentSyncMessage(phase) != null)
        }
        assertNull(incidentSyncMessage(null))
    }

    @Test
    fun `saving needs a student and a description`() {
        val base = TeacherIncidentUi(students = listOf(student), description = "Se cayó")
        val withSeverity = IncidentSeverity.MODERADO

        assertFalse("no student chosen", base.copy(severity = withSeverity).canSave)
        assertTrue(
            "a complete form saves",
            base.copy(selectedId = "stu-1", severity = withSeverity).canSave,
        )
        assertFalse(
            "empty description",
            base.copy(selectedId = "stu-1", severity = withSeverity, description = " ").canSave,
        )
    }

    @Test
    fun `saving needs a severity, since the tutor triages by it`() {
        val complete = TeacherIncidentUi(
            students = listOf(student),
            selectedId = "stu-1",
            description = "Se cayó",
        )

        assertFalse("no severity chosen", complete.canSave)
        assertTrue(complete.copy(severity = IncidentSeverity.GRAVE).canSave)
    }

    @Test
    fun `the footer locks once the incident is queued`() {
        val saved = TeacherIncidentUi(
            students = listOf(student),
            selectedId = "stu-1",
            description = "Se cayó",
            saved = true,
            sync = SyncPhase.PENDING,
        )

        assertFalse(saved.canSave)
    }

    @Test
    fun `the selected student is resolved from the roster`() {
        val ui = TeacherIncidentUi(students = listOf(student), selectedId = "stu-1")

        assertEquals(student, ui.selected)
        assertNull(ui.copy(selectedId = "stu-404").selected)
    }

    @Test
    fun `severity labels match the mockup and the wire values match the schema`() {
        assertEquals("Leve", IncidentSeverity.LEVE.label)
        assertEquals("Moderado", IncidentSeverity.MODERADO.label)
        assertEquals("Grave", IncidentSeverity.GRAVE.label)
        assertEquals("leve", IncidentSeverity.LEVE.wire)
        assertEquals("moderado", IncidentSeverity.MODERADO.wire)
        assertEquals("grave", IncidentSeverity.GRAVE.wire)
    }

    @Test
    fun `every category has a label and the default is otro`() {
        assertEquals("Otro", TeacherIncidentUi().category.label)
        IncidentCategory.entries.forEach { kind ->
            assertTrue("missing label for $kind", kind.label.isNotBlank())
        }
        assertEquals(
            listOf("discusion", "caida", "danio", "desacato", "otro"),
            IncidentCategory.entries.map { it.wire },
        )
    }

    @Test
    fun `the date card prints the stored instant in the mockup's shape`() {
        val zone = TimeZone.getTimeZone("America/Mexico_City")
        val instant = Calendar.getInstance(zone, Locale.US)
            .apply {
                clear()
                set(2026, Calendar.NOVEMBER, 3, 10, 42, 0)
            }
            .timeInMillis

        assertEquals(
            "Mar 3 nov 2026 · 10:42",
            incidentTimestampLabel(instant, zone),
        )
    }

    @Test
    fun `the footer waits for the photo that is still being prepared`() {
        val complete = TeacherIncidentUi(
            students = listOf(student),
            selectedId = "stu-1",
            severity = IncidentSeverity.GRAVE,
            description = "Se cayó",
        )

        assertTrue(complete.canSave)
        assertFalse(
            "saving now would drop the photo the store is still compressing",
            complete.copy(preparing = true).canSave,
        )
    }

    @Test
    fun `the add tile disappears at the configured limit and not at three`() {
        val files = List(2) { IncidentEvidenceUi(localPath = "/tmp/$it.jpg") }
        val ui = TeacherIncidentUi(evidence = files, mediaLimit = 2)

        assertFalse("the limit is Remote Config's, whatever it says", ui.canAddEvidence)
        assertTrue(ui.copy(evidence = files.take(1)).canAddEvidence)
        assertFalse(
            "a queued incident takes no more photos",
            ui.copy(evidence = files.take(1), saved = true).canAddEvidence,
        )
    }

    @Test
    fun `the counter uses the delivered limit`() {
        assertEquals("2 de 3 archivos", incidentMediaCountLabel(2, 3))
        assertEquals("0 de 5 archivos", incidentMediaCountLabel(0, 5))
    }

    @Test
    fun `every upload phase has its own label and an unsaved file has none`() {
        MediaUploadPhase.entries.forEach { phase ->
            assertTrue("missing label for $phase", mediaPhaseLabel(phase) != null)
        }
        assertNull("the local file has nothing to report yet", mediaPhaseLabel(null))
        assertEquals("No se pudo subir", mediaPhaseLabel(MediaUploadPhase.FAILED))
    }
}
