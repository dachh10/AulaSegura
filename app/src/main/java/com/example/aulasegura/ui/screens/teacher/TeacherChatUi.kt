package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.ui.chat.chatInitials
import com.example.aulasegura.ui.chat.chatTimeLabel
import java.util.TimeZone

/**
 * What the "Mensajes" list renders, already resolved.
 *
 * The screen gets rows whose time label is already a string ("10:41", "Ayer",
 * "Lun"), because a relative label is exactly what breaks silently when the clock
 * or the timezone changes — and it is what the tests pin. The label itself lives
 * in `ui/chat/ChatFormatting.kt` so the family screen can use the same one.
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
    val otherUid: String,
    val label: String,
    val groupLabel: String,
)

const val CHAT_ALL_FILTER: String = "Todos"

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
    otherUid = contact.otherUid,
    label = contact.label,
    groupLabel = contact.groupLabel,
)

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
