package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AccountRole
import com.example.aulasegura.domain.repository.AdminUserFields

/**
 * What the users screen renders.
 *
 * `empty` is derived so a failed load never shows the "no results" copy: an error
 * is a different state from an empty school. Students carry their own load state
 * so a failed students read does not blank out Docentes/Padres, or the other way
 * around.
 */
data class AdminUsersUiState(
    val loading: Boolean = true,
    val users: List<AdminUser> = emptyList(),
    val students: List<AdminStudent> = emptyList(),
    val studentsLoading: Boolean = true,
    val studentsError: String? = null,
    /** Real group names, keyed by group id, used instead of raw Firestore ids. */
    val groupNames: Map<String, String> = emptyMap(),
    /** Student whose detail dialog is open; null keeps every dialog closed. */
    val selectedStudentId: String? = null,
    val linkingCode: String? = null,
    val detailLoading: Boolean = false,
    val detailSaving: Boolean = false,
    val detailError: String? = null,
    val error: String? = null,
    val message: String? = null,
    // Account creation has its own state: a failure there must not blank out the
    // list, and a success must not close the user's edit dialog by accident.
    val createLoading: Boolean = false,
    val createError: String? = null,
    val createSuccess: String? = null,
) {
    val empty: Boolean get() = !loading && error == null && users.isEmpty()
}

/** The three tabs of the users screen. */
enum class AdminUsersTab(
    val label: String,
    val searchHint: String,
    val hint: String,
) {
    DOCENTES(
        label = "Docentes",
        searchHint = "Buscar docente",
        hint = "Los docentes reciben su acceso por correo cuando los das de alta.",
    ),
    PADRES(
        label = "Padres",
        searchHint = "Buscar padre",
        hint = "Las familias reciben su invitación cuando registras al alumno.",
    ),
    ALUMNOS(
        label = "Alumnos",
        searchHint = "Buscar alumno",
        hint = "Los alumnos activos aparecen en el pase de lista del docente. " +
            "Para dar de baja a un alumno, cambia su estado a inactivo.",
    ),
}

/** One rendered row of the list. */
data class AdminUserRowUi(
    val uid: String,
    val name: String,
    val subtitle: String,
    val active: Boolean,
)

/** The editable form of one user. Multi-value fields arrive as comma-separated text. */
data class AdminUserForm(
    val uid: String,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val subjects: String = "",
    val groupIds: String = "",
    val studentIds: String = "",
    val active: Boolean = true,
)

/** Prefills the form from a row of the real list. */
fun formFrom(user: AdminUser): AdminUserForm = AdminUserForm(
    uid = user.uid,
    firstName = user.firstName,
    lastName = user.lastName,
    phone = user.phone,
    subjects = user.subjects.joinToString(", "),
    groupIds = user.groupIds.joinToString(", "),
    studentIds = user.studentIds.joinToString(", "),
    active = user.active,
)

/** The in-app account creation form. Password is a one-off temporary credential. */
data class CreateAccountForm(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val subjects: String = "",
    val groupIds: String = "",
)

/** Minimal email shape: something@somewhere.tld. Firebase does the authoritative check. */
private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private const val MIN_PASSWORD_LENGTH = 6

/** The tab decides the role; there is no dropdown that could produce a directivo. */
fun accountRoleFromTab(tab: AdminUsersTab): AccountRole? = when (tab) {
    AdminUsersTab.DOCENTES -> AccountRole.DOCENTE
    AdminUsersTab.PADRES -> AccountRole.TUTOR
    AdminUsersTab.ALUMNOS -> null
}

/** Validation errors for the create form, in Spanish and safe to show under a field. */
fun createAccountValidationError(form: CreateAccountForm): String? = when {
    !EMAIL_REGEX.matches(form.email.trim()) -> "Ingresa un correo electrónico válido."
    form.password.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos 6 caracteres."
    form.firstName.isBlank() -> "El nombre es obligatorio."
    form.lastName.isBlank() -> "El apellido es obligatorio."
    else -> null
}

fun isValidCreateAccount(form: CreateAccountForm): Boolean =
    createAccountValidationError(form) == null

/**
 * The new-account document, exposed here so the screen's write contract is tested
 * next to the rest of the users UI. The real builder lives in the domain so the
 * data layer can use it without importing UI code; both timestamps are markers
 * that the data layer swaps for server timestamps at the write boundary.
 */
