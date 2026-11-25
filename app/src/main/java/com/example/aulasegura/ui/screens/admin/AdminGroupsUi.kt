package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminScheduleSlot
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AdminGroupError
import com.example.aulasegura.domain.repository.AdminGroupFields
import com.example.aulasegura.domain.repository.AdminGroupResult
import com.example.aulasegura.domain.repository.AdminStudentFields
import com.example.aulasegura.domain.repository.GroupAssignmentDescription
import com.example.aulasegura.domain.repository.describeGroupAssignment

/**
 * What the groups screens render.
 *
 * Everything in this file is pure: no Firebase, no ViewModel, no Compose. A bug in
 * the grade filter, the form guard or the two-sided assignment description is a bug
 * in a plain function that a unit test can pin down.
 */
data class AdminGroupsUiState(
    val loading: Boolean = true,
    val groups: List<AdminGroup> = emptyList(),
    val error: String? = null,
    val message: String? = null,
) {
    val empty: Boolean get() = !loading && error == null && groups.isEmpty()
}

/** State of the group detail: the loaded group, the school's teachers and the editable slots. */
data class AdminGroupDetailUiState(
    val groupId: String = "",
    val loading: Boolean = true,
    val group: AdminGroup? = null,
    val users: List<AdminUser> = emptyList(),
    /** The school's other groups, offered as move targets when deleting a group with students. */
    val destinationOptions: List<AdminGroup> = emptyList(),
    /** Students of this group, filtered client-side from the school-wide student list. */
    val students: List<AdminStudent> = emptyList(),
    val studentsLoading: Boolean = false,
    val slots: List<AdminScheduleSlot> = emptyList(),
    val query: String = "",
    val saving: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** Set while the director picks a subject for a manual assignment. */
    val pendingSubject: AdminGroupDetailViewModel.PendingSubject? = null,
) {
    val missing: Boolean get() = !loading && error == null && group == null
}

/** The grade chips of the list. Mirrors the sample screen's Todos/1°/2°/3°. */
enum class AdminGroupGradeFilter(
    val label: String,
    val grade: Int?,
) {
    Todos("Todos", null),
    Primero("1°", 1),
    Segundo("2°", 2),
    Tercero("3°", 3),
}

/** One rendered row of the group list. */
data class AdminGroupRowUi(
    val id: String,
    val name: String,
    val subtitle: String,
    val teacherCount: Int,
    val studentCount: Int,
)

/** The create-group form. Grade is text here so an invalid value can be shown. */
data class AdminGroupForm(
    val name: String = "",
    val grade: String = "",
    val section: String = "",
)

/** A teacher row of the detail screen, with the assignment state of this group. */
data class AdminTeacherRowUi(
    val uid: String,
    val name: String,
    val subtitle: String,
    val assigned: Boolean,
)

/** Filters real groups by grade. `null` means "all grades". */
fun filterGroupsByGrade(
    groups: List<AdminGroup>,
    grade: Int?,
): List<AdminGroup> = if (grade == null) groups else groups.filter { it.grade == grade }

/** Maps a group to its list row, with honest counts and no fabricated metrics. */
fun AdminGroup.toRow(): AdminGroupRowUi = AdminGroupRowUi(
    id = id,
    name = label,
    subtitle = listOf(
        "$studentCount alumnos",
        "${teacherIds.size} docentes",
    ).joinToString(" · "),
    teacherCount = teacherIds.size,
    studentCount = studentCount,
)

/** Parses the grade field. Only 1..6 are valid (CONTEXT §6.2). */
fun parseGrade(value: String): Int? = value.trim().toIntOrNull()?.takeIf { it in 1..6 }

/** Error under the create form, or null when it can be saved. */
fun validationError(form: AdminGroupForm): String? = when {
    form.name.isBlank() -> "El nombre del grupo es obligatorio."
    parseGrade(form.grade) == null -> "El grado debe ser un número entre 1 y 6."
    form.section.isBlank() -> "La sección es obligatoria."
    else -> null
}

fun isValid(form: AdminGroupForm): Boolean = validationError(form) == null

/**
 * Builds the exact new-group document. Delegates to the domain builder so the form
 * and the Firestore write cannot drift apart.
 */
fun buildGroupDocument(
    form: AdminGroupForm,
    schoolId: String,
): Map<String, Any> = AdminGroupFields.newGroupDocument(
    name = form.name,
    grade = parseGrade(form.grade) ?: 0,
    section = form.section,
    schoolId = schoolId,
)

/**
 * Fields of a group edit. The repository adds updatedAt at the data boundary;
 * schoolId and the derived counters are never part of this payload.
 */
