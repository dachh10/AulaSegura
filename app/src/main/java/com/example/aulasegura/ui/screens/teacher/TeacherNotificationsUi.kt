package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.ui.chat.chatTimeLabel
import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import java.util.TimeZone

/**
 * What the Notifications screen renders.
 *
 * The grouping ("HOY" / "ANTERIORES") and the time label are resolved here and
 * not in the composable: a bucket that quietly moves a notification to the wrong
 * day is invisible on a screenshot and obvious in a test.
 */
data class TeacherNotificationsUi(
    val groups: List<NotificationGroupUi> = emptyList(),
    val filters: List<String> = listOf(NOTIFICATIONS_ALL_FILTER),
    val activeFilter: String = NOTIFICATIONS_ALL_FILTER,
    val unread: Int = 0,
    val loading: Boolean = true,
    val notice: String? = null,
    /** `notifications.deepLink` of the row the teacher tapped, to be consumed. */
    val pendingDeepLink: String? = null,
)

/** One bucket with its rows. */
data class NotificationGroupUi(
    val label: String,
    val rows: List<NotificationRowUi>,
)

/** One notification, with everything the card needs. */
data class NotificationRowUi(
    val id: String,
    val title: String,
    val body: String,
    val time: String,
    val type: NotificationType,
    val read: Boolean,
    val deepLink: String?,
)

const val NOTIFICATIONS_ALL_FILTER: String = "Todas"
const val NOTIFICATIONS_TODAY_LABEL: String = "HOY"
const val NOTIFICATIONS_OLDER_LABEL: String = "ANTERIORES"

/**
 * Filters offered by the screen: "Todas" plus one per category that has rows.
 *
 * Fixed categories and not free text: the chip must map to a `type`, or the
 * teacher filters and the list answers with nothing.
 */
fun notificationFiltersFor(rows: List<NotificationRowUi>): List<String> {
    val present = rows.map { it.type.label() }.distinct()
    val known = NOTIFICATION_CATEGORIES.filter { it in present }
    return listOf(NOTIFICATIONS_ALL_FILTER) + known
}

/** Chip → types. "Sistema" is the catch-all for the technical notifications. */
fun filterNotifications(rows: List<NotificationRowUi>, filter: String): List<NotificationRowUi> {
    if (filter == NOTIFICATIONS_ALL_FILTER) return rows
    return rows.filter { it.type.label() == filter }
}

/**
 * Splits the tray by recency. [now] and [zone] are parameters so the bucketing is
 * reproducible in a test.
 */
fun notificationGroups(
    items: List<AppNotification>,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): List<NotificationGroupUi> {
    if (items.isEmpty()) return emptyList()

    val rows = items.map { notification ->
        NotificationRowUi(
            id = notification.id,
            title = notification.title,
            body = notification.body,
            time = chatTimeLabel(notification.createdAt, now, zone),
            type = notification.type,
            read = notification.read,
            deepLink = notification.deepLink,
        )
    }

    val todayFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.forLanguageTag("es-MX"))
        .apply { timeZone = zone }
    val today = todayFormat.format(java.util.Date(now))

    val (fromToday, older) = rows.partition { row ->
        val source = items.first { it.id == row.id }
        todayFormat.format(java.util.Date(source.createdAt)) == today
    }

    return listOfNotNull(
        fromToday.takeIf { it.isNotEmpty() }?.let {
            NotificationGroupUi(label = NOTIFICATIONS_TODAY_LABEL, rows = it)
        },
        older.takeIf { it.isNotEmpty() }?.let {
            NotificationGroupUi(label = NOTIFICATIONS_OLDER_LABEL, rows = it)
        },
    )
}

/** How many rows still count as unread. */
fun unreadNotifications(items: List<AppNotification>): Int = items.count { !it.read }

/** The chip a type belongs to. */
private fun NotificationType.label(): String = when (this) {
    NotificationType.MENSAJE -> "Mensajes"
    NotificationType.AVISO, NotificationType.TAREA -> "Avisos"
    NotificationType.ASISTENCIA -> "Asistencia"
    NotificationType.INCIDENTE -> "Incidentes"
    NotificationType.SISTEMA, NotificationType.OTRO -> "Sistema"
}

private val NOTIFICATION_CATEGORIES =
    listOf("Mensajes", "Avisos", "Asistencia", "Incidentes", "Sistema")