fun buildNewAccountDocument(
    uid: String,
    email: String,
    firstName: String,
    lastName: String,
    role: AccountRole,
    phone: String,
    subjects: List<String>,
    groupIds: List<String>,
    schoolId: String,
): Map<String, Any> = AdminUserFields.newAccountDocument(
    uid = uid,
    email = email,
    firstName = firstName,
    lastName = lastName,
    role = role,
    phone = phone,
    subjects = subjects,
    groupIds = groupIds,
    schoolId = schoolId,
)

/**
 * Filters the real users by role tab and by free text (name or email).
 *
 * Alumnos is excluded on purpose: students live in their own collection and are
 * filtered by [filterStudents]. Splitting roles here, after the single school
 * query, is what keeps Docentes and Padres on one read.
 */
fun filterUsers(
    users: List<AdminUser>,
    tab: AdminUsersTab,
    query: String,
): List<AdminUserRowUi> {
    val role = when (tab) {
        AdminUsersTab.DOCENTES -> UserRole.TEACHER
        AdminUsersTab.PADRES -> UserRole.TUTOR
        AdminUsersTab.ALUMNOS -> return emptyList()
    }

    val needle = query.trim()
    return users
        .asSequence()
        .filter { it.role == role }
        .filter {
            needle.isEmpty() ||
                it.fullName.contains(needle, ignoreCase = true) ||
                it.email.contains(needle, ignoreCase = true)
        }
        .map { it.toRow() }
        .toList()
}

/** Error to show under the form, or null when it can be saved. */
fun validationError(form: AdminUserForm): String? = when {
    form.firstName.isBlank() -> "El nombre es obligatorio."
    form.lastName.isBlank() -> "El apellido es obligatorio."
    else -> null
}

fun isValid(form: AdminUserForm): Boolean = validationError(form) == null

/** Splits comma-separated input into clean, unique values. */
fun parseCsv(value: String): List<String> =
    value.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

/**
 * Builds the exact payload the principal is allowed to write.
 *
 * Only whitelisted keys are present; identity fields are never added. `fullName`
 * is derived here so a name edit cannot leave a stale composed name in Firestore.
 * `updatedAt` is added by the data layer as a server timestamp.
 *
 * `groupIds` and `studentIds` are deliberately excluded even though the data layer
 * would accept them: those links are the rules' source of truth on the other side
 * (`groups/{id}.teacherIds`, `students.tutorIds`). Writing one side here would
 * desynchronize them. Two-sided assignment belongs to the Grupos flow.
 */
fun buildUpdatePayload(form: AdminUserForm): Map<String, Any> {
    val firstName = form.firstName.trim()
    val lastName = form.lastName.trim()
    return mapOf(
        AdminUserFields.FIRST_NAME to firstName,
        AdminUserFields.LAST_NAME to lastName,
        AdminUserFields.FULL_NAME to "$firstName $lastName".trim(),
        AdminUserFields.PHONE to form.phone.trim(),
        AdminUserFields.SUBJECTS to parseCsv(form.subjects),
        AdminUserFields.ACTIVE to form.active,
    )
}

/**
 * The single action of the student detail dialog, in both directions.
 *
 * A deactivated student used to keep a disabled "Dar de baja" and nothing else,
 * so the logical baja could not be undone from the app: the directivo had to go
 * to the console. The same slot now toggles the state and `saving` only changes
 * the verb.
 */
fun studentStatusAction(active: Boolean, saving: Boolean): String = when {
    saving && active -> "Dando de baja…"
    saving -> "Reactivando…"
    active -> "Dar de baja"
    else -> "Reactivar"
}

private fun AdminUser.toRow(): AdminUserRowUi = AdminUserRowUi(
    uid = uid,
    name = fullName.ifBlank { "$firstName $lastName".trim() }.ifBlank { email },
    subtitle = subtitleForRole(),
    active = active,
)

private fun AdminUser.subtitleForRole(): String = when (role) {
    UserRole.TEACHER -> listOf(
        subjects.joinToString(", "),
        groupIds.joinToString(", "),
    ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Docente" }

    UserRole.TUTOR -> {
        val noun = if (studentIds.size == 1) "alumno" else "alumnos"
        "Tutor · ${studentIds.size} $noun"
    }

    UserRole.ADMIN, UserRole.UNKNOWN -> email
}
