package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The tutor's profile shows the account and the children it really owns.
 *
 * A constant left in the screen is a constant the family cannot correct: the
 * mock carried "Carlos Gómez Pérez" and two invented children, and a family
 * with a different account would read them as their own.
 */
class ParentProfileUiTest {

    @Test
    fun `the header shows the name, the email and the avatar of the session`() {
        val ui = user(fullName = "Ana Ruiz", email = "ana@familia.mx", photoUrl = "https://cdn/a.jpg")
            .toParentProfileUi()

        assertEquals("Ana Ruiz", ui.fullName)
        assertEquals("ana@familia.mx", ui.email)
        assertEquals("https://cdn/a.jpg", ui.photoUrl)
    }

    @Test
    fun `an account without a photo shows no picture instead of a broken one`() {
        val ui = user(fullName = "Ana Ruiz").toParentProfileUi()

        assertEquals(null, ui.photoUrl)
    }

    @Test
    fun `the child card shows the first name and the group of the school`() {
        val child = child(
            studentId = "stu-1",
            fullName = "Mariana Ruiz",
            firstName = "Mariana",
            groupLabel = "4° D",
        )

        val card = child.toChildCardUi()

        assertEquals("stu-1", card.studentId)
        assertEquals("Mariana", card.name)
        assertEquals("4° D", card.grade)
        assertEquals("M", card.initial)
    }

    @Test
    fun `a child without a first name falls back to the full name`() {
        val card = child(fullName = "Mariana Ruiz", firstName = "").toChildCardUi()

        assertEquals("Mariana Ruiz", card.name)
        assertEquals("M", card.initial)
    }

    @Test
    fun `a child without any name keeps a readable initial`() {
        val card = child(fullName = "  ", firstName = "").toChildCardUi()

        assertEquals("?", card.initial)
    }

    private fun user(
        fullName: String,
        email: String = "familia@correo.mx",
        photoUrl: String? = null,
    ) = AppUser(
        uid = "tutor-1",
        email = email,
        role = UserRole.TUTOR,
        fullName = fullName,
        schoolId = "school-1",
        active = true,
        photoUrl = photoUrl,
    )

    private fun child(
        studentId: String = "stu-1",
        fullName: String = "Mariana Ruiz",
        firstName: String = "Mariana",
        groupLabel: String = "4° D",
    ) = ParentChild(
        studentId = studentId,
        fullName = fullName,
        firstName = firstName,
        listNumber = 3,
        groupId = "group-1",
        groupLabel = groupLabel,
    )
}
