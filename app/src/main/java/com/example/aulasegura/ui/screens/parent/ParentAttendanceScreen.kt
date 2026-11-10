package com.example.aulasegura.ui.screens.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

private enum class DayState { Present, Current, Absent, Late, Holiday, Justified }

private data class DayCell(val day: Int, val state: DayState)

@Composable
fun ParentAttendanceScreen(
    onJustificar: () -> Unit,
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
            Reveal(0) { AttendanceTopBar() }
            Reveal(60) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    MonthSummaryCard()
                }
            }
            Reveal(120) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    CalendarCard()
                }
            }
            Reveal(180) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    RecentDetail(onJustificar = onJustificar)
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

@Composable
private fun AttendanceTopBar() {
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
        StudentPill(name = "Mariana", grade = "3° B")
    }
}

@Composable
private fun MonthSummaryCard() {
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
                MonthChevron(icon = Icons.Outlined.ChevronLeft)
                Text(
                    text = "Noviembre 2026",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                MonthChevron(icon = Icons.Outlined.ChevronRight)
            }
            ProgressRing(
                progress = 0.94f,
                trackColor = Color(0xFFE6E1D8),
                progressColor = PrimaryContainer,
                size = 48.dp,
                strokeWidth = 3.5.dp,
            ) {
                Text(
                    text = "94%",
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
                value = "18",
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
                value = "1",
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
                value = "1",
                valueColor = AmberDark,
                label = "Retardo",
                dotColor = AmberDot,
            )
        }

        Spacer(Modifier.height(14.dp))
        SegmentedAttendanceBar()
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "20 días hábiles",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
            Text(
                text = "Objetivo mensual: 95%",
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun MonthChevron(icon: ImageVector) {
    val (interaction, pressModifier) = rememberPressScale(0.9f)
    Box(
        modifier = pressModifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Primary,
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
private fun SegmentedAttendanceBar() {
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
            Box(
                modifier = Modifier
                    .weight(90f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(TertiaryContainer),
            )
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(SecondaryContainer),
            )
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(AmberDot),
            )
        }
    }
}

@Composable
private fun CalendarCard() {
    val weeks = listOf(
        listOf(
            DayCell(2, DayState.Present),
            DayCell(3, DayState.Current),
            DayCell(4, DayState.Present),
            DayCell(5, DayState.Present),
            DayCell(6, DayState.Present),
        ),
        listOf(
            DayCell(9, DayState.Present),
            DayCell(10, DayState.Present),
            DayCell(11, DayState.Absent),
            DayCell(12, DayState.Present),
            DayCell(13, DayState.Late),
        ),
        listOf(
            DayCell(16, DayState.Holiday),
            DayCell(17, DayState.Present),
            DayCell(18, DayState.Justified),
            DayCell(19, DayState.Present),
            DayCell(20, DayState.Present),
        ),
        listOf(
            DayCell(23, DayState.Present),
            DayCell(24, DayState.Present),
            DayCell(25, DayState.Present),
            DayCell(26, DayState.Present),
            DayCell(27, DayState.Present),
        ),
    )

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

        weeks.forEachIndexed { index, week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        DayBubble(day = cell.day, state = cell.state)
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
private fun DayBubble(day: Int, state: DayState) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (state == DayState.Current) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, Primary, CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                DayCircle(day = day, state = DayState.Present)
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
        DayState.Present, DayState.Current -> {
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
        DayState.Holiday -> {
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
private fun RecentDetail(onJustificar: () -> Unit) {
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

        RecentRecord(
            weekday = "Lun",
            day = "03",
            title = "Día completo",
            subtitle = "Entrada 07:52",
            subtitleIcon = Icons.Outlined.Schedule,
            subtitleColor = TertiaryContainer,
            dayColor = Primary,
            pillText = "Presente · 07:52",
            pillIcon = Icons.Filled.CheckCircle,
            pillBackground = TertiaryFixed.copy(alpha = 0.3f),
            pillForeground = TertiaryContainer,
            pillBorder = TertiaryFixed.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(10.dp))

        AbsentRecord(onJustificar = onJustificar)
        Spacer(Modifier.height(10.dp))

        RecentRecord(
            weekday = "Jue",
            day = "30",
            title = "Día completo",
            subtitle = "Tolerancia máx: 08:00",
            subtitleIcon = Icons.Outlined.Schedule,
            subtitleColor = AmberDark,
            dayColor = AmberText,
            pillText = "Retardo · 08:21",
            pillIcon = Icons.Outlined.Timer,
            pillBackground = AmberBg,
            pillForeground = AmberPillText,
            pillBorder = AmberPillBorder,
        )
        Spacer(Modifier.height(10.dp))

        RecentRecord(
            weekday = "Mié",
            day = "29",
            title = "Día completo",
            subtitle = "Aprobado por Dirección",
            subtitleIcon = Icons.Filled.Verified,
            subtitleColor = PurpleText,
            dayColor = PurpleText,
            pillText = "Justificada",
            pillIcon = Icons.Filled.Verified,
            pillBackground = PurpleBg,
            pillForeground = PurplePillText,
            pillBorder = PurplePillBorder,
        )
    }
}

@Composable
private fun RecentRecord(
    weekday: String,
    day: String,
    title: String,
    subtitle: String,
    subtitleIcon: ImageVector,
    subtitleColor: Color,
    dayColor: Color,
    pillText: String,
    pillIcon: ImageVector,
    pillBackground: Color,
    pillForeground: Color,
    pillBorder: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateBadge(weekday = weekday, day = day, dayColor = dayColor)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = subtitleIcon,
                    contentDescription = null,
                    tint = subtitleColor,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = subtitle,
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
            text = pillText,
            icon = pillIcon,
            background = pillBackground,
            foreground = pillForeground,
            border = pillBorder,
        )
    }
}

@Composable
private fun AbsentRecord(onJustificar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateBadge(weekday = "Vie", day = "31", dayColor = Secondary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Día completo",
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "31 de octubre",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
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
            JustifyButton(onClick = onJustificar)
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
