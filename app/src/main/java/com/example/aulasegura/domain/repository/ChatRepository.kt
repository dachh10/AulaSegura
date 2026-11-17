package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.domain.model.ChatMessage
import com.example.aulasegura.domain.model.ChatOpenOutcome
import com.example.aulasegura.domain.model.ChatSendOutcome
import kotlinx.coroutines.flow.Flow

/**
 * Chat: messages in Realtime Database, summaries in Firestore.
 *
 * Two stores and one message (`FASE1.md` bloque 9): the message is written to
 * RTDB and only then the summary is updated, so a summary can never describe a
 * message that does not exist.
 *
 * Online-only on purpose: unlike attendance or incidents there is no Room cache
 * here, because RTDB already keeps its own disk cache and a second copy would be
 * a second truth. The list of conversations does come from Firestore, so it needs
 * network the first time.
 */
interface ChatRepository {

    /** Conversations the signed-in user takes part in, newest activity first. */
    fun observeConversations(): Flow<List<ChatConversation>>

    /** Last messages of [conversationId], oldest first, capped to 200. */
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    /** Whether the other participant is connected to this conversation. */
    fun observeOnline(conversationId: String, otherUid: String): Flow<Boolean>

    /**
     * Tutors the teacher can talk to: one row per distinct tutor among the
     * students of their groups.
     */
    suspend fun contacts(): List<ChatContact>

    /** Opens the chat with [contact], creating the summary if it does not exist. */
    suspend fun ensureConversation(contact: ChatContact): ChatOpenOutcome

    suspend fun sendMessage(conversationId: String, text: String): ChatSendOutcome

    /** Clears this user's unread counter. Never fails loudly: it is cosmetic. */
    suspend fun markRead(conversationId: String)

    /** Announces presence for this device; goes offline automatically on disconnect. */
    suspend fun goOnline(conversationId: String)

    suspend fun goOffline(conversationId: String)
}
