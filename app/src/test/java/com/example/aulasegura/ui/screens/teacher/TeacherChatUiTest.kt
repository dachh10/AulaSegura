package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.ChatContact
import com.example.aulasegura.domain.model.ChatConversation
import com.example.aulasegura.ui.chat.chatInitials
import com.example.aulasegura.ui.chat.chatTimeLabel
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A relative time label and a search box are the parts that break without anyone
 * noticing: "Ayer" that says "Ayer" tomorrow, or a filter that hides the chat the
 * teacher is looking for. Pinned against a fixed clock and timezone.
 */
class TeacherChatUiTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = at(2026, Calendar.NOVEMBER, 17, 12, 0)

    @Test
    fun `today shows the hour and yesterday says so`() {
        assertEquals("10:41", chatTimeLabel(at(2026, Calendar.NOVEMBER, 17, 10, 41), now, utc))
        assertEquals("Ayer", chatTimeLabel(at(2026, Calendar.NOVEMBER, 16, 23, 5), now, utc))
    }

    @Test
    fun `inside the last week it shows the weekday, then the date`() {
        assertEquals("Sáb", chatTimeLabel(at(2026, Calendar.NOVEMBER, 14, 9, 0), now, utc))
        assertEquals("5 oct", chatTimeLabel(at(2026, Calendar.OCTOBER, 5, 9, 0), now, utc))
    }

    @Test
    fun `a conversation that never had a message prints no time`() {
        assertEquals("", chatTimeLabel(0L, now, utc))
    }

    @Test
    fun `the avatar takes the first letters of the first two words`() {
        assertEquals("CG", chatInitials("Carlos Gómez Pérez"))
        assertEquals("T", chatInitials("Tutor"))
        assertEquals("?", chatInitials("   "))
    }

    @Test
    fun `a chat without messages still has a row`() {
        val row = chatRowOf(conversation(lastMessage = ""), now, utc)

        assertEquals("Todavía no hay mensajes.", row.preview)
        assertEquals("Tutor de Mariana Gómez", row.name)
        assertEquals("Tutor de Mariana Gómez", row.subtitle)
        assertEquals("3° B", row.group)
        assertEquals(2, row.unread)
        assertEquals("MG", row.initials)
    }

    @Test
    fun `the row of a real conversation prints its last message and hour`() {
        val row = chatRowOf(
            conversation(lastMessage = "Gracias, profesor.", lastMessageAt = at(2026, Calendar.NOVEMBER, 17, 9, 3)),
            now,
            utc,
        )

        assertEquals("Gracias, profesor.", row.preview)
        assertEquals("09:03", row.time)
    }

    @Test
    fun `the group chips come from the chats that exist`() {
        val rows = listOf(
            row(id = "1", group = "3° B"),
            row(id = "2", group = "4° A"),
            row(id = "3", group = "3° B"),
        )

        assertEquals(listOf("Todos", "3° B", "4° A"), chatFiltersFor(rows))
        assertEquals(listOf("Todos"), chatFiltersFor(emptyList()))
    }

    @Test
    fun `search matches the family, the child and the last message`() {
        val rows = listOf(
            row(id = "1", name = "Tutor de Mariana Gómez", preview = "¿Qué temas incluye?"),
            row(id = "2", name = "Tutor de Bruno Ríos", preview = "Buenos días"),
        )

        assertEquals(listOf("1"), filterChatRows(rows, "mariana", "Todos").map { it.id })
        assertEquals(listOf("2"), filterChatRows(rows, "BUENOS", "Todos").map { it.id })
        assertEquals(emptyList<String>(), filterChatRows(rows, "nadie", "Todos").map { it.id })
    }

    @Test
    fun `the group chip narrows the list and keeps the search`() {
        val rows = listOf(
            row(id = "1", group = "3° B", name = "Tutor de Mariana Gómez"),
            row(id = "2", group = "4° A", name = "Tutor de Bruno Ríos"),
        )

        assertEquals(listOf("2"), filterChatRows(rows, "", "4° A").map { it.id })
        assertEquals(emptyList<String>(), filterChatRows(rows, "mariana", "4° A").map { it.id })
        assertTrue(filterChatRows(rows, "", "Todos").size == 2)
    }

    @Test
    fun `a tutor row keeps its group and its label`() {
        val contact = ChatContact(
            otherUid = "uid-tutor",
            label = "Tutor de Mariana Gómez",
            studentId = "stu-1",
            studentName = "Mariana Gómez",
            groupLabel = "3° B",
        )

        val row = contactRowOf(contact)

        assertEquals("uid-tutor", row.otherUid)
        assertEquals("Tutor de Mariana Gómez", row.label)
        assertEquals("3° B", row.groupLabel)
    }

    @Test
    fun `a chat with no group is not offered as a chip`() {
        assertEquals(listOf("Todos"), chatFiltersFor(listOf(row(id = "1", group = null))))
        assertNull(row(id = "1", group = null).group)
    }

    private fun conversation(
        lastMessage: String,
        lastMessageAt: Long = at(2026, Calendar.NOVEMBER, 17, 11, 0),
    ) = ChatConversation(
        id = "uid-docente__uid-tutor",
        title = "Tutor de Mariana Gómez",
        subtitle = "Tutor de Mariana Gómez",
        lastMessage = lastMessage,
        lastMessageAt = lastMessageAt,
        unread = 2,
        studentId = "stu-1",
        groupLabel = "3° B",
        otherUid = "uid-tutor",
    )

    private fun row(
        id: String,
        group: String? = "3° B",
        name: String = "Tutor de Mariana Gómez",
        preview: String = "",
    ) = TeacherChatRowUi(
        id = id,
        name = name,
        subtitle = "Tutor",
        preview = preview,
        time = "",
        unread = 0,
        group = group,
        initials = "MG",
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
