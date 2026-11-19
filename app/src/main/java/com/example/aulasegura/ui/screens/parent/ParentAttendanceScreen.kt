package com.example.aulasegura.ui.screens.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.ParentAttendanceCounts
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.ProgressRing
import com.example.aulasegura.ui.components.StudentPill
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryContainer
import com.example.aulasegura.ui.theme.TertiaryFixed
import com.example.aulasegura.ui.theme.TertiaryFixedDim

/** 2.2 — Asistencia. Ported from Stitch screen `9f38f3246cab47ebb7de406444a03096`. */

private val AmberBg = Color(0xFFFEF3C7)
private val AmberText = Color(0xFFB45309)
private val AmberDot = Color(0xFFFBBF24)
private val AmberDark = Color(0xFFD97706)
private val AmberPillText = Color(0xFF92400E)
private val AmberPillBorder = Color(0xFFFDE68A)
private val PurpleBg = Color(0xFFF3E8FF)
private val PurpleText = Color(0xFF7E22CE)
private val PurpleDot = Color(0xFFC084FC)
private val PurplePillText = Color(0xFF6B21A8)
private val PurplePillBorder = Color(0xFFE9D5FF)

private enum class DayState { Present, Absent, Late, NoRecord, Justified }

private fun stateOf(cell: ParentCalendarDay): DayState = when (cell.mark) {
    AttendanceMark.PRESENT -> DayState.Present
    AttendanceMark.LATE -> DayState.Late
    AttendanceMark.ABSENT -> DayState.Absent
    AttendanceMark.JUSTIFIED -> DayState.Justified
    null -> DayState.NoRecord
}

@Composable
fun ParentAttendanceScreen(
    ui: ParentAttendanceUiState,
    onJustificar: (ParentAttendanceRecordUi) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCycleChild: () -> Unit,
    onSelectTab: (ParentTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
    ) {
        AttendanceShieldWatermark(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 104.dp),
        ) {
            when (ui) {
                is ParentAttendanceUiState.Loaded -> {
                    Reveal(0) {
                        AttendanceTopBar(
                            childName = ui.childName,
                            groupLabel = ui.groupLabel,
                            childCount = ui.childCount,
                            onCycleChild = onCycleChild,
                        )
                    }
                    Reveal(60) {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            MonthSummaryCard(
                                ui = ui,
                                onPreviousMonth = onPreviousMonth,
                                onNextMonth = onNextMonth,
                            )
                        }
                    }
                    Reveal(120) {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            CalendarCard(weeks = ui.weeks)
                        }
                    }
                    Reveal(180) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            RecentDetail(records = ui.recent, onJustificar = onJustificar)
                        }
                    }
                }

                else -> {
                    Reveal(0) { AttendanceTopBar() }
                    Reveal(60) {
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            AttendancePlaceholder(ui)
                        }
                    }
                }
            }
        }

        ParentBottomBar(
            active = ParentTab.Asistencia,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Loading, no linked child, or a failed read.
 *
 * A month without sessions is not an error and is rendered by the calendar
 * itself; this card only covers what stops the month from existing at all.
 */
@Composable
private fun AttendancePlaceholder(ui: ParentAttendanceUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        when (ui) {
            ParentAttendanceUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryContainer)
            }

            ParentAttendanceUiState.NoChildren -> {
                Text(
                    text = "Aún no hay hijos vinculados",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pide a la escuela el código del alumno y regístrate de nuevo para ver su asistencia.",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            is ParentAttendanceUiState.Failed -> {
                Text(
                    text = "No se pudo cargar",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = ui.message,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            is ParentAttendanceUiState.Loaded -> Unit
        }
    }
}

@Composable
private fun AttendanceTopBar(
    childName: String? = null,
    groupLabel: String = "",
    childCount: Int = 0,
    onCycleChild: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Asistencia",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.3).sp,
            )
            Text(
                text = "Ciclo Escolar 2026-2027",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        if (childName != null) {
            StudentPill(
                name = childName,
                grade = groupLabel,
                // One child means there is nothing to switch to; the pill keeps
                // its look but stops pretending to be a selector.
                onClick = if (childCount > 1) onCycleChild else ({}),
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(
    ui: ParentAttendanceUiState.Loaded,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthChevron(
                    icon = Icons.Outlined.ChevronLeft,
                    onClick = onPreviousMonth,
                )
                Text(
                    text = ui.monthLabel,
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                MonthChevron(
                    icon = Icons.Outlined.ChevronRight,
                    enabled = ui.canGoNext,
                    onClick = onNextMonth,
                )
            }
            ProgressRing(
                progress = (ui.percent ?: 0) / 100f,
                trackColor = Color(0xFFE6E1D8),
                progressColor = PrimaryContainer,
                size = 48.dp,
                strokeWidth = 3.5.dp,
            ) {
                Text(
                    text = ui.percent?.let { "$it%" } ?: "—",
                    color = Primary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            StatBlock(
                modifier = Modifier.weight(1f),
                value = ui.counts.present.toString(),
                valueColor = TertiaryContainer,
                label = "Presentes",
                dotColor = TertiaryFixedDim,
            )
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(1.dp)
                    .background(SurfaceContainer),
            )
            StatBlock(
                modifier = Modifier.weight(1f),
                value = ui.counts.absent.toString(),
                valueColor = Secondary,
                label = "Falta",
                dotColor = SecondaryContainer,
            )
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(1.dp)
                    .background(SurfaceContainer),
            )
            StatBlock(
                modifier = Modifier.weight(1f),
                value = ui.counts.late.toString(),
                valueColor = AmberDark,
                label = "Retardo",
                dotColor = AmberDot,
            )
        }

        Spacer(Modifier.height(14.dp))
        SegmentedAttendanceBar(counts = ui.counts)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${ui.classDays} días hábiles",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
            Text(
                text = "${ui.counts.total} registros",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun MonthChevron(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.9f)
    Box(
        modifier = pressModifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Primary else Outline.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StatBlock(
    modifier: Modifier,
    value: String,
    valueColor: Color,
    label: String,
    dotColor: Color,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SegmentedAttendanceBar(counts: ParentAttendanceCounts) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(SurfaceContainer)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (counts.total == 0) {
                // No records yet: one neutral track instead of a bar that would
                // read as a perfect month.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(SurfaceContainer),
                )
            } else {
                Segment(counts.present, counts.total, TertiaryContainer)
                Segment(counts.late, counts.total, AmberDot)
                Segment(counts.absent, counts.total, SecondaryContainer)
                Segment(counts.justified, counts.total, PurpleDot)
            }
        }
    }
}

@Composable
private fun RowScope.Segment(value: Int, total: Int, color: Color) {
    if (value <= 0) return

    Box(
        modifier = Modifier
            .weight(value.toFloat())
            .fillMaxHeight()
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun CalendarCard(weeks: List<List<ParentCalendarDay?>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf("L", "M", "M", "J", "V").forEach { letter ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = letter,
                        color = Primary,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)
        Spacer(Modifier.height(10.dp))

        if (weeks.isEmpty()) {
            Text(
                text = "Sin días de clase este mes",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        weeks.forEachIndexed { index, week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (cell == null) {
                            // Leading and trailing blanks keep the columns aligned
                            // with the real weekday of the month.
                            Spacer(Modifier.size(32.dp))
                        } else {
                            DayBubble(
                                day = cell.day,
                                state = stateOf(cell),
                                isCurrent = cell.isCurrent,
                            )
                        }
                    }
                }
            }
            if (index != weeks.lastIndex) {
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LegendItem("Presente", TertiaryFixedDim)
            LegendItem("Falta", SecondaryContainer)
            LegendItem("Retardo", AmberDot)
            LegendItem("Justificada", PurpleDot)
        }
    }
}

@Composable
private fun DayBubble(day: Int, state: DayState, isCurrent: Boolean) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Primary, CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                DayCircle(day = day, state = state)
            }
        } else {
            DayCircle(day = day, state = state)
        }
    }
}

