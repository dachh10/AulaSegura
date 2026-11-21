package com.example.aulasegura.sync

import com.example.aulasegura.domain.repository.ApprovedStudentInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The approval payload travels through the outbox as JSON. It must round-trip
 * every field needed to reproduce the two-document batch later, including the
 * second-tutor case, and reject a corrupt row instead of writing a wrong student.
 */
class StudentApprovalPayloadTest {

    private val approval = PendingStudentApproval.from(
        input = ApprovedStudentInput(
            alumno = "Ana Pérez",
            tutorUid = "tutor-42",
            schoolId = "sch-1",
            createdBy = "directora-1",
            grado = "3",
            grupo = "B",
            groupId = "grp-3b",
            duplicateStudentId = "stu-2a-01",
        ),
        studentId = "stu-qr-1234",
    )

    @Test
    fun `a pending approval round trips through json`() {
        val json = encodeStudentApproval(approval)

        val decoded = decodeStudentApproval(json)

        assertEquals(approval, decoded)
    }

    @Test
    fun `a corrupt row is rejected without throwing`() {
        assertNull(decodeStudentApproval("not json"))
        assertNull(decodeStudentApproval("""{"studentId":"s"}"""))
    }
}
