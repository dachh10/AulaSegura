package com.example.aulasegura.ui.chat

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Formatting shared by both sides of the chat.
 *
 * The teacher and the family see the same rows with different names on them, so
 * the clock label and the avatar initials live here instead of being copied into
 * two screens. Everything is pure and takes `now` and `zone` as arguments: a
 * relative label ("Ayer") is exactly what breaks silently when the clock moves,
 * and that is what the tests pin.
 */

/**
 * `"Carlos Gómez Pérez"` → `"CG"`, `"Tutor de Mariana Gómez"` → `"MG"`.
 *
 * The `Tutor de` prefix is dropped first: without that, every family avatar would
 * read "TD" and no teacher would be able to tell two chats apart at a glance.
 * Lowercase connector words ("de", "del", "y") are skipped for the same reason.
 */
fun chatInitials(name: String): String {
    val trimmed = name.trim()
    val withoutRole = listOf("Tutor de ", "Tutora de ", "Tutor ", "Tutora ", "Docente de ", "Docente ")
        .firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
        ?.let { trimmed.drop(it.length) }
        ?.trim()
        .orEmpty()
    val subject = withoutRole.ifBlank { trimmed }

    val words = subject.split(' ')
        .filter { it.isNotBlank() }
        .filter { word -> word.firstOrNull()?.isUpperCase() == true }
        .take(2)

    return words.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }
}

/**
 * The row time: the hour for today, "Ayer" for yesterday, the weekday inside the
 * last week, and the date for anything older — the convention every messenger
 * uses and the only one a family reads without thinking.
 */
fun chatTimeLabel(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (epochMillis <= 0L) return ""

    val dayFormat = SimpleDateFormat("yyyyMMdd", SPANISH_MX).apply { timeZone = zone }
    val today = dayFormat.format(Date(now))
    val thatDay = dayFormat.format(Date(epochMillis))
    if (today == thatDay) return clockLabel(epochMillis, zone)

    return chatDayLabel(epochMillis, now, zone)
}

/**
 * The chip that separates one day from the next inside a conversation: "Hoy",
 * "Ayer", the weekday inside the last week, or the date.
 *
 * Same scale as [chatTimeLabel] minus the hour, so a bubble says "10:41" and the
 * chip above it says which day that hour belongs to.
 */
fun chatDayLabel(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): String {
    if (epochMillis <= 0L) return ""

    val dayFormat = SimpleDateFormat("yyyyMMdd", SPANISH_MX).apply { timeZone = zone }
    val today = dayFormat.format(Date(now))
    val thatDay = dayFormat.format(Date(epochMillis))
    if (today == thatDay) return "Hoy"

    val yesterday = dayFormat.format(Date(now - ONE_DAY_MILLIS))
    if (thatDay == yesterday) return "Ayer"

    val daysApart = daysBetween(epochMillis, now, zone)
    if (daysApart in 2..6) {
        return SimpleDateFormat("EEE", SPANISH_MX)
            .apply { timeZone = zone }
            .format(Date(epochMillis))
            .replace(".", "")
            .replaceFirstChar { it.uppercase(SPANISH_MX) }
    }

    return SimpleDateFormat("d MMM", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))
        .replace(".", "")
}

/** `"10:41"`, the hour inside a bubble. */
fun clockLabel(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("HH:mm", SPANISH_MX)
        .apply { timeZone = zone }
        .format(Date(epochMillis))

private fun daysBetween(from: Long, to: Long, zone: TimeZone): Int {
    val calendar: Calendar = Calendar.getInstance(zone).apply { timeInMillis = from }
    val start = Calendar.getInstance(zone).apply {
        timeInMillis = calendar.timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val end = Calendar.getInstance(zone).apply {
        timeInMillis = to
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return ((end.timeInMillis - start.timeInMillis) / ONE_DAY_MILLIS).toInt()
}

private const val ONE_DAY_MILLIS = 86_400_000L
private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")
