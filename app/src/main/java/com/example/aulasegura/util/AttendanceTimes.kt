package com.example.aulasegura.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The three date shapes the attendance flow needs, in one place.
 *
 * `java.time` is not used on purpose: `minSdk` is 24 and the module has no core
 * library desugaring, so `LocalDate`/`DateTimeFormatter` would crash on API 24
 * and 25. `SimpleDateFormat` is the boring option that works everywhere.
 *
 * The ISO pattern is fixed to `Locale.US` because `yyyy-MM-dd` must stay
 * lexicographically sortable in Room; a locale with a different numbering
 * system would silently break the `date = :date` query. The display label is
 * fixed to Spanish (`es-MX`) because the whole UI is Spanish regardless of the
 * device language.
 */
object AttendanceTimes {

    private const val ISO_DATE = "yyyy-MM-dd"
    private const val CLOCK_TIME = "HH:mm"
    private const val DAY_LABEL = "EEE d MMM"

    /** `"2026-11-14"` — the value stored in `attendance_sessions.date`. */
    fun isoDate(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
        format(epochMillis, ISO_DATE, Locale.US, zone)

    /** `"08:12"` — the arrival time stored in `records.time`. */
    fun clockTime(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
        format(epochMillis, CLOCK_TIME, Locale.US, zone)

    /** `"Vie 14 nov"` — the header of the roll call screen. */
    fun dayLabel(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String {
        val label = format(epochMillis, DAY_LABEL, Locale("es", "MX"), zone)
        return label.replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }

    private fun format(
        epochMillis: Long,
        pattern: String,
        locale: Locale,
        zone: TimeZone,
    ): String = SimpleDateFormat(pattern, locale)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
}
