package com.example.aulasegura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure rules for roster numbers: next available, cap at 30, alphabetical by surname. */
class ListNumberTest {

    private fun student(id: String, first: String, last: String, number: Int = 0) = AdminStudent(
        id = id,
        firstName = first,
        lastName = last,
        fullName = "$first $last",
        curp = "",
        listNumber = number,
        groupId = "grp-1a",
        schoolId = "sch-1",
        tutorIds = listOf("tutor-$id"),
        active = true,
    )

    @Test
    fun `the next number is the current maximum plus one`() {
        val roster = listOf(student("a", "Ana", "Lopez", 1), student("b", "Luis", "Ruiz", 3))

        assertEquals(4, nextListNumber(roster))
    }

    @Test
    fun `an empty roster starts at one`() {
        assertEquals(1, nextListNumber(emptyList()))
    }

    @Test
    fun `a full group of 30 has no next number`() {
        val full = (1..30).map { student("s$it", "N$it", "Apellido", it) }

        assertNull(nextListNumber(full))
    }

    @Test
    fun `renumbering sorts by paternal surname then first name`() {
        val roster = listOf(
            student("a", "Andres", "Lopez Obregon"),
            student("b", "Enrique", "Peña Nieto"),
            student("c", "Ana", "Lopez Obregon"),
        )

        val numbered = renumberRoster(roster)

        assertEquals(listOf("c", "a", "b"), numbered.map { it.studentId })
        assertEquals(listOf(1, 2, 3), numbered.map { it.listNumber })
    }

    @Test
    fun `renumbering never exceeds the 30 cap`() {
        val roster = (1..31).map { student("s$it", "N$it", "Apellido", 0) }

        val numbered = renumberRoster(roster)

        assertEquals(30, numbered.size)
        assertEquals(30, numbered.last().listNumber)
    }
}
