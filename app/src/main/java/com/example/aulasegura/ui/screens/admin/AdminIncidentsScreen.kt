package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.incidentThumbnailUrl

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.AdminIncident
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.IncidentStatus
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.ErrorContainer
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnErrorContainer
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSecondaryFixedVariant
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 4.5 — Bandeja de incidentes. Stitch layout restored; every value comes from Firestore. */

private val LeveBg = Color(0xFFFEF3C7)
private val LeveText = Color(0xFF92400E)
private val VerifiedGreen = Color(0xFF16A34A)
private val EditedAmber = Color(0xFFD97706)
private val TileShadow = Color(0x081B1D3A)
private val CardShadow = Color(0x0A1B1D3A)
private val TileShape = RoundedCornerShape(20.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val ThumbShape = RoundedCornerShape(20.dp)

/** Chips are filters: null means "Todos" and applies no clause. */
private val severityOptions = listOf(
    null to "Todos",
    IncidentSeverity.LEVE to "Leve",
    IncidentSeverity.MODERADO to "Moderado",
    IncidentSeverity.GRAVE to "Grave",
)

private val statusOptions = listOf(
    null to "Todos",
    IncidentStatus.NUEVO to "Nuevos",
    IncidentStatus.REVISADO to "Revisados",
    IncidentStatus.CERRADO to "Cerrados",
)

@Composable
fun AdminIncidentsScreen(
    onBack: () -> Unit = {},
    onIncidente: (Int) -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    val viewModel: AdminIncidentsViewModel = viewModel(
        factory = rememberAppContainer().viewModelFactory,
    )
    val uiState by viewModel.state.collectAsState()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    val stats = incidentStats(uiState.incidents)

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
        ) {
            Reveal(0) { IncidentsHeader(onBack = onBack) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(60)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IncidentStat(
                    value = stats.total.toString(),
                    label = "Este mes",
                    valueColor = PrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                IncidentStat(
                    value = stats.news.toString(),
                    label = "Nuevos",
                    valueColor = Color(0xFFFC6955),
                    modifier = Modifier.weight(1f),
                )
                IncidentStat(
                    value = stats.grave.toString(),
                    label = "Graves",
                    valueColor = Error,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(120)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                severityOptions.forEach { (severity, label) ->
                    IncidentFilterChip(
                        label = label,
                        selected = uiState.severityFilter == severity,
                        onClick = { viewModel.setSeverityFilter(severity) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(140)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                statusOptions.forEach { (status, label) ->
                    IncidentFilterChip(
                        label = label,
                        selected = uiState.statusFilter == status,
                        onClick = { viewModel.setStatusFilter(status) },
                    )
                }
            }

            if (uiState.actionError != null) {
                Reveal(160) {
                    IncidentsErrorCard(
                        message = uiState.actionError.orEmpty(),
                        onRetry = viewModel::clearActionError,
                        retryLabel = "Entendido",
                    )
                }
            }

            when {
                uiState.loading -> Reveal(180) { IncidentsStatusCard("Cargando incidentes…") }
                uiState.error != null -> Reveal(180) {
                    IncidentsErrorCard(
                        message = uiState.error.orEmpty(),
                        onRetry = viewModel::refresh,
                    )
                }

                uiState.empty -> Reveal(180) {
                    IncidentsStatusCard(
                        "No hay incidentes registrados para este plantel. " +
                            "Cuando un docente reporte uno, aparecerá aquí.",
                    )
                }

                uiState.visibleIncidents.isEmpty() -> Reveal(180) {
                    IncidentsStatusCard("Ningún incidente coincide con los filtros elegidos.")
                }

                else -> Column(
                    modifier = Modifier.revealOnEnter(180),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.visibleIncidents.forEach { incident ->
                        IncidentCard(
                            incident = incident,
                            expanded = expandedId == incident.id,
                            updating = uiState.updating,
                            onToggle = {
                                expandedId = if (expandedId == incident.id) null else incident.id
                            },
                            onStatusChange = { status -> viewModel.setStatus(incident.id, status) },
                        )
                    }
                }
            }
        }

        AdminBottomBar(
            active = AdminTab.Mas,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> onPanel()
                    AdminTab.Usuarios -> onUsuarios()
                    AdminTab.Grupos -> onGrupos()
                    AdminTab.Reportes -> onReportes()
                    AdminTab.Mas -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun IncidentsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = PrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Incidentes",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.56).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

    }
}

@Composable
private fun IncidentStat(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .shadow(4.dp, TileShape, ambientColor = TileShadow, spotColor = TileShadow)
            .clip(TileShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), TileShape)
            .padding(12.dp),
    ) {
        Text(
            text = value,
            color = valueColor,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.44).sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun IncidentFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "incidentChipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(percent = 50))
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryContainer else OutlineVariant.copy(alpha = 0.40f),
                shape = RoundedCornerShape(percent = 50),
            )
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) OnPrimary else OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun IncidentsStatusCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CardShape)
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
private fun IncidentsErrorCard(
    message: String,
    onRetry: () -> Unit,
    retryLabel: String = "Reintentar",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(ErrorContainer)
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CardShape)
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
            text = retryLabel,
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}

@Composable
private fun IncidentCard(
    incident: AdminIncident,
    expanded: Boolean,
    updating: Boolean,
    onToggle: () -> Unit,
    onStatusChange: (IncidentStatus) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.99f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "incidentCardScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(6.dp, CardShape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CardShape)
            .clickable(interactionSource = source, indication = null, onClick = onToggle)
            .padding(14.dp),
    ) {
        Row {
            IncidentThumbnail(incident = incident)
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    SeverityChip(severity = incident.severity)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatIncidentDate(incident.occurredAt),
                        color = Outline,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                    )
                }
                Text(
                    text = incident.title.ifBlank { "Incidente sin título" },
                    color = OnSurface,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = incident.studentName.ifBlank { "Sin nombre" },
                        color = Outline,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = " · ",
                        color = Outline,
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = incident.groupId,
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(OutlineVariant.copy(alpha = 0.30f)),
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Registró: ",
                    color = Outline,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Text(
                    text = incident.reportedByName.ifBlank { "—" },
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val verified = incident.location?.verified == true
                Icon(
                    imageVector = if (verified) Icons.Filled.LocationOn else Icons.Filled.EditLocationAlt,
                    contentDescription = null,
                    tint = if (verified) VerifiedGreen else EditedAmber,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (verified) "Ubicación verificada" else "Ubicación editada",
                    color = if (verified) VerifiedGreen else EditedAmber,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.33.sp,
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OutlineVariant.copy(alpha = 0.30f)),
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Estado: ${incident.statusLabel} · ${incident.severityLabel}",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            DetailRows(rows = incidentDetailRows(incident))

            if (incident.status != IncidentStatus.CERRADO) {
                val next = if (incident.status == IncidentStatus.NUEVO) {
                    IncidentStatus.REVISADO
                } else {
                    IncidentStatus.CERRADO
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusChangeLabel(incident.status, next),
                    color = PrimaryContainer,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = !updating) { onStatusChange(next) },
                )
            }
        }
    }
}

