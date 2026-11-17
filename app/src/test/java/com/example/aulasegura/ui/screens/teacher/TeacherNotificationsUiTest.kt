package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tray splits by day and filters by category; both are invisible failures — a
 * notification filed under the wrong day or a chip that hides everything still
 * looks fine on a screenshot.
 */
class TeacherNotificationsUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = at(2026, Calendar.NOVEMBER, 17, 12, 0)

    @Test
    fun `today and older are separated, newest first inside each bucket`() {
        val groups = notificationGroups(
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
        val groups = notificationGroups(
            items = listOf(notification("n-hoy", at(2026, Calendar.NOVEMBER, 17, 7, 0))),
            now = now,
            zone = utc,
        )

        assertEquals(listOf("HOY"), groups.map { it.label })
        assertTrue(notificationGroups(emptyList(), now, utc).isEmpty())
    }

    @Test
    fun `the time of a row follows the same convention as the chat list`() {
        val groups = notificationGroups(
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

        assertEquals(2, unreadNotifications(items))
    }

    @Test
    fun `the chips only offer categories that have rows`() {
        val rows = listOf(
            row("n-1", NotificationType.MENSAJE),
            row("n-2", NotificationType.ASISTENCIA),
            row("n-3", NotificationType.MENSAJE),
        )

        assertEquals(listOf("Todas", "Mensajes", "Asistencia"), notificationFiltersFor(rows))
        assertEquals(listOf("Todas"), notificationFiltersFor(emptyList()))
    }

    @Test
    fun `a chip narrows to its own types and nothing else`() {
        val rows = listOf(
            row("m-1", NotificationType.MENSAJE),
            row("a-1", NotificationType.AVISO),
            row("a-2", NotificationType.TAREA),
            row("s-1", NotificationType.SISTEMA),
        )

        assertEquals(listOf("m-1"), filterNotifications(rows, "Mensajes").map { it.id })
        assertEquals(listOf("a-1", "a-2"), filterNotifications(rows, "Avisos").map { it.id })
        assertEquals(rows, filterNotifications(rows, NOTIFICATIONS_ALL_FILTER))
    }

    @Test
    fun `an unknown firestore type falls back to the catch-all instead of crashing`() {
        assertEquals(NotificationType.OTRO, NotificationType.from("desconocido"))
        assertEquals(NotificationType.OTRO, NotificationType.from(null))
        assertEquals(NotificationType.MENSAJE, NotificationType.from("mensaje"))
    }

    private fun row(id: String, type: NotificationType) = NotificationRowUi(
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
    ) = AppNotification(
        id = id,
        title = "Aviso",
        body = "Cuerpo del aviso",
        type = type,
        deepLink = "teacher/history",
        read = read,
        createdAt = createdAt,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
