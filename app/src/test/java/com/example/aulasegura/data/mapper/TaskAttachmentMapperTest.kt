package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.TaskEntity
import com.example.aulasegura.domain.model.AttachmentSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The task PDF has one job: reach the document with a URL, or not reach it at
 * all. Both halves fail silently in production (an empty array looks like "the
 * teacher attached nothing"), so they are pinned here without a device.
 */
class TaskAttachmentMapperTest {

    @Test
    fun `a task without a PDF writes an empty attachments array`() {
        val map = task().toFirestoreMap()

        assertEquals(emptyList<Map<String, Any?>>(), map["attachments"])
        assertNull(task().toPublishedTask(groupNames = listOf("3° B"), pending = true).attachmentName)
    }

    @Test
    fun `an uploaded PDF travels with its url and file name`() {
        val map = task().withAttachment(uploaded()).toFirestoreMap()

        @Suppress("UNCHECKED_CAST")
        val attachments = map["attachments"] as List<Map<String, Any?>>

        assertEquals(1, attachments.size)
        assertEquals("hoja-de-ejercicios.pdf", attachments[0]["fileName"])
        assertEquals(
            "https://res.cloudinary.com/demo/raw/upload/v1/aulasegura/tasks/tsk-1/hoja.pdf",
            attachments[0]["url"],
        )
        assertEquals("pdf", attachments[0]["type"])
        assertEquals(204_800L, attachments[0]["bytes"])
        assertEquals(
            "the family opens it by type, without parsing the extension",
            "application/pdf",
            attachments[0]["mimeType"],
        )
    }

    @Test
    fun `a PDF still uploading is left out of the document`() {
        val map = task().withAttachment(pending()).toFirestoreMap()

        assertEquals(
            "publishing an empty array is what the upload-first rule prevents",
            emptyList<Map<String, Any?>>(),
            map["attachments"],
        )
    }

    @Test
    fun `the attachment survives the JSON column`() {
        val stored = task().withAttachment(pending()).attachments().single()

        assertEquals("hoja-de-ejercicios.pdf", stored.fileName)
        assertEquals(StoredTaskAttachment.PENDING, stored.state)
        assertEquals("/data/app/task-attachments/TASK_1.pdf", stored.localPath)
        assertEquals("application/pdf", stored.mimeType)
    }

    @Test
    fun `an upload that finished drops the local path`() {
        val stored = task().withAttachment(uploaded()).attachments().single()

        assertTrue(stored.isUploaded)
        assertNull("Cloudinary is the source of truth now", stored.localPath)
    }

    @Test
    fun `a corrupted column reads as no attachment`() {
        val broken = task().copy(attachmentsJson = "{ esto no es json")

        assertTrue(
            "the task still publishes; only the file is lost",
            broken.attachments().isEmpty(),
        )
        assertEquals(emptyList<Map<String, Any?>>(), broken.toFirestoreMap()["attachments"])
    }

    @Test
    fun `the history prints the name of the attached PDF`() {
        val row = task()
            .withAttachment(uploaded())
            .toPublishedTask(groupNames = listOf("3° B"), pending = false)

        assertEquals("hoja-de-ejercicios.pdf", row.attachmentName)
        assertEquals(AttachmentSyncState.UPLOADED, row.attachmentState)
    }

    @Test
    fun `an attachment that failed is reported as such in the history`() {
        val row = task()
            .withAttachment(pending().copy(state = StoredTaskAttachment.FAILED, localPath = null))
            .toPublishedTask(groupNames = listOf("3° B"), pending = false)

        assertEquals(AttachmentSyncState.FAILED, row.attachmentState)
    }

    @Test
    fun `an attachment still queued is reported as pending`() {
        val row = task()
            .withAttachment(pending())
            .toPublishedTask(groupNames = listOf("3° B"), pending = true)

        assertEquals(AttachmentSyncState.PENDING, row.attachmentState)
    }

    private fun pending() = StoredTaskAttachment(
        attachmentId = "att-1",
        fileName = "hoja-de-ejercicios.pdf",
        bytes = 204_800L,
        localPath = "/data/app/task-attachments/TASK_1.pdf",
    )

    private fun uploaded() = StoredTaskAttachment(
        attachmentId = "att-1",
        fileName = "hoja-de-ejercicios.pdf",
        bytes = 204_800L,
        publicId = "aulasegura/tasks/tsk-1/hoja",
        url = "https://res.cloudinary.com/demo/raw/upload/v1/aulasegura/tasks/tsk-1/hoja.pdf",
        state = StoredTaskAttachment.UPLOADED,
    )

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
}
