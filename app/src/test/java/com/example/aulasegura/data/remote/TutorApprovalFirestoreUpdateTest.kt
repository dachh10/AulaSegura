package com.example.aulasegura.data.remote

import com.example.aulasegura.domain.repository.tutorProfileOnApproval
import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression: approving another child must ADD to the tutor's denormalised
 * links. Replacing them hides the first child's group from the read rules.
 */
class TutorApprovalFirestoreUpdateTest {

    @Test
    fun `student and group links are written as array unions`() {
        val plain = tutorProfileOnApproval("stu-2", "grp-2a")

        val update = tutorApprovalFirestoreUpdate(plain)

        assertTrue(update["studentIds"] is FieldValue)
        assertTrue(update["groupIds"] is FieldValue)
        assertEquals("vinculado", update["vinculacion"])
        assertFalse(update.containsKey("updatedAt"))
    }

    @Test
    fun `a blank group leaves groupIds absent`() {
        val update = tutorApprovalFirestoreUpdate(tutorProfileOnApproval("stu-2", ""))

        assertTrue(update["studentIds"] is FieldValue)
        assertFalse(update.containsKey("groupIds"))
    }
}
