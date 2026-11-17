package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * What the "Mensajes" list renders, already resolved.
 *
 * The screen gets rows whose time label is already a string ("10:41", "Ayer",
 * "Lun"), because a relative label is exactly what breaks silently when the clock
 * or the timezone changes — and it is what the tests pin.
 */
data class TeacherChatUi(
    val rows: List<TeacherChatRowUi> = emptyList(),
    val filters: List<String> = listOf(CHAT_ALL_FILTER),
    val activeFilter: String = CHAT_ALL_FILTER,
    val query: String = "",
    val loading: Boolean = true,
    val contacts: List<TeacherContactUi> = emptyList(),
    val contactsOpen: Boolean = false,
    /** Tutors are still being read; the sheet shows it instead of an empty list. */
    val contactsLoading: Boolean = false,
    val notice: String? = null,
)

/** One conversation row. */
data class TeacherChatRowUi(
    val id: String,
    val name: String,
    val subtitle: String,
    val preview: String,
    val time: String,
    val unread: Int,
    val group: String?,
    val initials: String,
)

/** One tutor in the "new message" sheet. */
data class TeacherContactUi(
    val tutorUid: String,
    val label: String,
    val groupLabel: String,
)

const val CHAT_ALL_FILTER: String = "Todos"

/**
 * `"Carlos Gómez Pérez"` → `"CG"`, `"Tutor de Mariana Gómez"` → `"MG"`.
 *
 * The `Tutor de` prefix is dropped first: without that, every family avatar would
 * read "TD" and no teacher would be able to tell two chats apart at a glance.
 * Lowercase connector words ("de", "del", "y") are skipped for the same reason.
 */
fun chatInitials(name: String): String {
    val trimmed = name.trim()
    val withoutRole = listOf("Tutor de ", "Tutora de ", "Tutor ", "Tutora ", "Docente de ")
        .firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
        ?.let { trimmed.drop(it.length) }
        ?.trim()
        .orEmpty()
    val subject = withoutRole.ifBlank { trimmed }

    val words = subject.split(' ')
        .filter { it.isNotBlank() }
        .filter { word -> word.firstOrNull()?.isUpperCase() == true }
        .take(2)

    return words.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }
}

/**
 * The row time: the hour for today, "Ayer" for yesterday, the weekday inside the
 * last week, and the date for anything older — the convention every messenger
 * uses and the only one a teacher reads without thinking.
 */
fun chatTimeLabel(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (epochMillis <= 0L) return ""

    val dayFormat = SimpleDateFormat("yyyyMMdd", SPANISH_MX).apply { timeZone = zone }
    val today = dayFormat.format(Date(now))
    val thatDay = dayFormat.format(Date(epochMillis))
    if (today == thatDay) return clockLabel(epochMillis, zone)

    val yesterday = dayFormat.format(Date(now - ONE_DAY_MILLIS))
    if (thatDay == yesterday) return "Ayer"

    val daysApart = daysBetween(epochMillis, now, zone)
    if (daysApart in 2..6) {
        return SimpleDateFormat("EEE", SPANISH_MX)
            .apply { timeZone = zone }
            .format(Date(epochMillis))
            .replace(".", "")
            .replaceFirstChar { it.uppercase(SPANISH_MX) }
    }

    return SimpleDateFormat("d MMM", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
        .replace(".", "")
}

/** `"10:41"`, the hour inside a bubble. */
fun clockLabel(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("HH:mm", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))

/** Turns the domain conversation into the row, resolving the clock here. */
fun chatRowOf(
    conversation: ChatConversation,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): TeacherChatRowUi = TeacherChatRowUi(
    id = conversation.id,
    name = conversation.title,
    subtitle = conversation.subtitle,
    preview = conversation.lastMessage.ifBlank { "Todavía no hay mensajes." },
    time = chatTimeLabel(conversation.lastMessageAt, now, zone),
    unread = conversation.unread,
    group = conversation.groupLabel,
    initials = chatInitials(conversation.title),
)

/** `"Todos"` plus one chip per group that actually has a chat. */
fun chatFiltersFor(rows: List<TeacherChatRowUi>): List<String> =
    listOf(CHAT_ALL_FILTER) +
        rows.mapNotNull { it.group }.filter { it.isNotBlank() }.distinct().sorted()

/** Search across name, who they are and the preview, plus the group chip. */
fun filterChatRows(
    rows: List<TeacherChatRowUi>,
    query: String,
    filter: String,
): List<TeacherChatRowUi> = rows.filter { row ->
    val matchesGroup = filter == CHAT_ALL_FILTER || row.group == filter
    val term = query.trim()
    val matchesQuery = term.isEmpty() ||
        row.name.contains(term, ignoreCase = true) ||
        row.subtitle.contains(term, ignoreCase = true) ||
        row.preview.contains(term, ignoreCase = true)
    matchesGroup && matchesQuery
}

/** The sheet row of a tutor. */
fun contactRowOf(contact: ChatContact): TeacherContactUi = TeacherContactUi(
    tutorUid = contact.tutorUid,
    label = contact.label,
    groupLabel = contact.groupLabel,
)

private fun daysBetween(from: Long, to: Long, zone: TimeZone): Int {
    val calendar: Calendar = Calendar.getInstance(zone).apply { timeInMillis = from }
    val start = Calendar.getInstance(zone).apply {
        timeInMillis = calendar.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = Calendar.getInstance(zone).apply {
        timeInMillis = to
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return ((end.timeInMillis - start.timeInMillis) / ONE_DAY_MILLIS).toInt()
}

/** One message inside the conversation. */
data class TeacherChatMessageUi(
    val id: String,
    val text: String,
    val mine: Boolean,
    val time: String,
)

/** What "Conversación" renders. */
data class TeacherConversationUi(
    val title: String = "",
    val subtitle: String = "",
    val online: Boolean = false,
    val messages: List<TeacherChatMessageUi> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val notice: String? = null,
    val loading: Boolean = true,
)

private const val ONE_DAY_MILLIS = 86_400_000L
private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")
