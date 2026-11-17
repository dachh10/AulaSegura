package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.domain.model.ChatMessage
import com.example.aulasegura.domain.model.ChatOpenOutcome
import com.example.aulasegura.domain.model.ChatSendOutcome
import com.example.aulasegura.domain.model.chatConversationId
import com.example.aulasegura.domain.model.otherParticipant
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ChatRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
import com.google.firebase.Timestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * The chat, split across the two stores the schema chose.
 *
 * Write order is the contract: RTDB first (the message itself), Firestore second
 * (the summary the list paints). If the second fails the message still exists and
 * the send is reported as [ChatSendOutcome.Queued]; the inverse order would leave
 * a list row for a message nobody can open.
 *
 * Messages carry a client timestamp instead of `ServerValue.TIMESTAMP`: the
 * placeholder RTDB writes locally is a map, not a number, and the optimistic
 * bubble would show an empty hour until the server answered. Clock skew between
 * two phones is acceptable for a school chat and it is noted in `FASE1.md`.
 */
class FirestoreChatRepository(
    private val firestore: FirebaseFirestore,
    private val database: FirebaseDatabase,
    private val groups: GroupRepository,
    private val auth: AuthRepository,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
    private val clock: () -> Long = System::currentTimeMillis,
) : ChatRepository {

    override fun observeConversations(): Flow<List<ChatConversation>> {
        val user = auth.currentUser ?: return flowOf(emptyList())

        return callbackFlow {
            val registration = firestore.collection(CONVERSATIONS)
                .whereArrayContains(FIELD_PARTICIPANTS, user.uid)
                .orderBy(FIELD_LAST_MESSAGE_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.documents.orEmpty().mapNotNull { it.toConversation(user.uid) })
                }

            awaitClose { registration.remove() }
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        callbackFlow {
            val reference = database.getReference()
                .child(CONVERSATIONS).child(conversationId).child(MESSAGES)
                .orderByChild(FIELD_SENT_AT)
                .limitToLast(MESSAGE_WINDOW)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.children.mapNotNull { it.toMessage() }.sortedBy { it.sentAt })
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }

            reference.addValueEventListener(listener)
            awaitClose { reference.removeEventListener(listener) }
        }

    override fun observeOnline(conversationId: String, otherUid: String): Flow<Boolean> =
        callbackFlow {
            val reference = database.getReference()
                .child(CONVERSATIONS).child(conversationId)
                .child(PRESENCE).child(otherUid).child(FIELD_ONLINE)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.getValue(Boolean::class.java) ?: false)
                }

                // Presence is decoration: a denied or offline read means "not shown".
                override fun onCancelled(error: DatabaseError) = Unit
            }

            reference.addValueEventListener(listener)
            awaitClose { reference.removeEventListener(listener) }
        }

    override suspend fun contacts(): List<ChatContact> {
        if (auth.currentUser == null) return emptyList()
        val mine = groups.observeMyGroups().first()
        if (mine.isEmpty()) return emptyList()

        val labels = mine.associate { it.id to it.label }

        val documents = runCatching {
            firestore.collection(STUDENTS)
                // A teacher teaches a handful of groups; the query limit is 10.
                .whereIn(FIELD_GROUP_ID, mine.map { it.id }.take(MAX_GROUPS_PER_QUERY))
                .get()
                .await()
        }.getOrNull()?.documents ?: return emptyList()

        // One row per tutor: a parent with two children appears once, under the
        // first child the roster returns, and the teacher still reaches them.
        val byTutor = linkedMapOf<String, ChatContact>()
        documents.forEach { document ->
            val studentName = document.getString(FIELD_FULL_NAME).orEmpty()
            val groupLabel = labels[document.getString(FIELD_GROUP_ID)].orEmpty()
            val tutorIds = document.get(FIELD_TUTOR_IDS) as? List<*> ?: return@forEach

            tutorIds.filterIsInstance<String>().forEach { tutorUid ->
                byTutor.getOrPut(tutorUid) {
                    ChatContact(
                        tutorUid = tutorUid,
                        label = if (studentName.isBlank()) {
                            "Tutor"
                        } else {
                            "Tutor de $studentName"
                        },
                        studentId = document.id,
                        studentName = studentName,
                        groupLabel = groupLabel,
                    )
                }
            }
        }

        return byTutor.values.sortedWith(compareBy({ it.groupLabel }, { it.label }))
    }

    override suspend fun ensureConversation(contact: ChatContact): ChatOpenOutcome {
        val user = auth.currentUser ?: return ChatOpenOutcome.SignedOut
        val conversationId = chatConversationId(user.uid, contact.tutorUid)
        val document = firestore.collection(CONVERSATIONS).document(conversationId)
        val participants = listOf(user.uid, contact.tutorUid)

        return try {
            // The rules deny `get()` on a document that does not exist (the read
            // clause needs `participants`), so existence is discovered by trying
            // the update first. On an existing chat it touches nothing else, which
            // is what keeps the unread counters intact.
            document.update(
                mapOf(
                    FIELD_PARTICIPANTS to participants,
                    FIELD_STUDENT_ID to contact.studentId,
                    FIELD_STUDENT_NAME to contact.studentName,
                    FIELD_GROUP_LABEL to contact.groupLabel,
                ),
            ).await()
            ChatOpenOutcome.Ready(conversationId)
        } catch (notThere: FirebaseFirestoreException) {
            try {
                document.set(initialDocument(user.uid, user.fullName, user.role.name.lowercase(), contact))
                    .await()
                ChatOpenOutcome.Ready(conversationId)
            } catch (failed: Exception) {
                ChatOpenOutcome.Failed(failed.message ?: "No se pudo abrir el chat.")
            }
        }
    }

    override suspend fun sendMessage(conversationId: String, text: String): ChatSendOutcome {
        val user = auth.currentUser ?: return ChatSendOutcome.SignedOut
        val clean = text.trim().take(MAX_TEXT)
        if (clean.isEmpty()) return ChatSendOutcome.Empty

        val now = clock()
        val reference = database.getReference()
            .child(CONVERSATIONS).child(conversationId).child(MESSAGES)
            .push()

        return try {
            reference.setValue(
                mapOf(
                    FIELD_SENDER_ID to user.uid,
                    FIELD_TEXT to clean,
                    FIELD_SENT_AT to now,
                ),
            ).await()
            analytics.log(AnalyticsEvent.ChatMessageSent)

            val summaryWritten = otherParticipant(conversationId, user.uid)?.let { other ->
                runCatching {
                    firestore.collection(CONVERSATIONS).document(conversationId).update(
                        mapOf(
                            FIELD_LAST_MESSAGE to clean,
                            FIELD_LAST_MESSAGE_AT to FieldValue.serverTimestamp(),
                            FIELD_LAST_SENDER_ID to user.uid,
                            "unread.$other" to FieldValue.increment(1),
                        ),
                    ).await()
                }.isSuccess
            } ?: true

            if (summaryWritten) ChatSendOutcome.Sent else ChatSendOutcome.Queued
        } catch (failed: Exception) {
            ChatSendOutcome.Failed(failed.message ?: "No se pudo enviar el mensaje.")
        }
    }

    override suspend fun markRead(conversationId: String) {
        val user = auth.currentUser ?: return
        runCatching {
            firestore.collection(CONVERSATIONS).document(conversationId)
                .update("unread.${user.uid}", 0)
                .await()
        }
    }

    override suspend fun goOnline(conversationId: String) {
        val user = auth.currentUser ?: return
        val presence = database.getReference()
            .child(CONVERSATIONS).child(conversationId)
            .child(PRESENCE).child(user.uid)

        runCatching {
            presence.setValue(mapOf(FIELD_ONLINE to true, FIELD_LAST_SEEN to clock()))
            presence.onDisconnect().setValue(
                mapOf(FIELD_ONLINE to false, FIELD_LAST_SEEN to clock()),
            )
        }
    }

    override suspend fun goOffline(conversationId: String) {
        val user = auth.currentUser ?: return
        runCatching {
            database.getReference()
                .child(CONVERSATIONS).child(conversationId)
                .child(PRESENCE).child(user.uid)
                .setValue(mapOf(FIELD_ONLINE to false, FIELD_LAST_SEEN to clock()))
        }
    }

    private fun initialDocument(
        myUid: String,
        myName: String,
        myRole: String,
        contact: ChatContact,
    ): Map<String, Any?> = mapOf(
        FIELD_PARTICIPANTS to listOf(myUid, contact.tutorUid),
        "participantNames" to mapOf(myUid to myName, contact.tutorUid to contact.label),
        "participantRoles" to mapOf(myUid to myRole, contact.tutorUid to ROLE_TUTOR),
        FIELD_STUDENT_ID to contact.studentId,
        FIELD_STUDENT_NAME to contact.studentName,
        FIELD_GROUP_LABEL to contact.groupLabel,
        FIELD_LAST_MESSAGE to "",
        FIELD_LAST_MESSAGE_AT to Timestamp.now(),
        FIELD_LAST_SENDER_ID to "",
        "unread" to mapOf(myUid to 0, contact.tutorUid to 0),
        "createdAt" to Timestamp.now(),
    )

    private fun DocumentSnapshot.toConversation(myUid: String): ChatConversation? {
        val participants = (get(FIELD_PARTICIPANTS) as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty()
        if (myUid !in participants) return null

        val other = participants.firstOrNull { it != myUid } ?: return null
        val names = get("participantNames") as? Map<*, *>
        val roles = get("participantRoles") as? Map<*, *>
        val unread = get("unread") as? Map<*, *>
        val studentName = getString(FIELD_STUDENT_NAME).orEmpty()

        val title = (names?.get(other) as? String)?.takeIf { it.isNotBlank() }
            ?: if (studentName.isBlank()) "Tutor" else "Tutor de $studentName"
        val role = (roles?.get(other) as? String).orEmpty()

        return ChatConversation(
            id = id,
            title = title,
            subtitle = subtitleFor(role, studentName),
            lastMessage = getString(FIELD_LAST_MESSAGE).orEmpty(),
            lastMessageAt = getTimestamp(FIELD_LAST_MESSAGE_AT)?.toDate()?.time ?: 0L,
            unread = (unread?.get(myUid) as? Number)?.toInt() ?: 0,
            studentId = getString(FIELD_STUDENT_ID),
            groupLabel = getString(FIELD_GROUP_LABEL),
            otherUid = other,
        )
    }

    private fun subtitleFor(role: String, studentName: String): String = when {
        role == ROLE_TUTOR && studentName.isNotBlank() -> "Tutor de $studentName"
        role == ROLE_TUTOR -> "Tutor"
        role == ROLE_DOCENTE -> "Docente"
        studentName.isNotBlank() -> studentName
        else -> ""
    }

    private fun DataSnapshot.toMessage(): ChatMessage? {
        val id = key ?: return null
        val senderId = child(FIELD_SENDER_ID).getValue(String::class.java) ?: return null
        val text = child(FIELD_TEXT).getValue(String::class.java) ?: return null
        // A pending local write uses our own Long, so this read never sees the
        // `{".sv": "timestamp"}` map.
        val sentAt = child(FIELD_SENT_AT).getValue(Long::class.java) ?: 0L
        return ChatMessage(id = id, senderId = senderId, text = text, sentAt = sentAt)
    }

    private companion object {
        const val CONVERSATIONS = "conversations"
        const val MESSAGES = "messages"
        const val PRESENCE = "presence"
        const val STUDENTS = "students"

        const val FIELD_PARTICIPANTS = "participants"
        const val FIELD_LAST_MESSAGE = "lastMessage"
        const val FIELD_LAST_MESSAGE_AT = "lastMessageAt"
        const val FIELD_LAST_SENDER_ID = "lastSenderId"
        const val FIELD_STUDENT_ID = "studentId"
        const val FIELD_STUDENT_NAME = "studentName"
        const val FIELD_GROUP_LABEL = "groupLabel"
        const val FIELD_GROUP_ID = "groupId"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_TUTOR_IDS = "tutorIds"

        const val FIELD_SENDER_ID = "senderId"
        const val FIELD_TEXT = "text"
        const val FIELD_SENT_AT = "sentAt"
        const val FIELD_ONLINE = "online"
        const val FIELD_LAST_SEEN = "lastSeen"

        const val ROLE_TUTOR = "tutor"
        const val ROLE_DOCENTE = "docente"

        const val MAX_TEXT = 2000
        const val MESSAGE_WINDOW = 200
        const val MAX_GROUPS_PER_QUERY = 10
    }
}
