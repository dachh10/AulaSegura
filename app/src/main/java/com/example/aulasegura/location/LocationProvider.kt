package com.example.aulasegura.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * One reading of the teacher's position.
 *
 * A plain interface so the geofence can be exercised in a unit test with a fixed
 * [LocationFix] and no Play Services at all.
 */
interface LocationProvider {

    /** Whether the app currently holds a location permission. */
    fun hasPermission(): Boolean

    /**
     * Fresh fix if one arrives within the timeout, last known position otherwise.
     *
     * `null` means the device genuinely has nothing: no permission, GPS off and
     * no previous reading.
     */
    suspend fun currentFix(): LocationFix?
}

/**
 * `FusedLocationProviderClient` with the fallback order the phase asks for.
 *
 * The order matters: a fresh high-accuracy read is what proves the teacher is in
 * school *now*, but it can take several seconds indoors. Falling back to the last
 * known position keeps the gate usable in a classroom, and the accuracy of that
 * reading travels with it so [evaluateGeofence] can still refuse to verify a
 * stale 500 m fix.
 */
class AndroidLocationProvider(
    context: Context,
) : LocationProvider {

    private val appContext: Context = context.applicationContext

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun currentFix(): LocationFix? {
        if (!hasPermission()) return null

        val fresh = runCatching {
            withTimeoutOrNull(FRESH_FIX_TIMEOUT_MILLIS) {
                val client = LocationServices.getFusedLocationProviderClient(appContext)
                val token = com.google.android.gms.tasks.CancellationTokenSource().token
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).awaitOrNull()
            }
        }.getOrNull()

        val location = fresh ?: lastKnownLocation()
        return location?.let {
            LocationFix(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
            )
        }
    }

    /**
     * Last position the platform remembers, GPS first.
     *
     * Network last-known is deliberately second: it is usually a cell-tower
     * estimate hundreds of metres off, and a GPS reading from two minutes ago is
     * a better answer for a 150 m fence.
     */
    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? = runCatching {
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
    }.getOrNull()

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
            addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
        }

    private companion object {
        /**
         * How long a fresh fix may take before the last known position is used.
         *
         * Eight seconds is long enough for a cold GPS start on a mid-range phone
         * and short enough that a teacher does not abandon the screen.
         */
        const val FRESH_FIX_TIMEOUT_MILLIS = 8_000L
    }
}
