package com.example.aulasegura.ui.screens.justifications

import com.example.aulasegura.domain.model.JustificationRecord
import com.example.aulasegura.domain.model.JustificationStatus
import com.example.aulasegura.domain.repository.JustificationReviewFailure
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone

/**
 * The card both inboxes render: the teacher reads it, the principal decides on
 * it. One model keeps the same report looking the same in the two screens.
 */
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
    /** Subject whose absence this report excuses; blank on legacy documents. */
    val subject: String = "",
)

/**
 * Pending first, then the ones the school already resolved. Absence date
 * descending in both: the newest report is the one a reader opens for.
 */
data class JustificationSections(
    val pending: List<JustificationRecord>,
    val reviewed: List<JustificationRecord>,
)

fun justificationSections(records: List<JustificationRecord>): JustificationSections {
    val byDate = compareByDescending<JustificationRecord> { it.dateFromMillis }
        .thenByDescending { it.createdAtMillis }
    return JustificationSections(
        pending = records.filter { it.status == JustificationStatus.PENDING }.sortedWith(byDate),
        reviewed = records.filter { it.status != JustificationStatus.PENDING }.sortedWith(byDate),
    )
}

fun JustificationRecord.toCardUi(
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
        subject = subject,
    )
}

fun justificationReasonLabel(reason: String): String = when (reason) {
    "enfermedad" -> "Enfermedad"
    "cita" -> "Cita médica"
    "viaje" -> "Viaje"
    "otro" -> "Otro"
    else -> reason.ifBlank { "Sin motivo" }
}

fun justificationStatusLabel(status: JustificationStatus): String = when (status) {
    JustificationStatus.PENDING -> "Pendiente"
    JustificationStatus.APPROVED -> "Aprobada"
    JustificationStatus.REJECTED -> "Rechazada"
}

fun justificationFailureMessage(reason: RosterFailure): String = when (reason) {
    RosterFailure.OFFLINE -> "Sin conexión. Revisa tu red e intenta de nuevo."
    RosterFailure.DENIED -> "No tienes permiso para ver estas justificaciones."
    RosterFailure.NO_GROUP -> "Esta cuenta todavía no tiene un grupo asignado."
    RosterFailure.UNKNOWN -> "No se pudieron cargar las justificaciones."
}

/** What a failed decision means to the principal: retry, reload, or give up. */
fun justificationReviewFailureMessage(reason: JustificationReviewFailure): String = when (reason) {
    JustificationReviewFailure.OFFLINE ->
        "Sin conexión. La decisión no se guardó; inténtalo de nuevo."

    JustificationReviewFailure.DENIED ->
        "La escuela no pudo guardar la decisión. Revisa los permisos de tu cuenta."

    JustificationReviewFailure.NOT_FOUND ->
        "La justificación ya no existe. Actualiza la lista."

    JustificationReviewFailure.ABSENCE_NOT_FOUND ->
        "No se pudo justificar: no se encontró esa falta en la asistencia."

    JustificationReviewFailure.UNKNOWN ->
        "No se pudo guardar la decisión. Inténtalo de nuevo."
}