@Composable
private fun DayCircle(day: Int, state: DayState) {
    val background: Color
    val foreground: Color
    val weight: FontWeight
    when (state) {
        DayState.Present -> {
            background = TertiaryFixed.copy(alpha = 0.35f)
            foreground = TertiaryContainer
            weight = FontWeight.SemiBold
        }
        DayState.Absent -> {
            background = SecondaryFixed
            foreground = Secondary
            weight = FontWeight.Bold
        }
        DayState.Late -> {
            background = AmberBg
            foreground = AmberText
            weight = FontWeight.Bold
        }
        DayState.NoRecord -> {
            background = SurfaceContainer
            foreground = Outline
            weight = FontWeight.Normal
        }
        DayState.Justified -> {
            background = PurpleBg
            foreground = PurpleText
            weight = FontWeight.Bold
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            color = foreground,
            fontFamily = JetBrainsMono,
            fontWeight = weight,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun LegendItem(label: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun RecentDetail(
    records: List<ParentAttendanceRecordUi>,
    onJustificar: (ParentAttendanceRecordUi) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Detalle reciente",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
            Text(
                text = "Últimas sesiones",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = "Sin sesiones registradas este mes",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                )
            }
        } else {
            // Only the newest absence offers the justify action: a row of buttons
            // that do not say which class they target would be worse than none.
            val justifyIndex = records.indexOfFirst { it.mark == AttendanceMark.ABSENT }
            records.forEachIndexed { index, record ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                if (index == justifyIndex) {
                    AbsentRecord(record = record, onJustificar = onJustificar)
                } else {
                    RecentRecord(record = record)
                }
            }
        }
    }
}

/** Colours and icons the pill and the subtitle pick for one mark. */
private data class RecordStyle(
    val subtitleIcon: ImageVector,
    val subtitleColor: Color,
    val dayColor: Color,
    val pillIcon: ImageVector,
    val pillBackground: Color,
    val pillForeground: Color,
    val pillBorder: Color,
)

private fun styleFor(record: ParentAttendanceRecordUi): RecordStyle = when (record.mark) {
    AttendanceMark.PRESENT -> RecordStyle(
        subtitleIcon = Icons.Outlined.Schedule,
        subtitleColor = TertiaryContainer,
        dayColor = Primary,
        pillIcon = Icons.Filled.CheckCircle,
        pillBackground = TertiaryFixed.copy(alpha = 0.3f),
        pillForeground = TertiaryContainer,
        pillBorder = TertiaryFixed.copy(alpha = 0.5f),
    )
    AttendanceMark.LATE -> RecordStyle(
        subtitleIcon = Icons.Outlined.Schedule,
        subtitleColor = AmberDark,
        dayColor = AmberText,
        pillIcon = Icons.Outlined.Timer,
        pillBackground = AmberBg,
        pillForeground = AmberPillText,
        pillBorder = AmberPillBorder,
    )
    AttendanceMark.ABSENT -> RecordStyle(
        subtitleIcon = Icons.Filled.Cancel,
        subtitleColor = Secondary,
        dayColor = Secondary,
        pillIcon = Icons.Filled.Cancel,
        pillBackground = SecondaryFixed,
        pillForeground = Secondary,
        pillBorder = Color(0xFFFFB4A8),
    )
    AttendanceMark.JUSTIFIED -> RecordStyle(
        subtitleIcon = Icons.Filled.Verified,
        subtitleColor = PurpleText,
        dayColor = PurpleText,
        pillIcon = Icons.Filled.Verified,
        pillBackground = PurpleBg,
        pillForeground = PurplePillText,
        pillBorder = PurplePillBorder,
    )
    null -> RecordStyle(
        subtitleIcon = Icons.Outlined.Schedule,
        subtitleColor = OnSurfaceVariant,
        dayColor = OnSurfaceVariant,
        pillIcon = Icons.Outlined.Schedule,
        pillBackground = SurfaceContainer,
        pillForeground = OnSurfaceVariant,
        pillBorder = OutlineVariant,
    )
}

@Composable
private fun RecentRecord(record: ParentAttendanceRecordUi) {
    val style = styleFor(record)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateBadge(weekday = record.weekday, day = record.day, dayColor = style.dayColor)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = record.title,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = style.subtitleIcon,
                    contentDescription = null,
                    tint = style.subtitleColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = record.subtitle,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(
            text = record.pillText,
            icon = style.pillIcon,
            background = style.pillBackground,
            foreground = style.pillForeground,
            border = style.pillBorder,
        )
    }
}

