package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone

/** What the teacher's justification inbox renders. */
sealed interface TeacherJustificationsUiState {

    data object Loading : TeacherJustificationsUiState

    data class Loaded(
        val pending: List<JustificationCardUi>,
        val reviewed: List<JustificationCardUi>,
    ) : TeacherJustificationsUiState {

        val isEmpty: Boolean get() = pending.isEmpty() && reviewed.isEmpty()
    }

    data class Failed(val message: String) : TeacherJustificationsUiState
}

/** One report, already resolved for the card. */
data class JustificationCardUi(
    val id: String,
    val studentName: String,
    val groupLabel: String,
    val dateLabel: String,
    val reasonLabel: String,
    val description: String,
    val evidenceUrl: String?,
    val evidenceType: String?,
    val status: JustificationStatus,
    val statusLabel: String,
    val reviewNote: String?,
)

/**
 * Pending first, then the ones the school already resolved. Absence date
 * descending in both: the newest report is the one a teacher opens for.
 */
internal data class JustificationSections(
    val pending: List<JustificationRecord>,
    val reviewed: List<JustificationRecord>,
)

internal fun justificationSections(records: List<JustificationRecord>): JustificationSections {
    val byDate = compareByDescending<JustificationRecord> { it.dateFromMillis }
        .thenByDescending { it.createdAtMillis }
    return JustificationSections(
        pending = records.filter { it.status == JustificationStatus.PENDING }.sortedWith(byDate),
        reviewed = records.filter { it.status != JustificationStatus.PENDING }.sortedWith(byDate),
    )
}

internal fun JustificationRecord.toCardUi(
    zone: TimeZone = TimeZone.getDefault(),
): JustificationCardUi {
    val isoDate = AttendanceTimes.isoDate(dateFromMillis, zone)
    return JustificationCardUi(
        id = justificationId,
        studentName = studentName.ifBlank { "Alumno" },
        groupLabel = groupLabel.ifBlank { groupId },
        dateLabel = AttendanceTimes.longDateLabel(isoDate, zone),
        reasonLabel = justificationReasonLabel(reason),
        description = description,
        evidenceUrl = evidence?.url,
        evidenceType = evidence?.type,
        status = status,
        statusLabel = justificationStatusLabel(status),
        reviewNote = reviewNote?.takeIf { it.isNotBlank() },
    )
}

internal fun justificationReasonLabel(reason: String): String = when (reason) {
    "enfermedad" -> "Enfermedad"
    "cita" -> "Cita médica"
    "viaje" -> "Viaje"
    "otro" -> "Otro"
    else -> reason.ifBlank { "Sin motivo" }
}

internal fun justificationStatusLabel(status: JustificationStatus): String = when (status) {
    JustificationStatus.PENDING -> "Pendiente"
    JustificationStatus.APPROVED -> "Aprobada"
    JustificationStatus.REJECTED -> "Rechazada"
}

internal fun justificationFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver estas justificaciones."
    RosterFailure.UNKNOWN -> "No se pudieron cargar las justificaciones."
}
