package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentNoticeItem
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone

/**
 * What the tutor's notice inbox renders.
 *
 * Unread is the tutor's own state (`users/{uid}/noticeReads/{noticeId}`), never a
 * field of the notice: marking one read cannot touch what the school published.
 */
sealed interface ParentNoticesUiState {

    data object Loading : ParentNoticesUiState

    data class Loaded(
        val unreadCount: Int,
        /** "Último: hoy, 09:10", or "Estás al día" when nothing is unread. */
        val latestLabel: String,
        val filter: ParentNoticeFilter,
        val sections: List<ParentNoticeSectionUi>,
        val emptyMessage: String,
        /** Child whose group these notices were filtered to. */
        val childLabel: String = "",
        /** The read mark could not be stored; the notice is still on screen. */
        val error: String?,
    ) : ParentNoticesUiState

    data class Failed(val message: String) : ParentNoticesUiState
}

enum class ParentNoticeFilter(val label: String) {
    All("Todos"),
    School("Escuela"),
    MyGroup("Mi grupo"),
    Urgent("Urgentes"),
}

data class ParentNoticeSectionUi(val label: String, val notices: List<ParentNoticeRowUi>)

data class ParentNoticeRowUi(
    val noticeId: String,
    val fromSchool: Boolean,
    /** "Dirección" for a school notice, the publisher's name otherwise. */
    val sourceName: String,
    val timeLabel: String,
    val title: String,
    val body: String,
    val unread: Boolean,
    val urgent: Boolean,
)

internal enum class ParentNoticeSection(val label: String) {
    Today("HOY"),
    ThisWeek("ESTA SEMANA"),
    Older("ANTERIORES"),
}

internal fun noticeSectionOf(
    notice: ParentNoticeItem,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ParentNoticeSection {
    val created = AttendanceTimes.isoDate(notice.createdAtMillis, zone)
    return when {
        created == AttendanceTimes.isoDate(now, zone) -> ParentNoticeSection.Today
        created >= isoAfter(now, -6, zone) -> ParentNoticeSection.ThisWeek
        else -> ParentNoticeSection.Older
    }
}

internal fun noticesFor(
    filter: ParentNoticeFilter,
    notices: List<ParentNoticeItem>,
): List<ParentNoticeItem> = when (filter) {
    ParentNoticeFilter.All -> notices
    ParentNoticeFilter.School -> notices.filter { it.fromSchool }
    ParentNoticeFilter.MyGroup -> notices.filterNot { it.fromSchool }
    ParentNoticeFilter.Urgent -> notices.filter { it.urgent }
}

internal fun noticeSections(
    filter: ParentNoticeFilter,
    notices: List<ParentNoticeItem>,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): List<ParentNoticeSectionUi> = noticesFor(filter, notices)
    .groupBy { noticeSectionOf(it, now, zone) }
    .toSortedMap(compareBy { it.ordinal })
    .map { (section, rows) ->
        ParentNoticeSectionUi(
            label = section.label,
            notices = rows
                .sortedByDescending { it.createdAtMillis }
                .map { it.toRowUi(now, zone) },
        )
    }

internal fun ParentNoticeItem.toRowUi(
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ParentNoticeRowUi = ParentNoticeRowUi(
    noticeId = noticeId,
    fromSchool = fromSchool,
    sourceName = if (fromSchool) {
        "Dirección"
    } else {
        createdByName.ifBlank { "Docente del grupo" }
    },
    timeLabel = noticeTimeLabel(createdAtMillis, now, zone),
    title = title,
    body = body,
    unread = !read,
    urgent = urgent,
)

internal fun noticeTimeLabel(
    millis: Long,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (millis <= 0) return ""
    val day = AttendanceTimes.isoDate(millis, zone)
    val clock = AttendanceTimes.clockTime(millis, zone)
    return if (day == AttendanceTimes.isoDate(now, zone)) {
        "Hoy, $clock"
    } else {
        "${AttendanceTimes.dayLabel(millis, zone)}, $clock"
    }
}

/**
 * The summary card's second line.
 *
 * With nothing unread it says so instead of repeating the newest notice's time:
 * "Último: …" next to "0 avisos" reads like a contradiction.
 */
internal fun noticesLatestLabel(notices: List<ParentNoticeItem>, unread: Int, now: Long, zone: TimeZone = TimeZone.getDefault()): String {
    if (unread == 0) return "Estás al día"
    val newest = notices.maxByOrNull { it.createdAtMillis } ?: return "Estás al día"
    val time = noticeTimeLabel(newest.createdAtMillis, now, zone)
    return if (time.isEmpty()) "Estás al día" else "Último: $time"
}

internal fun noticesEmptyMessage(filter: ParentNoticeFilter): String = when (filter) {
    ParentNoticeFilter.All -> "Cuando la escuela publique un aviso aparecerá aquí."
    ParentNoticeFilter.School -> "Sin avisos de la escuela."
    ParentNoticeFilter.MyGroup -> "Sin avisos del grupo."
    ParentNoticeFilter.Urgent -> "Sin avisos urgentes."
}

internal fun noticesFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver los avisos."
    RosterFailure.NO_GROUP -> "Tu hijo aún no tiene grupo asignado."
    RosterFailure.UNKNOWN -> "No se pudieron cargar los avisos."
}
