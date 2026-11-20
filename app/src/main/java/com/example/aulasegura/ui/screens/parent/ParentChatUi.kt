package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.domain.model.ChatMessage
import com.example.aulasegura.ui.chat.chatDayLabel
import com.example.aulasegura.ui.chat.chatInitials
import com.example.aulasegura.ui.chat.chatTimeLabel
import com.example.aulasegura.ui.chat.clockLabel
import java.util.TimeZone

/**
 * What the tutor's "Mensajes" list renders, already resolved.
 *
 * The time is a string ("10:42", "Ayer", "Lun") because a relative label is
 * exactly what breaks silently when the clock moves; it comes from the label the
 * docente's list uses, so both sides of the same chat read the same way.
 */
data class ParentChatUi(
    val rows: List<ParentChatRowUi> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val contacts: List<ParentTeacherUi> = emptyList(),
    val contactsOpen: Boolean = false,
    /** Teachers are still being read; the sheet shows it instead of an empty list. */
    val contactsLoading: Boolean = false,
    val notice: String? = null,
)

/** One conversation row. */
data class ParentChatRowUi(
    val id: String,
    val name: String,
    val subtitle: String,
    val preview: String,
    val time: String,
    /** Only drawn when there is something unread. */
    val unread: Int?,
    val initials: String,
)

/** One teacher in the "new message" sheet. */
data class ParentTeacherUi(
    val otherUid: String,
    val label: String,
    val groupLabel: String,
    val initials: String,
)

/** Turns the domain conversation into the row, resolving the clock here. */
fun parentChatRowOf(
    conversation: ChatConversation,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): ParentChatRowUi = ParentChatRowUi(
    id = conversation.id,
    name = conversation.title,
    subtitle = conversation.subtitle,
    preview = conversation.lastMessage.ifBlank { "Todavía no hay mensajes." },
    time = chatTimeLabel(conversation.lastMessageAt, now, zone),
    unread = conversation.unread.takeIf { it > 0 },
    initials = chatInitials(conversation.title),
)

/** Search across the teacher's name, who they are and the last message. */
fun filterParentChatRows(
    rows: List<ParentChatRowUi>,
    query: String,
): List<ParentChatRowUi> {
    val term = query.trim()
    if (term.isEmpty()) return rows
    return rows.filter { row ->
        row.name.contains(term, ignoreCase = true) ||
            row.subtitle.contains(term, ignoreCase = true) ||
            row.preview.contains(term, ignoreCase = true)
    }
}

/** The sheet row of a teacher. */
fun parentTeacherRowOf(contact: ChatContact): ParentTeacherUi = ParentTeacherUi(
    otherUid = contact.otherUid,
    label = contact.label,
    groupLabel = contact.groupLabel,
    initials = chatInitials(contact.label),
)

/** One message inside the conversation. */
data class ParentMessageUi(
    val id: String,
    val text: String,
    val mine: Boolean,
    val time: String,
    /** Set only on the first bubble of a day: "Hoy", "Ayer", "Lun", "12 nov". */
    val dayLabel: String? = null,
)

/** What "Conversación" renders. */
data class ParentConversationUi(
    val title: String = "",
    val subtitle: String = "",
    val online: Boolean = false,
    val messages: List<ParentMessageUi> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val notice: String? = null,
    val loading: Boolean = true,
)

/**
 * The bubbles of a conversation, oldest first, each carrying the day chip only
 * when it starts a new day.
 *
 * There is no "pendiente de enviar" flag: RTDB paints the message as soon as the
 * write reaches the local cache, and the snapshot carries no per-message ack, so
 * inventing one would show a state the database cannot confirm.
 */
fun parentMessagesOf(
    messages: List<ChatMessage>,
    myUid: String,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): List<ParentMessageUi> {
    var previousDay: String? = null
    return messages.map { message ->
        val day = chatDayLabel(message.sentAt, now, zone)
        val chip = day.takeIf { it.isNotBlank() && it != previousDay }
        previousDay = day

        ParentMessageUi(
            id = message.id,
            text = message.text,
            mine = message.isMine(myUid),
            time = clockLabel(message.sentAt, zone),
            dayLabel = chip,
        )
    }
}
