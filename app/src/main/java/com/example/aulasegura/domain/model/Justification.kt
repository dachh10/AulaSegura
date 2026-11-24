package com.example.aulasegura.domain.model

/**
 * What the tutor fills in when justifying an absence.
 *
 * [absenceDateMillis] is local midnight of the day the school marked absent, not
 * "now": the justification points at a real session, and a date invented at send
 * time would be a lie the principal cannot match to anything.
 *
 * [reasonCode] is the schema value (`enfermedad | cita | viaje | otro`); the
 * visible label belongs to the UI.
 */
data class JustificationDraft(
    val studentId: String,
    val groupId: String,
    val absenceDateMillis: Long,
    val reasonCode: String,
    val description: String,
    val attachment: JustificationAttachmentDraft? = null,
    /**
     * Session the absence belongs to. New reports carry it so the principal can
     * flip exactly one record to "justified"; legacy reports resolve it by
     * group and date instead.
     */
    val sessionId: String = "",
    /** Denormalised for the inbox; the reader has no roster open. */
    val studentName: String = "",
    val groupLabel: String = "",
)

/** The receipt the tutor picked, already in memory and ready to be stored. */
class JustificationAttachmentDraft(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String,
)
