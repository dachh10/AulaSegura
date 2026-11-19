package com.example.aulasegura.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.aulasegura.location.LocationFix
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

/** Geocerca del plantel: centro y radio permitido para pasar lista. */
object CampusGeo {
    val SanAgustin = LatLng(19.2826, -99.6557)
    const val RadiusMeters = 150f
}

/**
 * Mapa de Google con la geocerca del plantel y el punto del docente.
 *
 * @param campus centro real del plantel leído de `schools.location`. `null`
 *   significa que no hay plantel cacheado: se dibuja solo el punto del docente
 *   en vez de inventar un centro.
 * @param outsideRadius pinta la geocerca en rojo cuando el docente está fuera.
 * @param schoolName nombre del plantel para el marcador; `null` deja un rótulo
 *   neutro en lugar de "Colegio San Agustín", que no es esta escuela.
 * @param pin marcador que mueve la persona que reporta (incidente). Es una
 *   coordenada real, no una fracción de dibujo; `null` no dibuja marcador.
 * @param onPinChange si no es `null`, tocar el mapa mueve el [pin] a ese punto.
 * @param scrollGesturesEnabled el incidente vive dentro de un formulario con
 *   scroll; ahí se apaga para que el dedo siga desplazando la pantalla.
 */
@Composable
fun CampusMap(
    teacherFix: LocationFix?,
    modifier: Modifier = Modifier,
    outsideRadius: Boolean = false,
    campus: LatLng? = CampusGeo.SanAgustin,
    radiusMeters: Float = CampusGeo.RadiusMeters,
    schoolName: String? = null,
    pin: LatLng? = null,
    pinTitle: String = "Incidente",
    onPinChange: ((LatLng) -> Unit)? = null,
    scrollGesturesEnabled: Boolean = true,
) {
    val geofenceColor = if (outsideRadius) Color(0xFFE5484D) else Color(0xFF5ED3B0)

    val cameraPositionState = rememberCameraPositionState {
        // El pin manda cuando no hay plantel: es la única coordenada real que la
        // pantalla conoce, y centrar el mapa en un plantel de ejemplo sería
        // enseñarle al docente un lugar que no existe.
        position = CameraPosition.fromLatLngZoom(
            campus ?: pin ?: CampusGeo.SanAgustin,
            16.2f,
        )
    }

    // The school arrives from the cache a few milliseconds after the screen is
    // composed. Without this, the camera would stay on the fallback point while
    // the geofence circle sits somewhere off screen. With a pin on the map the
    // camera stays put: the teacher's point is what they are looking at.
    LaunchedEffect(campus) {
        if (pin == null) {
            campus?.let { cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 16.2f) }
        }
    }

    val context = LocalContext.current
    var markerIcons by remember { mutableStateOf<MapMarkerIcons?>(null) }

    LaunchedEffect(context) {
        // El SDK de Maps se inicializa de forma diferida: si los descriptores se
        // crean durante la composición, BitmapDescriptorFactory aún no existe y
        // la pantalla revienta con NPE. Se inicializa y luego se crean los íconos.
        MapsInitializer.initialize(context.applicationContext)
        markerIcons = runCatching {
            MapMarkerIcons(
                school = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                teacher = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                incident = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
            )
        }.getOrNull()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // Tapping places the pin; it is not a gesture, so it keeps working with
        // the scroll gestures off inside the incident form.
        onMapClick = { point -> onPinChange?.invoke(point) },
        properties = MapProperties(isBuildingEnabled = true),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
            scrollGesturesEnabled = scrollGesturesEnabled,
        ),
    ) {
        campus?.let { site ->
            Circle(
                center = site,
                radius = radiusMeters.toDouble(),
                fillColor = geofenceColor.copy(alpha = 0.14f),
                strokeColor = geofenceColor,
                strokeWidth = 4f,
            )

            Marker(
                state = rememberMarkerState(position = site),
                title = schoolName ?: "Plantel",
                snippet = "Radio permitido · ${radiusMeters.toInt()} m",
                icon = markerIcons?.school,
            )
        }

        pin?.let { point ->
            // `rememberMarkerState` solo usa `position` para inicializar, así que
            // la clave es la coordenada: cada toque crea un estado con la posición
            // nueva en vez de dejar el marcador clavado en el primero.
            Marker(
                state = rememberMarkerState(
                    key = "${point.latitude},${point.longitude}",
                    position = point,
                ),
                title = pinTitle,
                icon = markerIcons?.incident,
            )
        }

        teacherFix?.let { fix ->
            Circle(
                center = LatLng(fix.latitude, fix.longitude),
                radius = 18.0,
                fillColor = Color(0x332563EB),
                strokeColor = Color(0xFF2563EB),
                strokeWidth = 2f,
            )
            Marker(
                state = rememberMarkerState(
                    position = LatLng(fix.latitude, fix.longitude),
                ),
                title = "Tú (docente)",
                icon = markerIcons?.teacher,
            )
        }
    }
}

