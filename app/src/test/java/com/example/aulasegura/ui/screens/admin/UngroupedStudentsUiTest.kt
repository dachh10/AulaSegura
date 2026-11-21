package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AdminGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure rules of the "Alumnos sin grupo" screen: what is unassigned and where it goes. */
class UngroupedStudentsUiTest {

    private fun student(id: String, groupId: String, name: String = "Alumno $id") = AdminStudent(
        id = id,
        firstName = name,
        lastName = "",
        fullName = name,
        curp = "",
        listNumber = 0,
        groupId = groupId,
        schoolId = "sch-1",
        tutorIds = listOf("tutor-$id"),
        active = true,
    )

    private fun group(id: String, grade: Int, section: String) = AdminGroup(
        id = id,
        name = "$grade° $section",
        grade = grade,
        section = section,
        schoolId = "sch-1",
        teacherIds = emptyList(),
        titularTeacherId = "",
        studentCount = 0,
        cycle = "2026-2027",
    )

    @Test
    fun `only students with a blank group id are unassigned`() {
        val all = listOf(
            student("a", ""),
            student("b", "grp-3b"),
            student("c", ""),
        )

        val ungrouped = ungroupedStudents(all, query = "")

        assertEquals(listOf("a", "c"), ungrouped.map { it.id })
    }

    @Test
    fun `the search filters by student or tutor name`() {
        val all = listOf(
            student("a", "", name = "Ana Pérez"),
            student("c", "", name = "Carlos Ruiz"),
        )

        assertEquals(
            listOf("c"),
            ungroupedStudents(all, query = "CARLOS").map { it.id },
        )
    }

    @Test
    fun `the selected grade and group resolve to the matching group id`() {
        val groups = listOf(group("grp-3b", 3, "B"), group("grp-4a", 4, "A"))

        val target = groupForAssignment(groups, grado = 3, grupo = "B")

        assertNotNull(target)
        assertEquals("grp-3b", target!!.id)
    }

    @Test
    fun `an unknown grade-group pair resolves to nothing`() {
        val groups = listOf(group("grp-3b", 3, "B"))

        assertNull(groupForAssignment(groups, grado = 5, grupo = "F"))
    }

    @Test
    fun `the batch updates exactly the selected ids`() {
        val selected = listOf("a", "c")
        val target = group("grp-3b", 3, "B")

        val updates = assignmentUpdates(selected, target)

        assertEquals(2, updates.size)
        assertTrue(updates.all { it.groupId == "grp-3b" && it.grado == 3 && it.grupo == "B" })
    }
}
