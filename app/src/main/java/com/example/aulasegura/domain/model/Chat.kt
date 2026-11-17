package com.example.aulasegura.domain.model

/**
 * One chat as the list shows it.
 *
 * The messages live in Realtime Database; this is only the summary Firestore
 * keeps so the list paints without opening every conversation (`CONTEXT.md`
 * §`conversations/{conversationId}`).
 *
 * [title] is the other participant's name, [subtitle] who they are ("Tutor de
 * Mariana Gómez"), [unread] only this device's counter.
 */
data class ChatConversation(
    val id: String,
    val title: String,
    val subtitle: String,
    val lastMessage: String,
    val lastMessageAt: Long,
    val unread: Int,
    val studentId: String?,
    /** `"3° B"`, the chip the list filters by. */
    val groupLabel: String?,
    /** The other participant, needed to read presence and to title the screen. */
    val otherUid: String,
)

/**
 * A tutor the teacher can start a chat with.
 *
 * Built from the roster: a docente cannot read `users/{uid}` of a tutor (the
 * rules only allow the owner or the directivo), so the name comes from the child
 * — "Tutor de Mariana Gómez" — and never from a profile the app cannot read.
 */
data class ChatContact(
    val tutorUid: String,
    val label: String,
    val studentId: String?,
    val studentName: String,
    val groupLabel: String,
)

/** One message, already out of RTDB. */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val sentAt: Long,
) {

    fun isMine(myUid: String): Boolean = senderId == myUid
}

/**
 * Conversation id, derived from the two participants.
 *
 * Deterministic and sorted so both sides compute the same document id without a
 * lookup, and so a second attempt to start the same chat cannot duplicate it.
 * The `__` separator is what `docs/rtdb.rules` uses to check membership from the
 * key alone: RTDB rules cannot read Firestore.
 */
fun chatConversationId(uidA: String, uidB: String): String {
    val first = if (uidA <= uidB) uidA else uidB
    val second = if (uidA <= uidB) uidB else uidA
    return "$first$SEPARATOR$second"
}

/** The other participant of [conversationId], or `null` when the id is malformed. */
fun otherParticipant(conversationId: String, myUid: String): String? =
    conversationId.split(SEPARATOR)
        .firstOrNull { it.isNotBlank() && it != myUid }

private const val SEPARATOR = "__"

/** What happened when the teacher tried to send a message. */
sealed interface ChatSendOutcome {

    /** RTDB accepted it and the Firestore summary was updated. */
    data object Sent : ChatSendOutcome

    /**
     * RTDB has it (possibly only locally, until there is network) but the summary
     * could not be written. The message is not lost; the list catches up later.
     */
    data object Queued : ChatSendOutcome

    data object Empty : ChatSendOutcome

    data object SignedOut : ChatSendOutcome

    data class Failed(val reason: String) : ChatSendOutcome
}

/** What happened when the teacher opened (or created) a conversation. */
sealed interface ChatOpenOutcome {

    data class Ready(val conversationId: String) : ChatOpenOutcome

    data object SignedOut : ChatOpenOutcome

    data class Failed(val reason: String) : ChatOpenOutcome
}
