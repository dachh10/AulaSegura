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
 * The other person of a chat the signed-in user wants to start.
 *
 * Built from the data each role may read. The teacher takes it from the roster:
 * a docente cannot read a tutor's `users/{uid}`, so the name comes from the child
 * — "Tutor de Mariana Gómez". The family takes it from its child's group and
 * then reads the teacher's profile, which the rules open to the tutors of that
 * teacher's groups.
 */
data class ChatContact(
    val otherUid: String,
    val label: String,
    /** `"tutor"` or `"docente"`, the value written to `participantRoles`. */
    val otherRole: String = ROLE_TUTOR,
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

const val ROLE_TUTOR: String = "tutor"
const val ROLE_DOCENTE: String = "docente"

/**
 * Who the other person is, in the words the current user reads.
 *
 * The same document is a "Tutor de Mariana Gómez" for the teacher and a
 * "Docente · 3° B" for the family, so the subtitle is derived from the role and
 * never stored: a stored copy would be wrong for one of the two sides.
 */
fun chatConversationSubtitle(
    role: String,
    studentName: String,
    groupLabel: String?,
): String = when {
    role == ROLE_TUTOR && studentName.isNotBlank() -> "Tutor de $studentName"
    role == ROLE_TUTOR -> "Tutor"
    role == ROLE_DOCENTE && !groupLabel.isNullOrBlank() -> "Docente · $groupLabel"
    role == ROLE_DOCENTE -> "Docente"
    studentName.isNotBlank() -> studentName
    else -> ""
}

/** The other participant of [conversationId], or `null` when the id is malformed. */
fun otherParticipant(conversationId: String, myUid: String): String? =
    conversationId.split(SEPARATOR)
        .firstOrNull { it.isNotBlank() && it != myUid }

/**
 * The name the family sees for a teacher.
 *
 * A group can have more than one teacher, so the label has to tell them apart:
 * when the profile is readable it is the real name, and when it is not (offline,
 * or a legacy group) the fallback names the titular instead of printing
 * "Docente de 4° D" twice on the same screen.
 */
fun chatTeacherLabel(
    fullName: String?,
    groupLabel: String,
    titular: Boolean,
): String {
    val name = fullName?.trim().orEmpty()
    if (name.isNotEmpty()) return name

    return when {
        groupLabel.isBlank() -> "Docente"
        titular -> "Docente titular de $groupLabel"
        else -> "Docente de $groupLabel"
    }
}

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
