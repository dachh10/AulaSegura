package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Tune
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
import com.example.aulasegura.ui.components.TeacherBottomBar
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * Teacher — Historial. Ported from Stitch screen `cbe12103d8494161b9e19735c062de66`
 * ("Historial - AulaSegura", project 7677408035297541086).
 *
 * Visual shell only: the segmented control keeps its selected state, the month
 * arrows are decorative and the list is sample data.
 */

private val HistoryCardShape = RoundedCornerShape(24.dp)
private val HistoryPillShape = RoundedCornerShape(percent = 50)
private val HistoryHairline = OutlineVariant.copy(alpha = 0.4f)

private val PresentBg = Color(0xFFE6F7F0)
private val PresentText = Color(0xFF0E7C55)
private val AbsentBg = Color(0xFFFFEBE8)
private val AbsentText = Color(0xFFDE350B)
private val LateBg = Color(0xFFFFF8E6)
private val LateText = Color(0xFFB78103)
private val PendingText = Color(0xFFD97706)

private data class AttendanceRecord(
    val day: String,
    val weekday: String,
    val group: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val synced: Boolean,
)

private val thisWeekRecords = listOf(
    AttendanceRecord("03", "LUN", "3° A · Matemáticas", 28, 1, 1, true),
    AttendanceRecord("03", "LUN", "2° C · Matemáticas", 26, 1, 0, false),
)

private val lastWeekRecords = listOf(
    AttendanceRecord("31", "VIE", "3° B · Matemáticas", 27, 0, 1, true),
    AttendanceRecord("30", "JUE", "3° A · Matemáticas", 29, 1, 0, true),
)

private val historySegments = listOf("Listas", "Incidentes", "Publicaciones")

@Composable
fun TeacherHistoryScreen(
    onSelectTab: (TeacherTab) -> Unit,
) {
    var segment by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bottom = 108.dp),
        ) {
            Reveal(0) { HistoryHeader() }
            Reveal(60) {
                HistorySegments(
                    selected = segment,
                    onSelect = { segment = it },
                )
            }
            Reveal(120) { MonthSelector() }
            Reveal(180) {
                HistorySection(
                    label = "ESTA SEMANA",
                    records = thisWeekRecords,
                )
            }
            Spacer(Modifier.height(16.dp))
            Reveal(240) {
                HistorySection(
                    label = "SEMANA ANTERIOR",
                    records = lastWeekRecords,
                )
            }
        }

        TeacherBottomBar(
            active = TeacherTab.Historial,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Historial",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.36).sp,
        )
        val (interaction, pressModifier) = rememberPressScale(0.95f)
        Box(
            modifier = pressModifier
                .size(44.dp)
                .clip(HistoryPillShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.5f), HistoryPillShape)
                .clickable(interactionSource = interaction, indication = null, onClick = {}),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filtrar",
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun HistorySegments(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(44.dp)
            .clip(HistoryPillShape)
            .background(SurfaceContainer)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), HistoryPillShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        historySegments.forEachIndexed { index, label ->
            HistorySegment(
                label = label,
                selected = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HistorySegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) PrimaryContainer else Color.Transparent,
        label = "historySegmentBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) SurfaceContainerLowest else OnSurfaceVariant,
        label = "historySegmentContent",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(HistoryPillShape)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun MonthSelector() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowLeft)
        Text(
            text = "Noviembre 2026",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.16).sp,
        )
        MonthArrow(Icons.AutoMirrored.Filled.KeyboardArrowRight)
    }
}

@Composable
private fun MonthArrow(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val (interaction, pressModifier) = rememberPressScale(0.9f)
    Box(
        modifier = pressModifier
            .size(32.dp)
            .clip(HistoryPillShape)
            .clickable(interactionSource = interaction, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HistorySection(
    label: String,
    records: List<AttendanceRecord>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.55.sp,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.3f)),
            )
        }

        records.forEach { record ->
            AttendanceCard(record = record)
        }
    }
}

@Composable
private fun AttendanceCard(record: AttendanceRecord) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .clip(HistoryCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, HistoryHairline, HistoryCardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = {})
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateBlock(day = record.day, weekday = record.weekday)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.group,
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatChip("${record.present} P", PresentBg, PresentText)
                StatChip("${record.absent} F", AbsentBg, AbsentText)
                if (record.late > 0) {
                    StatChip("${record.late} R", LateBg, LateText)
                } else {
                    StatChip("${record.late} R", SurfaceContainer, OnSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        SyncPill(synced = record.synced)
    }
}

@Composable
private fun DateBlock(
    day: String,
    weekday: String,
) {
    Column(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = day,
            color = PrimaryContainer,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            letterSpacing = (-0.32).sp,
        )
        Text(
            text = weekday,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    background: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(HistoryPillShape)
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = (-0.22).sp,
        )
    }
}

@Composable
private fun SyncPill(synced: Boolean) {
    val background = if (synced) PresentBg.copy(alpha = 0.6f) else LateBg
    val borderColor = if (synced) PresentText.copy(alpha = 0.2f) else PendingText.copy(alpha = 0.3f)
    val contentColor = if (synced) PresentText else PendingText

    Row(
        modifier = Modifier
            .clip(HistoryPillShape)
            .background(background)
            .border(1.dp, borderColor, HistoryPillShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (synced) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (synced) "Sincronizado" else "Pendiente",
            color = contentColor,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = (-0.2).sp,
        )
    }
}
