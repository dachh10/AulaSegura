package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentAttachment
import com.example.aulasegura.domain.model.ParentNoticeItem
import com.example.aulasegura.domain.model.ParentTaskItem
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The strings the tutor reads around tasks and notices.
 *
 * These helpers are the whole difference between "Pendiente" and "Vencida", and
 * between "hoy" and a real date, so they are tested without Compose and without
 * Firebase: the input is a fixed millisecond value and a fixed zone.
 */
class ParentPublicationsUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun `a task due today is pending, not overdue`() {
        val task = task(due = at(2026, 11, 18, 8))

        assertEquals(ParentTaskSection.Today, taskSectionOf(task, now = at(2026, 11, 18, 12), zone = utc))
        assertFalse(isTaskOverdue(task, now = at(2026, 11, 18, 12), zone = utc))
        assertEquals("Hoy", task.toRowUi(now = at(2026, 11, 18, 12), zone = utc).dueLabel)
    }

    @Test
    fun `a task due yesterday is overdue and reads the real date`() {
        val task = task(due = at(2026, 11, 17, 8))
        val row = task.toRowUi(now = at(2026, 11, 18, 12), zone = utc)

        assertTrue(row.overdue)
        assertEquals("Mar 17 nov", row.dueLabel)
    }

    @Test
    fun `tomorrow and next week are different sections`() {
        val now = at(2026, 11, 18, 12)

        assertEquals(ParentTaskSection.Tomorrow, taskSectionOf(task(at(2026, 11, 19, 8)), now, utc))
        assertEquals(ParentTaskSection.ThisWeek, taskSectionOf(task(at(2026, 11, 23, 8)), now, utc))
        assertEquals(ParentTaskSection.Later, taskSectionOf(task(at(2026, 12, 3, 8)), now, utc))
    }

    @Test
    fun `pending drops overdue tasks and the overdue chip keeps only those`() {
        val tasks = listOf(
            task(due = at(2026, 11, 17, 8), title = "Ayer"),
            task(due = at(2026, 11, 20, 8), title = "Viernes"),
        )
        val now = at(2026, 11, 18, 12)

        assertEquals(
            listOf("Viernes"),
            tasksFor(ParentTaskFilter.Pending, tasks, now, utc).map { it.title },
        )
        assertEquals(
            listOf("Ayer"),
            tasksFor(ParentTaskFilter.Overdue, tasks, now, utc).map { it.title },
        )
        assertEquals(2, tasksFor(ParentTaskFilter.All, tasks, now, utc).size)
    }

    @Test
    fun `the delivered filter is empty because the school records no submissions`() {
        val now = at(2026, 11, 18, 12)
        val tasks = listOf(task(due = at(2026, 11, 20, 8)))

        assertTrue(tasksFor(ParentTaskFilter.Delivered, tasks, now, utc).isEmpty())
        assertTrue(taskSections(ParentTaskFilter.Delivered, tasks, now, utc).isEmpty())
        assertTrue(tasksEmptyMessage(ParentTaskFilter.Delivered).contains("no registra entregas"))
    }

    @Test
    fun `sections run from overdue to later with one task each`() {
        val now = at(2026, 11, 18, 12)
        val sections = taskSections(
            filter = ParentTaskFilter.All,
            tasks = listOf(
                task(due = at(2026, 12, 3, 8), title = "Lejos"),
                task(due = at(2026, 11, 17, 8), title = "Tarde"),
                task(due = at(2026, 11, 18, 8), title = "Hoy"),
            ),
            now = now,
            zone = utc,
        )

        assertEquals(listOf("VENCIDAS", "HOY", "MÁS ADELANTE"), sections.map { it.label })
        assertEquals(listOf("Tarde"), sections[0].tasks.map { it.title })
    }

    @Test
    fun `a task with a PDF shows the file, otherwise the teacher`() {
        val withFile = task(due = at(2026, 11, 20, 8)).copy(
            attachments = listOf(
                ParentAttachment("guia.pdf", "https://cdn/guia.pdf", "application/pdf"),
            ),
        )
        val withoutFile = task(due = at(2026, 11, 20, 8))

        assertEquals(
            "guia.pdf",
            withFile.toRowUi(at(2026, 11, 18, 12), utc).attachmentName,
        )
        assertEquals(
            "Prof. Ricardo Salinas",
            withoutFile.toRowUi(at(2026, 11, 18, 12), utc).teacher,
        )
    }

    @Test
    fun `notices are bucketed by age and newest first inside a section`() {
        val now = at(2026, 11, 18, 12)
        val sections = noticeSections(
            filter = ParentNoticeFilter.All,
            notices = listOf(
                notice("viejo", at(2026, 11, 2, 9)),
                notice("hoy-temprano", at(2026, 11, 18, 8)),
                notice("hoy-tarde", at(2026, 11, 18, 10)),
                notice("semana", at(2026, 11, 14, 9)),
            ),
            now = now,
            zone = utc,
        )

        assertEquals(listOf("HOY", "ESTA SEMANA", "ANTERIORES"), sections.map { it.label })
        assertEquals(
            listOf("hoy-tarde", "hoy-temprano"),
            sections[0].notices.map { it.noticeId },
        )
        assertEquals(listOf("viejo"), sections[2].notices.map { it.noticeId })
    }

    @Test
    fun `notice filters separate school, group and urgent`() {
        val notices = listOf(
            notice("escuela", 0L, category = "escuela"),
            notice("grupo", 0L, category = "grupo"),
            notice("urgente", 0L, category = "urgente", urgent = true),
        )

        assertEquals(
            listOf("escuela"),
            noticesFor(ParentNoticeFilter.School, notices).map { it.noticeId },
        )
        assertEquals(
            listOf("grupo", "urgente"),
            noticesFor(ParentNoticeFilter.MyGroup, notices).map { it.noticeId },
        )
        assertEquals(
            listOf("urgente"),
            noticesFor(ParentNoticeFilter.Urgent, notices).map { it.noticeId },
        )
    }

    @Test
    fun `the summary names the newest unread notice, or says you are up to date`() {
        val now = at(2026, 11, 18, 12)
        val notices = listOf(
            notice("a", at(2026, 11, 18, 9, 10)),
            notice("b", at(2026, 11, 14, 9, 10)),
        )

        assertEquals(
            "Último: Hoy, 09:10",
            noticesLatestLabel(notices, unread = 1, now = now, zone = utc),
        )
        assertEquals(
            "Estás al día",
            noticesLatestLabel(notices.map { it.copy(read = true) }, unread = 0, now = now, zone = utc),
        )
    }

    @Test
    fun `a notice without a publisher name still has a source`() {
        val row = notice("x", at(2026, 11, 18, 9), category = "grupo")
            .copy(createdByName = "")
            .toRowUi(at(2026, 11, 18, 12), utc)

        assertEquals("Docente del grupo", row.sourceName)
        assertTrue(row.unread)
    }

    @Test
    fun `the attachment label never prints a size the document does not carry`() {
        assertEquals("Documento PDF", attachmentMeta("application/pdf"))
        assertEquals("Imagen", attachmentMeta("image/jpeg"))
        assertEquals("application/zip", attachmentMeta("application/zip"))
        assertEquals("Archivo", attachmentMeta(""))
    }

    private fun task(
        due: Long,
        title: String = "Fracciones",
        subject: String = "Matemáticas",
    ): ParentTaskItem = ParentTaskItem(
        taskId = "tsk-${title.hashCode()}",
        groupIds = listOf("grp-3b"),
        subject = subject,
        title = title,
        description = "",
        dueDateMillis = due,
        createdAtMillis = at(2026, 11, 10, 10),
        createdByName = "Prof. Ricardo Salinas",
    )

    private fun notice(
        id: String,
        createdAt: Long,
        category: String = "grupo",
        urgent: Boolean = false,
    ): ParentNoticeItem = ParentNoticeItem(
        noticeId = id,
        title = "Aviso $id",
        body = "Cuerpo del aviso",
        category = category,
        urgent = urgent,
        createdByName = "Prof. Ricardo Salinas",
        createdAtMillis = createdAt,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis
}
