package com.example.aulasegura.ui.screens.admin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import kotlin.math.roundToInt

/** 4.7 — Configuración del plantel. Ported 1:1 from the Stitch markup. */

private val Hairline = Color(0xFFE6E1D8)
private val SchoolBadgeBg = Color(0xFFE8E9F7)
private val GpsPillBg = Color(0xFFEAF6F0)
private val GpsPillText = Color(0xFF1B7A50)
private val GpsDot = Color(0xFF2E9E6B)
private val MapBase = Color(0xFFF4F1EA)
private val MapGreen = Color(0xFFD5F3EA)
private val MapGreenSoft = Color(0xFFE2F6EF)
private val MapBuilding = Color(0xFFE8E4DA)
private val MapRoadLine = Color(0xFFEAE6DC)
private val SaveCoral = Color(0xFFFF6B57)
private val SaveShadow = Color(0x4DFF6B57)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun AdminCampusScreen(
    onBack: () -> Unit = {},
    onAyuda: () -> Unit = {},
    onGuardar: () -> Unit = {},
) {
    var radius by rememberSaveable { mutableFloatStateOf(150f) }
    var exigirUbicacion by rememberSaveable { mutableStateOf(true) }
    var animatedIn by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { animatedIn = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream)
            .imePadding(),
    ) {
        Reveal(0) { CampusTopBar(onBack = onBack, onAyuda = onAyuda) }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .revealOnEnter(60)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SchoolIdentityCard()
            LocationCard(animatedIn = animatedIn)
            RadiusCard(radius = radius, onRadiusChange = { radius = it })
            ScheduleCard(
                exigirUbicacion = exigirUbicacion,
                onExigirUbicacionChange = { exigirUbicacion = it },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest.copy(alpha = 0.95f))
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.30f)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SaveConfigurationButton(onClick = onGuardar)
            }
        }
    }
}

@Composable
private fun CampusTopBar(onBack: () -> Unit, onAyuda: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Configuración del plantel",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.2).sp,
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onAyuda),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.HelpOutline,
                contentDescription = "Ayuda",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SchoolIdentityCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SchoolBadgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Nombre del plantel",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.24.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Escuela Primaria Benito Juárez",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Clave:",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.33.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "15EPR0123X",
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.65.sp,
                )
            }
        }
    }
}

@Composable
private fun LocationCard(animatedIn: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ubicación de la escuela",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            GpsActivePill()
        }

        StylizedCampusMap(animatedIn = animatedIn)

        Row(
            modifier = Modifier.padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Toca el mapa para fijar el punto de la escuela",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.24.sp,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CoordinateBox(label = "Latitud", value = "19.292600", modifier = Modifier.weight(1f))
            CoordinateBox(label = "Longitud", value = "-99.655700", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GpsActivePill() {
    val transition = rememberInfiniteTransition(label = "gpsPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "gpsDotAlpha",
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(GpsPillBg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(GpsDot.copy(alpha = pulse)),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "GPS Activo",
            color = GpsPillText,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun StylizedCampusMap(animatedIn: Boolean) {
    val transition = rememberInfiniteTransition(label = "radiusPulse")
    val radiusAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "radiusAlpha",
    )
    val pinDrop by animateFloatAsState(
        targetValue = if (animatedIn) 1f else 0.6f,
        animationSpec = tween(durationMillis = 400),
        label = "pinDrop",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MapBase)
            .border(1.dp, OutlineVariant.copy(alpha = 0.50f), RoundedCornerShape(20.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Campus green zone
            val campus = Path().apply {
                moveTo(w * 0.21f, h * 0.20f)
                cubicTo(w * 0.27f, h * 0.08f, w * 0.49f, h * 0.14f, w * 0.64f, h * 0.25f)
                cubicTo(w * 0.73f, h * 0.31f, w * 0.79f, h * 0.55f, w * 0.73f, h * 0.70f)
                cubicTo(w * 0.67f, h * 0.85f, w * 0.46f, h * 0.85f, w * 0.33f, h * 0.80f)
                cubicTo(w * 0.24f, h * 0.75f, w * 0.18f, h * 0.60f, w * 0.18f, h * 0.40f)
                close()
            }
            drawPath(campus, color = MapGreen, alpha = 0.85f)

            val corner = Path().apply {
                moveTo(0f, h * 0.65f)
                cubicTo(w * 0.12f, h * 0.68f, w * 0.21f, h * 0.85f, w * 0.20f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(corner, color = MapGreenSoft, alpha = 0.6f)

            // Roads
            drawLine(Color.White, Offset(-10f, h * 0.225f), Offset(w + 10f, h * 0.225f), 8.dp.toPx())
            drawLine(Color.White, Offset(-10f, h * 0.80f), Offset(w + 10f, h * 0.80f), 10.dp.toPx())
            drawLine(Color.White, Offset(w * 0.23f, -10f), Offset(w * 0.23f, h + 10f), 8.dp.toPx())
            drawLine(Color.White, Offset(w * 0.75f, -10f), Offset(w * 0.75f, h + 10f), 8.dp.toPx())

            // Diagonal avenue
            drawLine(Color.White, Offset(-10f, h * 0.95f), Offset(w + 10f, h * 0.10f), 12.dp.toPx())
            drawLine(
                color = MapRoadLine,
                start = Offset(-10f, h * 0.95f),
                end = Offset(w + 10f, h * 0.10f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f),
            )

            // Building footprints
            fun footprint(left: Float, top: Float, width: Float, height: Float) {
                drawRoundRect(
                    color = MapBuilding,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    alpha = 0.8f,
                )
            }
            footprint(w * 0.26f, h * 0.33f, w * 0.105f, 24.dp.toPx())
            footprint(w * 0.40f, h * 0.30f, w * 0.14f, 30.dp.toPx())
            footprint(w * 0.30f, h * 0.52f, w * 0.17f, 28.dp.toPx())
            footprint(w * 0.50f, h * 0.53f, w * 0.12f, 22.dp.toPx())
        }

        // Allowed radius ring
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(130.dp)
                .graphicsLayer { alpha = radiusAlpha },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                val r = size.minDimension / 2f - stroke / 2f
                drawCircle(color = BrandMint.copy(alpha = 0.25f), radius = r)
                drawCircle(
                    color = BrandMint,
                    radius = r,
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                            0f,
                        ),
                    ),
                )
                drawCircle(color = BrandMint.copy(alpha = 0.20f), radius = 38.dp.toPx())
            }
        }

        // Campus pin
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-36).dp)
                .graphicsLayer {
                    alpha = pinDrop
                    translationY = (1f - pinDrop) * -12.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Canvas(
                modifier = Modifier
                    .size(width = 10.dp, height = 7.dp)
                    .offset(y = (-1).dp),
            ) {
                val pointer = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(pointer, PrimaryContainer)
            }
            Box(
                modifier = Modifier
                    .size(width = 10.dp, height = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PrimaryContainer.copy(alpha = 0.30f)),
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 24.dp),
        ) {
            Text(
                text = "CAMPUS CENTRAL",
                color = Outline,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 8.sp,
                letterSpacing = 0.32.sp,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(40.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Mi ubicación",
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CoordinateBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.70f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.33.sp,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = PrimaryContainer,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            letterSpacing = (-0.27).sp,
        )
    }
}

@Composable
private fun RadiusCard(radius: Float, onRadiusChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Radio permitido",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Text(
                text = "${radius.roundToInt()} m",
                color = PrimaryContainer,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 22.sp,
            )
        }

        RadiusSlider(radius = radius, onRadiusChange = onRadiusChange)

        Column(modifier = Modifier.padding(top = 4.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.20f)),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = Color(0xFF004031),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Se actualiza en los celulares sin publicar una nueva versión.",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.24.sp,
                )
            }
        }
    }
}

