package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AccountRole
import com.example.aulasegura.domain.repository.AdminUserFields
import com.example.aulasegura.domain.repository.orphanedAfterCleanup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The list split, the form guard and the write payload are all pure functions: a
 * regression here would either hide a user in the wrong tab or send an identity
 * field the Firestore rules reject.
 */
class AdminUsersUiTest {

    private val docente = AdminUser(
        uid = "u-teacher",
        email = "ricardo@aula.test",
        role = UserRole.TEACHER,
        firstName = "Ricardo",
        lastName = "Salinas",
        fullName = "Ricardo Salinas",
        phone = "5551000001",
        subjects = listOf("Matemáticas"),
        groupIds = listOf("g-3b"),
        studentIds = emptyList(),
        active = true,
    )
    private val tutor = AdminUser(
        uid = "u-tutor",
        email = "carlos@aula.test",
        role = UserRole.TUTOR,
        firstName = "Carlos",
        lastName = "Gómez",
        fullName = "Carlos Gómez",
        phone = "5551000002",
        subjects = emptyList(),
        groupIds = listOf("g-3b"),
        studentIds = listOf("s-1", "s-2"),
        active = false,
    )
    private val unknown = docente.copy(
        uid = "u-unknown",
        email = "otro@aula.test",
        role = UserRole.UNKNOWN,
        fullName = "Otro Usuario",
    )
    private val all = listOf(docente, tutor, unknown)

    @Test
    fun `tabs split by role and unknown roles are simply not listed`() {
        assertEquals(listOf("u-teacher"), filterUsers(all, AdminUsersTab.DOCENTES, "").map { it.uid })
        assertEquals(listOf("u-tutor"), filterUsers(all, AdminUsersTab.PADRES, "").map { it.uid })
        assertTrue(filterUsers(all, AdminUsersTab.ALUMNOS, "").isEmpty())
    }

    @Test
    fun `query matches name or email, case insensitively, within the tab`() {
        assertEquals(
            listOf("u-teacher"),
            filterUsers(all, AdminUsersTab.DOCENTES, "RICARDO").map { it.uid },
        )
        assertEquals(
            listOf("u-teacher"),
            filterUsers(all, AdminUsersTab.DOCENTES, "@AULA.test").map { it.uid },
        )
        assertEquals(
            listOf("u-tutor"),
            filterUsers(all, AdminUsersTab.PADRES, "gómez").map { it.uid },
        )
        assertTrue(filterUsers(all, AdminUsersTab.DOCENTES, "carlos").isEmpty())
    }

    @Test
    fun `a blank first or last name blocks saving`() {
        val base = formFrom(docente)
        assertEquals("El nombre es obligatorio.", validationError(base.copy(firstName = "  ")))
        assertEquals("El apellido es obligatorio.", validationError(base.copy(lastName = "")))
        assertNull(validationError(base))
        assertFalse(isValid(base.copy(firstName = "")))
        assertTrue(isValid(base))
    }

    @Test
    fun `payload carries only editable keys and derives fullName`() {
        val form = formFrom(docente).copy(
            firstName = "  Ana  ",
            lastName = " Ruiz ",
            phone = " 5559990000 ",
            subjects = "Español, Historia, Español",
            groupIds = "g-3a, g-3b",
            studentIds = "s-9",
            active = false,
        )
        val payload = buildUpdatePayload(form)

        assertEquals("Ana", payload[AdminUserFields.FIRST_NAME])
        assertEquals("Ruiz", payload[AdminUserFields.LAST_NAME])
        assertEquals("Ana Ruiz", payload[AdminUserFields.FULL_NAME])
        assertEquals("5559990000", payload[AdminUserFields.PHONE])
        assertEquals(listOf("Español", "Historia"), payload[AdminUserFields.SUBJECTS])
        assertEquals(false, payload[AdminUserFields.ACTIVE])

        // Regression guard: denormalized links are two-sided and belong to Grupos,
        // so this screen must never write either side of them.
        assertFalse(payload.containsKey(AdminUserFields.GROUP_IDS))
        assertFalse(payload.containsKey(AdminUserFields.STUDENT_IDS))

        val forbidden = setOf(
            "uid", "email", "role", "schoolId", "createdAt", "updatedAt",
            AdminUserFields.GROUP_IDS, AdminUserFields.STUDENT_IDS,
        )
        assertTrue(payload.keys.none { it in forbidden })
        assertEquals(6, payload.size)
    }

    @Test
    fun `role fallback keeps unknown Firestore values out of the way`() {
        assertEquals(UserRole.TEACHER, AdminUser.roleFromFirestore("docente"))
        assertEquals(UserRole.ADMIN, AdminUser.roleFromFirestore("directivo"))
        assertEquals(UserRole.TUTOR, AdminUser.roleFromFirestore("tutor"))
        assertEquals(UserRole.UNKNOWN, AdminUser.roleFromFirestore("alumno"))
        assertEquals(UserRole.UNKNOWN, AdminUser.roleFromFirestore(null))
    }

    private val validCreateForm = CreateAccountForm(
        email = "ana@aulasegura.com",
        password = "temporal123",
        firstName = " Ana ",
        lastName = " López ",
        phone = " 5551234567 ",
        subjects = "Español, Historia",
        groupIds = "grp-3b, grp-4a",
    )

