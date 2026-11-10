package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * Pase de lista en modo esquemático: sin conexión, el mapa real no está
 * disponible pero la geocerca se dibuja con un radar y la ubicación se valida.
 */
@Composable
fun TeacherRollCallOfflineScreen(
    groupId: String = "3B",
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    var entries by remember { mutableStateOf(demoRoster()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GpsCream),
    ) {
        // Zona 1 — cabecera con radar esquemático
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Color(0xFFE8E9F7)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MapCircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Volver",
                    tint = Color(0xFF141761),
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
                    icon = Icons.Filled.CloudOff,
                    description = "Sin conexión",
                    tint = GpsAmberDeep,
                    onClick = {},
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFF8E1))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = GpsAmberDeep,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sin conexión · El mapa no está disponible, pero tu ubicación sí se valida",
                    color = Color(0xFF451A03),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }

            OfflineRadar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp),
            )
        }

        // Zona 2 — lista y acciones
        Column(
            modifier = Modifier
                .weight(1f)
                .revealOnEnter(60),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color(0xFFE5E2DD))
                        .align(Alignment.CenterHorizontally),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE8F8F2))
                        .border(1.dp, GpsMint.copy(alpha = 0.50f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GpsSuccess),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Estás dentro de la escuela",
                            color = Color(0xFF1B4332),
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            lineHeight = 17.sp,
                        )
                        Text(
                            text = "32 m del centro · radio 150 m",
                            color = Color(0xFF2D6A4F),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = GpsSuccess,
                        modifier = Modifier.size(18.dp),
                    )
                }

                MetricCardGrid()

                RosterCard(
                    entries = entries,
                    onSelect = { index, status ->
                        entries = entries.toMutableList().also {
                            it[index] = it[index].copy(status = status)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    showNumberColumn = false,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = GpsIndigo,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Validación biométrica y geocerca activa",
                        color = Color(0xFF464651),
                        fontFamily = DmSans,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "AulaSegura v2.4",
                        color = Color(0xFF464651),
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
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
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF78716C),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Se guardará y se enviará al reconectar",
                        color = Color(0xFF57534E),
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                CoralCta(
                    text = "Guardar asistencia",
                    icon = Icons.Filled.Save,
                    onClick = onSave,
                )

                Spacer(modifier = Modifier.height(8.dp))
                GesturePill()
            }
        }
    }
}

@Composable
private fun OfflineRadar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "teacherPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "teacherPulseScale",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Anillos del radar
        Box(
            modifier = Modifier
                .size(184.dp)
                .clip(CircleShape)
                .border(1.dp, GpsIndigo.copy(alpha = 0.25f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(138.dp)
                .clip(CircleShape)
                .background(GpsMint.copy(alpha = 0.10f))
                .border(2.dp, GpsMint, CircleShape),
        )
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(1.dp, GpsIndigo.copy(alpha = 0.30f), CircleShape),
        )

        // Etiqueta del radio permitido
        Text(
            text = "Radio 150m",
            color = Color(0xFF00513F),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 148.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(SurfaceContainerLowest)
                .border(1.dp, GpsMint, RoundedCornerShape(percent = 50))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )

        // Pin del plantel
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GpsIndigo),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141761).copy(alpha = 0.40f)),
            )
        }

        // Docente dentro del radio (32 m)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 80.dp, top = 76.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .scale(pulse)
                        .alpha(0.30f)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB)),
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB))
                        .border(2.dp, Color.White, CircleShape),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tú (32m)",
                color = Color(0xFF1E3A8A),
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.90f))
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }

        // Indicador de mapa esquemático
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(SurfaceContainerLowest.copy(alpha = 0.85f))
                .border(1.dp, GpsHairline, RoundedCornerShape(percent = 50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Map,
                contentDescription = null,
                tint = Color(0xFF777682),
                modifier = Modifier.size(13.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Modo esquemático offline",
                color = Color(0xFF777682),
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        }
    }
}
