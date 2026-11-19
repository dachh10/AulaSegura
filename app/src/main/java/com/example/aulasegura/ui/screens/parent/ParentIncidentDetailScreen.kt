package com.example.aulasegura.ui.screens.parent

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.TertiaryFixedDim

/** 2.7 — Detalle del incidente. Ported from Stitch screen `98e94dd122564074be5ecb0978a219df`. */

private val WarmBorder = Color(0xFFE6E1D8)
private val BodyText = Color(0xFF3A3B44)
private val ThumbTop = Color(0xFFDCE5E1)
private val ThumbBottom = Color(0xFFB8C7C2)
private val AmberBg = Color(0xFFFEF3C7)
private val AmberText = Color(0xFF78350F)

/**
 * One incident, with the evidence the teacher attached.
 *
 * "Enterado" writes `seenByTutorIds` and then stays put: the rules let the tutor
 * add themselves but never remove themselves, so the button is not a toggle.
 */
@Composable
fun ParentIncidentDetailScreen(
    ui: ParentIncidentDetailUiState,
    onBack: () -> Unit,
    onOpenMap: () -> Unit,
    onTalkToTeacher: () -> Unit,
    onAcknowledge: () -> Unit,
    onRetry: () -> Unit,
) {
    val loaded = ui as? ParentIncidentDetailUiState.Loaded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        DetailTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (ui) {
                is ParentIncidentDetailUiState.Loaded -> {
                    Reveal(0) {
                        IncidentEvidence(
                            media = ui.media,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )
                    }
                    Reveal(70) { IncidentHeader(ui) }
                    Reveal(140) { IncidentDataCard(ui = ui, onOpenMap = onOpenMap) }
                    Reveal(210) { DescriptionSection(ui.description) }
                }

                ParentIncidentDetailUiState.Loading -> Reveal(0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = PrimaryContainer)
                    }
                }

                ParentIncidentDetailUiState.NotFound -> Reveal(0) {
                    DetailMessage(
                        title = "Este incidente ya no está disponible",
                        message = "La escuela lo retiró o el enlace está incompleto.",
                    )
                }

                is ParentIncidentDetailUiState.Failed -> Reveal(0) {
                    DetailMessage(
                        title = "No se pudo cargar el incidente",
                        message = ui.message,
                        onRetry = onRetry,
                    )
                }
            }

            loaded?.error?.let { message ->
                Reveal(240) { AcknowledgeError(message) }
            }
        }

        if (loaded != null) {
            IncidentAcknowledgeFooter(
                seen = loaded.seen,
                marking = loaded.marking,
                onTalkToTeacher = onTalkToTeacher,
                onAcknowledge = onAcknowledge,
            )
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (backInteraction, backPress) = rememberPressScale(0.9f)
        Box(
            modifier = backPress
                .size(40.dp)
                .clip(CircleShape)
                .clickable(interactionSource = backInteraction, indication = null, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Text(
            text = "Detalle del incidente",
            modifier = Modifier.weight(1f),
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )

        // An empty square keeps the title centred; the old "more" menu had no
        // action behind it and is gone.
        Spacer(Modifier.size(40.dp))
    }
}

/**
 * The first piece of evidence, or an explicit "no evidence" card.
 *
 * Tapping opens the Cloudinary URL in whatever app handles it: images and videos
 * are served there, so the phone does not need a second viewer.
 */
@Composable
internal fun IncidentEvidence(
    media: List<ParentIncidentMediaUi>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val first = media.firstOrNull()

    Column {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(ThumbTop, ThumbBottom)))
                .clickable(enabled = first != null) {
                    first?.let { item ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (first?.previewUrl != null) {
                AsyncImage(
                    model = first.previewUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = if (first == null) Icons.Outlined.Image else Icons.Outlined.School,
                    contentDescription = null,
                    tint = PrimaryContainer.copy(alpha = 0.22f),
                    modifier = Modifier.size(72.dp),
                )
            }

            if (first == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                ) {
                    Text(
                        text = "Sin evidencia adjunta",
                        color = OnSurface,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, PrimaryContainer.copy(alpha = 0.35f)),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (first.isVideo) Icons.Filled.PlayArrow else Icons.Outlined.Image,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = first.label,
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }

                if (first.isVideo) {
                    val (playInteraction, playPress) = rememberPressScale(0.92f)
                    Box(
                        modifier = playPress
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable(
                                interactionSource = playInteraction,
                                indication = null,
                            ) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(first.url)),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Reproducir evidencia",
                            tint = Color.White,
                            modifier = Modifier
                                .offset(x = 2.dp)
                                .size(30.dp),
                        )
                    }
                }

                first.durationLabel?.let { duration ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
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

        if (media.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${media.size} evidencias · la primera abierta",
                    color = OnSurfaceVariant,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun IncidentHeader(ui: ParentIncidentDetailUiState.Loaded) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(severityBackground(ui.severity))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = ui.severity.label,
                    color = severityForeground(ui.severity),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = incidentFolio(ui.incidentId),
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = ui.title,
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 29.sp,
            letterSpacing = (-0.4).sp,
        )
    }
}

