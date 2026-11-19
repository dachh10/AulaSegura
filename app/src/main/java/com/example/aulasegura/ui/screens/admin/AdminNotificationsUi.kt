package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What the principal's Notifications screen renders.
 *
 * Grouping, chips, unread count and time labels are resolved here and not in the
 * composable: a bucket that quietly moves a notification to the wrong day is
 * invisible on a screenshot and obvious in a test. No Firebase in this layer.
 */
data class AdminNotificationsUi(
    val groups: List<AdminNotificationGroupUi> = emptyList(),
    val filters: List<String> = listOf(ADMIN_NOTIFICATIONS_ALL_FILTER),
    val activeFilter: String = ADMIN_NOTIFICATIONS_ALL_FILTER,
    val unread: Int = 0,
    val loading: Boolean = true,
    val notice: String? = null,
    /** Relative label of the newest row, for the summary card ("hace 6 min"). */
    val lastNotificationTime: String? = null,
    /** `notifications.deepLink` of the row the principal tapped, to be consumed. */
    val pendingDeepLink: String? = null,
)

/** One bucket with its rows. */
data class AdminNotificationGroupUi(
    val label: String,
    val rows: List<AdminNotificationRowUi>,
)

/** One notification, with everything the card needs. */
data class AdminNotificationRowUi(
    val id: String,
    val title: String,
    val body: String,
    val time: String,
    val type: NotificationType,
    val category: String,
    val read: Boolean,
    val deepLink: String?,
)

const val ADMIN_NOTIFICATIONS_ALL_FILTER: String = "Todas"
const val ADMIN_NOTIFICATIONS_TODAY_LABEL: String = "HOY"
const val ADMIN_NOTIFICATIONS_OLDER_LABEL: String = "ANTERIORES"

/** The chip labels kept from the mockup. */
private val ADMIN_NOTIFICATION_CATEGORIES = listOf("Incidentes", "Asistencia", "Sistema")

/**
 * Chip a type belongs to. The mockup only offers four chips, so every technical
 * type lands in "Sistema"; a type with no chip would leave a row unreachable.
 */
fun NotificationType.adminCategory(): String = when (this) {
    NotificationType.INCIDENTE -> "Incidentes"
    NotificationType.ASISTENCIA -> "Asistencia"
    NotificationType.MENSAJE,
    NotificationType.AVISO,
    NotificationType.TAREA,
    NotificationType.SISTEMA,
    NotificationType.OTRO,
    -> "Sistema"
}

/** Filters offered by the screen: "Todas" plus one per category that has rows. */
fun adminNotificationFiltersFor(rows: List<AdminNotificationRowUi>): List<String> {
    val present = rows.map { it.category }.toSet()
    return listOf(ADMIN_NOTIFICATIONS_ALL_FILTER) +
        ADMIN_NOTIFICATION_CATEGORIES.filter { it in present }
}

/** Chip → rows. "Todas" is the only chip that does not map to a category. */
fun filterAdminNotifications(
    rows: List<AdminNotificationRowUi>,
    filter: String,
): List<AdminNotificationRowUi> {
    if (filter == ADMIN_NOTIFICATIONS_ALL_FILTER) return rows
    return rows.filter { it.category == filter }
}

/**
 * Splits the tray by recency. [now] and [zone] are parameters so the bucketing is
 * reproducible in a test.
 */
fun adminNotificationGroups(
    items: List<AppNotification>,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): List<AdminNotificationGroupUi> {
    if (items.isEmpty()) return emptyList()

    val rows = items.map { item ->
        AdminNotificationRowUi(
            id = item.id,
            title = item.title,
            body = item.body,
            time = adminTimeLabel(item.createdAt, now, zone),
            type = item.type,
            category = item.type.adminCategory(),
            read = item.read,
            deepLink = item.deepLink,
        )
    }

    val dayFormat = SimpleDateFormat("yyyyMMdd", ADMIN_LOCALE).apply { timeZone = zone }
    val today = dayFormat.format(Date(now))
    val (fromToday, older) = rows.partition { row ->
        val createdAt = items.first { it.id == row.id }.createdAt
        dayFormat.format(Date(createdAt)) == today
    }

    return listOfNotNull(
        fromToday.takeIf { it.isNotEmpty() }?.let {
            AdminNotificationGroupUi(label = ADMIN_NOTIFICATIONS_TODAY_LABEL, rows = it)
        },
        older.takeIf { it.isNotEmpty() }?.let {
            AdminNotificationGroupUi(label = ADMIN_NOTIFICATIONS_OLDER_LABEL, rows = it)
        },
    )
}

/** How many rows still count as unread. */
fun unreadAdminNotifications(items: List<AppNotification>): Int = items.count { !it.read }

/** Short relative label for the summary card. */
fun adminRelativeTime(
    createdAt: Long,
    now: Long = System.currentTimeMillis(),
): String {
    if (createdAt <= 0L) return ""
    val minutes = ((now - createdAt) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "ahora"
        minutes < 60L -> "hace $minutes min"
        minutes < 24 * 60L -> "hace ${minutes / 60L} h"
        else -> "hace ${minutes / (24 * 60L)} d"
    }
}

/**
 * The row time: the hour for today, "Ayer" for yesterday, the weekday inside the
 * last week and the date for anything older. Same convention as the rest of the
 * app, resolved here so the composable stays dumb.
 */
fun adminTimeLabel(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (epochMillis <= 0L) return ""

    val dayFormat = SimpleDateFormat("yyyyMMdd", ADMIN_LOCALE).apply { timeZone = zone }
    val today = dayFormat.format(Date(now))
    val thatDay = dayFormat.format(Date(epochMillis))
    if (thatDay == today) {
        return SimpleDateFormat("HH:mm", ADMIN_LOCALE)
            .apply { timeZone = zone }
            .format(Date(epochMillis))
    }
    if (thatDay == dayFormat.format(Date(now - DAY_MILLIS))) return "Ayer"

    val daysApart = ((dayFormat.parse(today)!!.time - dayFormat.parse(thatDay)!!.time) /
        DAY_MILLIS).coerceAtLeast(0L)
    if (daysApart in 2..6) {
        return SimpleDateFormat("EEE", ADMIN_LOCALE)
            .apply { timeZone = zone }
            .format(Date(epochMillis))
            .replace(".", "")
            .replaceFirstChar { it.uppercase(ADMIN_LOCALE) }
    }

    return SimpleDateFormat("d MMM", ADMIN_LOCALE)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
        .replace(".", "")
}

private val ADMIN_LOCALE: Locale = Locale.forLanguageTag("es")
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
