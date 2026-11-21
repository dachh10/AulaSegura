package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tutor's tray splits by day, filters by `type` and derives the "Importante"
 * tag from the notification itself. All three are invisible failures: a row under
 * the wrong day, a chip that hides everything or a badge on a routine message
 * still look fine on a screenshot.
 */
class ParentNotificationsUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = at(2026, Calendar.NOVEMBER, 17, 12, 0)

    @Test
    fun `today and older are separated, newest first inside each bucket`() {
        val groups = parentNotificationGroups(
            items = listOf(
                notification("n-vieja", at(2026, Calendar.NOVEMBER, 10, 9, 0)),
                notification("n-hoy-1", at(2026, Calendar.NOVEMBER, 17, 10, 41)),
                notification("n-hoy-2", at(2026, Calendar.NOVEMBER, 17, 8, 0)),
            ),
            now = now,
            zone = utc,
        )

        assertEquals(listOf("HOY", "ANTERIORES"), groups.map { it.label })
        assertEquals(listOf("n-hoy-1", "n-hoy-2"), groups[0].rows.map { it.id })
        assertEquals(listOf("n-vieja"), groups[1].rows.map { it.id })
    }

    @Test
    fun `a bucket that would be empty is not printed`() {
        val groups = parentNotificationGroups(
            items = listOf(notification("n-hoy", at(2026, Calendar.NOVEMBER, 17, 7, 0))),
            now = now,
            zone = utc,
        )

        assertEquals(listOf("HOY"), groups.map { it.label })
        assertTrue(parentNotificationGroups(emptyList(), now, utc).isEmpty())
    }

    @Test
    fun `the time of a row follows the same convention as the chat list`() {
        val groups = parentNotificationGroups(
            items = listOf(
                notification("n-1", at(2026, Calendar.NOVEMBER, 17, 10, 41)),
                notification("n-2", at(2026, Calendar.NOVEMBER, 16, 9, 0)),
                notification("n-3", at(2026, Calendar.NOVEMBER, 5, 9, 0)),
            ),
            now = now,
            zone = utc,
        )

        val rows = groups.flatMap { it.rows }.associateBy { it.id }
        assertEquals("10:41", rows.getValue("n-1").time)
        assertEquals("Ayer", rows.getValue("n-2").time)
        assertEquals("5 nov", rows.getValue("n-3").time)
    }

    @Test
    fun `only unread rows count towards the badge`() {
        val items = listOf(
            notification("n-1", now, read = false),
            notification("n-2", now, read = true),
            notification("n-3", now, read = false),
        )

        assertEquals(2, unreadParentNotifications(items))
    }

    @Test
    fun `an absence or an incident is important, a task or a notice is not`() {
        assertTrue(row("a", NotificationType.ASISTENCIA).urgent)
        assertTrue(row("i", NotificationType.INCIDENTE).urgent)
        assertFalse(row("t", NotificationType.TAREA).urgent)
        assertFalse(row("v", NotificationType.AVISO).urgent)
        assertFalse(row("m", NotificationType.MENSAJE).urgent)
    }

    @Test
    fun `the chips keep the design order and only offer categories with rows`() {
        val rows = listOf(
            row("m-1", NotificationType.MENSAJE),
            row("a-1", NotificationType.ASISTENCIA),
        )

        assertEquals(
            listOf(
                ParentNotificationFilter.Todas,
                ParentNotificationFilter.Asistencia,
                ParentNotificationFilter.Mensajes,
            ),
            parentNotificationFiltersFor(rows),
        )
        assertEquals(
            listOf(ParentNotificationFilter.Todas),
            parentNotificationFiltersFor(emptyList()),
        )
    }

    @Test
    fun `a chip narrows by type and the unknown type falls under Sistema`() {
        val rows = listOf(
            row("m-1", NotificationType.MENSAJE),
            row("v-1", NotificationType.AVISO),
            row("t-1", NotificationType.TAREA),
            row("s-1", NotificationType.SISTEMA),
            row("o-1", NotificationType.OTRO),
        )

        assertEquals(
            listOf("m-1"),
            filterParentNotifications(rows, ParentNotificationFilter.Mensajes).map { it.id },
        )
        assertEquals(
            listOf("v-1"),
            filterParentNotifications(rows, ParentNotificationFilter.Avisos).map { it.id },
        )
        assertEquals(
            listOf("s-1", "o-1"),
            filterParentNotifications(rows, ParentNotificationFilter.Sistema).map { it.id },
        )
        assertEquals(rows, filterParentNotifications(rows, ParentNotificationFilter.Todas))
    }

    @Test
    fun `a row keeps the deep link and the read mark of the document`() {
        val groups = parentNotificationGroups(
            items = listOf(
                notification(
                    id = "n-1",
                    createdAt = now,
                    read = true,
                    type = NotificationType.TAREA,
                    deepLink = "parent/tasks/t-9",
                ),
            ),
            now = now,
            zone = utc,
        )

        val row = groups.single().rows.single()
        assertEquals("parent/tasks/t-9", row.deepLink)
        assertTrue(row.read)
    }

    private fun row(id: String, type: NotificationType) = ParentNotificationRowUi(
        id = id,
        title = "Título",
        body = "Cuerpo",
        time = "10:41",
        type = type,
        read = false,
        deepLink = null,
    )

    private fun notification(
        id: String,
        createdAt: Long,
        read: Boolean = false,
        type: NotificationType = NotificationType.SISTEMA,
        deepLink: String? = "parent/notifications",
    ) = AppNotification(
        id = id,
        title = "Aviso",
        body = "Cuerpo del aviso",
        type = type,
        deepLink = deepLink,
        read = read,
        createdAt = createdAt,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
