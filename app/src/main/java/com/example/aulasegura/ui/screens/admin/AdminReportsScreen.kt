package com.example.aulasegura.ui.screens.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.ErrorContainer
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 4.6 — Reportes de Asistencia. Ported 1:1 from the Stitch markup of the attendance report. */

private val Hairline = Color(0xFFE6E1D8)
private val Amber = Color(0xFFF59E0B)
private val TrackIndigo = Color(0xFFE8E9F7)
private val StudentCoralBg = Color(0xFFFFE5E1)
private val StudentCoralBorder = Color(0xFFFFDAD4)
private val CardShape = RoundedCornerShape(24.dp)

private data class WeeklyPoint(val label: String, val percent: Int, val fraction: Float, val highlight: Boolean)

private val weekly = listOf(
    WeeklyPoint("Sem 1", 92, 0.82f, false),
    WeeklyPoint("Sem 2", 94, 0.94f, true),
    WeeklyPoint("Sem 3", 91, 0.78f, false),
    WeeklyPoint("Sem 4", 93, 0.86f, false),
)

private data class GroupShare(val name: String, val percent: Int)

private val groupShares = listOf(
    GroupShare("3° B", 94),
    GroupShare("3° A", 91),
    GroupShare("2° C", 96),
)

private data class AbsenceRow(
    val initials: String,
    val name: String,
    val group: String,
    val shift: String,
    val absences: Int,
    val coral: Boolean,
)

private val absences = listOf(
    AbsenceRow("CD", "Camila Domínguez", "3° B", "Matutino", 5, true),
    AbsenceRow("MR", "Mateo Ríos", "2° C", "Matutino", 4, false),
)

@Composable
fun AdminReportsScreen(
    onDescargar: () -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val ringProgress by animateFloatAsState(
        targetValue = if (animated) 0.93f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "reportRing",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) { ReportsHeader(onDescargar = onDescargar) }
            Reveal(60) { FilterChipsRow() }
            Reveal(120) { AttendanceSummary(ringProgress = ringProgress) }
            Reveal(180) { WeeklyChart(animated = animated) }
            Reveal(240) { PerGroupCard(animated = animated) }
            Reveal(300) { AbsencesCard() }
        }

        AdminBottomBar(
            active = AdminTab.Reportes,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> onPanel()
                    AdminTab.Usuarios -> onUsuarios()
                    AdminTab.Grupos -> onGrupos()
                    AdminTab.Reportes -> Unit
                    AdminTab.Mas -> onMas()
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ReportsHeader(onDescargar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Reportes",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.56).sp,
            )
            Text(
                text = "Asistencia institucional · Ciclo 2026",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.33.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, CircleShape)
                .clickable(onClick = onDescargar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Descargar reporte",
                tint = PrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun FilterChipsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipValue(
            value = "Noviembre 2026",
            mono = true,
            modifier = Modifier.weight(1f),
        )
        FilterChipValue(
            value = "Todos los grupos",
            mono = false,
            modifier = Modifier.weight(1f),
        )
        FilterChipValue(
            value = "Todos",
            mono = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilterChipValue(
    value: String,
    mono: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(percent = 50))
            .clickable { }
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            color = OnSurface,
            fontFamily = if (mono) JetBrainsMono else DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AttendanceSummary(ringProgress: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(116.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = BrandMint.copy(alpha = 0.20f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = BrandMint,
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "93%",
                    color = PrimaryContainer,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Asistencia\ngeneral",
                    color = Outline,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .height(96.dp)
                .width(1.dp)
                .background(SurfaceContainer),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryRow(dotColor = BrandMint, label = "Presentes", value = "1,420", valueColor = PrimaryContainer)
            SummaryRow(dotColor = BrandCoral, label = "Faltas", value = "74", valueColor = BrandCoral)
            SummaryRow(dotColor = Amber, label = "Retardos", value = "31", valueColor = OnSurface)
        }
    }
}

@Composable
private fun SummaryRow(
    dotColor: Color,
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun WeeklyChart(animated: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Asistencia por semana",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.18).sp,
            )
            Text(
                text = "Promedio: 92.5%",
                color = Outline,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
            ) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                val ys = listOf(0f, size.height / 2f, size.height)
                ys.forEachIndexed { index, y ->
                    drawLine(
                        color = Hairline.copy(alpha = if (index == 2) 0.9f else 0.55f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                weekly.forEach { point ->
                    WeeklyBar(point = point, animated = animated, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WeeklyBar(point: WeeklyPoint, animated: Boolean, modifier: Modifier = Modifier) {
    val barHeight by animateFloatAsState(
        targetValue = if (animated) point.fraction else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "barHeight",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (point.highlight) {
                val transition = rememberInfiniteTransition(label = "pulse")
                val pulse by transition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulseAlpha",
                )
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrandCoral.copy(alpha = pulse)),
                )
            }
            Text(
                text = "${point.percent}%",
                color = if (point.highlight) BrandCoral else PrimaryContainer,
                fontFamily = JetBrainsMono,
                fontWeight = if (point.highlight) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(32.dp)
                .height((96 * barHeight).dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(if (point.highlight) BrandCoral else PrimaryContainer),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = point.label,
            color = if (point.highlight) PrimaryContainer else OnSurfaceVariant,
            fontFamily = JetBrainsMono,
            fontWeight = if (point.highlight) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun PerGroupCard(animated: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Por grupo",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.18).sp,
            )
            Text(
                text = "Ver desglose",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.33.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable { }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            groupShares.forEach { share ->
                val progress by animateFloatAsState(
                    targetValue = if (animated) share.percent / 100f else 0f,
                    animationSpec = tween(durationMillis = 700),
                    label = "groupProgress",
                )
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = share.name,
                            color = PrimaryContainer,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                        )
                        Text(
                            text = "${share.percent}%",
                            color = PrimaryContainer,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(TrackIndigo),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(10.dp)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(PrimaryContainer),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AbsencesCard() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Alumnos con más faltas",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.18).sp,
            )
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = BrandCoral,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, Hairline, CardShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            absences.forEachIndexed { index, row ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SurfaceContainer),
                    )
                }
                AbsenceRowItem(row)
            }
        }
    }
}

@Composable
private fun AbsenceRowItem(row: AbsenceRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (row.coral) StudentCoralBg else TrackIndigo)
                    .border(
                        1.dp,
                        if (row.coral) StudentCoralBorder else Color(0xFFE0E0FF),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.initials,
                    color = if (row.coral) Color(0xFFAE3123) else PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = row.name,
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Grupo ${row.group}",
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.24.sp,
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Outline),
                    )
                    Text(
                        text = row.shift,
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        letterSpacing = 0.24.sp,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(ErrorContainer)
                .border(1.dp, Error.copy(alpha = 0.10f), RoundedCornerShape(percent = 50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "${row.absences} faltas",
                color = Error,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = (-0.11).sp,
            )
        }
    }
}
