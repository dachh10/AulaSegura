package com.example.aulasegura.domain.model

import kotlin.math.roundToInt

/**
 * Read-side models for the principal's attendance reports.
 *
 * Nothing here knows Firestore or collection names. A report is built from many
 * `attendanceSessions` documents, so every total is a sum of per-session tallies,
 * never a value copied from a single document. Percentages are derived, not stored.
 */

/**
 * Attendance percentage in `0..100`.
 *
 * `total <= 0` returns `0` instead of dividing by zero: a range with no marks is
 * "sin datos", not a perfect or a failed score.
 */
fun attendancePercent(present: Int, total: Int): Int =
    if (total <= 0) 0 else ((present.toFloat() * 100f) / total.toFloat())
        .roundToInt()
        .coerceIn(0, 100)

/**
 * Totals of every session inside a date range (also used for the single-day card).
 *
 * [empty] is true only when no session was found, so the UI can show an explicit
 * empty state instead of a 0% ring.
 */
data class AdminReportRangeSummary(
    val sessionCount: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val justifiedCount: Int = 0,
) {
    val totalMarks: Int
        get() = presentCount + absentCount + lateCount + justifiedCount

    val attendancePercentage: Int
        get() = attendancePercent(presentCount, totalMarks)

    val empty: Boolean get() = sessionCount == 0
}

/** One group's totals inside the same date range as [AdminReportRangeSummary]. */
data class AdminGroupBreakdown(
    val groupId: String = "",
    val groupLabel: String = "",
    val sessionCount: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val justifiedCount: Int = 0,
) {
    val totalMarks: Int
        get() = presentCount + absentCount + lateCount + justifiedCount

    val attendancePercentage: Int
        get() = attendancePercent(presentCount, totalMarks)
}

/** A calendar day with the absences recorded across all of that day's sessions. */
data class AdminTopAbsenceDay(
    val date: String = "",
    val sessionCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
)
