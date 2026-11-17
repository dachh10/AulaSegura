package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.MediaUploadPhase
import com.example.aulasegura.domain.model.SchoolSite
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

    // B14: el chip de la tarjeta dice la verdad del pin real y no "Verificada"
    // siempre. El `verified` que se guarda sale de este mismo veredicto.

    private val school = SchoolSite(
        schoolId = "school-aulasegu",
        name = "Secundaria AulaSegura",
        latitude = 19.292600,
        longitude = -99.655700,
        radiusMeters = 500.0,
    )

    @Test
    fun `an unmarked pin says so and verifies nothing`() {
        val verdict = incidentPinVerdict(null, null, school)

        assertFalse(verdict.verified)
        assertFalse(verdict.inside)
        assertNull(verdict.distanceMeters)
        assertEquals("Marca el lugar en el mapa", incidentPinChipLabel(verdict))
    }

    @Test
    fun `a pin on the school is inside and verified`() {
        val verdict = incidentPinVerdict(school.latitude, school.longitude, school)

        assertTrue(verdict.verified)
        assertTrue(verdict.inside)
        assertEquals(0.0, verdict.distanceMeters!!, 0.5)
        assertEquals("Dentro del plantel · Verificada", incidentPinChipLabel(verdict))
    }

    @Test
    fun `a pin outside the radius is not verified and prints the distance`() {
        // ~2.2 km al norte del plantel: fuera de un radio de 500 m.
        val verdict = incidentPinVerdict(school.latitude + 0.02, school.longitude, school)

        assertFalse("un pin fuera no puede guardarse como verificado", verdict.verified)
        assertFalse(verdict.inside)
        assertTrue("la distancia tiene que ser real", verdict.distanceMeters!! > school.radiusMeters)
        val chip = incidentPinChipLabel(verdict)
        assertTrue(chip, chip.startsWith("Fuera del plantel · "))
        assertTrue(chip, chip.endsWith(" m"))
    }

    @Test
    fun `without a cached school the pin cannot be verified`() {
        val verdict = incidentPinVerdict(19.2926, -99.6557, null)

        assertFalse(verdict.verified)
        assertNull(verdict.distanceMeters)
        assertEquals("Sin geocerca del plantel", incidentPinChipLabel(verdict))
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
