package com.example.aulasegura.ui.screens.teacher

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.AttachmentSyncState
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.MediaUploadPhase
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
    onToggleIncident: (String) -> Unit = {},
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
                when (segment) {
                    0 -> when {
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

                    1 -> when {
                        ui.incidentSections.isEmpty() -> IncidentEmptyState(refreshing = ui.refreshing)
                        else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ui.incidentSections.forEach { section ->
                                IncidentSection(
                                    section = section,
                                    onToggleIncident = onToggleIncident,
                                )
                            }
                        }
                    }

                    else -> when {
                        ui.publications.isEmpty() ->
                            PublicationEmptyState(refreshing = ui.refreshing)

                        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ui.publications.forEach { publication ->
                                PublicationCard(publication = publication)
                            }
                        }
                    }
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
 * One task or notice the teacher published.
 *
 * Deliberately flatter than [SessionCard]: a publication has no roster to expand,
 * so it is a single card with its audience and its sync state. The pill is per
 * row because one publication can be waiting in the outbox while the next one
 * already reached Firestore.
 */
@Composable
private fun PublicationCard(publication: HistoryPublicationUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(HistoryCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, HistoryHairline, HistoryCardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PublicationKindChip(
                label = publication.kindLabel,
                urgent = publication.urgent,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = publication.postedLabel,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                maxLines = 1,
            )
        }

        Text(
            text = publication.title,
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (publication.detail.isNotBlank()) {
            Text(
                text = publication.detail,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // The attached PDF and what happened to it. The teacher asked for this
        // row to be able to confirm, days later, that the file really went up.
        publication.attachmentLabel?.let { label ->
            val tint = when (publication.attachmentState) {
                AttachmentSyncState.UPLOADED -> PresentText
                AttachmentSyncState.FAILED -> AbsentText
                else -> PendingText
            }

            Row(
                modifier = Modifier
                    .clip(HistoryPillShape)
                    .background(SurfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = label,
                    color = tint,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = publication.audience,
                modifier = Modifier.weight(1f),
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = publicationSyncLabel(publication.pending),
                color = if (publication.pending) PendingText else PresentText,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PublicationKindChip(label: String, urgent: Boolean) {
    Text(
        text = label,
        color = if (urgent) AbsentText else MutedChipText,
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        modifier = Modifier
            .clip(HistoryPillShape)
            .background(if (urgent) AbsentBg else MutedChipBg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun PublicationEmptyState(refreshing: Boolean) {
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
                "Buscando tus publicaciones…"
            } else {
                "Aún no has publicado tareas ni avisos. Lo que publiques aparece aquí, " +
                    "también sin conexión."
            },
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun IncidentSection(
    section: IncidentSectionUi,
    onToggleIncident: (String) -> Unit,
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

        section.incidents.forEach { incident ->
            IncidentCard(
                incident = incident,
                onToggle = { onToggleIncident(incident.id) },
            )
        }
    }
}

/**
 * One incident of the teacher's own history.
 *
 * Same shape as [SessionCard] on purpose: the two segments are the same screen,
 * and a teacher should not have to learn a second card to read what they
 * reported.
 */
@Composable
private fun IncidentCard(
    incident: HistoryIncidentUi,
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
            DateBlock(day = incident.day, weekday = incident.weekday)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.studentName,
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = incident.title,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    incident.severity?.let { SeverityChip(label = it) }
                    StatChip(label = incident.status, background = MutedChipBg, contentColor = MutedChipText)
                    if (incident.mediaCount > 0) {
                        StatChip(
                            label = if (incident.mediaCount == 1) "1 foto" else "${incident.mediaCount} fotos",
                            background = MutedChipBg,
                            contentColor = MutedChipText,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            SyncPill(sync = incident.sync)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (incident.expanded) "Cerrar detalle" else "Ver el incidente",
                tint = Outline,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (incident.expanded) 90f else 0f),
            )
        }

        if (incident.expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.35f)),
            )
            IncidentDetail(incident = incident)
        }
    }
}

@Composable
private fun SeverityChip(label: String) {
    val (background, contentColor) = when (label) {
        "Grave" -> AbsentBg to AbsentText
        "Moderado" -> LateBg to LateText
        else -> PresentBg to PresentText
    }
    StatChip(label = label, background = background, contentColor = contentColor)
}

@Composable
private fun IncidentDetail(incident: HistoryIncidentUi) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = incident.description,
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Text(
            text = listOf(incident.category, incident.occurredLabel)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
        incident.locationLabel?.let { label ->
            Text(
                text = label,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }

        if (incident.evidence.isEmpty()) {
            RecordsHint("No hay archivos guardados de este incidente en el dispositivo.")
        } else {
            incident.evidence.forEach { file -> EvidenceRow(evidence = file) }
        }
    }
}

/**
 * One evidence file. Tapping it opens the Cloudinary URL, which is the only copy
 * that exists once the upload finished: the local file was deleted on purpose.
 */
@Composable
private fun EvidenceRow(evidence: HistoryEvidenceUi) {
    val context = LocalContext.current
    val url = evidence.url
    val canOpen = url != null && evidence.phase == MediaUploadPhase.UPLOADED

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HistoryCardShape)
            .clickable(enabled = canOpen) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoCamera,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = evidence.label,
            color = PrimaryContainer,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (canOpen) "Abrir" else mediaPhaseLabel(evidence.phase) ?: "Sin archivo",
            color = if (canOpen) PresentText else PendingText,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
private fun IncidentEmptyState(refreshing: Boolean) {
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
                "Buscando tus incidentes…"
            } else {
                "Aún no has registrado incidentes. Los que reportes aparecen aquí, " +
                    "también sin conexión."
            },
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
