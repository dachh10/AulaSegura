package com.example.aulasegura.ui.notifications

import com.example.aulasegura.domain.model.AppNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Splits a newest-first tray into today's rows and older ones, keeping the
 * repository order inside each bucket.
 *
 * The split lives outside the screens because every role's tray needs the same
 * answer. A row filed under the wrong day looks fine on a screenshot and is
 * obvious in a test, so it is resolved once and tested once.
 */
fun splitByToday(
    items: List<AppNotification>,
    now: Long,
    zone: TimeZone,
): Pair<List<AppNotification>, List<AppNotification>> {
    if (items.isEmpty()) return emptyList<AppNotification>() to emptyList()

    val dayFormat = SimpleDateFormat(DAY_PATTERN, Locale.forLanguageTag("es-MX"))
        .apply { timeZone = zone }
    val today = dayFormat.format(Date(now))
    return items.partition { dayFormat.format(Date(it.createdAt)) == today }
}

private const val DAY_PATTERN = "yyyyMMdd"
