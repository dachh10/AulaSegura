package com.example.aulasegura.ui.screens.admin

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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** 4.5 — Gestión de Incidentes. Ported 1:1 from the Stitch markup of the incident hub. */

private val LeveBg = Color(0xFFFEF3C7)
private val LeveText = Color(0xFF92400E)
private val VerifiedGreen = Color(0xFF16A34A)
private val EditedAmber = Color(0xFFD97706)
private val TileShadow = Color(0x081B1D3A)
private val CardShadow = Color(0x0A1B1D3A)
private val TileShape = RoundedCornerShape(20.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val ThumbShape = RoundedCornerShape(20.dp)

private enum class Severity { Leve, Moderado, Grave }

private enum class IncidentFilter(val label: String) {
    Todos("Todos"),
    Nuevos("Nuevos"),
    Moderados("Moderados"),
    Graves("Graves"),
}

private enum class MediaKind { Photo, Video, None }

private data class AdminIncident(
    val severity: Severity,
    val nuevo: Boolean,
    val media: MediaKind,
    val mediaBadge: String?,
    val time: String,
    val title: String,
    val student: String,
    val group: String,
    val registeredBy: String,
    val locationVerified: Boolean,
)

private val incidents = listOf(
    AdminIncident(
        severity = Severity.Moderado,
        nuevo = false,
        media = MediaKind.Photo,
        mediaBadge = null,
        time = "Mar 21 oct · 13:05",
        title = "Discusión con compañero",
        student = "Mariana Gómez",
        group = "3° B",
        registeredBy = "Prof. Salinas",
        locationVerified = true,
    ),
    AdminIncident(
        severity = Severity.Leve,
        nuevo = true,
        media = MediaKind.Video,
        mediaBadge = "0:24",
        time = "Jue 30 oct · 11:20",
        title = "Caída en el recreo",
        student = "Mariana Gómez",
        group = "3° B",
        registeredBy = "Prof. Salinas",
        locationVerified = true,
    ),
    AdminIncident(
        severity = Severity.Grave,
        nuevo = true,
        media = MediaKind.Photo,
        mediaBadge = null,
        time = "Lun 27 oct · 12:10",
        title = "Daño al mobiliario",
        student = "Mateo Ríos",
        group = "2° C",
        registeredBy = "Prof. Salinas",
        locationVerified = false,
    ),
    AdminIncident(
        severity = Severity.Leve,
        nuevo = false,
        media = MediaKind.None,
        mediaBadge = null,
        time = "Vie 24 oct · 09:45",
        title = "Desacato de normas",
        student = "Sebastián Vargas",
        group = "1° A",
        registeredBy = "Prof. Salinas",
        locationVerified = false,
    ),
)

@Composable
fun AdminIncidentsScreen(
    onBack: () -> Unit = {},
    onFiltrar: () -> Unit = {},
    onIncidente: (Int) -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
    onMas: () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(IncidentFilter.Todos) }

    val visible = incidents.filter { incident ->
        when (filter) {
            IncidentFilter.Todos -> true
            IncidentFilter.Nuevos -> incident.nuevo
            IncidentFilter.Moderados -> incident.severity == Severity.Moderado
            IncidentFilter.Graves -> incident.severity == Severity.Grave
        }
    }

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
            Reveal(0) { IncidentsHeader(onBack = onBack, onFiltrar = onFiltrar) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(60)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IncidentStat(value = "9", label = "Este mes", valueColor = PrimaryContainer, modifier = Modifier.weight(1f))
                IncidentStat(value = "2", label = "Nuevos", valueColor = Color(0xFFFC6955), modifier = Modifier.weight(1f))
                IncidentStat(value = "1", label = "Graves", valueColor = Error, modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .revealOnEnter(120)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IncidentFilter.entries.forEach { option ->
                    IncidentFilterChip(
                        label = option.label,
                        selected = option == filter,
                        onClick = { filter = option },
                    )
                }
            }

            Column(
                modifier = Modifier.revealOnEnter(180),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                visible.forEachIndexed { index, incident ->
                    IncidentCard(
                        incident = incident,
                        onClick = { onIncidente(index) },
                    )
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
private fun IncidentsHeader(onBack: () -> Unit, onFiltrar: () -> Unit) {
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

        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.40f), CircleShape)
                .clickable(onClick = onFiltrar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filtrar",
                tint = PrimaryContainer,
                modifier = Modifier.size(22.dp),
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
private fun IncidentCard(incident: AdminIncident, onClick: () -> Unit) {
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
            .clickable(interactionSource = source, indication = null, onClick = onClick)
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
                        text = incident.time,
                        color = Outline,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                    )
                }
                Text(
                    text = incident.title,
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
                        text = incident.student,
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
                        text = incident.group,
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
                    text = incident.registeredBy,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (incident.locationVerified) {
                        Icons.Filled.LocationOn
                    } else {
                        Icons.Filled.EditLocationAlt
                    },
                    contentDescription = null,
                    tint = if (incident.locationVerified) VerifiedGreen else EditedAmber,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (incident.locationVerified) "Ubicación verificada" else "Ubicación editada",
                    color = if (incident.locationVerified) VerifiedGreen else EditedAmber,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.33.sp,
                )
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: Severity) {
    val background: Color
    val content: Color
    when (severity) {
        Severity.Leve -> {
            background = LeveBg
            content = LeveText
        }
        Severity.Moderado -> {
            background = SecondaryFixed
            content = OnSecondaryFixedVariant
        }
        Severity.Grave -> {
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
            text = severity.name,
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
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(ThumbShape)
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.20f), ThumbShape),
    ) {
        // Sin Coil en el proyecto: la foto del mockup se representa con un placeholder
        // del design system (mismo encuadre y misma capa de badge).
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = when (incident.media) {
                    MediaKind.Video -> Icons.Filled.PlayArrow
                    MediaKind.Photo -> Icons.Filled.PhotoCamera
                    MediaKind.None -> Icons.Filled.Image
                },
                contentDescription = null,
                tint = OutlineVariant,
                modifier = Modifier.size(30.dp),
            )
        }

        if (incident.media == MediaKind.Video) {
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

        if (incident.mediaBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF31302D).copy(alpha = 0.80f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = incident.mediaBadge,
                    color = Color.White,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                )
            }
        } else if (incident.media == MediaKind.Photo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(PrimaryContainer.copy(alpha = 0.80f))
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
