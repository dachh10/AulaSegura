package com.example.aulasegura.domain.model

/**
 * One closed roll call, as the history list shows it.
 *
 * [groupLabel] and [date] are display-ready because the list is grouped by day;
 * the counters come from the session document, which the sync handler recomputes
 * from the records (`AttendanceSyncMapper.withCountsFrom`), so they are the
 * numbers the tutor is going to see too.
 *
 * [sync] is `SYNCED` for every session that came from the server and keeps its
 * real phase for the ones still sitting in the outbox: a session marked
 * "sincronizado" before Firestore accepted it is exactly the lie this field
 * exists to prevent.
 */
data class AttendanceSessionSummary(
    val sessionId: String,
    val groupId: String,
    val groupLabel: String,
    val subject: String,
    /** ISO date, `"2026-11-15"`. */
    val date: String,
    val startedAt: Long,
    val counts: AttendanceCounts,
    /** `null` when the session was saved without a GPS fix. */
    val verified: Boolean?,
    val sync: RollCallSync,
)

/** One student inside a saved session. */
data class AttendanceSessionRecord(
    val studentId: String,
    val listNumber: Int,
    val fullName: String,
    val mark: AttendanceMark?,
    /** Arrival time, only for `present` and `late`. */
    val time: String?,
)

/** A session plus its records, for the expanded row of the history. */
data class AttendanceSessionDetail(
    val summary: AttendanceSessionSummary,
    val records: List<AttendanceSessionRecord>,
)
