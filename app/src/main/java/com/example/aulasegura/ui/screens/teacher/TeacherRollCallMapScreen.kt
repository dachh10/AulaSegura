package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.location.distanceMeters
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.ui.components.CampusGeo
import com.example.aulasegura.ui.components.CampusMap
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * Pase de lista con mapa: el docente está dentro del radio del plantel.
 * Mitad superior mapa de Google con geocerca + hoja inferior con la lista.
 */
@Composable
fun TeacherRollCallMapScreen(
    groupId: String = "3B",
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onOpenOfflineMode: () -> Unit = {},
) {
    var entries by remember { mutableStateOf(demoRoster()) }
    val location = rememberCurrentLocation()
    val fix = location.fix
    val distance = fix?.let {
        distanceMeters(it.latitude, it.longitude, CampusGeo.SanAgustin.latitude, CampusGeo.SanAgustin.longitude)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GpsCream),
    ) {
        // Zona 1 — mapa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        ) {
            CampusMap(
                teacherFix = fix,
                modifier = Modifier.fillMaxSize(),
            )

            Column(modifier = Modifier.fillMaxSize()) {
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
                        icon = Icons.Filled.MyLocation,
                        description = "Mi ubicación",
                        tint = GpsIndigo,
                        onClick = location.retry,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                InsideBanner(
                    distanceMeters = distance,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        // Zona 2 — hoja inferior
        Column(
            modifier = Modifier
                .weight(1f)
                .revealOnEnter(60),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 10.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFFD1CBBF))
                        .align(Alignment.CenterHorizontally),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$groupId · Matemáticas",
                            color = GpsIndigo,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 23.sp,
                            maxLines = 1,
                        )
                        Text(
                            text = "Prof. Alberto Hernández Morales",
                            color = GpsMuted,
                            fontFamily = DmSans,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "Mar 3 nov",
                        color = Color(0xFF6B7280),
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEBE8E3))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }

                MetricPillRow()

                MarkAllPresentButton(
                    onClick = {
                        entries = entries.map { it.copy(status = AttendanceState.Present) }
                    },
                )

                RosterCard(
                    entries = entries,
                    onSelect = { index, status ->
                        entries = entries.toMutableList().also {
                            it[index] = it[index].copy(status = status)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                if (!location.permissionGranted) {
                    PermissionNotice(
                        onRequest = location.requestPermission,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = GpsMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Modo esquemático sin conexión",
                        color = GpsMuted,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.5.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenOfflineMode),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainerLowest)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GpsHairline),
                )
                Spacer(modifier = Modifier.height(10.dp))

                CoralCta(
                    text = "Guardar asistencia",
                    icon = Icons.Filled.Verified,
                    onClick = onSave,
                )

                Spacer(modifier = Modifier.height(8.dp))
                GesturePill()
            }
        }
    }
}

@Composable
private fun InsideBanner(distanceMeters: Float?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, GpsHairline, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(GpsSuccess),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Estás dentro de la escuela",
            color = Color(0xFF1F2937),
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = if (distanceMeters != null) "${distanceMeters.toInt()} m" else "— m",
            color = GpsSuccess,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Color(0xFFEAF6F0))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MarkAllPresentButton(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(36.dp)
            .then(pressModifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainerLowest)
            .border(1.dp, GpsIndigo.copy(alpha = 0.25f), RoundedCornerShape(percent = 50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = null,
            tint = GpsIndigo,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Marcar todos presentes",
            color = GpsIndigo,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Buscar alumno",
            tint = GpsMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun PermissionNotice(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF8E1))
            .border(1.dp, GpsAmber.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = null,
            tint = GpsAmberDeep,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Activa tu ubicación para validar la geocerca del plantel.",
            color = Color(0xFF78350F),
            fontFamily = DmSans,
            fontSize = 11.5.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Activar",
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(GpsAmberDeep)
                .clickable(onClick = onRequest)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
