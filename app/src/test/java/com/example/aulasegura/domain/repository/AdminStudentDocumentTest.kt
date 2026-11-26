package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminStudent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * The linking flow writes the Spanish field names from the requirement doc while
 * the roster still reads the English ones. Both vocabularies have to survive a
 * round trip: a student created by the principal must remain visible to the
 * teacher's roster, and a legacy document must remain readable by the director.
 */
class AdminStudentDocumentTest {

    @Test
    fun `assigning a classroom syncs the tutors of every moved student`() {
        val students = listOf(
            AdminStudent.fromDocument(
                id = "stu-1",
                data = mapOf("nombre" to "Ana", "tutorIds" to listOf("tutor-1", "tutor-2")),
            ),
            AdminStudent.fromDocument(
                id = "stu-2",
                data = mapOf("nombre" to "Luis", "tutores" to listOf("tutor-2")),
            ),
            AdminStudent.fromDocument(
                id = "stu-3",
                data = mapOf("nombre" to "Sin tutor", "tutorIds" to emptyList<String>()),
            ),
        )

        assertEquals(
            setOf("tutor-1", "tutor-2"),
            tutorGroupSyncTargets(students),
        )
    }

    @Test
    fun `a blank tutor id never becomes a user document to sync`() {
        val students = listOf(
            AdminStudent.fromDocument(
                id = "stu-1",
                data = mapOf("nombre" to "Ana", "tutorIds" to listOf("", "  ", "tutor-1")),
            ),
        )

        assertEquals(setOf("tutor-1"), tutorGroupSyncTargets(students))
    }

    @Test
    fun `the new field names map into the domain model`() {
        val student = AdminStudent.fromDocument(
            id = "stu-new-1",
            data = mapOf(
                "nombre" to "Ana Pérez",
                "tutores" to listOf("tutor-1", "tutor-2"),
                "grupo" to "3B",
                "grado" to "3",
                "creadoPor" to "directora-1",
                "syncStatus" to "PENDIENTE",
                "active" to true,
            ),
        )

        assertEquals("Ana Pérez", student.fullName)
        assertEquals(listOf("tutor-1", "tutor-2"), student.tutores)
        assertEquals("3B", student.grupo)
        assertEquals("3", student.grado)
        assertEquals("directora-1", student.creadoPor)
        assertEquals("PENDIENTE", student.syncStatus)
        // The legacy fields stay in sync so existing screens keep working.
        assertEquals(listOf("tutor-1", "tutor-2"), student.tutorIds)
        assertEquals("3B", student.groupId)
    }

    @Test
    fun `a legacy document keeps its fields`() {
        val student = AdminStudent.fromDocument(
            id = "stu-old-1",
            data = mapOf(
                "fullName" to "Luis García",
                "tutorIds" to listOf("tutor-9"),
                "groupId" to "grp-2a",
                "active" to false,
            ),
        )

        assertEquals("Luis García", student.fullName)
        assertEquals(listOf("tutor-9"), student.tutores)
        assertEquals("grp-2a", student.grupo)
        assertEquals("", student.grado)
        assertEquals(false, student.active)
        assertEquals("SYNCED", student.syncStatus)
    }

    @Test
    fun `a new student document writes both vocabularies and no student code`() {
        val document = AdminStudentFields.newStudentDocument(
            input = AdminStudentInput(
                firstName = "Ana",
                lastName = "Pérez",
                curp = "",
                groupId = "grp-3b",
            ),
            schoolId = "sch-benito-juarez",
            listNumber = 7,
            createdBy = "directora-1",
        )

        assertEquals("Ana Pérez", document[AdminStudentFields.NOMBRE])
        assertEquals("Ana Pérez", document[AdminStudentFields.FULL_NAME])
        assertEquals(emptyList<String>(), document[AdminStudentFields.TUTORES])
        assertEquals(emptyList<String>(), document[AdminStudentFields.TUTOR_IDS])
        assertEquals("grp-3b", document[AdminStudentFields.GRUPO])
        assertEquals("grp-3b", document[AdminStudentFields.GROUP_ID])
        assertEquals("", document[AdminStudentFields.GRADO])
        assertEquals("directora-1", document[AdminStudentFields.CREADO_POR])
        assertEquals("SYNCED", document[AdminStudentFields.SYNC_STATUS])
        assertFalse(document.containsKey("registrationCode"))
        assertFalse(document.containsKey("codigoAlumno"))
    }
}
