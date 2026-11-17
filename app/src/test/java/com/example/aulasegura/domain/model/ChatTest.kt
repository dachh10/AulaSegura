package com.example.aulasegura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The conversation id is the only thing both sides must agree on without a
 * lookup, and `docs/rtdb.rules` derives membership from it. Two different ids for
 * the same pair of users would be two chats that never see each other.
 */
class ChatTest {

    @Test
    fun `the id is the same whichever side computes it`() {
        assertEquals(chatConversationId("uid-b", "uid-a"), chatConversationId("uid-a", "uid-b"))
    }

    @Test
    fun `the id is sorted and separated, so the rules can split it`() {
        assertEquals("uid-a__uid-b", chatConversationId("uid-b", "uid-a"))
    }

    @Test
    fun `the other participant comes out of the id`() {
        val id = chatConversationId("uid-a", "uid-b")

        assertEquals("uid-b", otherParticipant(id, "uid-a"))
        assertEquals("uid-a", otherParticipant(id, "uid-b"))
    }

    @Test
    fun `an id with a single participant has no counterpart`() {
        assertNull(otherParticipant("uid-a", "uid-a"))
    }

    @Test
    fun `only the sender sees their own bubble on the right`() {
        val message = ChatMessage(id = "m1", senderId = "uid-a", text = "Hola", sentAt = 1L)

        assertEquals(true, message.isMine("uid-a"))
        assertEquals(false, message.isMine("uid-b"))
    }
}
