package com.example.aulasegura.domain.model

/**
 * The outcome the school gives a justification.
 *
 * [wire] is the exact value stored in `justifications/{id}.status`. Only the
 * principal writes it (`firestore.rules`); the tutor creates the document as
 * `pendiente` and never updates it.
 */
enum class JustificationStatus(val wire: String) {

    PENDING("pendiente"),
    APPROVED("aprobada"),
    REJECTED("rechazada"),
    ;

    companion object {

        /**
         * `null`/unknown values are read as pending: an old document the app does
         * not understand must stay visible and actionable instead of disappearing
         * from the inbox.
         */
        fun fromWire(value: String?): JustificationStatus = when (value) {
            APPROVED.wire, "aceptada" -> APPROVED
            REJECTED.wire -> REJECTED
            else -> PENDING
        }
    }
}

/** What the principal decided about a report. */
enum class JustificationDecision { APPROVE, REJECT }

/**
 * One decision, as the principal makes it.
 *
 * [note] is optional; a blank note clears any previous one, which is what the
 * dialog shows when it opens empty.
 */
data class JustificationReview(
    val justificationId: String,
    val decision: JustificationDecision,
    val note: String = "",
)

/** The receipt of a justification, already uploaded to Cloudinary. */
data class JustificationEvidence(
    val publicId: String?,
    val url: String,
    /** `image` | `pdf`; drives how the reader opens it. */
    val type: String,
)

/**
 * One justification as the teacher and the principal read it.
 *
 * The display fields are denormalised in the document ([studentName],
 * [groupLabel]) because neither inbox has a roster open. Documents written
 * before those fields existed leave them empty and the inbox falls back to the
 * raw ids.
 */
data class JustificationRecord(
    val justificationId: String,
    val studentId: String,
    val studentName: String,
    val groupId: String,
    val groupLabel: String,
    /** Local midnight of the absence the school marked. */
    val dateFromMillis: Long,
    val reason: String,
    val description: String,
    val status: JustificationStatus,
    val evidence: JustificationEvidence?,
    val createdAtMillis: Long,
    val reviewedAtMillis: Long?,
    val reviewNote: String?,
    /** Empty on documents written before the field existed. */
    val sessionId: String,
)
