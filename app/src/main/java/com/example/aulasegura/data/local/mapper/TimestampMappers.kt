package com.example.aulasegura.data.local.mapper

import com.google.firebase.Timestamp

/**
 * Timestamp conversion between the wire type and Room's `Long` columns.
 *
 * Room entities deliberately avoid `com.google.firebase.Timestamp` so the local
 * database can be built and tested without Firebase; the mappers are the only
 * place where the two representations meet.
 */

private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_MILLI = 1_000_000L

fun Timestamp.toEpochMillis(): Long =
    seconds * MILLIS_PER_SECOND + nanoseconds / NANOS_PER_MILLI

fun Long.toTimestamp(): Timestamp {
    val secondsPart = Math.floorDiv(this, MILLIS_PER_SECOND)
    val millisPart = Math.floorMod(this, MILLIS_PER_SECOND)
    return Timestamp(secondsPart, (millisPart * NANOS_PER_MILLI).toInt())
}

fun Timestamp?.toEpochMillisOrNull(): Long? = this?.toEpochMillis()

fun Long?.toTimestampOrNull(): Timestamp? = this?.toTimestamp()
