package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.NoticeEntity
import com.example.aulasegura.data.local.entity.TaskEntity
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two documents this block writes are the contract with the tutor and the
 * admin screens, and the rules validate them field by field: a `groupIds` that is
 * not an array or an `audience` written flat is a publication that never leaves
 * the phone. Pinned here without a device.
 */
class PublicationMapperTest {

    @Test
    fun `a task document carries the schema fields the rules check`() {
        val map = task().toFirestoreMap()

        assertEquals("sch-benito-juarez", map["schoolId"])
        assertEquals(listOf("grp-3b"), map["groupIds"])
        assertEquals("uid-docente", map["createdBy"])
        assertEquals("Diego Hernández", map["createdByName"])
        assertEquals("Matemáticas", map["subject"])
        assertEquals(true, map["active"])
        assertTrue("dueDate goes as a Timestamp", map["dueDate"] is Timestamp)
        assertTrue(map["createdAt"] is Timestamp)
        assertEquals(
            "an empty array is a shape the reader can rely on",
            emptyList<Map<String, Any>>(),
            map["attachments"],
        )
    }

    @Test
    fun `a notice document nests its audience instead of flattening it`() {
        val map = notice().toFirestoreMap()
        val audience = map["audience"] as Map<*, *>

        assertEquals(listOf("tutor"), audience["roles"])
        assertEquals(listOf("grp-3b", "grp-4a"), audience["groupIds"])
        assertEquals(emptyList<String>(), audience["studentIds"])
        assertEquals("urgente", map["category"])
        assertEquals(true, map["urgent"])
        assertTrue("expiresAt is part of the shape, even when null", map.containsKey("expiresAt"))
        assertNull(map["expiresAt"])
    }

    @Test
    fun `a cached task becomes the row the history prints`() {
        val row = task().toPublishedTask(groupNames = listOf("3° B"), pending = true)

        assertEquals("tsk-1", row.taskId)
        assertEquals("Ecuaciones de primer grado", row.title)
        assertEquals("Matemáticas", row.subject)
        assertEquals(listOf("3° B"), row.groupNames)
        assertTrue("still in the outbox", row.pending)
    }

    @Test
    fun `a cached notice becomes the row the history prints`() {
        val row = notice().toPublishedNotice(groupNames = listOf("3° B", "4° A"), pending = false)

        assertEquals("not-1", row.noticeId)
        assertEquals("Suspensión de clases", row.title)
        assertEquals(listOf("3° B", "4° A"), row.groupNames)
        assertEquals(true, row.urgent)
        assertTrue(row.pending.not())
    }

    @Test
    fun `an unknown group is skipped instead of printing its raw id`() {
        val labels = mapOf("grp-3b" to "3° B")

        assertEquals(listOf("3° B"), groupNamesFor(listOf("grp-3b", "grp-borrado"), labels))
    }

    private fun task() = TaskEntity(
        taskId = "tsk-1",
        schoolId = "sch-benito-juarez",
        groupIds = listOf("grp-3b"),
        subject = "Matemáticas",
        title = "Ecuaciones de primer grado",
        description = "Ejercicios 4 a 12 del libro.",
        dueDate = 1_763_200_000_000L,
        createdBy = "uid-docente",
        createdByName = "Diego Hernández",
        active = true,
        createdAt = 1_763_100_000_000L,
        updatedAt = 1_763_100_000_000L,
    )

    private fun notice() = NoticeEntity(
        noticeId = "not-1",
        schoolId = "sch-benito-juarez",
        audienceRoles = listOf("tutor"),
        audienceGroupIds = listOf("grp-3b", "grp-4a"),
        audienceStudentIds = emptyList(),
        title = "Suspensión de clases",
        body = "El viernes no hay clases por consejo técnico.",
        category = "urgente",
        urgent = true,
        createdBy = "uid-docente",
        createdByName = "Diego Hernández",
        createdAt = 1_763_100_000_000L,
    )
}
