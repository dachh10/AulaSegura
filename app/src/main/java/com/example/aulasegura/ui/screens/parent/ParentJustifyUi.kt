package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentDayAttendance
import com.example.aulasegura.util.AttendanceTimes
import java.util.TimeZone

/** What the tutor's justification form renders. */
sealed interface ParentJustifyUiState {

    data object Loading : ParentJustifyUiState

    data class Loaded(
        val childName: String,
        val groupLabel: String,
        /**
         * The absence the form will justify, or `null` when there is nothing to
         * justify. Never a placeholder date: a justification that does not match
         * a session is a document the school cannot act on.
         */
        val absence: JustifyAbsenceUi?,
        val reason: JustificationReason,
        val details: String,
        val receipt: JustifyReceiptUi?,
        val sending: Boolean,
        /** Set once the report is on disk; the screen then leaves the form. */
        val sent: Boolean,
        /** A real failure to explain, never a decorative warning. */
        val error: String?,
        /** Why there is nothing to send, when [absence] is `null`. */
        val emptyMessage: String?,
        /** Every absence offered by the form; more than one when subjects coincide. */
        val absences: List<JustifyAbsenceUi> = emptyList(),
        /** Sessions selected by the tutor, or the single default when untouched. */
        val selectedSessionIds: Set<String> = emptySet(),
        /** The "Usar este justificante para todas las materias" switch. */
        val applyToAll: Boolean = false,
    ) : ParentJustifyUiState {

        /** One report is written per selected absence, addressed to its teacher. */
        val selectedAbsences: List<JustifyAbsenceUi>
            get() = when {
                applyToAll -> absences
                absences.isNotEmpty() -> absences.filter { it.key in selectedSessionIds }
                absence != null -> listOf(absence)
                else -> emptyList()
            }

        /** "Asunto familiar" is the only reason that needs no photo. */
        val needsPhoto: Boolean get() = reason != JustificationReason.FAMILY

        val canSend: Boolean
            get() = selectedAbsences.isNotEmpty() && !sending && !sent &&
                (!needsPhoto || receipt != null)
    }

    data object NoChildren : ParentJustifyUiState

    data class Failed(val message: String) : ParentJustifyUiState
}

/** The absence card: which day, for which child. */
data class JustifyAbsenceUi(
    val isoDate: String,
    /** "Viernes 31 de octubre". */
    val dateLabel: String,
    /** Session the record belongs to; empty when the school stored none. */
    val sessionId: String = "",
    /** Subject whose roll call produced the absence. */
    val subject: String = "",
    val subjectId: String = "",
    /** Teacher who marked the absence and receives this report. */
    val teacherId: String = "",
) {
    /** Legacy sessions may have no id; date plus subject still identifies the row. */
    val key: String get() = sessionId.ifBlank { "$isoDate|$subject" }
}

/**
 * The reason chips of the design, mapped to the four values the schema accepts
 * (`CONTEXT.md` §6). "Asunto familiar" is stored as `otro` on purpose: adding a
 * fifth category would change the document contract for a label.
 */
enum class JustificationReason(val code: String, val label: String) {
    ILLNESS("enfermedad", "Enfermedad"),
    APPOINTMENT("cita", "Cita médica"),
    FAMILY("otro", "Asunto familiar"),
    OTHER("otro", "Otro"),
}

/**
 * The receipt the tutor picked, still in memory.
 *
 * A plain class and not a data class: the bytes are the identity here, and
 * comparing two megabytes to answer "did the state change?" would make every
 * recomposition expensive.
 */
class JustifyReceiptUi(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {

    val isImage: Boolean get() = mimeType.startsWith(IMAGE_PREFIX)

    private companion object {
        const val IMAGE_PREFIX = "image/"
    }
}

/**
 * The absence the form points at: the touched record, or the newest one the
 * school already registered.
 *
 * [days] comes newest first from `ParentRepository.loadMonth`, and an absence
 * already justified is stored as `JUSTIFIED`, so filtering `ABSENT` is what
 * keeps the form from pointing at a day the principal already resolved.
 */
internal fun newestAbsence(days: List<ParentDayAttendance>): ParentDayAttendance? =
    days.firstOrNull { it.mark == AttendanceMark.ABSENT }

internal fun ParentDayAttendance.toAbsenceUi(
    zone: TimeZone = TimeZone.getDefault(),
): JustifyAbsenceUi = JustifyAbsenceUi(
    isoDate = date,
    dateLabel = AttendanceTimes.longDateLabel(date, zone),
    sessionId = sessionId,
    subject = subject,
    subjectId = subjectId,
    teacherId = teacherId,
)
