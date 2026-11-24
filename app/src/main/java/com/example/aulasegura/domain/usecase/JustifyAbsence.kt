package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.JustificationDecision

/**
 * The attendance write that follows a principal approving a family's report.
 *
 * [counts] is the tally of the whole session after the flip. It is recomputed
 * from every record, never adjusted by hand: the denormalised `counts` document
 * has to agree with the records the tutor and the teacher read.
 */
data class JustificationAttendanceEffect(
    val sessionId: String,
    val studentId: String,
    val status: String,
    val counts: AttendanceCounts,
)

/**
 * Decides whether a review has to change an attendance record.
 *
 * Only an APPROVE of an `absent` record has an effect:
 *  - a REJECT leaves the absence as the teacher marked it;
 *  - an already `justified` record needs no second write, so approving twice is
 *    harmless;
 *  - a `present` or `late` record is never downgraded: the teacher's later mark
 *    wins over a report that described an older state;
 *  - an unknown status is not an absence either.
 *
 * A student with no record at all is not decided here. The caller owns that read
 * and must tell the principal the absence could not be found instead of filing
 * an approval that changes nothing.
 */
fun justifyAbsence(
    decision: JustificationDecision,
    sessionId: String,
    studentId: String,
    statusesByStudent: Map<String, String>,
): JustificationAttendanceEffect? {
    if (decision != JustificationDecision.APPROVE) return null
    if (sessionId.isBlank() || studentId.isBlank()) return null
    if (statusesByStudent[studentId] != AttendanceMark.ABSENT.wire) return null

    val after = statusesByStudent.toMutableMap().apply {
        put(studentId, AttendanceMark.JUSTIFIED.wire)
    }
    return JustificationAttendanceEffect(
        sessionId = sessionId,
        studentId = studentId,
        status = AttendanceMark.JUSTIFIED.wire,
        counts = AttendanceCounts.of(after.values.mapNotNull(AttendanceMark::fromWire)),
    )
}
