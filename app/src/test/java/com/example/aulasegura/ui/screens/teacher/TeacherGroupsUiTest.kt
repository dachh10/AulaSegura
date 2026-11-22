package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The group card prints what Firestore actually has and hides what it does not.
 * A fake "94%" is worse than an empty ring, because a teacher believes it.
 */
class TeacherGroupsUiTest {

    private val group = Group(
        id = "grp-3b",
        name = "3° B",
        grade = 3,
        section = "B",
        schoolId = "sch-benito-juarez",
        titularTeacherId = "uid-docente",
        teacherIds = listOf("uid-docente"),
        studentCount = 28,
        cycle = "2026-2027",
    )

    @Test
    fun `the subject comes from the profile`() {
        val ui = group.toUi(subject = "Matemáticas")

        assertEquals("Matemáticas", ui.subject)
        assertEquals("3° B", ui.label)
        assertEquals(28, ui.students)
    }

    @Test
    fun `a profile with no subject is said out loud instead of left blank`() {
        val ui = group.toUi(subject = null)

        assertEquals("Sin materia asignada", ui.subject)
    }

    @Test
    fun `the subject filter offers every distinct subject, not only the first`() {
        val cards = listOf(
            group.toUi("Matemáticas III"),
            group.copy(id = "grp-5a", name = "5° A").toUi("Historia de México II"),
            group.copy(id = "grp-3a", name = "3° A").toUi("Matemáticas III"),
        )

        assertEquals(
            listOf("Matemáticas III", "Historia de México II"),
            distinctGroupSubjects(cards),
        )
    }

    @Test
    fun `attendance, schedule and roll call stay hidden until data exists`() {
        val ui = group.toUi(subject = "Matemáticas")

        assertNull(ui.attendance)
        assertNull(ui.time)
        assertNull(ui.rollCallDone)
    }
}
