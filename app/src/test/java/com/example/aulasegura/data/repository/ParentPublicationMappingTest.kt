package com.example.aulasegura.data.repository

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mapping `tasks/{id}` and `notices/{id}` into what the tutor sees.
 *
 * The notice mapper also carries the audience contract the rules cannot express in
 * one query (see `docs/firestore.rules` §notices), so these tests are the only
 * place that contract is written down for a future change.
 */
class ParentPublicationMappingTest {

    @Test
    fun `a task keeps the fields the detail screen prints`() {
        val task = parentTaskFrom(
            id = "tsk-1",
            data = mapOf(
                "groupIds" to listOf("grp-3b"),
                "subject" to "Matemáticas",
                "title" to "Fracciones",
                "description" to "Ejercicios 1-10",
                "dueDate" to timestamp(2026, 11, 20, 8, 0),
                "createdAt" to timestamp(2026, 11, 18, 10, 15),
                "createdByName" to "Prof. Ricardo Salinas",
                "attachments" to listOf(
                    mapOf(
                        "name" to "guia.pdf",
                        "url" to "https://res.cloudinary.com/x/guia.pdf",
                        "type" to "application/pdf",
                    ),
                ),
            ),
        )

        assertEquals("tsk-1", task?.taskId)
        assertEquals(listOf("grp-3b"), task?.groupIds)
        assertEquals("Fracciones", task?.title)
        assertEquals("Ejercicios 1-10", task?.description)
        assertEquals("Prof. Ricardo Salinas", task?.createdByName)
        assertEquals(1, task?.attachments?.size)
        assertTrue(task?.attachments?.first()?.isPdf == true)
    }

    @Test
    fun `a task without a title is not rendered as an empty card`() {
        assertNull(parentTaskFrom("tsk-2", mapOf("groupIds" to listOf("grp-3b"))))
        assertNull(
            parentTaskFrom("tsk-3", mapOf("groupIds" to listOf("grp-3b"), "title" to "  ")),
        )
    }

    @Test
    fun `an attachment that never reached a URL is dropped`() {
        val task = parentTaskFrom(
            id = "tsk-4",
            data = mapOf(
                "title" to "Maqueta",
                "attachments" to listOf(
                    mapOf("name" to "sin_subir.pdf", "url" to "", "type" to "application/pdf"),
                    mapOf("name" to "sin_url.pdf", "type" to "application/pdf"),
                ),
            ),
        )

        assertTrue(task?.attachments?.isEmpty() == true)
    }

    @Test
    fun `a notice addressed to tutors is read with its read mark`() {
        val notice = parentNoticeFrom(
            id = "ntc-1",
            data = notice(
                roles = listOf("tutor"),
                groupIds = listOf("grp-3b"),
                category = "grupo",
            ),
            readIds = setOf("ntc-1"),
            now = 1_000L,
        )

        assertEquals("ntc-1", notice?.noticeId)
        assertTrue(notice?.read == true)
        assertFalse(notice?.fromSchool == true)
        assertEquals("Prof. Ricardo Salinas", notice?.createdByName)
    }

    @Test
    fun `a notice for the staff never reaches a family`() {
        val notice = parentNoticeFrom(
            id = "ntc-2",
            data = notice(roles = listOf("docente"), groupIds = listOf("grp-3b"), category = "grupo"),
            readIds = emptySet(),
            now = 1_000L,
        )

        assertNull(notice)
    }

    @Test
    fun `an expired notice is not shown`() {
        val data = notice(roles = listOf("tutor"), groupIds = listOf("grp-3b"))
            .toMutableMap()
            .apply { put("expiresAt", timestamp(2026, 11, 17, 23, 59)) }
        val expiry = parentEpochMillis(data["expiresAt"])

        assertNull(parentNoticeFrom("ntc-3", data, emptySet(), now = expiry))
        assertEquals("ntc-3", parentNoticeFrom("ntc-3", data, emptySet(), now = expiry - 1)?.noticeId)
    }

    @Test
    fun `a notice without an audience is not readable`() {
        assertNull(parentNoticeFrom("ntc-4", mapOf("title" to "Sin público"), emptySet(), 0L))
    }

    @Test
    fun `a school notice is painted as Dirección`() {
        val notice = parentNoticeFrom(
            id = "ntc-5",
            data = notice(roles = listOf("tutor"), groupIds = emptyList(), category = "escuela"),
            readIds = emptySet(),
            now = 0L,
        )

        assertTrue(notice?.fromSchool == true)
    }

    private fun notice(
        roles: List<String>,
        groupIds: List<String>,
        category: String = "grupo",
    ): Map<String, Any?> = mapOf(
        "schoolId" to "sch-1",
        "audience" to mapOf(
            "roles" to roles,
            "groupIds" to groupIds,
            "studentIds" to emptyList<String>(),
        ),
        "title" to "Suspensión de clases",
        "body" to "No hay clases el 17 de noviembre.",
        "category" to category,
        "urgent" to true,
        "createdBy" to "uid-teacher",
        "createdByName" to "Prof. Ricardo Salinas",
        "createdAt" to timestamp(2026, 11, 18, 9, 10),
    )

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
    ): Timestamp {
        val calendar = java.util.Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }
        return Timestamp(Date(calendar.timeInMillis))
    }
}
