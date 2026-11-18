package com.example.aulasegura.ui.screens.admin

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.AdminGroupBreakdown
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.model.AdminTopAbsenceDay
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
import com.example.aulasegura.util.AttendanceTimes

/** 4.6 — Reportes de Asistencia. Stitch layout kept; every value comes from Firestore. */

private val Hairline = Color(0xFFE6E1D8)
private val Amber = Color(0xFFF59E0B)
private val TrackIndigo = Color(0xFFE8E9F7)
private val StudentCoralBg = Color(0xFFFFE5E1)
private val StudentCoralBorder = Color(0xFFFFDAD4)
private val CardShape = RoundedCornerShape(24.dp)

/** One bar of the status chart, built from a real count in the range summary. */
private data class WeeklyPoint(
    val label: String,
    val value: Int,
    val fraction: Float,
    val highlight: Boolean,
)

private fun statusPoints(values: ReportChartValues): List<WeeklyPoint> {
    val rows = listOf(
        "Presentes" to values.present,
        "Faltas" to values.absent,
        "Retardos" to values.late,
        "Justificadas" to values.justified,
    )
    val max = rows.maxOf { it.second }.coerceAtLeast(1)
    return rows.map { (label, value) ->
        WeeklyPoint(
            label = label,
            value = value,
            fraction = value.toFloat() / max.toFloat(),
            highlight = label == "Faltas" && value > 0,
        )
    }
}

/** One bar of the per-group card, built from a group's summed sessions. */
private data class GroupShare(val name: String, val percent: Int)

private fun groupShares(breakdowns: List<AdminGroupBreakdown>): List<GroupShare> =
    breakdowns.map { GroupShare(name = it.groupLabel, percent = it.attendancePercentage) }

/** One row of the "most absences" list: a real calendar day, never a mock student. */
private data class AbsenceRow(
    val dayNumber: String,
    val dateLabel: String,
    val sessionsLabel: String,
    val absences: Int,
    val coral: Boolean,
)

private fun absenceRows(days: List<AdminTopAbsenceDay>): List<AbsenceRow> =
    days.mapIndexed { index, day ->
        val dateLabel = formatIsoDate(day.date)
        AbsenceRow(
            dayNumber = day.date.takeLast(2).ifBlank { "—" },
            dateLabel = dateLabel,
            sessionsLabel = if (day.sessionCount == 1) "1 sesión" else "${day.sessionCount} sesiones",
            absences = day.absentCount,
            coral = index == 0,
        )
    }

