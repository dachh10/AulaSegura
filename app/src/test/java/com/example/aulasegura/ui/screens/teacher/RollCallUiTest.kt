package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.RollCallStudent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The chips and the rows must be describable without Compose: these are the
 * numbers the teacher reads before deciding the session is done.
 */
class RollCallUiTest {

    private fun student(
        id: String,
        number: Int,
        name: String,
        mark: AttendanceMark? = null,
    ) = RollCallStudent(
        id = id,
        listNumber = number,
        fullName = name,
        initials = "XX",
        mark = mark,
    )

    @Test
    fun `the subtitle skips the subject only when it is missing`() {
        assertEquals(
            "3° B · Matemáticas · Vie 14 nov",
            rollCallSubtitle("3° B", "Matemáticas", "Vie 14 nov"),
        )
        assertEquals(
            "3° B · Sin materia asignada · Vie 14 nov",
            rollCallSubtitle("3° B", "   ", "Vie 14 nov"),
        )
    }

    @Test
    fun `counts are derived from the marks, never from a stored counter`() {
        val counts = AttendanceCounts.of(
            listOf(
                AttendanceMark.PRESENT,
                AttendanceMark.PRESENT,
                AttendanceMark.ABSENT,
                AttendanceMark.LATE,
                AttendanceMark.JUSTIFIED,
                null,
            ),
        )

        assertEquals(2, counts.present)
        assertEquals(1, counts.absent)
        assertEquals(1, counts.late)
        assertEquals(1, counts.justified)
        assertEquals(5, counts.marked)
    }

    @Test
    fun `a row carries the official number and the initials of the names`() {
        val row = RollCallStudent(
            id = "stu-4",
            listNumber = 4,
            fullName = "Mariana Gómez Ruiz",
            initials = initialsOf("Mariana", "Gómez Ruiz"),
            mark = AttendanceMark.PRESENT,
        ).toRowUi()

        assertEquals("04", row.number)
        assertEquals("MG", row.initials)
        assertEquals("04", row.detail)
    }

    @Test
    fun `initials separate the given name from the surname, not first from last word`() {
        assertEquals("MG", initialsOf("Mariana", "Gómez Ruiz"))
        assertEquals("AA", initialsOf("Ana Sofía", "Aguilar"))
        assertEquals("?", initialsOf("", ""))
    }

    @Test
    fun `an untouched row says so instead of pretending a status`() {
        val row = student("stu-5", 5, "Elena Fuentes").toRowUi()

        assertNull(row.mark)
        assertEquals("05 · Sin registrar", row.detail)
    }

    @Test
    fun `each mark has its own second line`() {
        assertEquals(
            "07 · Retardo",
            student("s", 7, "Diego Escobar", AttendanceMark.LATE).toRowUi().detail,
        )
        assertEquals(
            "08 · Falta",
            student("s", 8, "Camila Domínguez", AttendanceMark.ABSENT).toRowUi().detail,
        )
        assertEquals(
            "09 · Justificado",
            student("s", 9, "Ana Sofía Aguilar", AttendanceMark.JUSTIFIED).toRowUi().detail,
        )
    }
}
