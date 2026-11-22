package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminStudent
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardCountTest {

    private fun student(id: String, groupId: String) = AdminStudent(
        id = id,
        firstName = "Ana",
        lastName = "Pérez",
        fullName = "Ana Pérez",
        curp = "",
        listNumber = 1,
        groupId = groupId,
        schoolId = "sch-test",
        tutorIds = emptyList(),
        active = true,
    )

    @Test
    fun `the dashboard counts only students without a group`() {
        val students = listOf(
            student("a", ""),
            student("b", "grp-3a"),
            student("c", "   "),
            student("d", "grp-1b"),
        )

        assertEquals(2, ungroupedStudentCount(students))
    }

    @Test
    fun `an empty roster has no ungrouped students`() {
        assertEquals(0, ungroupedStudentCount(emptyList()))
    }
}
