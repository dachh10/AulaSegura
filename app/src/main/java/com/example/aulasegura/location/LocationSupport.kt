package com.example.aulasegura.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Última lectura de GPS del docente. */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

/** Estado de ubicación que consumen las pantallas de pase de lista. */
data class LocationUiState(
    val permissionGranted: Boolean,
    val fix: LocationFix?,
    val locating: Boolean,
    val deniedPermanently: Boolean,
    val requestPermission: () -> Unit,
    val retry: () -> Unit,
)

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Pide permiso de ubicación y expone la posición actual con actualizaciones
 * mientras el permiso esté concedido. Si el usuario niega, queda en
 * [LocationUiState.deniedPermanently] para poder mostrar el aviso.
 */
@Composable
fun rememberCurrentLocation(): LocationUiState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasLocationPermission()) }
    var fix by remember { mutableStateOf<LocationFix?>(null) }
    var locating by remember { mutableStateOf(false) }
    var deniedPermanently by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val accepted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        granted = accepted || context.hasLocationPermission()
        deniedPermanently = !granted
        if (granted) attempt++
    }

    val requestPermission: () -> Unit = {
        if (context.hasLocationPermission()) {
            granted = true
            attempt++
        } else {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(granted, attempt) {
        if (!granted) return@LaunchedEffect
        locating = true
        val updated = readCurrentLocation(context)
        if (updated != null) fix = updated
        locating = false
    }

    return LocationUiState(
        permissionGranted = granted,
        fix = fix,
        locating = locating,
        deniedPermanently = deniedPermanently,
        requestPermission = requestPermission,
        retry = { attempt++ },
    )
}

@SuppressLint("MissingPermission")
private suspend fun readCurrentLocation(context: Context): LocationFix? {
    val fused = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val token = com.google.android.gms.tasks.CancellationTokenSource().token
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token).awaitOrNull()
    }.getOrNull()

    val location: Location? = fused ?: runCatching {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
    }.getOrNull()

    return location?.let {
        LocationFix(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else null)
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitOrNull(): T? =
    suspendCancellableCoroutine<T?> { continuation ->
        addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
        addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
        addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
    }

/** Distancia en metros entre dos coordenadas. */
fun distanceMeters(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double,
): Float {
    val result = FloatArray(1)
    Location.distanceBetween(fromLat, fromLng, toLat, toLng, result)
    return result[0]
}
