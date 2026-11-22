package com.example.aulasegura.domain.repository

/**
 * Turns a campus centre into the human-readable name shown across the app.
 * Implemented with the platform geocoder; injectable so the campus ViewModel
 * can be unit-tested without Android.
 */
fun interface SchoolNameResolver {
    suspend fun resolve(latitude: Double, longitude: Double): String?
}

/** Picks the most useful address line, preferring the city over the state. */
fun resolvedSchoolName(
    locality: String?,
    subAdminArea: String?,
    adminArea: String?,
    featureName: String?,
): String? = listOf(locality, subAdminArea, adminArea, featureName)
    .firstOrNull { !it.isNullOrBlank() }
    ?.trim()
