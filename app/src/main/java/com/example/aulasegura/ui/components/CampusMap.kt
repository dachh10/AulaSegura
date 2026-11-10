package com.example.aulasegura.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * @param outsideRadius pinta la geocerca en rojo cuando el docente está fuera.
 */
@Composable
fun CampusMap(
    teacherFix: LocationFix?,
    modifier: Modifier = Modifier,
    outsideRadius: Boolean = false,
    campus: LatLng = CampusGeo.SanAgustin,
    radiusMeters: Float = CampusGeo.RadiusMeters,
) {
    val geofenceColor = if (outsideRadius) Color(0xFFE5484D) else Color(0xFF5ED3B0)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(campus, 16.2f)
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
            )
        }.getOrNull()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isBuildingEnabled = true),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false,
        ),
    ) {
        Circle(
            center = campus,
            radius = radiusMeters.toDouble(),
            fillColor = geofenceColor.copy(alpha = 0.14f),
            strokeColor = geofenceColor,
            strokeWidth = 4f,
        )

        Marker(
            state = rememberMarkerState(position = campus),
            title = "Colegio San Agustín",
            snippet = "Radio permitido · ${radiusMeters.toInt()} m",
            icon = markerIcons?.school,
        )

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

/** Íconos del mapa, creados solo cuando el SDK de Maps ya está inicializado. */
private data class MapMarkerIcons(
    val school: BitmapDescriptor,
    val teacher: BitmapDescriptor,
)