@Composable
private fun RadiusSlider(radius: Float, onRadiusChange: (Float) -> Unit) {
    var trackWidth by remember { mutableStateOf(0) }
    val fraction = ((radius - 50f) / 450f).coerceIn(0f, 1f)
    val knobRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }

    fun update(x: Float) {
        if (trackWidth > 0) {
            val f = (x / trackWidth).coerceIn(0f, 1f)
            onRadiusChange(((50f + f * 450f) / 5f).roundToInt() * 5f)
        }
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .onSizeChanged { trackWidth = it.width }
                .pointerInput(Unit) {
                    detectTapGestures { offset -> update(offset.x) }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ -> update(change.position.x) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(SurfaceContainerHigh),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.001f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PrimaryContainer),
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (trackWidth * fraction).roundToInt() - knobRadiusPx.roundToInt(),
                            y = 0,
                        )
                    }
                    .size(20.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .border(2.dp, Color.White, CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "50 m",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            )
            Text(
                text = "500 m",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ScheduleCard(
    exigirUbicacion: Boolean,
    onExigirUbicacionChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Horario de pase de lista",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeBox(
                label = "Desde",
                value = "07:30",
                icon = { Icon(Icons.Filled.Schedule, null, tint = PrimaryContainer, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
            TimeBox(
                label = "Hasta",
                value = "09:00",
                icon = { Icon(Icons.Filled.AlarmOn, null, tint = PrimaryContainer, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OutlineVariant.copy(alpha = 0.25f)),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Exigir ubicación para pasar lista",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            AulaSwitch(checked = exigirUbicacion, onCheckedChange = onExigirUbicacionChange)
        }
    }
}

@Composable
private fun TimeBox(label: String, value: String, icon: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow.copy(alpha = 0.80f))
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label.uppercase(),
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.30.sp,
            )
            Text(
                text = value,
                color = PrimaryContainer,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun AulaSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val offset by animateDpAsState(
        targetValue = if (checked) 28.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "switchOffset",
    )

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 28.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) PrimaryContainer else OutlineVariant)
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SaveConfigurationButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "saveCampusScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, RoundedCornerShape(percent = 50), ambientColor = SaveShadow, spotColor = SaveShadow)
            .clip(RoundedCornerShape(percent = 50))
            .background(SaveCoral)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Save,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Guardar configuración",
            color = Color.White,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
    }
}
