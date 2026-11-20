package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.domain.model.ChatMessage
import com.example.aulasegura.domain.model.ROLE_DOCENTE
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parts of the tutor's chat that break without anyone noticing: a row that
 * says "Ayer" tomorrow, a search that hides the chat the family is looking for,
 * or a day chip repeated on every bubble of the same day. Pinned against a fixed
 * clock and timezone.
 */
class ParentChatUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = at(2026, Calendar.NOVEMBER, 17, 12, 0)

    @Test
    fun `a conversation with no messages does not invent one`() {
        val row = parentChatRowOf(conversation(lastMessage = "", lastMessageAt = 0L), now, utc)

        assertEquals("Todavía no hay mensajes.", row.preview)
        assertEquals("", row.time)
        assertNull(row.unread)
    }

    @Test
    fun `only unread counts above zero reach the row`() {
        assertEquals(3, parentChatRowOf(conversation(unread = 3), now, utc).unread)
        assertNull(parentChatRowOf(conversation(unread = 0), now, utc).unread)
    }

    @Test
    fun `the search matches the teacher, who they are and the last message`() {
        val rows = listOf(
            parentChatRowOf(conversation(title = "Docente de 3° B"), now, utc),
            parentChatRowOf(
                conversation(
                    title = "Docente de 4° D",
                    subtitle = "Docente · 4° D",
                    lastMessage = "Mariana tendrá examen el jueves",
                ),
                now,
                utc,
            ),
        )

        assertEquals(2, filterParentChatRows(rows, "").size)
        assertEquals(1, filterParentChatRows(rows, "3°").size)
        assertEquals(1, filterParentChatRows(rows, "EXAMEN").size)
        assertEquals(2, filterParentChatRows(rows, "  ").size)
        assertTrue(filterParentChatRows(rows, "5° A").isEmpty())
    }

    @Test
    fun `the day chip is drawn once per day, not on every bubble`() {
        val messages = listOf(
            message(id = "m1", sentAt = at(2026, Calendar.NOVEMBER, 16, 9, 0)),
            message(id = "m2", sentAt = at(2026, Calendar.NOVEMBER, 17, 10, 41)),
            message(id = "m3", sentAt = at(2026, Calendar.NOVEMBER, 17, 10, 42)),
        )

        val bubbles = parentMessagesOf(messages, myUid = "uid-me", now = now, zone = utc)

        assertEquals(listOf("Ayer", "Hoy", null), bubbles.map { it.dayLabel })
        assertEquals(listOf("09:00", "10:41", "10:42"), bubbles.map { it.time })
    }

    @Test
    fun `only the sender sees their own bubble on the right`() {
        val bubble = parentMessagesOf(
            listOf(message(id = "m1", senderId = "uid-me")),
            myUid = "uid-me",
            now = now,
            zone = utc,
        ).single()

        assertTrue(bubble.mine)
    }

    @Test
    fun `the teacher row carries the group and the initials of the name`() {
        val row = parentTeacherRowOf(
            ChatContact(
                otherUid = "uid-teacher",
                otherRole = ROLE_DOCENTE,
                label = "Docente de 3° B",
                studentId = "stu-1",
                studentName = "Mariana Gómez",
                groupLabel = "3° B",
            ),
        )

        assertEquals("uid-teacher", row.otherUid)
        assertEquals("3° B", row.groupLabel)
        assertEquals("B", row.initials)
    }

    private fun conversation(
        title: String = "Docente de 3° B",
        subtitle: String = "Docente · 3° B",
        lastMessage: String = "Nos vemos el jueves",
        lastMessageAt: Long = at(2026, Calendar.NOVEMBER, 17, 10, 41),
        unread: Int = 0,
    ) = ChatConversation(
        id = "uid-me__uid-teacher",
        title = title,
        subtitle = subtitle,
        lastMessage = lastMessage,
        lastMessageAt = lastMessageAt,
        unread = unread,
        studentId = "stu-1",
        groupLabel = "3° B",
        otherUid = "uid-teacher",
    )

    private fun message(
        id: String,
        sentAt: Long = at(2026, Calendar.NOVEMBER, 17, 10, 41),
        senderId: String = "uid-teacher",
    ) = ChatMessage(id = id, senderId = senderId, text = "Hola", sentAt = sentAt)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
