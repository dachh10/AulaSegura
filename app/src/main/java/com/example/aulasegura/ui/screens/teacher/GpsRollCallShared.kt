package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnboardingHeading
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

internal val GpsHairline = Color(0xFFE6E1D8)
internal val GpsCoral = Color(0xFFFF6B57)
internal val GpsMint = Color(0xFF5ED3B0)
internal val GpsSuccess = Color(0xFF2E9E6B)
internal val GpsDanger = Color(0xFFE5484D)
internal val GpsAmber = Color(0xFFF2A93B)
internal val GpsAmberDeep = Color(0xFFB45309)
internal val GpsIndigo = Color(0xFF2B2F77)
internal val GpsMuted = Color(0xFF6B6E8F)
internal val GpsCream = Color(0xFFFBF8F3)

internal enum class AttendanceState { Present, Late, Absent, Pending }

internal data class RollCallEntry(
    val number: String,
    val name: String,
    val initials: String,
    val avatarBackground: Color,
    val avatarForeground: Color,
    val status: AttendanceState = AttendanceState.Pending,
    val time: String? = null,
)

internal fun demoRoster(): List<RollCallEntry> = listOf(
    RollCallEntry("01", "Ana Sofía Aguilar", "AS", Color(0xFFE8E9F7), GpsIndigo, AttendanceState.Present, "07:54"),
    RollCallEntry("02", "Bruno Castillo", "BC", Color(0xFFFFEAE5), Color(0xFFAE3123), AttendanceState.Late, "07:58"),
    RollCallEntry("03", "Camila Domínguez", "CD", Color(0xFFFFDAD6), Color(0xFFB91C1C), AttendanceState.Pending),
    RollCallEntry("04", "Diego Escobar", "DE", Color(0xFFFFF0D4), GpsAmberDeep, AttendanceState.Present, "08:02"),
    RollCallEntry("05", "Elena Fuentes", "EF", Color(0xFFF3E8FF), Color(0xFF7E22CE), AttendanceState.Pending),
    RollCallEntry("06", "Mariana Gómez", "MG", Color(0xFFCCFBF1), Color(0xFF0F766E), AttendanceState.Present, "07:51"),
)

internal fun AttendanceState.label(time: String?): String = when (this) {
    AttendanceState.Present -> if (time != null) "Presente $time" else "Presente"
    AttendanceState.Late -> "Retardo (+12 min)"
    AttendanceState.Absent -> "Ausencia justificada"
    AttendanceState.Pending -> "Sin marcar"
}

internal fun AttendanceState.color(): Color = when (this) {
    AttendanceState.Present -> GpsSuccess
    AttendanceState.Late -> Color(0xFFD97706)
    AttendanceState.Absent -> Color(0xFFE11D48)
    AttendanceState.Pending -> GpsMuted
}

// ---------------------------------------------------------------- métricas

internal data class MetricSpec(
    val icon: ImageVector,
    val count: String,
    val label: String,
    val accent: Color,
    val container: Color,
    val border: Color,
)

internal fun metricSpecs(): List<MetricSpec> = listOf(
    MetricSpec(Icons.Filled.CheckCircle, "24", "Presentes", Color(0xFF065F46), Color(0xFFECFDF5), Color(0xFF34D399)),
    MetricSpec(Icons.Filled.Cancel, "2", "Faltas", Color(0xFF9F1239), Color(0xFFFFF1F2), Color(0xFFFB7185)),
    MetricSpec(Icons.Filled.Schedule, "1", "Retardos", GpsAmberDeep, Color(0xFFFFFBEB), Color(0xFFFBBF24)),
    MetricSpec(Icons.Filled.RadioButtonUnchecked, "1", "Pendiente", Color(0xFF57534E), Color(0xFFF5F5F4), Color(0xFFD6D3D1)),
)