/**
 * Editable campus map for the principal's form.
 *
 * The single draggable marker and the geofence circle share one center: the valid
 * draft coordinates, or the stored school location, or a sane default. Every move
 * (drag or tap) is reported through [onLocationChange] so the text fields and the
 * map edit the same draft state.
 */
@Composable
fun EditableCampusMap(
    latitude: Double?,
    longitude: Double?,
    fallbackLatitude: Double?,
    fallbackLongitude: Double?,
    radiusMeters: Float,
    onLocationChange: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = when {
        latitude != null && longitude != null -> LatLng(latitude, longitude)
        fallbackLatitude != null && fallbackLongitude != null &&
            (fallbackLatitude != 0.0 || fallbackLongitude != 0.0) ->
            LatLng(fallbackLatitude, fallbackLongitude)
        else -> CampusGeo.SanAgustin
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 16.2f)
    }
    val markerState = rememberMarkerState(position = center)

    // The camera must not fight the user mid-drag. Outside a drag, keep it in sync
    // with the draft coordinates and move the marker if the parent changed them.
    LaunchedEffect(center) {
        if (!markerState.isDragging) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(center, 16.2f)
            if (markerState.position != center) markerState.position = center
        }
    }

    // maps-compose 6.1.2 exposes no marker-drag callbacks. MarkerState.position is
    // a snapshot-backed mutable state that the SDK updates while isDragging is
    // true, so observing the pair reports live drag positions (and the final one
    // when isDragging flips back to false).
    LaunchedEffect(markerState, onLocationChange) {
        var wasDragging = false
        snapshotFlow { markerState.position to markerState.isDragging }
            .collect { (position, dragging) ->
                when {
                    dragging -> {
                        wasDragging = true
                        onLocationChange(position)
                    }
                    wasDragging -> {
                        wasDragging = false
                        onLocationChange(position)
                    }
                }
            }
    }

    val context = LocalContext.current
    var markerIcons by remember { mutableStateOf<MapMarkerIcons?>(null) }
    LaunchedEffect(context) {
        // Same initialization order as CampusMap: creating descriptors before the
        // SDK is ready crashes with an NPE.
        MapsInitializer.initialize(context.applicationContext)
        markerIcons = runCatching {
            MapMarkerIcons(
                school = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                teacher = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                incident = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
            )
        }.getOrNull()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = { latLng -> onLocationChange(latLng) },
        properties = MapProperties(isBuildingEnabled = true),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
        ),
    ) {
        Circle(
            center = center,
            radius = radiusMeters.toDouble(),
            fillColor = Color(0x245ED3B0),
            strokeColor = Color(0xFF5ED3B0),
            strokeWidth = 4f,
        )
        Marker(
            state = markerState,
            title = "Plantel",
            snippet = "Arrastra para mover el centro",
            icon = markerIcons?.school,
            draggable = true,
        )
    }
}

/** Íconos del mapa, creados solo cuando el SDK de Maps ya está inicializado. */
private data class MapMarkerIcons(
    val school: BitmapDescriptor,
    val teacher: BitmapDescriptor,
    val incident: BitmapDescriptor,
)