fun buildGroupUpdateMap(form: AdminGroupForm): Map<String, Any> =
    AdminGroupFields.groupUpdateDocument(
        name = form.name,
        grade = parseGrade(form.grade) ?: 0,
        section = form.section,
    )

/**
 * Maps schedule rows to deterministic (slotId, Firestore map) pairs.
 *
 * The same rows always produce the same ids, so `saveSchedule` is an idempotent
 * upsert rather than a growing pile of duplicate slots.
 */
fun buildSlotDocuments(
    slots: List<AdminScheduleSlot>,
): List<Pair<String, Map<String, Any>>> = slots.map { slot ->
    AdminGroupFields.slotId(slot.weekday, slot.startTime) to AdminGroupFields.slotDocument(slot)
}

/**
 * The two-sided assignment contract, exposed for the UI and the regression test.
 * The data layer writes the same paths in one batch.
 */
fun buildAssignmentDescription(
    groupId: String,
    teacherUid: String,
    assign: Boolean,
): GroupAssignmentDescription = describeGroupAssignment(groupId, teacherUid, assign)

/**
 * A group with students cannot simply disappear: a destination must be chosen.
 * The rule is deliberately derived from the same count the repository reads.
 */
fun needsDestinationGroup(studentCount: Int): Boolean = studentCount > 0

/** The complementary verdict used by the delete dialog to skip the dropdown. */
fun canDeleteGroupDirectly(studentCount: Int): Boolean = !needsDestinationGroup(studentCount)

/**
 * Students of one group, taken from the school-wide student list. The repository
 * exposes no by-group read, so the filter stays here as a plain, testable function.
 */
fun studentsForGroup(students: List<AdminStudent>, groupId: String): List<AdminStudent> =
    students.filter { it.groupId == groupId }

/**
 * Pure description of the ONE WriteBatch used to delete a group.
 *
 * Both the UI and the data layer name the same paths and fields; a regression that
 * drops the student moves or one of the counter updates fails a plain unit test.
 */
data class GroupDeletionDescription(
    val sourceGroupId: String,
    val destinationGroupId: String?,
    val studentIds: List<String>,
    val scheduleSlotIds: List<String>,
) {
    val movesStudents: Boolean get() = studentIds.isNotEmpty()
    val canDeleteDirectly: Boolean get() = !movesStudents
    val sourceCountDelta: Int get() = if (movesStudents) -studentIds.size else 0
    val destinationCountDelta: Int get() = if (movesStudents) studentIds.size else 0

    val studentPaths: List<String>
        get() = studentIds.map(AdminStudentFields::studentPath)

    val schedulePaths: List<String>
        get() = scheduleSlotIds.map {
            "${AdminGroupFields.groupPath(sourceGroupId)}/${AdminGroupFields.SCHEDULE_COLLECTION}/$it"
        }

    val summary: String get() = buildString {
        if (movesStudents) {
            studentPaths.forEach { append("update $it.${AdminStudentFields.GROUP_ID} = $destinationGroupId; ") }
            append(
                "${AdminGroupFields.groupPath(sourceGroupId)}.${AdminGroupFields.STUDENT_COUNT} " +
                    "$sourceCountDelta; ",
            )
            append(
                "${AdminGroupFields.groupPath(destinationGroupId.orEmpty())}." +
                    "${AdminGroupFields.STUDENT_COUNT} +$destinationCountDelta; ",
            )
        }
        schedulePaths.forEach { append("delete $it; ") }
        append("delete ${AdminGroupFields.groupPath(sourceGroupId)}")
    }
}

/** Builds the description from the same paths the Firestore batch writes. */
fun describeGroupDeletion(
    sourceGroupId: String,
    destinationGroupId: String?,
    studentIds: List<String>,
    scheduleSlotIds: List<String>,
): GroupDeletionDescription = GroupDeletionDescription(
    sourceGroupId = sourceGroupId,
    destinationGroupId = destinationGroupId,
    studentIds = studentIds,
    scheduleSlotIds = scheduleSlotIds,
)

/**
 * Dropdown options for the slot's "Docente del bloque" picker. Only teachers
 * assigned to THIS group (`groups/{id}.teacherIds`) are offered, with their
 * subjects in the label so the principal can tell them apart; the uid is kept
 * as the value written to the schedule document.
 */
fun teacherPickerOptions(
    users: List<AdminUser>,
    assignedTeacherIds: Set<String>,
): List<Pair<String, String>> =
    users.asSequence()
        .filter { it.role == UserRole.TEACHER && it.uid in assignedTeacherIds }
        .filter { it.uid.isNotBlank() }
        .map { user -> user.uid to teacherLabel(user) }
        .toList()

