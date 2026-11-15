package com.example.aulasegura.location

import com.example.aulasegura.domain.model.SchoolSite
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Why the teacher is, or is not, allowed to take attendance right now.
 *
 * The three "cannot verify" reasons are deliberately separate from [OUTSIDE]:
 * "the GPS does not know where you are" and "you are 6 km away" are different
 * facts, and only one of them deserves a red screen. Collapsing them would make
 * the app accuse a teacher of being outside school when it simply has no fix.
 */
enum class GeofenceReason {

    /** Inside the radius with an accuracy good enough to trust it. */
    INSIDE,

    /** The fix is farther than the school radius. */
    OUTSIDE,

    /**
     * Inside the radius, but the reported accuracy is worse than the radius
     * itself (or missing), so the fix cannot prove anything.
     */
    ACCURACY_TOO_LOW,

    /** Permission denied, GPS off, or no first fix yet. */
    NO_FIX,

    /** No cached school site: the distance cannot be computed. */
    NO_SCHOOL,
}

/**
 * Result of one geofence evaluation.
 *
 * [verified] is the only thing that reaches the session document, and it is true
 * only for [GeofenceReason.INSIDE]. Everything else — no permission, no fix, no
 * cached school, a 400 m accuracy bubble — saves the session as **unverified**,
 * which is honest and does not block the class.
 */
data class GeofenceResult(
    val reason: GeofenceReason,
    val distanceMeters: Double? = null,
    val radiusMeters: Double? = null,
    val accuracyMeters: Double? = null,
) {

    val verified: Boolean get() = reason == GeofenceReason.INSIDE

    /**
     * True when the fix is within the radius, even if it could not be trusted.
     *
     * The banner uses this to say "dentro de la escuela, ubicación sin
     * verificar" instead of "fuera", which would be false.
     */
    val withinRadius: Boolean
        get() = reason == GeofenceReason.INSIDE || reason == GeofenceReason.ACCURACY_TOO_LOW
}

/**
 * Great-circle distance in metres between two coordinates.
 *
 * Pure Kotlin on purpose: `Location.distanceBetween` is an Android call, and the
 * distance is the one number in this block that a unit test can pin without a
 * device. The earth radius is the IUGG mean radius, which is more than enough
 * for a 150 m fence.
 */
fun haversineMeters(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double,
): Double {
    val deltaLat = Math.toRadians(toLat - fromLat)
    val deltaLng = Math.toRadians(toLng - fromLng)
    val a = sin(deltaLat / 2).pow(2) +
        cos(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) * sin(deltaLng / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

/**
 * Decides whether [fix] is inside [site].
 *
 * The order of the checks is the rule:
 *  1. Without a school site there is nothing to measure against.
 *  2. Without a fix there is nothing to measure.
 *  3. Outside the radius is outside, whatever the accuracy says: a 10 m fix at
 *     6 km is still 6 km away.
 *  4. Inside the radius, an accuracy larger than the radius means the fix could
 *     be anywhere — including outside — so it cannot set `verified = true`.
 *     This is the threshold requirement of the phase, and the reason a fix with
 *     `accuracy = 100 m` is accepted against a `150 m` radius but a `200 m` one
 *     is not.
 */
fun evaluateGeofence(fix: LocationFix?, site: SchoolSite?): GeofenceResult {
    if (site == null) return GeofenceResult(GeofenceReason.NO_SCHOOL)
    if (fix == null) return GeofenceResult(GeofenceReason.NO_FIX, radiusMeters = site.radiusMeters)

    val distance = haversineMeters(
        fromLat = fix.latitude,
        fromLng = fix.longitude,
        toLat = site.latitude,
        toLng = site.longitude,
    )
    val accuracy = fix.accuracyMeters?.toDouble()

    if (distance > site.radiusMeters) {
        return GeofenceResult(
            reason = GeofenceReason.OUTSIDE,
            distanceMeters = distance,
            radiusMeters = site.radiusMeters,
            accuracyMeters = accuracy,
        )
    }

    if (accuracy == null || accuracy > site.radiusMeters) {
        return GeofenceResult(
            reason = GeofenceReason.ACCURACY_TOO_LOW,
            distanceMeters = distance,
            radiusMeters = site.radiusMeters,
            accuracyMeters = accuracy,
        )
    }

    return GeofenceResult(
        reason = GeofenceReason.INSIDE,
        distanceMeters = distance,
        radiusMeters = site.radiusMeters,
        accuracyMeters = accuracy,
    )
}

/** Mean earth radius (IUGG), in metres. */
private const val EARTH_RADIUS_METERS = 6_371_000.0
