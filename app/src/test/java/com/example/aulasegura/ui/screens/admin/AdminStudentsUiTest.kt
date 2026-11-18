package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.StudentListEntry
import com.example.aulasegura.domain.model.orderStudentList
import com.example.aulasegura.domain.model.studentListComparator
import com.example.aulasegura.domain.repository.AdminStudentError
import com.example.aulasegura.domain.repository.AdminStudentFields
import com.example.aulasegura.domain.repository.CreateStudentResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The CURP guard, the alphabetical roster ordering, the new-student document and
 * the group filter are all pure functions. These tests pin the invariants the
 * teacher's roll call depends on: every created document carries groupId, schoolId,
 * active and an empty tutorIds list, and the list numbers follow apellidos.
 */
class AdminStudentsUiTest {

    private val form = AdminStudentForm(
        firstName = "  Ana  ",
        lastName = " Ruiz ",
        curp = "RUUA890101HDFRRS09",
        groupId = "grp-3b",
    )

    private fun entry(id: String, lastName: String, firstName: String, number: Int) =
        StudentListEntry(id, lastName, firstName, number)

    /** The last entry plays the new student; the rest are the existing roster. */
    private fun ordered(vararg entries: StudentListEntry) =
        orderStudentList(entries.dropLast(1), entries.last())

    private fun student(
        id: String,
        name: String,
        groupId: String,
        listNumber: Int,
        active: Boolean = true,
    ) = AdminStudent(
        id = id,
        firstName = name.substringBefore(' '),
        lastName = name.substringAfter(' ', ""),
        fullName = name,
        curp = "CURP$id",
        listNumber = listNumber,
        groupId = groupId,
        schoolId = "school-1",
        tutorIds = emptyList(),
        active = active,
    )

    private val students = listOf(
        student("stu-3b-07", "Ana Ruiz", "grp-3b", 7),
        student("stu-3b-08", "Luis Pérez", "grp-3b", 8),
        student("stu-1a-03", "Ana Torres", "grp-1a", 3),
        student("stu-1a-04", "Marta Ruiz", "grp-1a", 4, active = false),
    )

    @Test
    fun `curp accepts a valid 18-character uppercase value and rejects bad ones`() {
        assertTrue(isValidCurp("RUUA890101HDFRRS09"))
        assertTrue(isValidCurp(" RUUA890101HDFRRS09 "))
        assertFalse(isValidCurp("RUUA890101HDFRRS0")) // short
        assertFalse(isValidCurp("ruua890101hdfrrs09")) // lowercase
        assertFalse(isValidCurp("RUUA890101HDFRRS099")) // too long
        assertFalse(isValidCurp("RUUA890101HDFRRS0-")) // non-alphanumeric
        assertTrue(isValidCurp("")) // optional in the form

        assertNull(validationError(form))
        assertTrue(validationError(form.copy(curp = "ruua890101hdfrrs09"))!!.contains("CURP"))

        assertFalse(isValid(form.copy(firstName = " ")))
        assertFalse(isValid(form.copy(lastName = "")))
        assertFalse(isValid(form.copy(groupId = "")))
    }

    @Test
    fun `perez sorts before pineda by the second letter of the primer apellido`() {
        val result = ordered(
            entry("pineda", "Pineda", "Luis", 1),
            entry("perez", "Pérez", "Ana", 2),
        )

        assertEquals(listOf("perez", "pineda"), result.map { it.id })
        assertEquals(listOf(1, 2), result.map { it.listNumber })
    }

    @Test
    fun `accents and case do not change the apellido order`() {
        // Same id isolates the collation: accents and case are PRIMARY-equal.
        val accented = entry("same", "Álvarez", "Ana", 1)
        val plain = entry("same", "Alvarez", "ana", 2)
        assertEquals(0, studentListComparator.compare(accented, plain))

        val result = ordered(
            entry("a2", "Alvarez", "ana", 1),
            entry("a1", "Álvarez", "Ana", 2),
        )
        // With equal apellidos and nombres the document id breaks the tie.
        assertEquals(listOf("a1", "a2"), result.map { it.id })
    }

    @Test
    fun `enye sorts after n but before o`() {
        assertTrue(studentListComparator.compare(
            entry("n", "Nuñez", "Ana", 1),
            entry("enye", "Ñuñez", "Ana", 2),
        ) < 0)
        assertTrue(studentListComparator.compare(
            entry("enye", "Ñuñez", "Ana", 1),
            entry("o", "Ortiz", "Ana", 2),
        ) < 0)

        val result = ordered(
            entry("n", "Nuñez", "Ana", 1),
            entry("enye", "Ñuñez", "Ana", 2),
        )
        assertEquals(listOf("n", "enye"), result.map { it.id })
    }

