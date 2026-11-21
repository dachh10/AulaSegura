package com.example.aulasegura.ui.screens.parent

import org.junit.Assert.assertEquals
import org.junit.Test

/** Labels of the tutor's home when a child has no group yet. */
class ParentHomeLabelsTest {

    @Test
    fun `a child with no group shows a pending label, not a blank chip`() {
        assertEquals("Grupo por asignar", childGradeLabel(groupId = "", groupLabel = ""))
    }

    @Test
    fun `a child with a group shows its real name`() {
        assertEquals("3° B", childGradeLabel(groupId = "grp-3b", groupLabel = "3° B"))
    }

    @Test
    fun `a missing group name falls back to the group id`() {
        assertEquals("grp-4a", childGradeLabel(groupId = "grp-4a", groupLabel = ""))
    }

    @Test
    fun `a child with no attendance data says so honestly`() {
        assertEquals("Sin datos aún", childAttendanceStatus(percent = null))
        assertEquals("75%", childAttendanceStatus(percent = 75))
    }
}