@Composable
private fun AbsentRecord(
    record: ParentAttendanceRecordUi,
    onJustificar: (ParentAttendanceRecordUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateBadge(weekday = record.weekday, day = record.day, dayColor = Secondary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = record.subtitle,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(
                text = "Falta",
                icon = Icons.Filled.Cancel,
                background = SecondaryFixed,
                foreground = Secondary,
                border = Color(0xFFFFB4A8),
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 1.dp, color = SurfaceContainer)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Requiere comprobante médico o nota",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            JustifyButton(onClick = { onJustificar(record) })
        }
    }
}

@Composable
private fun JustifyButton(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Row(
        modifier = pressModifier
            .clip(CircleShape)
            .background(SecondaryContainer)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = OnPrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Justificar",
            color = OnPrimary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun DateBadge(weekday: String, day: String, dayColor: Color) {
    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = weekday.uppercase(),
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 10.sp,
        )
        Text(
            text = day,
            color = dayColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    border: Color,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = foreground,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun AttendanceShieldWatermark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 360f
        val sy = size.height / 800f
        val paint = Primary.copy(alpha = 0.06f)
        val minScale = minOf(sx, sy)
        val outline = Stroke(
            width = 3f * minScale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        val topShield = Path().apply {
            moveTo(180f * sx, 40f * sy)
            cubicTo(240f * sx, 40f * sy, 310f * sx, 50f * sy, 310f * sx, 120f * sy)
            cubicTo(310f * sx, 270f * sy, 230f * sx, 360f * sy, 180f * sx, 400f * sy)
            cubicTo(130f * sx, 360f * sy, 50f * sx, 270f * sy, 50f * sx, 120f * sy)
            cubicTo(50f * sx, 50f * sy, 120f * sx, 40f * sy, 180f * sx, 40f * sy)
            close()
        }
        drawPath(topShield, paint, style = outline)

        val check = Path().apply {
            moveTo(140f * sx, 180f * sy)
            lineTo(170f * sx, 210f * sy)
            lineTo(230f * sx, 140f * sy)
        }
        drawPath(
            check,
            paint,
            style = Stroke(
                width = 4f * minScale,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        val bottomShield = Path().apply {
            moveTo(180f * sx, 440f * sy)
            cubicTo(240f * sx, 440f * sy, 310f * sx, 450f * sy, 310f * sx, 520f * sy)
            cubicTo(310f * sx, 670f * sy, 230f * sx, 760f * sy, 180f * sx, 800f * sy)
            cubicTo(130f * sx, 760f * sy, 50f * sx, 670f * sy, 50f * sx, 520f * sy)
            cubicTo(50f * sx, 450f * sy, 120f * sx, 440f * sy, 180f * sx, 440f * sy)
            close()
        }
        drawPath(bottomShield, paint, style = outline)
    }
}
