package com.example.aulasegura.data.local.mapper

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapping is the layer where the real bugs live, and every one of them is a
 * silent one: a grade that arrives as `Double` and lands in Room as a string, a
 * list of the wrong type that becomes an empty list, a document with no name that
 * renders as a blank card. A `DocumentSnapshot` cannot be built in a unit test,
 * which is exactly why the mappers take plain maps.
 */
class RosterMappersTest {

    @Test
    fun `firestore doubles are narrowed to ints`() {
        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf(
                "name" to "3° B",
                "schoolId" to "sch-benito-juarez",
                "grade" to 3.0,
                "section" to "B",
                "studentCount" to 28.0,
                "cycle" to "2026-2027",
            ),
        )

        assertEquals(3, entity?.grade)
        assertEquals(28, entity?.studentCount)
    }

    @Test
    fun `missing timestamps map to zero and never to now`() {
        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf("name" to "3° B", "schoolId" to "sch-benito-juarez"),
        )

        assertEquals(0L, entity?.createdAt)
        assertEquals(0L, entity?.updatedAt)
    }

    @Test
    fun `a real timestamp keeps its millis`() {
        val timestamp = Timestamp(1_700_000_000L, 0)

        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf(
                "name" to "3° B",
                "schoolId" to "sch-benito-juarez",
                "updatedAt" to timestamp,
            ),
        )

        assertEquals(1_700_000_000_000L, entity?.updatedAt)
    }

    @Test
    fun `a document with no name is skipped instead of cached empty`() {
        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf("schoolId" to "sch-benito-juarez"),
        )

        assertNull(entity)
    }

    @Test
    fun `a student with no fullName is skipped`() {
        val entity = studentEntityFrom(
            id = "stu-3b-01",
            data = mapOf("groupId" to "grp-3b", "schoolId" to "sch-benito-juarez"),
        )

        assertNull(entity)
    }

    @Test
    fun `a list of the wrong type becomes empty instead of throwing`() {
        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf(
                "name" to "3° B",
                "schoolId" to "sch-benito-juarez",
                "teacherIds" to listOf(1, 2, null, "uid-docente"),
            ),
        )

        assertEquals(listOf("uid-docente"), entity?.teacherIds)
    }

    @Test
    fun `a teacherIds that is not a list becomes empty`() {
        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf(
                "name" to "3° B",
                "schoolId" to "sch-benito-juarez",
                "teacherIds" to "uid-docente",
            ),
        )

        assertTrue(entity?.teacherIds?.isEmpty() == true)
    }

    @Test
    fun `an absent active flag is treated as an active student`() {
        val entity = studentEntityFrom(
            id = "stu-3b-01",
            data = mapOf(
                "fullName" to "Diego Gómez Ruiz",
                "groupId" to "grp-3b",
                "schoolId" to "sch-benito-juarez",
                "listNumber" to 7.0,
            ),
        )

        assertEquals(true, entity?.active)
        assertEquals(7, entity?.listNumber)
    }

    @Test
    fun `the entity round trips into the domain model`() {
        val entity = groupEntityFrom(
            id = "grp-3b",
            data = mapOf(
                "name" to "3° B",
                "schoolId" to "sch-benito-juarez",
                "grade" to 3.0,
                "section" to "B",
                "studentCount" to 28.0,
                "cycle" to "2026-2027",
                "teacherIds" to listOf("uid-docente"),
            ),
        )!!

        val group = entity.toDomain()

        assertEquals("grp-3b", group.id)
        assertEquals("3° B", group.name)
        assertEquals("3° B", group.label)
        assertEquals(28, group.studentCount)
        assertEquals(listOf("uid-docente"), group.teacherIds)
    }
}
