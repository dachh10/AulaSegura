package com.example.aulasegura.data.local.mapper

import com.example.aulasegura.data.local.entity.SchoolEntity
import com.example.aulasegura.domain.model.SchoolSite
import com.google.firebase.Timestamp

/**
 * `schools/{schoolId}` to Room and to the domain.
 *
 * Takes a plain map, like the roster mappers, so the two places where the
 * geofence can silently break — `location` arriving as a nested map with
 * `Double` values, and `radiusMeters` arriving as a `Long` — are unit-testable
 * without Firebase.
 */

/** Radius accepted from the document; outside this range the default is used. */
private const val MIN_RADIUS_METERS = 20.0
private const val MAX_RADIUS_METERS = 5_000.0

fun schoolEntityFrom(id: String, data: Map<String, Any?>): SchoolEntity? {
    val name = data["name"] as? String ?: return null
    val location = data["location"] as? Map<*, *>
    val schedule = data["schedule"] as? Map<*, *>

    return SchoolEntity(
        schoolId = id,
        name = name,
        latitude = (location?.get("lat") as? Number)?.toDouble(),
        longitude = (location?.get("lng") as? Number)?.toDouble(),
        radiusMeters = (data["radiusMeters"] as? Number)?.toDouble(),
        updatedAt = (data["updatedAt"] as? Timestamp)?.toEpochMillis() ?: 0L,
        // A document written before the flag existed must keep requiring GPS.
        requireLocation = data["requireLocation"] as? Boolean ?: true,
        entryTime = schedule?.get("entryTime") as? String,
        exitTime = schedule?.get("exitTime") as? String,
    )
}

/**
 * The site the geofence can actually use, or `null` when it cannot.
 *
 * [fallbackRadiusMeters] is the validated Remote Config default. It is used when
 * the document has no radius or one outside `20..5000`: a `0` radius would make
 * every teacher "outside", which is a lockout dressed as data.
 */
fun SchoolEntity.toSchoolSite(fallbackRadiusMeters: Double): SchoolSite? {
    val latitude = latitude ?: return null
    val longitude = longitude ?: return null

    return SchoolSite(
        schoolId = schoolId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters.orDefault(fallbackRadiusMeters),
        requireLocation = requireLocation,
        entryTime = entryTime,
        exitTime = exitTime,
    )
}

private fun Double?.orDefault(fallback: Double): Double =
    if (this != null && this in MIN_RADIUS_METERS..MAX_RADIUS_METERS) this else fallback