/** Píldoras horizontales del mockup de mapa. */
@Composable
internal fun MetricPillRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        metricSpecs().forEach { spec ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(27.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(spec.container)
                    .border(1.dp, spec.border.copy(alpha = 0.55f), RoundedCornerShape(percent = 50)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = spec.accent,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${spec.label} ${spec.count}",
                    color = spec.accent,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Tarjetas de métrica en cuadrícula del mockup offline. */
@Composable
internal fun MetricCardGrid(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        metricSpecs().forEach { spec ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(spec.container)
                    .border(1.dp, spec.border.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = spec.accent,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = spec.count,
                        color = spec.accent,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = spec.label,
                    color = spec.accent,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- alumnos

@Composable
internal fun AttendanceButtons(
    selected: AttendanceState?,
    onSelect: (AttendanceState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatusCircle(
            icon = Icons.Filled.Check,
            description = "Marcar presente",
            activeColor = GpsSuccess,
            active = selected == AttendanceState.Present,
            onClick = { onSelect(AttendanceState.Present) },
        )
        StatusCircle(
            icon = Icons.Filled.Schedule,
            description = "Marcar retardo",
            activeColor = GpsAmber,
            active = selected == AttendanceState.Late,
            onClick = { onSelect(AttendanceState.Late) },
        )
        StatusCircle(
            icon = Icons.Filled.Close,
            description = "Marcar falta",
            activeColor = GpsDanger,
            active = selected == AttendanceState.Absent,
            onClick = { onSelect(AttendanceState.Absent) },
        )
    }
}

@Composable
private fun StatusCircle(
    icon: ImageVector,
    description: String,
    activeColor: Color,
    active: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.92f)

    Box(
        modifier = Modifier
            .size(32.dp)
            .then(pressModifier)
            .clip(CircleShape)
            .background(if (active) activeColor else SurfaceContainerLowest)
            .border(1.dp, if (active) activeColor else GpsHairline, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) Color.White else Color(0xFF9CA3AF),
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
internal fun StudentAttendanceRow(
    entry: RollCallEntry,
    onSelect: (AttendanceState) -> Unit,
    showNumberColumn: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val rowTint = when (entry.status) {
        AttendanceState.Absent -> Color(0xFFFFF1F2)
        AttendanceState.Late -> Color(0xFFFFFBEB)
        AttendanceState.Present -> GpsMint.copy(alpha = 0.06f)
        AttendanceState.Pending -> Color.Transparent
    }
    val statusColor = entry.status.color()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(rowTint)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showNumberColumn) {
            Text(
                text = entry.number,
                color = Color(0xFF9CA3AF),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.width(22.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(entry.avatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = entry.initials,
                color = entry.avatarForeground,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                color = Color(0xFF1F2937),
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = entry.status.label(entry.time),
                    color = statusColor,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        AttendanceButtons(
            selected = entry.status,
            onSelect = onSelect,
        )
    }
}

@Composable
internal fun RosterCard(
    entries: List<RollCallEntry>,
    onSelect: (index: Int, status: AttendanceState) -> Unit,
    modifier: Modifier = Modifier,
    showNumberColumn: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, GpsHairline, RoundedCornerShape(24.dp)),
    ) {
        entries.forEachIndexed { index, entry ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF0EDE9)),
                )
            }
            StudentAttendanceRow(
                entry = entry,
                onSelect = { onSelect(index, it) },
                showNumberColumn = showNumberColumn,
            )
        }
    }
}

// ---------------------------------------------------------------- botones

@Composable
internal fun CoralCta(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)
    val container = if (enabled) GpsCoral else Color(0xFFE2E0D8)
    val contentColor = if (enabled) Color.White else Color(0xFF8C8A85)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(pressModifier)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(percent = 50),
                ambientColor = GpsCoral.copy(alpha = 0.30f),
                spotColor = GpsCoral.copy(alpha = 0.30f),
            )
            .clip(RoundedCornerShape(percent = 50))
            .background(container)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = contentColor,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun RollCallHeaderPill(groupId: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(percent = 50))
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceContainerLowest.copy(alpha = 0.95f))
            .border(1.dp, GpsHairline, RoundedCornerShape(percent = 50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(GpsMint),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Pase de lista · $groupId",
            color = GpsIndigo,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun GesturePill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(96.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(OnboardingHeading.copy(alpha = 0.30f)),
    )
}

/** Marco redondeado blanco reutilizado por los controles flotantes del mapa. */
@Composable
internal fun MapCircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = OnboardingHeading,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val (interaction, pressModifier) = rememberPressScale(0.94f)

    Box(
        modifier = Modifier
            .size(size)
            .then(pressModifier)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, GpsHairline, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
