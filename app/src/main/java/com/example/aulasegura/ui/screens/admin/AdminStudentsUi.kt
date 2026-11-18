package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.repository.AdminStudentError
import com.example.aulasegura.domain.repository.AdminStudentFields
import com.example.aulasegura.domain.repository.AdminStudentInput
import com.example.aulasegura.domain.repository.AdminStudentResult
import com.example.aulasegura.domain.repository.CreateStudentResult
import com.example.aulasegura.domain.repository.LinkingCodeResult

/**
 * What the students screens render.
 *
 * Everything in this file is pure: no Firebase, no ViewModel, no Compose. A bug in
 * the CURP guard, the group filter or the document builder is a bug in a plain
 * function that a unit test can pin down. The document builder delegates to the
 * domain object so the form and the Firestore write cannot drift apart.
 */

/** State of the new-student form. */
data class AdminNewStudentUiState(
    val loading: Boolean = false,
    val groups: List<AdminGroup> = emptyList(),
    val form: AdminStudentForm = AdminStudentForm(),
    val linkingCode: String? = null,
    val error: String? = null,
    val message: String? = null,
)

/** One rendered row of the Alumnos tab. */
data class AdminStudentRowUi(
    val id: String,
    val name: String,
    val group: String,
    val listNumber: Int,
    val active: Boolean,
)

/** The create form. The list number is computed from the roster, never typed. */
data class AdminStudentForm(
    val firstName: String = "",
    val lastName: String = "",
    val curp: String = "",
    val groupId: String = "",
)

/** A CURP is optional here; when present it is exactly 18 uppercase alphanumerics. */
private val CURP_REGEX = Regex("^[A-Z0-9]{18}$")

fun isValidCurp(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.isEmpty() || CURP_REGEX.matches(trimmed)
}

/** Error under the new-student form, or null when it can be submitted. */
fun validationError(form: AdminStudentForm): String? = when {
    form.firstName.isBlank() -> "El nombre es obligatorio."
    form.lastName.isBlank() -> "Los apellidos son obligatorios."
    !isValidCurp(form.curp) -> "La CURP debe tener 18 caracteres en mayúsculas."
    form.groupId.isBlank() -> "Selecciona un grupo."
    else -> null
}

fun isValid(form: AdminStudentForm): Boolean = validationError(form) == null

/** Maps the form to the repository input; the number is supplied by the caller. */
fun AdminStudentForm.toInput(): AdminStudentInput = AdminStudentInput(
    firstName = firstName,
    lastName = lastName,
    curp = curp,
    groupId = groupId,
)

/**
 * Builds the exact new-student document. [listNumber] comes from the alphabetical
 * roster ordering, not from the form. Server timestamps are added by the data layer;
 * the document id is not a field on the document itself.
 */
fun buildStudentDocument(
    form: AdminStudentForm,
    schoolId: String,
    listNumber: Int,
): Map<String, Any> = AdminStudentFields.newStudentDocument(form.toInput(), schoolId, listNumber)

/**
 * Filters real students by an optional group and by free text (name or CURP).
 *
 * The teacher's roll-call query is `groupId == X AND active == true`, so the group
 * id must reach the document unchanged; only the label is prettified for display.
 */
fun filterStudents(
    students: List<AdminStudent>,
    groupId: String?,
    query: String,
    groupNames: Map<String, String> = emptyMap(),
): List<AdminStudentRowUi> {
    val needle = query.trim()
    return students.asSequence()
        .filter { groupId.isNullOrBlank() || it.groupId == groupId }
        .filter {
            needle.isEmpty() ||
                it.displayName.contains(needle, ignoreCase = true) ||
                it.curp.contains(needle, ignoreCase = true) ||
                it.listNumber.toString() == needle
        }
        .map { it.toRow(groupNames) }
        .toList()
}

/** Maps a student to its Alumnos row. */
fun AdminStudent.toRow(groupNames: Map<String, String> = emptyMap()): AdminStudentRowUi =
    AdminStudentRowUi(
        id = id,
        name = displayName,
        group = groupLabel(groupId, groupNames),
        listNumber = listNumber,
        active = active,
    )

/**
 * Human label of a stored group id.
 *
 * An in-app group has a random Firestore id, so its real name comes from
 * [groupNames]. Seed ids such as `grp-3b` or `3B` are not in that map and keep the
 * "3° B" formatting; truly unknown ids are shown verbatim rather than invented.
 */
fun groupLabel(groupId: String, groupNames: Map<String, String> = emptyMap()): String {
    groupNames[groupId]?.takeIf { it.isNotBlank() }?.let { return it }
    val bare = groupId.removePrefix("grp-")
    if (bare.length != 2 || !bare[0].isDigit()) return bare
    return "${bare[0]}° ${bare[1].uppercaseChar()}"
}

/** User-facing copy for a typed repository failure. */
fun AdminStudentError.toMessage(): String = when (this) {
    AdminStudentError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    AdminStudentError.NETWORK -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AdminStudentError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
    AdminStudentError.DENIED -> "No tienes permiso para realizar esta acción."
    AdminStudentError.DUPLICATE_LIST_NUMBER ->
        "Ese número de lista ya está ocupado en el grupo. Elige otro."

    AdminStudentError.UNKNOWN -> "No se pudo completar la operación."
}

/**
 * User-facing copy for a failed create, appending the sanitized Firestore detail
 * when the data layer supplied one. Shown verbatim in the new-student error text.
 */
fun CreateStudentResult.Failure.toMessage(): String = appendFirestoreDetail(reason.toMessage(), detail)

/** Same copy for a failed logical baja/alta. */
fun AdminStudentResult.Failure.toMessage(): String = appendFirestoreDetail(reason.toMessage(), detail)

/** The student may already be saved, so this copy never implies a rollback. */
fun LinkingCodeResult.Failure.toMessage(): String = appendFirestoreDetail(
    "Alumno guardado, pero no se pudo generar el código de vinculación.",
    detail,
)

private fun appendFirestoreDetail(base: String, detail: String?): String {
    val clean = detail?.takeIf { it.isNotBlank() } ?: return base
    return "$base (Firestore: $clean)"
}
