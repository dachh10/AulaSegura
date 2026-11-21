package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import com.example.aulasegura.ui.chat.chatTimeLabel
import com.example.aulasegura.ui.notifications.splitByToday
import java.util.TimeZone

/**
 * What the tutor's Notifications screen renders.
 *
 * Same shape as the teacher's tray: the ViewModel resolves the day buckets, the
 * filters and the unread count, so the composable only paints. A notification
 * filed under the wrong day or a chip that hides everything still looks fine on
 * a screenshot and breaks the family's only way to re-read a push.
 */
data class ParentNotificationsUi(
    val groups: List<ParentNotificationGroupUi> = emptyList(),
    val filters: List<ParentNotificationFilter> = listOf(ParentNotificationFilter.Todas),
    val activeFilter: ParentNotificationFilter = ParentNotificationFilter.Todas,
    val unread: Int = 0,
    val loading: Boolean = true,
    val notice: String? = null,
    /** `notifications.deepLink` of the row the tutor tapped, to be consumed. */
    val pendingDeepLink: String? = null,
)

/** One bucket ("HOY" / "ANTERIORES") with its rows. */
data class ParentNotificationGroupUi(
    val label: String,
    val rows: List<ParentNotificationRowUi>,
)

/** One real `notifications` document, with everything the card needs. */
data class ParentNotificationRowUi(
    val id: String,
    val title: String,
    val body: String,
    val time: String,
    val type: NotificationType,
    val read: Boolean,
    val deepLink: String?,
) {
    /**
     * The design's "Importante" tag. An absence or an incident is not routine for
     * a family; a task, a notice or a chat message is. The document has no
     * severity field, so this is the closest honest reading of `type`.
     */
    val urgent: Boolean
        get() = type == NotificationType.ASISTENCIA || type == NotificationType.INCIDENTE
}

/**
 * The design's chips, in the design's order. "Incidentes" and "Sistema" are not
 * drawn when there are no rows of that kind, so a family without incidents sees
 * exactly the five chips from the approved screen.
 */
enum class ParentNotificationFilter(val label: String) {
    Todas("Todas"),
    Asistencia("Asistencia"),
    Tareas("Tareas"),
    Avisos("Avisos"),
    Mensajes("Mensajes"),
    Incidentes("Incidentes"),
    Sistema("Sistema"),
}

const val PARENT_NOTIFICATIONS_TODAY_LABEL: String = "HOY"
const val PARENT_NOTIFICATIONS_OLDER_LABEL: String = "ANTERIORES"

/** Chips that make sense for the rows in hand; "Todas" is always offered. */
fun parentNotificationFiltersFor(
    rows: List<ParentNotificationRowUi>,
): List<ParentNotificationFilter> {
    val present = rows.map { it.type }.toSet()
    return ParentNotificationFilter.entries.filter { filter ->
        when (filter) {
            ParentNotificationFilter.Todas -> true
            ParentNotificationFilter.Asistencia -> NotificationType.ASISTENCIA in present
            ParentNotificationFilter.Tareas -> NotificationType.TAREA in present
            ParentNotificationFilter.Avisos -> NotificationType.AVISO in present
            ParentNotificationFilter.Mensajes -> NotificationType.MENSAJE in present
            ParentNotificationFilter.Incidentes -> NotificationType.INCIDENTE in present
            ParentNotificationFilter.Sistema ->
                NotificationType.SISTEMA in present || NotificationType.OTRO in present
        }
    }
}

/** Narrowing is by `type`, never by title: a title is free text the school writes. */
fun filterParentNotifications(
    rows: List<ParentNotificationRowUi>,
    filter: ParentNotificationFilter,
): List<ParentNotificationRowUi> = when (filter) {
    ParentNotificationFilter.Todas -> rows
    ParentNotificationFilter.Asistencia ->
        rows.filter { it.type == NotificationType.ASISTENCIA }
    ParentNotificationFilter.Tareas -> rows.filter { it.type == NotificationType.TAREA }
    ParentNotificationFilter.Avisos -> rows.filter { it.type == NotificationType.AVISO }
    ParentNotificationFilter.Mensajes -> rows.filter { it.type == NotificationType.MENSAJE }
    ParentNotificationFilter.Incidentes -> rows.filter { it.type == NotificationType.INCIDENTE }
    ParentNotificationFilter.Sistema -> rows.filter {
        it.type == NotificationType.SISTEMA || it.type == NotificationType.OTRO
    }
}

/** Splits the tray by recency, keeping the repository order inside each bucket. */
fun parentNotificationGroups(
    items: List<AppNotification>,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): List<ParentNotificationGroupUi> {
    val (fromToday, older) = splitByToday(items, now, zone)

    return listOfNotNull(
        fromToday.takeIf { it.isNotEmpty() }?.let {
            ParentNotificationGroupUi(
                label = PARENT_NOTIFICATIONS_TODAY_LABEL,
                rows = it.map { notification -> notification.toRowUi(now, zone) },
            )
        },
        older.takeIf { it.isNotEmpty() }?.let {
            ParentNotificationGroupUi(
                label = PARENT_NOTIFICATIONS_OLDER_LABEL,
                rows = it.map { notification -> notification.toRowUi(now, zone) },
            )
        },
    )
}

/** How many rows still count as unread. */
fun unreadParentNotifications(items: List<AppNotification>): Int = items.count { !it.read }

private fun AppNotification.toRowUi(now: Long, zone: TimeZone) = ParentNotificationRowUi(
    id = id,
    title = title,
    body = body,
    time = chatTimeLabel(createdAt, now, zone),
    type = type,
    read = read,
    deepLink = deepLink,
)
