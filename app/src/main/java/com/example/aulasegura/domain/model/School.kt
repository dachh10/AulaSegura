package com.example.aulasegura.domain.model

/**
 * The geofence a teacher has to be inside to take attendance.
 *
 * [latitude] and [longitude] mirror `schools/{schoolId}.location` and
 * [radiusMeters] mirrors `schools.radiusMeters`. They are resolved together on
 * purpose: a centre without its radius, or the other way round, cannot answer
 * "is the teacher in school?", and splitting them invites a screen that reads
 * one and invents the other.
 *
 * [radiusMeters] is never `0` or negative: the cache falls back to the
 * Remote Config default when the document carries an out of range value, so a
 * misconfigured school does not lock everybody out of the classroom.
 *
 * [requireLocation] mirrors `schools.requireLocation`. When the directora turns
 * it off, the teacher may pass roll call without a GPS proof, but the session
 * still stores whatever reading is available as unverified. It defaults to
 * `true` so an old cached document keeps today's fail-closed behaviour.
 */
data class SchoolSite(
    val schoolId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val requireLocation: Boolean = true,
)
