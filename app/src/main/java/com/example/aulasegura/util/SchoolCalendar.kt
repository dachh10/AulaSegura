package com.example.aulasegura.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Which days the school takes attendance.
 *
 * Monday to Friday, decided by the owner of the project: Saturday and Sunday are
 * not school days, so the roll call does not open. Holidays are **not** modelled
 * yet — there is no holiday collection and inventing one would need the
 * `directivo` screens that do not exist. When that collection arrives, this is the
 * only place that has to learn about it.
 *
 * `java.util.Calendar` and not `java.time`: `minSdk` is 24 without desugaring.
 */
object SchoolCalendar {

    /** `false` on Saturday and Sunday, in [zone]'s local time. */
    fun isClassDay(epochMillis: Long, zone: TimeZone = TimeZone.getDefault()): Boolean {
        val day = Calendar.getInstance(zone)
            .apply { timeInMillis = epochMillis }
            .get(Calendar.DAY_OF_WEEK)

        return day != Calendar.SATURDAY && day != Calendar.SUNDAY
    }
}