private fun severityBackground(severity: ParentIncidentSeverity): Color = when (severity) {
    ParentIncidentSeverity.Leve -> Color(0xFFFEF3C7)
    ParentIncidentSeverity.Moderado -> SecondaryFixed
    ParentIncidentSeverity.Grave -> Color(0xFFFEE2E2)
    ParentIncidentSeverity.Other -> SurfaceContainer
}

private fun severityForeground(severity: ParentIncidentSeverity): Color = when (severity) {
    ParentIncidentSeverity.Leve -> Color(0xFFB45309)
    ParentIncidentSeverity.Moderado -> Secondary
    ParentIncidentSeverity.Grave -> Color(0xFFDC2626)
    ParentIncidentSeverity.Other -> OnSurfaceVariant
}

@Composable
private fun IncidentDataCard(
    ui: ParentIncidentDetailUiState.Loaded,
    onOpenMap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        DataRow(
            icon = Icons.Outlined.CalendarToday,
            label = "Fecha y hora",
            value = ui.dateLabel,
            mono = true,
        )
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0EDE6))
        DataRow(
            icon = Icons.Outlined.LocationOn,
            label = "Ubicación",
            value = ui.locationLabel,
            secondary = ui.locationDetail ?: ui.coordinatesLabel,
            verified = ui.locationVerified,
            onClick = onOpenMap.takeIf { ui.latitude != null && ui.longitude != null },
        )
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0EDE6))
        DataRow(
            icon = Icons.Outlined.Person,
            label = "Registrado por",
            value = ui.reporterName.ifBlank { "Sin nombre registrado" },
        )
    }
}

@Composable
private fun DataRow(
    icon: ImageVector,
    label: String,
    value: String,
    secondary: String? = null,
    mono: Boolean = false,
    verified: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = OnSurface,
                    fontFamily = if (mono) JetBrainsMono else DmSans,
                    fontWeight = if (mono) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                )
                if (verified) {
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verificada",
                        tint = TertiaryFixedDim,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            secondary?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = it,
                    color = OnSurfaceVariant,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Ver en el mapa",
                tint = OnSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    Column {
        Text(
            text = "Descripción",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = description.ifBlank {
                "La escuela no agregó una descripción a este incidente."
            },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
            color = BodyText,
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
    }
}

@Composable
private fun AcknowledgeError(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmberBg)
            .border(1.dp, AmberText.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = AmberText,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            color = AmberText,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
internal fun IncidentAcknowledgeFooter(
    seen: Boolean,
    marking: Boolean,
    onTalkToTeacher: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (seen) PrimaryContainer else SecondaryContainer,
        animationSpec = tween(220),
        label = "ackBackground",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)),
    ) {
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0EDE6))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (talkInteraction, talkPress) = rememberPressScale(0.97f)
            Row(
                modifier = talkPress
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.5.dp, PrimaryContainer, CircleShape)
                    .clickable(
                        interactionSource = talkInteraction,
                        indication = null,
                        onClick = onTalkToTeacher,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Hablar con el docente",
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }

            val (ackInteraction, ackPress) = rememberPressScale(0.97f)
            Row(
                modifier = ackPress
                    .height(48.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(background)
                    .clickable(
                        enabled = !seen && !marking,
                        interactionSource = ackInteraction,
                        indication = null,
                        onClick = onAcknowledge,
                    )
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = when {
                        seen -> "Enterado"
                        marking -> "Guardando…"
                        else -> "Enterado"
                    },
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DetailMessage(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, WarmBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = title,
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Box(
                modifier = pressModifier
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .clickable(interactionSource = interaction, indication = null, onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Reintentar",
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
