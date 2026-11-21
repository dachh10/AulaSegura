package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.repository.approvedStudentDocument
import com.example.aulasegura.domain.repository.ApprovedStudentInput
import com.example.aulasegura.domain.repository.tutorProfileOnApproval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The review step turns a scanned QR plus a grade/group choice into one write
 * plan. Pure so the rules (duplicate, sin grupo, group ids) are verified without
 * Firestore or a device.
 */
class ReviewStudentUiTest {

    private fun form(
        grado: String = "3",
        grupo: String = "B",
        sinGrupo: Boolean = false,
    ) = ReviewStudentForm(grado = grado, grupo = grupo, sinGrupo = sinGrupo)

    @Test
    fun `a student with no group keeps grade and group empty`() {
        val plan = approvalPlan(
            tutorUid = "tutor-1",
            alumno = "Ana Pérez",
            form = form(grado = "3", grupo = "B", sinGrupo = true),
            schoolId = "sch-1",
            createdBy = "directora-1",
        )

        assertEquals("", plan.grado)
        assertEquals("", plan.grupo)
        assertEquals("", plan.groupId)
        assertNull(plan.duplicateStudentId)
    }

    @Test
    fun `an assigned student carries the grade and group id`() {
        val plan = approvalPlan(
            tutorUid = "tutor-1",
            alumno = "Ana Pérez",
            form = form(grado = "3", grupo = "B"),
            schoolId = "sch-1",
            createdBy = "directora-1",
        )

        assertEquals("3", plan.grado)
        assertEquals("B", plan.grupo)
        assertEquals("grp-3b", plan.groupId)
    }

    @Test
    fun `an existing name is offered as a second tutor instead of a duplicate`() {
        val plan = approvalPlan(
            tutorUid = "tutor-2",
            alumno = "Luis García",
            form = form(),
            schoolId = "sch-1",
            createdBy = "directora-1",
            existing = listOf(
                ExistingStudent(name = "Luis García", studentId = "stu-2a-01", groupId = "grp-2a"),
            ),
        )

        assertEquals("stu-2a-01", plan.duplicateStudentId)
        assertFalse(plan.grupo.isBlank())
    }

    @Test
    fun `the shared student document carries both tutor vocabularies`() {
        val input = ApprovedStudentInput(
            alumno = "Ana Pérez",
            tutorUid = "tutor-42",
            schoolId = "sch-1",
            createdBy = "directora-1",
            grado = "3",
            grupo = "B",
            groupId = "grp-3b",
        )

        val doc = approvedStudentDocument(input, schoolId = "sch-1")

        assertEquals("Ana Pérez", doc["nombre"])
        assertEquals(listOf("tutor-42"), doc["tutores"])
        assertEquals(listOf("tutor-42"), doc["tutorIds"])
        assertEquals("3", doc["grado"])
        assertEquals("B", doc["grupo"])
        assertEquals("grp-3b", doc["groupId"])
        assertEquals("directora-1", doc["creadoPor"])
        assertEquals("sch-1", doc["schoolId"])
    }

    @Test
    fun `approval links the student and group into the tutor profile`() {
        val update = tutorProfileOnApproval(
            studentId = "stu-qr-1",
            groupId = "grp-3b",
        )

        assertEquals(listOf("stu-qr-1"), update["studentIds"])
        assertEquals(listOf("grp-3b"), update["groupIds"])
        assertEquals("vinculado", update["vinculacion"])
    }

    @Test
    fun `a student with no group does not add a bogus group id`() {
        val update = tutorProfileOnApproval(studentId = "stu-qr-1", groupId = "")

        assertEquals(listOf("stu-qr-1"), update["studentIds"])
        assertFalse(update.containsKey("groupIds"))
        assertEquals("vinculado", update["vinculacion"])
    }

    @Test
    fun `an empty name is rejected before any write`() {
        val plan = approvalPlan(
            tutorUid = "tutor-1",
            alumno = "   ",
            form = form(),
            schoolId = "sch-1",
            createdBy = "directora-1",
        )

        assertNull(plan.student)
        assertTrue(plan.error != null)
    }
}