    @Test
    fun `account validation checks email shape, password length and names`() {
        assertNull(createAccountValidationError(validCreateForm))
        assertTrue(isValidCreateAccount(validCreateForm))

        assertEquals(
            "Ingresa un correo electrónico válido.",
            createAccountValidationError(validCreateForm.copy(email = "ana.sin.arroba")),
        )
        assertEquals(
            "La contraseña debe tener al menos 6 caracteres.",
            createAccountValidationError(validCreateForm.copy(password = "12345")),
        )
        assertFalse(isValidCreateAccount(validCreateForm.copy(password = "12345")))
        assertEquals(
            "El nombre es obligatorio.",
            createAccountValidationError(validCreateForm.copy(firstName = "  ")),
        )
        assertEquals(
            "El apellido es obligatorio.",
            createAccountValidationError(validCreateForm.copy(lastName = "")),
        )
    }

    @Test
    fun `new account document has every rule-required key and never creates a directivo`() {
        val docenteDoc = buildNewAccountDocument(
            uid = "uid-new",
            email = validCreateForm.email,
            firstName = validCreateForm.firstName,
            lastName = validCreateForm.lastName,
            role = AccountRole.DOCENTE,
            phone = validCreateForm.phone,
            subjects = parseCsv(validCreateForm.subjects),
            groupIds = parseCsv(validCreateForm.groupIds),
            schoolId = "school-1",
        )

        val requiredKeys = setOf(
            AdminUserFields.UID,
            AdminUserFields.SCHOOL_ID,
            AdminUserFields.ROLE,
            AdminUserFields.ACTIVE,
            AdminUserFields.EMAIL,
            AdminUserFields.FIRST_NAME,
            AdminUserFields.LAST_NAME,
            AdminUserFields.FULL_NAME,
            AdminUserFields.CREATED_AT,
        )
        assertTrue(requiredKeys.all { it in docenteDoc })
        assertEquals("uid-new", docenteDoc[AdminUserFields.UID])
        assertEquals("school-1", docenteDoc[AdminUserFields.SCHOOL_ID])
        assertEquals("docente", docenteDoc[AdminUserFields.ROLE])
        assertEquals(true, docenteDoc[AdminUserFields.ACTIVE])
        assertEquals("ana@aulasegura.com", docenteDoc[AdminUserFields.EMAIL])
        assertEquals("Ana", docenteDoc[AdminUserFields.FIRST_NAME])
        assertEquals("López", docenteDoc[AdminUserFields.LAST_NAME])
        assertEquals("Ana López", docenteDoc[AdminUserFields.FULL_NAME])
        assertEquals("5551234567", docenteDoc[AdminUserFields.PHONE])
        assertEquals(listOf("Español", "Historia"), docenteDoc[AdminUserFields.SUBJECTS])
        assertEquals("grp-3b", docenteDoc[AdminUserFields.REQUESTED_GROUP_ID])
        assertEquals("g3-espanol-iii", docenteDoc[AdminUserFields.REQUESTED_SUBJECT])
        assertEquals("pendiente", docenteDoc[AdminUserFields.ESTADO])
        assertFalse(docenteDoc.containsKey(AdminUserFields.GROUP_IDS))
        assertEquals(AdminUserFields.ServerTimestamp, docenteDoc[AdminUserFields.CREATED_AT])
        assertEquals(AdminUserFields.ServerTimestamp, docenteDoc[AdminUserFields.UPDATED_AT])

        assertNotEquals("directivo", docenteDoc[AdminUserFields.ROLE])
        assertTrue(AccountRole.entries.none { it.firestoreValue == "directivo" })

        val tutorDoc = buildNewAccountDocument(
            uid = "uid-tutor",
            email = "papa@aulasegura.com",
            firstName = "Carlos",
            lastName = "Gómez",
            role = AccountRole.TUTOR,
            phone = "",
            subjects = listOf("Español"),
            groupIds = emptyList(),
            schoolId = "school-1",
        )
        assertEquals("tutor", tutorDoc[AdminUserFields.ROLE])
        assertEquals(emptyList<String>(), tutorDoc[AdminUserFields.STUDENT_IDS])
        assertFalse(tutorDoc.containsKey(AdminUserFields.SUBJECTS))
        assertFalse(tutorDoc.containsKey(AdminUserFields.PHONE))
    }

    @Test
    fun `the role comes from the tab, so Alumnos cannot create an account`() {
        assertEquals(AccountRole.DOCENTE, accountRoleFromTab(AdminUsersTab.DOCENTES))
        assertEquals(AccountRole.TUTOR, accountRoleFromTab(AdminUsersTab.PADRES))
        assertNull(accountRoleFromTab(AdminUsersTab.ALUMNOS))
    }

    @Test
    fun `cleanup verdict is false only when the orphan was actually deleted`() {
        // delete().await() returns null on success - a null result is NOT failure.
        assertFalse(orphanedAfterCleanup(hadUser = true, cleanupSucceeded = true))
        assertTrue(orphanedAfterCleanup(hadUser = true, cleanupSucceeded = false))
        assertTrue(orphanedAfterCleanup(hadUser = false, cleanupSucceeded = true))
    }

    @Test
    fun `the dialog offers reactivation once the student is deactivated`() {
        // Regression: an inactive student kept a disabled "Dar de baja" and nothing
        // else, so the logical baja could not be undone from the app.
        assertEquals("Dar de baja", studentStatusAction(active = true, saving = false))
        assertEquals("Reactivar", studentStatusAction(active = false, saving = false))
    }

    @Test
    fun `the saving verb follows the direction being written`() {
        assertEquals("Dando de baja…", studentStatusAction(active = true, saving = true))
        assertEquals("Reactivando…", studentStatusAction(active = false, saving = true))
    }
}