    @Test
    fun `same apellidos break ties by nombre and then by document id`() {
        val result = ordered(
            entry("b", "Ruiz", "Zoe", 1),
            entry("a", "Ruiz", "Ana", 2),
            entry("c", "Ruiz", "Ana", 3),
        )

        assertEquals(listOf("a", "c", "b"), result.map { it.id })
        assertEquals(listOf(1, 2, 3), result.map { it.listNumber })
    }

    @Test
    fun `numbers are contiguous 1 to N whatever the stored numbers were`() {
        val result = ordered(
            entry("z", "Zapata", "Ana", 40),
            entry("m", "Morales", "Luis", 7),
            entry("p", "Pérez", "Ana", 12),
        )

        assertEquals(listOf("m", "p", "z"), result.map { it.id })
        assertEquals((1..3).toList(), result.map { it.listNumber })

        val first = ordered(entry("solo", "Solo", "Ana", 0))
        assertEquals(1, first.single().listNumber)
    }

    @Test
    fun `student document carries the roll-call fields and no identity field`() {
        val computedNumber = ordered(
            entry("previous", "Álvarez", "Ana", 1),
            entry("new", form.lastName.trim(), form.firstName.trim(), 0),
        ).first { it.id == "new" }.listNumber
        assertEquals(2, computedNumber)

        val doc = buildStudentDocument(form, "school-1", computedNumber)

        assertEquals("grp-3b", doc[AdminStudentFields.GROUP_ID])
        assertEquals("school-1", doc[AdminStudentFields.SCHOOL_ID])
        assertEquals(true, doc[AdminStudentFields.ACTIVE])
        assertEquals(emptyList<String>(), doc[AdminStudentFields.TUTOR_IDS])
        assertEquals("Ana", doc[AdminStudentFields.FIRST_NAME])
        assertEquals("Ruiz", doc[AdminStudentFields.LAST_NAME])
        assertEquals("Ana Ruiz", doc[AdminStudentFields.FULL_NAME])
        assertEquals("RUUA890101HDFRRS09", doc[AdminStudentFields.CURP])
        assertEquals(computedNumber, doc[AdminStudentFields.LIST_NUMBER])
        assertEquals("stu-3b-02", AdminStudentFields.studentId("grp-3b", computedNumber))

        val identityFields = setOf(
            "id", "uid", "studentId", "email", "role", "createdAt", "updatedAt",
        )
        assertTrue(doc.keys.none { it in identityFields })
    }

    @Test
    fun `filter selects by group first and then by free text`() {
        assertEquals(listOf("stu-3b-07", "stu-3b-08"), filterStudents(students, "grp-3b", "").map { it.id })
        assertEquals(listOf("stu-1a-03", "stu-1a-04"), filterStudents(students, "grp-1a", "").map { it.id })
        assertEquals(4, filterStudents(students, null, "").size)

        assertEquals(listOf("stu-3b-07"), filterStudents(students, "grp-3b", "ana").map { it.id })
        assertEquals(listOf("stu-1a-04"), filterStudents(students, null, "MARTA").map { it.id })
        assertEquals(listOf("stu-3b-08"), filterStudents(students, "grp-3b", "8").map { it.id })
        assertTrue(filterStudents(students, "grp-3b", "ana torres").isEmpty())
        assertTrue(filterStudents(students, "grp-9c", "").isEmpty())
    }

    @Test
    fun `malformed document falls back to a row and the group label is derived`() {
        val student = AdminStudent.fromDocument(
            "stu-x",
            mapOf("firstName" to 42, "groupId" to "grp-3b", "active" to "yes"),
        )
        assertEquals("stu-x", student.displayName)
        assertEquals("", student.firstName)
        assertEquals(0, student.listNumber)
        assertEquals("grp-3b", student.groupId)
        assertTrue(student.active)

        assertEquals("3° B", groupLabel("grp-3b"))
        assertEquals("3° B", groupLabel("3B"))
        assertEquals("grupo-raro", groupLabel("grupo-raro"))

        // An in-app group has a random id: its stored name wins over the fallback.
        val names = mapOf("grp-3b" to "4° D", "abc-123" to "2° C")
        assertEquals("4° D", groupLabel("grp-3b", names))
        assertEquals("2° C", groupLabel("abc-123", names))
        assertEquals("3° B", groupLabel("3B", names))
        assertEquals("4° D", filterStudents(students, null, "", names).first().group)
    }

    @Test
    fun `student failure detail is appended when present and omitted otherwise`() {
        val detail = "failed-precondition: the query requires an index"
        val withDetail = CreateStudentResult.Failure(AdminStudentError.UNKNOWN, detail)

        assertEquals(
            "No se pudo completar la operación. (Firestore: $detail)",
            withDetail.toMessage(),
        )

        val withoutDetail = CreateStudentResult.Failure(AdminStudentError.UNKNOWN)
        assertEquals("No se pudo completar la operación.", withoutDetail.toMessage())
    }
}
