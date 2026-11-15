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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.RollCallSync
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
 * The shell is the mockup's; the sessions are real (`TeacherHistoryViewModel`).
 * Tapping a row expands it in place instead of opening a new screen: the teacher
 * is looking for one day's roster, and a push/pop would lose the scroll position
 * of the list they were reading.
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
private val JustifiedBg = Color(0xFFEEF2FF)
private val JustifiedText = Color(0xFF2E5AAC)
private val PendingText = Color(0xFFD97706)
private val MutedChipBg = Color(0xFFF1EFEA)
private val MutedChipText = Color(0xFF6B6E8F)

private val historySegments = listOf("Listas", "Incidentes", "Publicaciones")

@Composable
fun TeacherHistoryScreen(
    ui: TeacherHistoryUi,
    onSelectTab: (TeacherTab) -> Unit,
    onRetry: () -> Unit = {},
    onToggleSession: (String) -> Unit = {},
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

            ui.notice?.let { message ->
                Reveal(160) {
                    HistoryNotice(
                        message = message,
                        onRetry = onRetry,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            Reveal(200) {
                if (segment == 0) {
                    when {
                        ui.sections.isEmpty() -> HistoryEmptyState(refreshing = ui.refreshing)
                        else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ui.sections.forEach { section ->
                                HistorySection(
                                    section = section,
                                    onToggleSession = onToggleSession,
                                )
                            }
                        }
                    }
                } else {
                    HistoryPendingTab(label = historySegments[segment])
                }
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
private fun HistoryNotice(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LateBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = PendingText,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = PendingText,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(HistoryPillShape)
                .background(PendingText)
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Reintentar",
                color = Color.White,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun HistoryEmptyState(refreshing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(120.dp)
            .clip(HistoryCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, HistoryHairline, HistoryCardShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (refreshing) {
                "Cargando tu historial…"
            } else {
                "Todavía no hay pases de lista guardados."
            },
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/**
 * Incidentes and Publicaciones still have no data: their blocks are 6, 8 and 9.
 * Saying so is better than an empty list that looks like a bug.
 */
@Composable
private fun HistoryPendingTab(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(120.dp)
            .clip(HistoryCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, HistoryHairline, HistoryCardShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "El historial de $label llega con su bloque de la fase. " +
                "Por ahora solo las listas tienen datos reales.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun HistorySection(
    section: HistorySectionUi,
    onToggleSession: (String) -> Unit,
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
                text = section.label,
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

        section.sessions.forEach { session ->
            SessionCard(
                session = session,
                onToggle = { onToggleSession(session.id) },
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: HistorySessionUi,
    onToggle: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HistoryCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, HistoryHairline, HistoryCardShape),
    ) {
        Row(
            modifier = pressModifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateBlock(day = session.day, weekday = session.weekday)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.title,
                        color = PrimaryContainer,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (session.verified == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verificado con GPS",
                            tint = PresentText.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatChip("${session.present} P", PresentBg, PresentText)
                    StatChip("${session.absent} F", AbsentBg, AbsentText)
                    if (session.late > 0) {
                        StatChip("${session.late} R", LateBg, LateText)
                    } else {
                        StatChip("${session.late} R", MutedChipBg, MutedChipText)
                    }
                    if (session.justified > 0) {
                        StatChip("${session.justified} J", JustifiedBg, JustifiedText)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            SyncPill(sync = session.sync)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (session.expanded) "Cerrar detalle" else "Ver alumnos",
                tint = Outline,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (session.expanded) 90f else 0f),
            )
        }

        if (session.expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.35f)),
            )
            SessionRecords(session = session)
        }
    }
}

@Composable
private fun SessionRecords(session: HistorySessionUi) {
    when {
        session.records.isNotEmpty() -> {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                session.records.forEachIndexed { index, record ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(OutlineVariant.copy(alpha = 0.25f)),
                        )
                    }
                    RecordRow(record = record)
                }
            }
        }

        session.recordsLoading -> RecordsHint("Cargando los alumnos de esta lista…")
        else -> RecordsHint("No hay alumnos guardados de esta lista en el dispositivo.")
    }
}

@Composable
private fun RecordRow(record: HistoryRecordUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = record.number,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            modifier = Modifier.width(26.dp),
        )
        Text(
            text = record.name,
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        record.time?.let { time ->
            Text(
                text = time,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        val (background, contentColor) = when (record.mark) {
            AttendanceMark.PRESENT -> PresentBg to PresentText
            AttendanceMark.ABSENT -> AbsentBg to AbsentText
            AttendanceMark.LATE -> LateBg to LateText
            AttendanceMark.JUSTIFIED -> JustifiedBg to JustifiedText
            null -> MutedChipBg to MutedChipText
        }
        StatChip(
            label = record.mark.label,
            background = background,
            contentColor = contentColor,
        )
    }
}

@Composable
private fun RecordsHint(message: String) {
    Text(
        text = message,
        color = OnSurfaceVariant,
        fontFamily = DmSans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
    )
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
private fun SyncPill(sync: RollCallSync) {
    val synced = sync == RollCallSync.SYNCED
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
            text = if (synced) "Sincronizado" else sync.shortLabel,
            color = contentColor,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = (-0.2).sp,
        )
    }
}

/**
 * The pill is small, so it says exactly where the session is: "Pendiente" for a
 * row still in the outbox, "Enviando" while the worker commits it and "Sin
 * sincronizar" when the last attempt failed. Collapsing those three into one word
 * would hide the difference between "waiting" and "stuck".
 */
private val RollCallSync.shortLabel: String
    get() = when (this) {
        RollCallSync.PENDING -> "Pendiente"
        RollCallSync.SYNCING -> "Enviando"
        RollCallSync.SYNCED -> "Sincronizado"
        RollCallSync.FAILED -> "Sin sincronizar"
    }

private val AttendanceMark?.label: String
    get() = when (this) {
        AttendanceMark.PRESENT -> "Presente"
        AttendanceMark.ABSENT -> "Falta"
        AttendanceMark.LATE -> "Retardo"
        AttendanceMark.JUSTIFIED -> "Justificado"
        null -> "Sin registrar"
    }