/** Label shared by the group picker and the stale-teacher fallback. */
private fun teacherLabel(user: AdminUser): String {
    val name = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }
        .ifBlank { user.email }
    val subjects = user.subjects.map { it.trim() }.filter { it.isNotBlank() }
    return if (subjects.isEmpty()) name else "$name — ${subjects.joinToString(", ")}"
}

/**
 * Full "Docente del bloque" options for one slot: "Sin asignar" first, then this
 * group's assigned teachers. A saved slot can still reference a teacher who was
 * later unassigned; keeping that teacher visible here means the saved value stays
 * editable (reassign or clear it) instead of silently disappearing.
 */
fun slotTeacherPickerOptions(
    slotTeacherId: String,
    assignedTeacherOptions: List<Pair<String, String>>,
    users: List<AdminUser>,
): List<Pair<String, String>> {
    val base = listOf("" to "Sin asignar") + assignedTeacherOptions
    if (slotTeacherId.isBlank() || base.any { it.first == slotTeacherId }) return base
    val label = users.firstOrNull { it.uid == slotTeacherId }?.let { teacherLabel(it) } ?: slotTeacherId
    return base + (slotTeacherId to label)
}

/**
 * Trimmed, distinct subjects of one teacher. Returns an empty list for an unknown
 * (or unassigned) teacher, which leaves the slot's free-text fallback available.
 */
fun subjectOptionsForTeacher(teacherId: String, users: List<AdminUser>): List<String> {
    if (teacherId.isBlank()) return emptyList()
    return users.firstOrNull { it.uid == teacherId }
        ?.subjects
        .orEmpty()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private val CLOCK_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * Validates every slot's HH:mm times and their order. Null when all slots are
 * valid; otherwise the first user-facing error to show in the detail banner.
 */
fun scheduleValidationError(slots: List<AdminScheduleSlot>): String? {
    for (slot in slots) {
        if (!CLOCK_REGEX.matches(slot.startTime) || !CLOCK_REGEX.matches(slot.endTime)) {
            return "El horario debe usar el formato HH:mm (por ejemplo, 08:00)."
        }
        if (slot.endTime <= slot.startTime) {
            return "La hora de fin debe ser posterior a la hora de inicio."
        }
    }
    return null
}

/**
 * School teachers of the detail screen, filtered by this group's assignment.
 * Students stay out on purpose: they are still a mock in this block.
 */
fun teacherRows(
    users: List<AdminUser>,
    assignedIds: List<String>,
    query: String,
): List<AdminTeacherRowUi> {
    val needle = query.trim()
    return users.asSequence()
        .filter { it.role == UserRole.TEACHER }
        .filter {
            needle.isEmpty() ||
                it.fullName.contains(needle, ignoreCase = true) ||
                it.email.contains(needle, ignoreCase = true)
        }
        .map { user ->
            AdminTeacherRowUi(
                uid = user.uid,
                name = user.fullName.ifBlank { "${user.firstName} ${user.lastName}".trim() }
                    .ifBlank { user.email },
                subtitle = user.subjects.joinToString(", ").ifBlank { "Docente" },
                assigned = user.uid in assignedIds,
            )
        }
        .toList()
}

/** User-facing copy for a typed repository failure. */
fun AdminGroupError.toMessage(): String = when (this) {
    AdminGroupError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    AdminGroupError.NETWORK -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AdminGroupError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
    AdminGroupError.DENIED -> "No tienes permiso para realizar esta acción."
    AdminGroupError.HAS_STUDENTS -> "El grupo todavía tiene alumnos."
    AdminGroupError.DESTINATION_REQUIRED -> "Elige el grupo de destino al que se moverán los alumnos."
    AdminGroupError.DESTINATION_SAME_AS_SOURCE ->
        "El grupo de destino debe ser distinto del grupo que se elimina."

    AdminGroupError.DESTINATION_NOT_FOUND -> "No se encontró el grupo de destino en esta escuela."
    AdminGroupError.UNKNOWN -> "No se pudo completar la operación."
}

/**
 * User-facing copy for a failure, appending the sanitized Firestore detail when the
 * data layer supplied one. Shown verbatim in the detail screen's error banner.
 */
fun AdminGroupResult.Failure.toMessage(): String {
    val base = reason.toMessage()
    val detail = detail?.takeIf { it.isNotBlank() } ?: return base
    return "$base (Firestore: $detail)"
}

/** Human label for a weekday stored as 1 (Monday) .. 7 (Sunday). */
fun weekdayLabel(weekday: Int): String = when (weekday) {
    1 -> "Lunes"
    2 -> "Martes"
    3 -> "Miércoles"
    4 -> "Jueves"
    5 -> "Viernes"
    6 -> "Sábado"
    7 -> "Domingo"
    else -> "Día $weekday"
}
