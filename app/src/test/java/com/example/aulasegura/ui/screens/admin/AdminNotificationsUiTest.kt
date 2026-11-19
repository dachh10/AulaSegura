package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The principal's tray splits by day, filters by the four mockup chips and counts unread. */
class AdminNotificationsUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = at(2026, Calendar.NOVEMBER, 17, 12, 0)

    @Test
    fun `today and older are separated, keeping the repository order`() {
        val groups = adminNotificationGroups(
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
    fun `an empty bucket is not printed`() {
        val groups = adminNotificationGroups(
            items = listOf(notification("n-hoy", at(2026, Calendar.NOVEMBER, 17, 7, 0))),
            now = now,
            zone = utc,
        )

        assertEquals(listOf("HOY"), groups.map { it.label })
        assertTrue(adminNotificationGroups(emptyList(), now, utc).isEmpty())
    }

    @Test
    fun `the row time follows the messenger convention`() {
        val groups = adminNotificationGroups(
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

        assertEquals(2, unreadAdminNotifications(items))
    }

    @Test
    fun `chips only offer the mockup categories that have rows`() {
        val rows = listOf(
            row("i-1", NotificationType.INCIDENTE),
            row("a-1", NotificationType.ASISTENCIA),
            row("s-1", NotificationType.MENSAJE),
            row("s-2", NotificationType.TAREA),
            row("s-3", NotificationType.SISTEMA),
            row("s-4", NotificationType.OTRO),
        )

        assertEquals(
            listOf("Todas", "Incidentes", "Asistencia", "Sistema"),
            adminNotificationFiltersFor(rows),
        )
        assertEquals(listOf("Todas"), adminNotificationFiltersFor(emptyList()))
        assertEquals("Sistema", NotificationType.AVISO.adminCategory())
    }

    @Test
    fun `a chip narrows to its own category and nothing else`() {
        val rows = listOf(
            row("i-1", NotificationType.INCIDENTE),
            row("a-1", NotificationType.ASISTENCIA),
            row("s-1", NotificationType.SISTEMA),
        )

        assertEquals(listOf("i-1"), filterAdminNotifications(rows, "Incidentes").map { it.id })
        assertEquals(listOf("a-1"), filterAdminNotifications(rows, "Asistencia").map { it.id })
        assertEquals(
            listOf("i-1", "a-1", "s-1"),
            filterAdminNotifications(rows, ADMIN_NOTIFICATIONS_ALL_FILTER).map { it.id },
        )
    }

    @Test
    fun `the relative label degrades gracefully`() {
        assertEquals("ahora", adminRelativeTime(now, now))
        assertEquals("hace 6 min", adminRelativeTime(now - 6 * 60_000L, now))
        assertEquals("hace 2 h", adminRelativeTime(now - 2 * 3_600_000L, now))
        assertEquals("hace 3 d", adminRelativeTime(now - 3 * 86_400_000L, now))
        assertEquals("", adminRelativeTime(0L, now))
    }

    private fun row(id: String, type: NotificationType) = AdminNotificationRowUi(
        id = id,
        title = "Título",
        body = "Cuerpo",
        time = "10:41",
        type = type,
        category = type.adminCategory(),
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
        deepLink = "admin/notifications",
        read = read,
        createdAt = createdAt,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
