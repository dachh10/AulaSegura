package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FmdBad
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.SchoolSite
import com.example.aulasegura.location.evaluateGeofence
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.ui.components.CampusGeo
import com.example.aulasegura.ui.components.CampusMap
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.google.android.gms.maps.model.LatLng

/**
 * Estado fuera del radio permitido: el docente no puede pasar lista hasta
 * acercarse al plantel.
 *
 * La distancia y el radio salen de `schools.location` y `schools.radiusMeters`
 * (vía [school]), no de un punto de demostración: el texto tiene que coincidir
 * con el mapa que el docente está mirando.
 */
@Composable
fun TeacherOutsideRadiusScreen(
    groupId: String = "3B",
    school: SchoolSite? = null,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onBackToGroups: () -> Unit = {},
) {
    val location = rememberCurrentLocation()
    val fix = location.fix
    val geofence = evaluateGeofence(fix, school)
    val distance = geofence.distanceMeters
    val radiusLabel = geofence.radiusMeters?.toInt()?.toString() ?: "—"
    val campus = school?.let { LatLng(it.latitude, it.longitude) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GpsCream),
    ) {
        // Zona 1 — mapa con geocerca en rojo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        ) {
            CampusMap(
                teacherFix = fix,
                modifier = Modifier.fillMaxSize(),
                outsideRadius = true,
                campus = campus,
                radiusMeters = geofence.radiusMeters?.toFloat() ?: CampusGeo.RadiusMeters,
                schoolName = school?.name,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapCircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Volver",
                    onClick = onBack,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    RollCallHeaderPill(groupId = groupId)
                }
                Spacer(modifier = Modifier.width(8.dp))
                MapCircleButton(
                    icon = Icons.Filled.Refresh,
                    description = "Reintentar ubicación",
                    tint = GpsIndigo,
                    onClick = onRetry,
                )
            }
        }

        // Zona 2 — explicación y acciones
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .revealOnEnter(60)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFFDCD8D0)),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFDAD6).copy(alpha = 0.6f))
                        .border(1.dp, Color(0xFFAE3123).copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.FmdBad,
                        contentDescription = null,
                        tint = Color(0xFFAE3123),
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fuera del radio permitido",
                        color = Color(0xFF141761),
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("Estás a ")
                            withStyle(SpanStyle(color = Color(0xFFAE3123), fontWeight = FontWeight.Bold)) {
                                append(if (distance != null) "${distance.toInt()} m" else "— m")
                            }
                            append(" de la escuela. La asistencia solo se puede registrar dentro de ")
                            withStyle(SpanStyle(color = GpsIndigo, fontWeight = FontWeight.Bold)) {
                                append("$radiusLabel m")
                            }
                            append(".")
                        },
                        color = Color(0xFF464651),
                        fontFamily = DmSans,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF0F1FA))
                    .border(1.dp, Color(0xFFD5D8F0), RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0FF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = GpsIndigo,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Acércate a la escuela y vuelve a intentar. Los reportes fuera del radio no se guardan en la bitácora oficial.",
                    color = GpsIndigo,
                    fontFamily = DmSans,
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            CoralCta(
                text = "Reintentar ubicación",
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onBackToGroups)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Volver a mis grupos",
                    color = GpsIndigo,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            GesturePill()
        }
    }
}
