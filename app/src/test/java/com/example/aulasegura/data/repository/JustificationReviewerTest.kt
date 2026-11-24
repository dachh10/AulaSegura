package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Only the teacher who marked the absence may decide on that family's report.
 * The principal keeps read access but never gets a decision button.
 */
class JustificationReviewerTest {

    @Test
    fun `the teacher of the session may decide`() {
        assertTrue(canDecideJustification(role = UserRole.TEACHER, isSessionTeacher = true))
    }

    @Test
    fun `a different teacher of the same group may not decide`() {
        assertFalse(canDecideJustification(role = UserRole.TEACHER, isSessionTeacher = false))
    }

    @Test
    fun `the principal may not decide even though they can read`() {
        assertFalse(canDecideJustification(role = UserRole.ADMIN, isSessionTeacher = true))
    }

    @Test
    fun `a tutor may never decide`() {
        assertFalse(canDecideJustification(role = UserRole.TUTOR, isSessionTeacher = true))
    }
}
