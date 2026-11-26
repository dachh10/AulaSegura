package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.ParentAttachment
import com.example.aulasegura.domain.model.ParentTaskItem
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone

/**
 * What the task detail renders.
 *
 * Every string comes from the document: the published date from `createdAt`, the
 * deadline from `dueDate`, the author from `createdByName`. The attachments carry
 * no size in the schema, so the row prints the file type and nothing it cannot
 * know.
 */
sealed interface ParentTaskDetailUiState {

    data object Loading : ParentTaskDetailUiState

    data class Loaded(
        val subject: String,
        val groupLabel: String,
        val title: String,
        val teacherName: String,
        val teacherInitials: String,
        /** `null` when the document has no publish timestamp. */
        val publishedLabel: String?,
        /** `null` when the document has no due date. */
        val dueLabel: String?,
        val dueSoon: Boolean,
        val overdue: Boolean,
        val description: String,
        val attachments: List<ParentAttachmentUi>,
    ) : ParentTaskDetailUiState

    /** The document is gone, or was never there: the screen says so. */
    data object NotFound : ParentTaskDetailUiState

    data class Failed(val message: String) : ParentTaskDetailUiState
}

data class ParentAttachmentUi(
    val name: String,
    val meta: String,
    val url: String,
    val isPdf: Boolean,
)

internal fun ParentTaskItem.toDetailUi(
    groupLabel: String,
    now: Long,
    zone: TimeZone = TimeZone.getDefault(),
): ParentTaskDetailUiState.Loaded {
    val section = taskSectionOf(this, now, zone)

    return ParentTaskDetailUiState.Loaded(
        subject = subject,
        groupLabel = groupLabel,
        title = title,
        teacherName = createdByName,
        teacherInitials = initialsOf(createdByName),
        publishedLabel = publishedLabel(createdAtMillis, zone),
        dueLabel = dueLabel(dueDateMillis, zone),
        dueSoon = section == ParentTaskSection.Today || section == ParentTaskSection.Tomorrow,
        overdue = section == ParentTaskSection.Overdue,
        description = description,
        attachments = attachments.map { it.toUi() },
    )
}

internal fun ParentAttachment.toUi(): ParentAttachmentUi = ParentAttachmentUi(
    name = name.ifBlank { "Adjunto" },
    meta = attachmentMeta(type),
    url = url,
    isPdf = isPdf,
)

/** The MIME type is all the document knows; no byte size is invented. */
internal fun attachmentMeta(type: String): String = when {
    type.contains("pdf", ignoreCase = true) -> "Documento PDF"
    type.startsWith("image/", ignoreCase = true) -> "Imagen"
    type.isBlank() -> "Archivo"
    else -> type
}

internal fun dueLabel(millis: Long, zone: TimeZone = TimeZone.getDefault()): String? {
    if (millis <= 0) return null
    val day = AttendanceTimes.longDateLabel(AttendanceTimes.isoDate(millis, zone), zone)
    return "$day · ${AttendanceTimes.clockTime(millis, zone)}"
}

internal fun publishedLabel(millis: Long, zone: TimeZone = TimeZone.getDefault()): String? {
    if (millis <= 0) return null
    val day = AttendanceTimes.dayLabel(millis, zone).replaceFirstChar { it.lowercase() }
    return "Publicada: $day, ${AttendanceTimes.clockTime(millis, zone)}"
}

internal fun taskDetailFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver esta tarea."
    RosterFailure.NO_GROUP -> "Tu hijo aún no tiene grupo asignado."
    RosterFailure.UNKNOWN -> "No se pudo cargar la tarea."
}