@Composable
private fun DetailRows(rows: List<IncidentDetailRow>) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = row.label,
                    color = Outline,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.value,
                        color = OnSurface,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (row.isMedia) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Abrir",
                            color = PrimaryContainer,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(row.mediaUrl)),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: IncidentSeverity) {
    val background: Color
    val content: Color
    when (severity) {
        IncidentSeverity.LEVE -> {
            background = LeveBg
            content = LeveText
        }
        IncidentSeverity.MODERADO -> {
            background = SecondaryFixed
            content = OnSecondaryFixedVariant
        }
        IncidentSeverity.GRAVE -> {
            background = ErrorContainer
            content = OnErrorContainer
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = severity.label,
            color = content,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun IncidentThumbnail(incident: AdminIncident) {
    val hasVideo = incident.media.any { it.type.equals("video", ignoreCase = true) }

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(ThumbShape)
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.20f), ThumbShape),
    ) {
        // The first image is the incident's thumbnail, like in the family view;
        // the icon stays as a fallback for videos and rows without evidence.
        val imageUrl = incidentThumbnailUrl(incident.media)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = "Evidencia del incidente",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = when {
                        hasVideo -> Icons.Filled.PlayArrow
                        incident.hasMedia -> Icons.Filled.PhotoCamera
                        else -> Icons.Filled.Image
                    },
                    contentDescription = null,
                    tint = OutlineVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        if (hasVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Primary.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (incident.photoCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PrimaryContainer.copy(alpha = 0.80f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = incident.photoCount.toString(),
                    color = Color.White,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}
