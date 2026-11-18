package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttachmentSyncState
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.PublishedTask
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "Publicaciones" segment is one timeline of two different documents, so the
 * merge, the order and the per-row pill are the parts that break silently: a task
 * sorted by its due date or a notice that hides the pending state both look fine
 * on a screenshot and lie to the teacher.
 */
class TeacherPublicationUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `tasks and notices share one timeline newest first`() {
        val rows = publicationRows(
            tasks = listOf(task("t-vieja", at(10, 8)), task("t-nueva", at(12, 9))),
            notices = listOf(notice("n-media", at(11, 7))),
            zone = utc,
        )

        assertEquals(listOf("t-nueva", "n-media", "t-vieja"), rows.map { it.id })
    }

    @Test
    fun `each kind keeps its own label and the urgent notice says so`() {
        val rows = publicationRows(
            tasks = listOf(task("t-1", at(10, 8))),
            notices = listOf(notice("n-1", at(10, 8), urgent = true), notice("n-2", at(10, 8))),
            zone = utc,
        ).associateBy { it.id }

        assertEquals("Tarea", rows.getValue("t-1").kindLabel)
        assertEquals("Aviso urgente", rows.getValue("n-1").kindLabel)
        assertTrue(rows.getValue("n-1").urgent)
        assertEquals("Aviso", rows.getValue("n-2").kindLabel)
        assertFalse(rows.getValue("n-2").urgent)
    }

    @Test
    fun `pending is per row and not per list`() {
        val rows = publicationRows(
            tasks = listOf(task("t-1", at(10, 8), pending = true)),
            notices = listOf(notice("n-1", at(11, 8), pending = false)),
            zone = utc,
        ).associateBy { it.id }

        assertTrue(rows.getValue("t-1").pending)
        assertFalse(rows.getValue("n-1").pending)
        assertEquals("Pendiente de enviar", publicationSyncLabel(true))
        assertEquals("Publicado", publicationSyncLabel(false))
    }

    @Test
    fun `the teacher's groups are printed joined and the notice body is cut to a line`() {
        val row = publicationRows(
            tasks = emptyList(),
            notices = listOf(
                notice("n-1", at(10, 8), body = "Primera línea\nSegunda línea"),
            ),
            zone = utc,
        ).single()

        assertEquals("Primera línea", row.detail)
        assertEquals("3° B · 4° A", row.audience)
    }

    @Test
    fun `a task with a PDF keeps its file name and reports the state in one word`() {
        val rows = publicationRows(
            tasks = listOf(
                task("t-subido", at(10, 8), attachmentName = "hoja.pdf", attachmentState = AttachmentSyncState.UPLOADED),
                task("t-pendiente", at(11, 8), attachmentName = "guia.pdf", attachmentState = AttachmentSyncState.PENDING),
                task("t-fallido", at(12, 8), attachmentName = "examen.pdf", attachmentState = AttachmentSyncState.FAILED),
                task("t-sin-archivo", at(13, 8)),
            ),
            notices = emptyList(),
            zone = utc,
        ).associateBy { it.id }

        assertEquals("hoja.pdf", rows.getValue("t-subido").attachmentName)
        assertEquals(AttachmentSyncState.UPLOADED, rows.getValue("t-subido").attachmentState)
        assertEquals("guia.pdf", rows.getValue("t-pendiente").attachmentName)
        assertEquals(
            "a task that went out without its file must not look like it has it",
            "examen.pdf",
            rows.getValue("t-fallido").attachmentName,
        )
        assertNull(rows.getValue("t-sin-archivo").attachmentName)

        // The card prints a `PDF` chip, not the name: `Abrir` is the only word an
        // uploaded file needs, because it only appears when there is a URL.
        assertNull(attachmentStateLabel(AttachmentSyncState.UPLOADED))
        assertEquals("Pendiente", attachmentStateLabel(AttachmentSyncState.PENDING))
        assertEquals("No se subió", attachmentStateLabel(AttachmentSyncState.FAILED))
    }

    @Test
    fun `nothing published renders no rows at all`() {
        assertTrue(publicationRows(emptyList(), emptyList(), utc).isEmpty())
    }

    @Test
    fun `the date labels follow the school's language`() {
        val due = at(15, 8).let { day ->
            Calendar.getInstance(utc).apply {
                timeInMillis = day
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
            }.timeInMillis
        }

        val dateLabel = taskDueDateLabel(due, utc)
        assertTrue(dateLabel, dateLabel.contains("15") && dateLabel.contains("nov"))
        assertTrue(dateLabel, dateLabel.contains("2026"))
        assertEquals("08:00", taskDueTimeLabel(due, utc))
        assertTrue(publicationTimestampLabel(due, utc).contains("08:00"))
    }

    @Test
    fun `a publication needs a group and a title`() {
        assertFalse(canPublishTask(selectedGroupId = null, title = "Tarea", publishing = false, published = false))
        assertFalse(canPublishTask(selectedGroupId = "grp-3b", title = " ", publishing = false, published = false))
        assertTrue(canPublishTask(selectedGroupId = "grp-3b", title = "Tarea", publishing = false, published = false))
        assertFalse(
            "already queued: the footer must not publish twice",
            canPublishTask(selectedGroupId = "grp-3b", title = "Tarea", publishing = false, published = true),
        )

        assertFalse(canPublishNotice(emptySet(), title = "Aviso", publishing = false, published = false))
        assertTrue(canPublishNotice(setOf("grp-3b"), title = "Aviso", publishing = false, published = false))
    }

    /** 2026-11-<[day]> at [hour]:00 UTC. */
    private fun at(day: Int, hour: Int): Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, 0, 0)
    }.timeInMillis

    @Test
    fun `the attachment URL reaches the row that prints it`() {
        val rows = publicationRows(
            tasks = listOf(
                task(
                    "t-1",
                    at(10, 8),
                    attachmentName = "hoja.pdf",
                    attachmentState = AttachmentSyncState.UPLOADED,
                    attachmentUrl = "https://res.cloudinary.com/demo/raw/upload/v1/hoja.pdf",
                ),
            ),
            notices = listOf(notice("n-1", at(10, 8))),
            zone = utc,
        ).associateBy { it.id }

        assertEquals(
            "https://res.cloudinary.com/demo/raw/upload/v1/hoja.pdf",
            rows.getValue("t-1").attachmentUrl,
        )
        assertNull("a notice carries no file", rows.getValue("n-1").attachmentUrl)
    }

    private fun task(
        id: String,
        createdAt: Long,
        pending: Boolean = false,
        attachmentName: String? = null,
        attachmentState: AttachmentSyncState? = null,
        attachmentUrl: String? = null,
    ) = PublishedTask(
        taskId = id,
        title = "Ecuaciones",
        subject = "Matemáticas",
        description = "Ejercicios 4 a 12",
        groupNames = listOf("3° B"),
        dueDate = createdAt + 86_400_000L,
        createdAt = createdAt,
        pending = pending,
        attachmentName = attachmentName,
        attachmentState = attachmentState,
        attachmentUrl = attachmentUrl,
    )

    private fun notice(
        id: String,
        createdAt: Long,
        urgent: Boolean = false,
        pending: Boolean = false,
        body: String = "El viernes no hay clases.",
    ) = PublishedNotice(
        noticeId = id,
        title = "Suspensión de clases",
        body = body,
        groupNames = listOf("3° B", "4° A"),
        urgent = urgent,
        createdAt = createdAt,
        pending = pending,
    )
}
