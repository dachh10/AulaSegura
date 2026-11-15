package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionRecord
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.util.AttendanceTimes

/**
 * What the history screen renders, already resolved.
 *
 * The screen gets this and nothing else: no repository, no Firestore, no Room.
 * Grouping by date is a pure function so the buckets can be tested without a
 * clock, which is the only way to test them at all.
 */
data class TeacherHistoryUi(
    val sections: List<HistorySectionUi> = emptyList(),
    val refreshing: Boolean = true,
    val notice: String? = null,
    val expandedId: String? = null,
    val detailLoading: Boolean = false,
    val totalSessions: Int = 0,
)

data class HistorySectionUi(
    val label: String,
    val sessions: List<HistorySessionUi>,
)

data class HistorySessionUi(
    val id: String,
    /** `"15"` — the big number of the date block. */
    val day: String,
    /** `"SÁB"` — the weekday under it. */
    val weekday: String,
    /** `"3° B · Matemáticas"`. */
    val title: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val justified: Int,
    val verified: Boolean?,
    val sync: RollCallSync,
    val expanded: Boolean,
    val records: List<HistoryRecordUi> = emptyList(),
    val recordsLoading: Boolean = false,
)

data class HistoryRecordUi(
    val number: String,
    val name: String,
    val mark: AttendanceMark?,
    val time: String?,
)

/**
 * Buckets the sessions by recency, newest first.
 *
 * [todayIso] is passed in instead of read from the clock so the grouping is
 * reproducible in a test. A session whose date cannot be parsed falls into
 * "ANTERIORES": showing it in the wrong bucket is better than hiding a saved
 * attendance because one document had a malformed date.
 */
fun historySections(
    sessions: List<AttendanceSessionSummary>,
    todayIso: String,
    expandedId: String? = null,
    detail: AttendanceSessionDetail? = null,
    detailLoading: Boolean = false,
): List<HistorySectionUi> {
    if (sessions.isEmpty()) return emptyList()

    val today = AttendanceTimes.isoDateToMillis(todayIso)
    val records = detail?.takeIf { it.summary.sessionId == expandedId }?.records.orEmpty()

    val buckets = linkedMapOf<String, MutableList<HistorySessionUi>>()
    sessions.forEach { session ->
        val label = bucketOf(session.date, today)
        val isOpen = session.sessionId == expandedId
        buckets.getOrPut(label) { mutableListOf() } += session.toUi(
            expanded = isOpen,
            // Only the open row gets the records: handing them to every session
            // would paint the same roster under each card.
            records = if (isOpen) records else emptyList(),
            recordsLoading = isOpen && detailLoading,
        )
    }

    // Fixed order, not insertion order: "HOY" must stay on top even when the
    // oldest session is the one that arrived last from the server.
    return SECTION_ORDER.mapNotNull { label ->
        buckets[label]?.let { HistorySectionUi(label = label, sessions = it) }
    }
}

private fun bucketOf(date: String, todayMillis: Long?): String {
    val sessionMillis = AttendanceTimes.isoDateToMillis(date)
    if (todayMillis == null || sessionMillis == null) return OLDER

    val days = ((todayMillis - sessionMillis) / MILLIS_PER_DAY).toInt()
    return when {
        days <= 0 -> TODAY
        days < 7 -> THIS_WEEK
        days < 14 -> LAST_WEEK
        else -> OLDER
    }
}

internal fun AttendanceSessionSummary.toUi(
    expanded: Boolean,
    records: List<AttendanceSessionRecord>,
    recordsLoading: Boolean,
): HistorySessionUi = HistorySessionUi(
    id = sessionId,
    day = date.substringAfterLast('-').padStart(2, '0'),
    weekday = AttendanceTimes.weekdayLabel(date),
    title = listOf(groupLabel, subject.ifBlank { null }).filterNotNull().joinToString(" · "),
    present = counts.present,
    absent = counts.absent,
    late = counts.late,
    justified = counts.justified,
    verified = verified,
    sync = sync,
    expanded = expanded,
    records = records.map { it.toUi() },
    recordsLoading = recordsLoading,
)

internal fun AttendanceSessionRecord.toUi(): HistoryRecordUi = HistoryRecordUi(
    number = listNumber.toString().padStart(2, '0'),
    name = fullName,
    mark = mark,
    time = time,
)

const val TODAY = "HOY"
const val THIS_WEEK = "ESTA SEMANA"
const val LAST_WEEK = "SEMANA ANTERIOR"
const val OLDER = "ANTERIORES"

private val SECTION_ORDER = listOf(TODAY, THIS_WEEK, LAST_WEEK, OLDER)
private const val MILLIS_PER_DAY = 86_400_000L
