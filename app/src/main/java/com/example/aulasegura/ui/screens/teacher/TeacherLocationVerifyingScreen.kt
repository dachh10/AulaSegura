package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.location.distanceMeters
import com.example.aulasegura.location.rememberCurrentLocation
import com.example.aulasegura.ui.components.CampusGeo
import com.example.aulasegura.ui.components.CampusMap
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import kotlinx.coroutines.delay

/**
 * Verificando ubicación: pide el permiso de ubicación, obtiene una lectura GPS
 * y navega al estado que corresponda (dentro o fuera del radio de 150 m).
 */
@Composable
fun TeacherLocationVerifyingScreen(
    groupId: String = "3B",
    onBack: () -> Unit = {},
    onInsideRadius: () -> Unit = {},
    onOutsideRadius: () -> Unit = {},
) {
    val location = rememberCurrentLocation()
    var permissionAsked by remember { mutableStateOf(false) }
    var tries by remember { mutableStateOf(0) }
    var timedOut by remember { mutableStateOf(false) }

    // Pedimos el permiso en cuanto entra a la pantalla.
    LaunchedEffect(Unit) {
        if (!location.permissionGranted && !permissionAsked) {
            permissionAsked = true
            location.requestPermission()
        }
    }

    // La lectura de GPS es puntual: si no hay fix (primer arranque, emulador
    // sin ubicación) reintentamos unas cuantas veces antes de rendirnos.
    LaunchedEffect(location.permissionGranted, location.fix, tries) {
        if (!location.permissionGranted || location.fix != null) return@LaunchedEffect
        if (tries >= 8) {
            timedOut = true
            return@LaunchedEffect
        }
        delay(2500)
        location.retry()
        tries += 1
    }

    LaunchedEffect(location.fix) {
        val fix = location.fix ?: return@LaunchedEffect
        if (timedOut) timedOut = false
        delay(1400)
        val distance = distanceMeters(
            fix.latitude,
            fix.longitude,
            CampusGeo.SanAgustin.latitude,
            CampusGeo.SanAgustin.longitude,
        )
        if (distance <= CampusGeo.RadiusMeters) onInsideRadius() else onOutsideRadius()
    }

    val accuracy = location.fix?.accuracyMeters

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GpsCream),
    ) {
        // Zona 1 — mapa con radar de escaneo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
        ) {
            CampusMap(
                teacherFix = location.fix,
                modifier = Modifier.fillMaxSize(),
            )

            RadarScan(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 24.dp),
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
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    description = "Ayuda",
                    onClick = {},
                )
            }
        }

        // Zona 2 — hoja inferior de verificación
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFFC7C5D2).copy(alpha = 0.6f)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verificando tu ubicación…",
                color = GpsIndigo,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Necesitamos confirmar que estás en la escuela para pasar lista.",
                color = Color(0xFF464651),
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFFEBE8E3).copy(alpha = 0.8f))
                    .border(1.dp, GpsHairline, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulsingDot()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        !location.permissionGranted -> "Permiso de ubicación pendiente"
                        timedOut -> "Sin señal GPS"
                        accuracy != null -> "Precisión GPS: ±${accuracy.toInt()} m"
                        else -> "Obteniendo señal GPS…"
                    },
                    color = Color(0xFF464651),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC7C5D2)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (location.locating) "Buscando" else "Validando",
                    color = GpsIndigo,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!location.permissionGranted) {
                PermissionNotice(onRequest = location.requestPermission)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (timedOut) {
                Text(
                    text = "No pudimos obtener tu ubicación. Verifica que el GPS esté activo y vuelve a intentarlo.",
                    color = GpsDanger,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoralCta(
                    text = "Reintentar verificación",
                    icon = Icons.Filled.Refresh,
                    onClick = {
                        timedOut = false
                        tries = 0
                        location.retry()
                    },
                )
            } else {
                LockedCta()
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancelar",
                    color = GpsIndigo,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            GesturePill()
        }
    }
}

@Composable
private fun LockedCta() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0xFFE2E0D8))
            .border(1.dp, Color(0xFFD5D2C9), RoundedCornerShape(percent = 50)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = Color(0xFF8C8A85),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Pasar lista",
            color = Color(0xFF8C8A85),
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun RadarScan(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "radarProgress",
    )

    Box(
        modifier = modifier.size(176.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(176.dp)
                .scale(0.35f + progress * 1.05f)
                .alpha((0.55f * (1f - progress)).coerceAtLeast(0f))
                .clip(CircleShape)
                .border(2.dp, GpsIndigo.copy(alpha = 0.55f), CircleShape)
                .background(GpsIndigo.copy(alpha = 0.10f)),
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, GpsIndigo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = GpsIndigo,
                modifier = Modifier.size(22.dp),
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GpsCoral),
            )
        }
    }
}

@Composable
private fun PulsingDot() {
    val transition = rememberInfiniteTransition(label = "gpsPulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gpsPulseAlpha",
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(GpsCoral),
    )
}
