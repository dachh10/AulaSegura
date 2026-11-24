package com.example.aulasegura.domain.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * A rejected teacher can reapply without deleting the Auth account. The write
 * goes to their own document, restores 'pendiente' and drops the old assignment
 * so the director decides again on a fresh request.
 */
class TeacherResubmitTest {

    @Test
    fun `the resubmit only touches the request fields and the state`() {
        val patch = teacherResubmitPatch("grp-2a", "g2-matematicas-ii", now = 1_700_000_000_000L)

        assertEquals("pendiente", patch["estado"])
        assertEquals("grp-2a", patch["requestedGroupId"])
        assertEquals("g2-matematicas-ii", patch["requestedSubject"])
        assertEquals(listOf("Matemáticas II"), patch["subjects"])
        assertEquals(1_700_000_000_000L, patch["updatedAt"])
        assertFalse("no identity or school may travel", "role" in patch)
        assertFalse("schoolId" in patch)
        assertFalse("active" in patch)
        assertFalse("groupIds" in patch)
    }

    @Test
    fun `an empty group or subject cannot be resubmitted`() {
        assertEquals(null, validateResubmit("", "g2-matematicas-ii"))
        assertEquals(null, validateResubmit("grp-2a", ""))
        assertEquals("grp-2a", validateResubmit("grp-2a", "g2-matematicas-ii")?.groupId)
    }
}
