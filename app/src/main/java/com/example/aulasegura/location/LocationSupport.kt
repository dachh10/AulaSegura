package com.example.aulasegura.location

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.aulasegura.domain.model.SessionLocation

/** Una lectura de GPS del docente. */
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
    /** A read with permission returned nothing: the button must say so. */
    val failed: Boolean,
    val deniedPermanently: Boolean,
    val requestPermission: () -> Unit,
    val retry: () -> Unit,
)

/**
 * Pide permiso de ubicación y expone la posición actual con actualizaciones
 * mientras el permiso esté concedido. Si el usuario niega, queda en
 * [LocationUiState.deniedPermanently] para poder mostrar el aviso.
 *
 * La lectura real vive en [AndroidLocationProvider]: acá solo queda el estado de
 * Compose. La geocerca no se decide en este archivo, sino en
 * `evaluateGeofence`, que es puro y se puede testear sin dispositivo.
 */
/** Forms that only centre a map skip the fresh fix; any explicit retry asks for it. */
internal fun shouldPreferFreshFix(useFreshFixOnEntry: Boolean, attempt: Int): Boolean =
    useFreshFixOnEntry || attempt > 0

@Composable
fun rememberCurrentLocation(
    /** Set false on forms that only centre a map: the fresh GPS wait is up to 8 s. */
    useFreshFixOnEntry: Boolean = true,
): LocationUiState {
    val context = LocalContext.current
    val provider = remember(context) { AndroidLocationProvider(context) }
    var granted by remember { mutableStateOf(provider.hasPermission()) }
    var fix by remember { mutableStateOf<LocationFix?>(null) }
    var locating by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var deniedPermanently by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val accepted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        granted = accepted || provider.hasPermission()
        deniedPermanently = !granted
        // Without permission the request is the answer, not a stale read failure.
        failed = false
        if (granted) attempt++
    }

    val requestPermission: () -> Unit = {
        if (provider.hasPermission()) {
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
        failed = false
        val updated = provider.currentFix(
            preferFresh = shouldPreferFreshFix(useFreshFixOnEntry, attempt),
        )
        locating = false
        if (updated != null) fix = updated else failed = true
    }

    return LocationUiState(
        permissionGranted = granted,
        fix = fix,
        locating = locating,
        failed = failed,
        deniedPermanently = deniedPermanently,
        requestPermission = requestPermission,
        retry = { attempt++ },
    )
}

/**
 * Turns the reading plus its geofence verdict into what the session stores.
 *
 * The session keeps the distance and the verdict, not just the coordinates:
 * months later, "was this attendance verified?" must be answerable without
 * recomputing against a school radius that may have changed.
 */
fun LocationFix.toSessionLocation(result: GeofenceResult): SessionLocation = SessionLocation(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters?.toDouble(),
    distanceMeters = result.distanceMeters,
    verified = result.verified,
)
