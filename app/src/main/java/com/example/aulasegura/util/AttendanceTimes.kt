package com.example.aulasegura.util

import java.text.SimpleDateFormat
import java.util.Calendar
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
    private const val LONG_DATE = "EEEE d 'de' MMMM"
    private const val WEEKDAY = "EEE"
    private const val MONTH_LABEL = "MMMM yyyy"

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

    /** `"SÁB"` - the weekday printed in the history date block. */
    fun weekdayLabel(iso: String, zone: TimeZone = TimeZone.getDefault()): String =
        format(isoDateToMillis(iso, zone) ?: 0L, WEEKDAY, Locale("es", "MX"), zone)
            .replace(".", "")
            .uppercase(Locale("es", "MX"))

    /**
     * `"Viernes 31 de octubre"` - the absence the tutor is justifying.
     *
     * Long form on purpose: the justification is a document the principal reads
     * days later, so the day cannot depend on the reader remembering which week
     * "Vie 31" was.
     */
    fun longDateLabel(iso: String, zone: TimeZone = TimeZone.getDefault()): String {
        val millis = isoDateToMillis(iso, zone) ?: return iso
        val label = SimpleDateFormat(LONG_DATE, Locale("es", "MX"))
            .apply { timeZone = zone }
            .format(Date(millis))
        return label.replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }

    /**
     * `"Noviembre 2026"` - the header of the tutor's attendance month.
     *
     * [month] is 1-based; `Calendar` is 0-based, so the conversion happens here
     * and nowhere else.
     */
    fun monthLabel(year: Int, month: Int, zone: TimeZone = TimeZone.getDefault()): String {
        val calendar = Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, 1)
        }
        val label = SimpleDateFormat(MONTH_LABEL, Locale("es", "MX"))
            .apply { timeZone = zone }
            .format(calendar.time)
        return label.replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }

    /**
     * Parses an ISO date back to millis at local midnight, or `null` if it is not
     * a date.
     *
     * Same fixed pattern and locale as [isoDate], so the round trip is stable and
     * a day difference is exactly `86_400_000` ms.
     */
    fun isoDateToMillis(iso: String, zone: TimeZone = TimeZone.getDefault()): Long? =
        runCatching {
            SimpleDateFormat(ISO_DATE, Locale.US)
                .apply {
                    timeZone = zone
                    isLenient = false
                }
                .parse(iso)
                ?.time
        }.getOrNull()

    private fun format(
        epochMillis: Long,
        pattern: String,
        locale: Locale,
        zone: TimeZone,
    ): String = SimpleDateFormat(pattern, locale)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
}
