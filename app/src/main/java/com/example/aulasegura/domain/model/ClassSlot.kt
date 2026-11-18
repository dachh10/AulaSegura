package com.example.aulasegura.domain.model

/**
 * One slot of `groups/{groupId}/schedule/{slotId}`.
 *
 * [weekday] follows the document, not `java.util.Calendar`: **1 = lunes … 7 =
 * domingo** (`CONTEXT.md` §`groups`). The conversion belongs to the caller, which
 * is the only place that also has a clock and a time zone.
 *
 * Times are the strings the document stores (`"08:00"`), and the minute-of-day
 * getters are the only arithmetic the home screen needs.
 */
data class ClassSlot(
    val slotId: String,
    val groupId: String,
    val weekday: Int,
    val startTime: String,
    val endTime: String,
    val subject: String,
    val teacherId: String?,
) {

    /** `08:00` → 480. `-1` when the document has no readable time. */
    val startMinutes: Int get() = minutesOf(startTime) ?: -1

    /** `08:45` → 525. `-1` when the document has no readable time. */
    val endMinutes: Int get() = minutesOf(endTime) ?: -1
}

/** `"08:05"` → 485; `null` when it is not a 24-hour time. */
fun minutesOf(time: String?): Int? {
    val parts = time?.trim()?.split(":") ?: return null
    if (parts.size != 2) return null

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null

    return hour * 60 + minute
}