@Composable
fun AdminReportsScreen(
    onDescargar: (AdminReportsUiState) -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    val viewModel: AdminReportsViewModel = viewModel(
        factory = rememberAppContainer().viewModelFactory,
    )
    val uiState by viewModel.state.collectAsState()

    var animated by remember { mutableStateOf(false) }
    val todayIso = remember { AttendanceTimes.isoDate(System.currentTimeMillis()) }
    var rangeMode by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.summary, uiState.fromDate, uiState.toDate) {
        animated = false
        if (uiState.hasData) animated = true
    }

    val summary = uiState.summary
    val ringProgress by animateFloatAsState(
        targetValue = if (animated && summary != null) {
            summary.attendancePercentage / 100f
        } else {
            0f
        },
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
            Reveal(0) { ReportsHeader(onDescargar = { onDescargar(uiState) }) }
            Reveal(60) {
                FilterChipsRow(
                    rangeLabel = rangeLabel(uiState.fromDate, uiState.toDate),
                    sessionsLabel = sessionsChipLabel(summary),
                    attendanceLabel = attendanceChipLabel(summary),
                    onRangeClick = {
                        rangeMode = (rangeMode + 1) % 2
                        val (from, to) = if (rangeMode == 0) {
                            monthRange(todayIso)
                        } else {
                            lastDaysRange(todayIso, 7)
                        }
                        viewModel.setRange(from, to)
                    },
                )
            }

            when {
                uiState.loading -> Reveal(120) { ReportsLoadingCard() }
                uiState.error != null -> Reveal(120) {
                    ReportsErrorCard(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
                }

                uiState.empty -> Reveal(120) {
                    ReportsEmptyCard(range = rangeLabel(uiState.fromDate, uiState.toDate))
                }

                summary != null -> {
                    Reveal(120) { AttendanceSummary(summary = summary, ringProgress = ringProgress) }
                    Reveal(180) {
                        WeeklyChart(animated = animated, values = chartValues(summary))
                    }
                    Reveal(240) {
                        PerGroupCard(
                            animated = animated,
                            shares = groupShares(sortedGroups(uiState.groups)),
                        )
                    }
                    Reveal(300) {
                        AbsencesCard(rows = absenceRows(sortedTopDays(uiState.topDays).take(5)))
                    }
                }
            }
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

private fun sessionsChipLabel(summary: AdminReportRangeSummary?): String =
    if (summary == null) "Sesiones · …" else "${summary.sessionCount} sesiones"

private fun attendanceChipLabel(summary: AdminReportRangeSummary?): String =
    if (summary == null) "Asistencia · …" else "${summary.presentCount} presentes"

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
private fun FilterChipsRow(
    rangeLabel: String,
    sessionsLabel: String,
    attendanceLabel: String,
    onRangeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChipValue(
            value = rangeLabel,
            mono = true,
            onClick = onRangeClick,
            modifier = Modifier.weight(1f),
        )
        FilterChipValue(
            value = sessionsLabel,
            mono = false,
            modifier = Modifier.weight(1f),
        )
        FilterChipValue(
            value = attendanceLabel,
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
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, RoundedCornerShape(percent = 50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
        if (onClick != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ReportsLoadingCard() {
    ReportsStatusCard(text = "Cargando reportes de asistencia…")
}

@Composable
private fun ReportsEmptyCard(range: String) {
    ReportsStatusCard(
        text = "No hay sesiones de asistencia entre $range. " +
            "Cuando se registre una lista, el resumen aparecerá aquí.",
    )
}

@Composable
private fun ReportsErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(ErrorContainer)
            .border(1.dp, Hairline, CardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            color = Error,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        Text(
            text = "Reintentar",
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}

@Composable
private fun ReportsStatusCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, Hairline, CardShape)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AttendanceSummary(summary: AdminReportRangeSummary, ringProgress: Float) {
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
                    text = formatAttendancePercent(summary.presentCount, summary.totalMarks),
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
                .height(112.dp)
                .width(1.dp)
                .background(SurfaceContainer),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SummaryRow(
                dotColor = BrandMint,
                label = "Presentes",
                value = summary.presentCount.toString(),
                valueColor = PrimaryContainer,
            )
            SummaryRow(
                dotColor = BrandCoral,
                label = "Faltas",
                value = summary.absentCount.toString(),
                valueColor = BrandCoral,
            )
            SummaryRow(
                dotColor = Amber,
                label = "Retardos",
                value = summary.lateCount.toString(),
                valueColor = OnSurface,
            )
            SummaryRow(
                dotColor = TrackIndigo,
                label = "Justificadas",
                value = summary.justifiedCount.toString(),
                valueColor = OnSurface,
            )
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
private fun WeeklyChart(animated: Boolean, values: ReportChartValues) {
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
                text = "Desglose de asistencia",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.18).sp,
            )
            Text(
                text = "Total: ${values.total} registros",
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                statusPoints(values).forEach { point ->
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
                text = point.value.toString(),
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
                .width(28.dp)
                .height((96 * barHeight).dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(if (point.highlight) BrandCoral else PrimaryContainer),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = point.label,
            color = if (point.highlight) PrimaryContainer else OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = if (point.highlight) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PerGroupCard(animated: Boolean, shares: List<GroupShare>) {
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
                text = "${shares.size} grupos",
                color = Outline,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shares.forEach { share ->
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
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
private fun AbsencesCard(rows: List<AbsenceRow>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Días con más faltas",
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
            rows.forEachIndexed { index, row ->
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
                    text = row.dayNumber,
                    color = if (row.coral) Color(0xFFAE3123) else PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = row.dateLabel,
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
                        text = row.sessionsLabel,
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
                        text = "Faltas del día",
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
