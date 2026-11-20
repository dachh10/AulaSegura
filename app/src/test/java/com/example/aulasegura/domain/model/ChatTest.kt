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

    @Test
    fun `the same chat reads as the tutor for the teacher and as the docente for the family`() {
        assertEquals(
            "Tutor de Mariana Gómez",
            chatConversationSubtitle(ROLE_TUTOR, "Mariana Gómez", "3° B"),
        )
        assertEquals(
            "Docente · 3° B",
            chatConversationSubtitle(ROLE_DOCENTE, "Mariana Gómez", "3° B"),
        )
        assertEquals("Docente", chatConversationSubtitle(ROLE_DOCENTE, "", null))
        assertEquals("Tutor", chatConversationSubtitle(ROLE_TUTOR, "", "3° B"))
    }

    @Test
    fun `the teacher label tells two teachers of the same group apart when the profile is unreadable`() {
        assertEquals(
            "Ricardo Salinas Ortega",
            chatTeacherLabel("Ricardo Salinas Ortega", "4° D", titular = false),
        )
        assertEquals(
            "Docente titular de 4° D",
            chatTeacherLabel(null, "4° D", titular = true),
        )
        assertEquals(
            "Docente de 4° D",
            chatTeacherLabel("   ", "4° D", titular = false),
        )
        assertEquals("Docente", chatTeacherLabel(null, "", titular = true))
    }
}
