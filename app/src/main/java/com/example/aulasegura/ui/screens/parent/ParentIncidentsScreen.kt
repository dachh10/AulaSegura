package com.example.aulasegura.ui.screens.parent

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.StudentPill
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainer

/** 2.6 — Incidentes Escolares. Ported from Stitch screen `6ae883e957924dd0a00d08578915749b`. */

private val InkPrimary = Color(0xFF1B1D3A)
private val InkSecondary = Color(0xFF6B6E8F)
private val WarmBorder = Color(0xFFE6E1D8)
private val HairLine = Color(0xFFF0EDE6)
private val Coral = Color(0xFFFF6B57)
private val AmberBg = Color(0xFFFEF3C7)
private val AmberText = Color(0xFFB45309)
private val RedBg = Color(0xFFFEE2E2)
private val RedText = Color(0xFFDC2626)
private val GraveBg = Color(0xFFFCA5A5)
private val GraveText = Color(0xFF7F1D1D)
private val NeutralBg = Color(0xFFF0EDE9)
private val NeutralText = Color(0xFF464651)
private val GreenCheck = Color(0xFF10B981)
private val GreenStat = Color(0xFF059669)
private val ThumbTint = Color(0xFFE8E9F7)

/**
 * The incidents the school registered about the child on screen.
 *
 * A card is "Nuevo" until the tutor acknowledges it in the detail; the
 * acknowledgement is a real write to `seenByTutorIds`, so the state survives a
 * reinstall and is shared by every device of the family.
 */
@Composable
fun ParentIncidentsScreen(
    ui: ParentIncidentsUiState,
    onBack: () -> Unit,
    onOpenIncident: (String) -> Unit,
    onCycleChild: () -> Unit,
    onRetry: () -> Unit,
) {
    // Coming back from the detail must repaint "Visto por ti". ON_START also fires
    // on the first composition, so the first event is skipped and only returns
    // (or a trip to the background) trigger a read.
    val lifecycleOwner = LocalLifecycleOwner.current
    val refresh = rememberUpdatedState(onRetry)
    DisposableEffect(lifecycleOwner) {
        var firstStart = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (firstStart) firstStart = false else refresh.value()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f,
            color = PrimaryContainer,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            when (ui) {
                is ParentIncidentsUiState.Loaded -> {
                    Reveal(0) {
                        IncidentsTopBar(
                            childName = ui.childName,
                            groupLabel = ui.groupLabel,
                            childCount = ui.childCount,
                            total = ui.total,
                            onBack = onBack,
                            onCycleChild = onCycleChild,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                    ) {
                        Reveal(60) {
                            IncidentStats(
                                total = ui.total,
                                newCount = ui.newCount,
                                graveCount = ui.graveCount,
                            )
                        }
                        Reveal(120) { VerifiedNote() }
                        if (ui.incidents.isEmpty()) {
                            Reveal(180) { EmptyIncidents() }
                        } else {
                            Reveal(180) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    ui.incidents.forEach { incident ->
                                        IncidentCard(
                                            incident = incident,
                                            onClick = { onOpenIncident(incident.incidentId) },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                }

                else -> {
                    Reveal(0) { IncidentsTopBar(onBack = onBack) }
                    Reveal(60) {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            IncidentsPlaceholder(ui = ui, onRetry = onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentsTopBar(
    childName: String? = null,
    groupLabel: String = "",
    childCount: Int = 0,
    total: Int = 0,
    onBack: () -> Unit,
    onCycleChild: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (interaction, pressModifier) = rememberPressScale(0.9f)
            Box(
                modifier = pressModifier
                    .size(44.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, WarmBorder, CircleShape)
                    .clickable(interactionSource = interaction, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Incidentes",
                color = InkPrimary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (childName != null) {
                StudentPill(
                    name = childName,
                    grade = groupLabel,
                    onClick = if (childCount > 1) onCycleChild else ({}),
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Text(
                text = when (total) {
                    0 -> "Sin registros"
                    1 -> "1 registro"
                    else -> "$total registros"
                },
                color = InkSecondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun IncidentStats(total: Int, newCount: Int, graveCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            value = total.toString(),
            label = "Registrados",
            valueColor = InkPrimary,
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value = newCount.toString(),
            label = "Nuevo",
            valueColor = Coral,
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value = graveCount.toString(),
            label = "Graves",
            valueColor = GreenStat,
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    value: String,
    label: String,
    valueColor: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 24.sp,
        )
        Text(
            text = label,
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun VerifiedNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Evidencia, fecha y ubicación registradas por la escuela.",
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun IncidentCard(
    incident: ParentIncidentRowUi,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Column(
        modifier = pressModifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
    ) {
        Row {
            IncidentThumbnail(incident = incident)
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .height(88.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    SeverityChip(severity = incident.severity)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = incident.title,
                        color = InkPrimary,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 21.sp,
                        letterSpacing = (-0.2).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = incident.dateLabel,
                        color = InkSecondary,
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = HairLine)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (incident.seen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = GreenCheck,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Visto por ti",
                        color = InkSecondary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.5.sp,
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Coral),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Nuevo",
                        color = Coral,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = incident.locationLabel,
                    color = InkSecondary,
                    fontFamily = DmSans,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: ParentIncidentSeverity) {
    val (background, foreground) = when (severity) {
        ParentIncidentSeverity.Leve -> AmberBg to AmberText
        ParentIncidentSeverity.Moderado -> RedBg to RedText
        ParentIncidentSeverity.Grave -> GraveBg to GraveText
        ParentIncidentSeverity.Other -> NeutralBg to NeutralText
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Text(
            text = severity.label,
            color = foreground,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun IncidentThumbnail(incident: ParentIncidentRowUi) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ThumbTint),
        contentAlignment = Alignment.Center,
    ) {
        if (incident.thumbnailUrl != null) {
            AsyncImage(
                model = incident.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = categoryIcon(incident.category),
                contentDescription = null,
                tint = PrimaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.size(30.dp),
            )
        }

        if (incident.isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir video",
                    tint = OnPrimary,
                    modifier = Modifier
                        .offset(x = 1.dp)
                        .size(16.dp),
                )
            }
            incident.videoDuration?.let { duration ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = duration,
                        color = OnPrimary,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

/** The category decides the icon when the incident has no evidence to show. */
private fun categoryIcon(category: String) = when (category) {
    "caida" -> Icons.Outlined.Park
    "discusion" -> Icons.Outlined.Forum
    "danio" -> Icons.Outlined.Palette
    "desacato" -> Icons.Outlined.Person
    else -> Icons.Outlined.Warning
}

@Composable
private fun EmptyIncidents() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, WarmBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sin incidentes registrados",
            color = InkPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Si la escuela reporta algo sobre tu hijo, aparecerá aquí.",
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun IncidentsPlaceholder(ui: ParentIncidentsUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        when (ui) {
            ParentIncidentsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryContainer)
            }

            ParentIncidentsUiState.NoChildren -> {
                Text(
                    text = "Aún no hay hijos vinculados",
                    color = InkPrimary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pide a la escuela el código del alumno y regístrate de nuevo " +
                        "para ver sus incidentes.",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            is ParentIncidentsUiState.Failed -> {
                Text(
                    text = "No se pudieron cargar",
                    color = InkPrimary,
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
                Spacer(Modifier.height(14.dp))
                val (interaction, pressModifier) = rememberPressScale(0.95f)
                Box(
                    modifier = pressModifier
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onRetry,
                        )
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Reintentar",
                        color = Color.White,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }

            is ParentIncidentsUiState.Loaded -> Unit
        }
    }
}
