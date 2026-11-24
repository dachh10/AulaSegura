package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.JustificationDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The attendance effect of a review is decided here, before any Firestore call.
 * A wrong flip is not a UI glitch: it rewrites the history the tutor and the
 * teacher read, so the rules are pinned without a device.
 */
class JustifyAbsenceTest {

    @Test
    fun `approving an absence flips it and moves one count`() {
        val effect = justifyAbsence(
            decision = JustificationDecision.APPROVE,
            sessionId = "ses-1",
            studentId = "stu-3",
            statusesByStudent = statuses("stu-3" to "absent"),
        )!!

        assertEquals("ses-1", effect.sessionId)
        assertEquals("stu-3", effect.studentId)
        assertEquals("justified", effect.status)
        assertEquals(
            AttendanceCounts(present = 2, absent = 0, late = 1, justified = 2),
            effect.counts,
        )
    }

    @Test
    fun `rejecting leaves the absence as the teacher marked it`() {
        assertNull(
            justifyAbsence(
                decision = JustificationDecision.REJECT,
                sessionId = "ses-1",
                studentId = "stu-3",
                statusesByStudent = statuses("stu-3" to "absent"),
            ),
        )
    }

    @Test
    fun `approving twice does not rewrite an already justified record`() {
        assertNull(
            justifyAbsence(
                decision = JustificationDecision.APPROVE,
                sessionId = "ses-1",
                studentId = "stu-3",
                statusesByStudent = statuses("stu-3" to "justified"),
            ),
        )
    }

    @Test
    fun `a later present or late mark is not downgraded`() {
        for (status in listOf("present", "late")) {
            assertNull(
                justifyAbsence(
                    decision = JustificationDecision.APPROVE,
                    sessionId = "ses-1",
                    studentId = "stu-3",
                    statusesByStudent = statuses("stu-3" to status),
                ),
            )
        }
    }

    @Test
    fun `a student without a record is not flipped`() {
        assertNull(
            justifyAbsence(
                decision = JustificationDecision.APPROVE,
                sessionId = "ses-1",
                studentId = "stu-99",
                statusesByStudent = statuses("stu-3" to "absent"),
            ),
        )
    }

    @Test
    fun `an unknown status is not an absence and does not enter the counts`() {
        val effect = justifyAbsence(
            decision = JustificationDecision.APPROVE,
            sessionId = "ses-1",
            studentId = "stu-3",
            statusesByStudent = statuses("stu-3" to "absent") + ("stu-9" to "en-revision"),
        )!!

        assertEquals(5, effect.counts.marked)
        assertEquals(AttendanceCounts(present = 2, absent = 0, late = 1, justified = 2), effect.counts)
    }

    @Test
    fun `an unresolved session or student never produces a write`() {
        assertNull(
            justifyAbsence(
                decision = JustificationDecision.APPROVE,
                sessionId = "",
                studentId = "stu-3",
                statusesByStudent = statuses("stu-3" to "absent"),
            ),
        )
        assertNull(
            justifyAbsence(
                decision = JustificationDecision.APPROVE,
                sessionId = "ses-1",
                studentId = "",
                statusesByStudent = statuses("" to "absent"),
            ),
        )
    }

    private fun statuses(absence: Pair<String, String>): Map<String, String> = linkedMapOf(
        "stu-1" to "present",
        "stu-2" to "present",
        absence,
        "stu-4" to "late",
        "stu-5" to "justified",
    )
}
