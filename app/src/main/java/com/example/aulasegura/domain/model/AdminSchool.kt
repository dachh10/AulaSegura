package com.example.aulasegura.domain.model

/**
 * The principal's view of `schools/{schoolId}`.
 *
 * The identity fields ([name], [key], [address], [cycle]) are read-only here: the
 * deployed rule forbids changing `key` and `createdAt`, and the campus form never
 * writes identity or address back. They are carried only so the screen can show
 * which school it is editing.
 *
 * Every field has a safe fallback. A malformed document must not crash the form,
 * and an unreadable radius must never silently become `0` — a zero radius would
 * leave the whole school "outside" the geofence. The authoritative range is
 * `20..5000` metres (CONTEXT.md §8.1); values outside it are left as read so the
 * screen's validation can force a correction before saving.
 *
 * [updatedAtMillis] is read-only and feeds the "last saved" hint.
 */
data class AdminSchool(
    val name: String = "",
    val key: String = "",
    val address: String = "",
    val cycle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Int = DEFAULT_RADIUS_METERS,
    val entryTime: String = "",
    val exitTime: String = "",
    val requireLocation: Boolean = true,
    val updatedAtMillis: Long? = null,
) {
    /** True when the document has a usable centre for the geofence. */
    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0

    companion object {
        /** Matches the seeded school and stays inside the valid `20..5000` range. */
        const val DEFAULT_RADIUS_METERS = 150
    }
}

/**
 * Only the fields the campus form is allowed to change.
 *
 * These are exactly the keys the principal writes, and they mirror the four
 * editable document fields plus the audit timestamp. Keeping them in a dedicated
 * type makes it impossible for a caller to pass `key` or `createdAt` to [toUpdateMap].
 */
data class AdminCampusConfig(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val entryTime: String,
    val exitTime: String,
    val requireLocation: Boolean,
)

/** Field names of `schools/{schoolId}` that the campus form owns. */
object AdminCampusFields {
    const val LOCATION = "location"
    const val RADIUS_METERS = "radiusMeters"
    const val SCHEDULE = "schedule"
    const val REQUIRE_LOCATION = "requireLocation"
    const val UPDATED_AT = "updatedAt"

    const val LOCATION_LAT = "lat"
    const val LOCATION_LNG = "lng"
    const val ENTRY_TIME = "entryTime"
    const val EXIT_TIME = "exitTime"
}

/**
 * Placeholder for [AdminCampusFields.UPDATED_AT] in a pure, Firebase-free map.
 *
 * The data layer swaps it for `FieldValue.serverTimestamp()` at write time. This
 * keeps the server clock authoritative while the map's shape stays unit-testable
 * without the Firebase SDK.
 */
const val SERVER_TIMESTAMP_SENTINEL = "__server_timestamp__"

/**
 * The exact `update()` payload for a campus save.
 *
 * This is the single source of truth for the writable keys: the UI helper exposes
 * it as a pure function for tests, and the Firestore repository uses the same
 * builder before replacing the sentinel. No identity field is reachable from here.
 */
fun AdminCampusConfig.toUpdateMap(): Map<String, Any?> = mapOf(
    AdminCampusFields.LOCATION to mapOf(
        AdminCampusFields.LOCATION_LAT to latitude,
        AdminCampusFields.LOCATION_LNG to longitude,
    ),
    AdminCampusFields.RADIUS_METERS to radiusMeters,
    AdminCampusFields.SCHEDULE to mapOf(
        AdminCampusFields.ENTRY_TIME to entryTime,
        AdminCampusFields.EXIT_TIME to exitTime,
    ),
    AdminCampusFields.REQUIRE_LOCATION to requireLocation,
    AdminCampusFields.UPDATED_AT to SERVER_TIMESTAMP_SENTINEL,
)
