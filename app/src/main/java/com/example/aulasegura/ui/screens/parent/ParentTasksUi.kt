package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentTaskItem
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.Calendar
import java.util.TimeZone

/**
 * What the tutor's task list renders.
 *
 * The school produces `tasks/{id}` and nothing else about homework: there is no
 * submission record in the schema, so "Entregada" cannot be a real state and the
 * list never claims one. The sections and the due labels are computed here, which
 * is what keeps the screen free of date arithmetic.
 */
sealed interface ParentTasksUiState {

    data object Loading : ParentTasksUiState

    data class Loaded(
        val childName: String,
        val groupLabel: String,
        /** More than one child makes the pill tappable. */
        val childCount: Int,
        val filter: ParentTaskFilter,
        val sections: List<ParentTaskSectionUi>,
        val emptyMessage: String,
    ) : ParentTasksUiState

    data object NoChildren : ParentTasksUiState

    data class Failed(val message: String) : ParentTasksUiState
}

enum class ParentTaskFilter(val label: String) {
    Pending("Pendientes"),
    Delivered("Entregadas"),
    Overdue("Vencidas"),
    All("Todas"),
}

data class ParentTaskSectionUi(val label: String, val tasks: List<ParentTaskRowUi>)

data class ParentTaskRowUi(
    val taskId: String,
    val subject: String,
    val title: String,
    /** Empty when the document does not carry the publisher's name. */
    val teacher: String,
    val attachmentName: String?,
    val dueLabel: String,
    /** Today or tomorrow: the row paints its pill amber, like the Stitch design. */
    val dueSoon: Boolean,
    val overdue: Boolean,
)

/** Buckets by when the task is due, not by when it was published. */
internal enum class ParentTaskSection(val label: String) {
    Overdue("VENCIDAS"),
    Today("HOY"),
    Tomorrow("MAÑANA"),
    ThisWeek("ESTA SEMANA"),
    Later("MÁS ADELANTE"),
}

internal fun taskSectionOf(
    task: ParentTaskItem,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ParentTaskSection {
    val due = AttendanceTimes.isoDate(task.dueDateMillis, zone)
    return when {
        due < AttendanceTimes.isoDate(now, zone) -> ParentTaskSection.Overdue
        due == AttendanceTimes.isoDate(now, zone) -> ParentTaskSection.Today
        due == isoAfter(now, 1, zone) -> ParentTaskSection.Tomorrow
        due <= isoAfter(now, 6, zone) -> ParentTaskSection.ThisWeek
        else -> ParentTaskSection.Later
    }
}

internal fun isTaskOverdue(
    task: ParentTaskItem,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): Boolean = taskSectionOf(task, now, zone) == ParentTaskSection.Overdue

/**
 * The slice each chip shows.
 *
 * `Delivered` is empty on purpose: `students` has no submission and `tasks` has no
 * per-student state, so a delivered task would be an invented one.
 */
internal fun tasksFor(
    filter: ParentTaskFilter,
    tasks: List<ParentTaskItem>,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): List<ParentTaskItem> = when (filter) {
    ParentTaskFilter.Pending -> tasks.filterNot { isTaskOverdue(it, now, zone) }
    ParentTaskFilter.Overdue -> tasks.filter { isTaskOverdue(it, now, zone) }
    ParentTaskFilter.Delivered -> emptyList()
    ParentTaskFilter.All -> tasks
}

internal fun taskSections(
    filter: ParentTaskFilter,
    tasks: List<ParentTaskItem>,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): List<ParentTaskSectionUi> = tasksFor(filter, tasks, now, zone)
    .groupBy { taskSectionOf(it, now, zone) }
    .toSortedMap(compareBy { it.ordinal })
    .map { (section, rows) ->
        ParentTaskSectionUi(
            label = section.label,
            tasks = rows.sortedBy { it.dueDateMillis }.map { it.toRowUi(now, zone) },
        )
    }

internal fun ParentTaskItem.toRowUi(
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ParentTaskRowUi {
    val dueIso = AttendanceTimes.isoDate(dueDateMillis, zone)
    val section = taskSectionOf(this, now, zone)

    val label = when (section) {
        ParentTaskSection.Today -> "Hoy"
        ParentTaskSection.Tomorrow -> "Mañana"
        else -> AttendanceTimes.dayLabel(dueDateMillis, zone)
    }

    return ParentTaskRowUi(
        taskId = taskId,
        subject = subject,
        title = title,
        teacher = createdByName,
        attachmentName = attachments.firstOrNull()?.name?.takeIf { it.isNotBlank() },
        dueLabel = label,
        dueSoon = section == ParentTaskSection.Today || section == ParentTaskSection.Tomorrow,
        overdue = dueIso < AttendanceTimes.isoDate(now, zone),
    )
}

internal fun tasksEmptyMessage(filter: ParentTaskFilter): String = when (filter) {
    ParentTaskFilter.Delivered -> "La escuela todavía no registra entregas."
    ParentTaskFilter.Overdue -> "No hay tareas vencidas."
    ParentTaskFilter.Pending -> "Sin tareas pendientes."
    ParentTaskFilter.All -> "Sin tareas publicadas para el grupo."
}

internal fun tasksFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver las tareas."
    RosterFailure.UNKNOWN -> "No se pudieron cargar las tareas."
}

/** `today + days` as an ISO day, through `Calendar` so a DST change cannot skip one. */
internal fun isoAfter(now: Long, days: Int, zone: TimeZone = TimeZone.getDefault()): String {
    val calendar = Calendar.getInstance(zone).apply {
        timeInMillis = now
        add(Calendar.DAY_OF_MONTH, days)
    }
    return AttendanceTimes.isoDate(calendar.timeInMillis, zone)
}
