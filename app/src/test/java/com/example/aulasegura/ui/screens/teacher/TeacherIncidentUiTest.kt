package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.SyncPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

        assertFalse("no student chosen", base.canSave)
        assertTrue(base.copy(selectedId = "stu-1").canSave)
        assertFalse("empty description", base.copy(selectedId = "stu-1", description = " ").canSave)
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
        assertNull(IncidentSeverity.fromWire("critico"))
    }
}
