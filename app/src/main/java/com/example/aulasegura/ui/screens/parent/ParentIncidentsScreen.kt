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
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.StudentPill
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.PrimaryContainer

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
private val GreenCheck = Color(0xFF10B981)
private val GreenStat = Color(0xFF059669)

private enum class Severity(val label: String, val background: Color, val foreground: Color) {
    Leve("Leve", AmberBg, AmberText),
    Moderado("Moderado", RedBg, RedText),
}

private data class Incident(
    val id: Int,
    val title: String,
    val date: String,
    val severity: Severity,
    val location: String,
    val seen: Boolean,
    val icon: ImageVector,
    val iconTint: Color,
    val thumbnailBackground: Color,
    val isVideo: Boolean = false,
    val videoDuration: String? = null,
)

private fun sampleIncidents(): List<Incident> = listOf(
    Incident(
        id = 1,
        title = "Caída en el recreo",
        date = "Jue 30 oct · 11:20",
        severity = Severity.Leve,
        location = "Patio Central · Verificada",
        seen = false,
        icon = Icons.Outlined.Park,
        iconTint = Color(0xFF2E7D5B),
        thumbnailBackground = Color(0xFFE3F1EA),
    ),
    Incident(
        id = 2,
        title = "Discusión con compañero",
        date = "Mar 21 oct · 13:05",
        severity = Severity.Moderado,
        location = "Pasillo 2° piso · Verificada",
        seen = true,
        icon = Icons.Outlined.Videocam,
        iconTint = PrimaryContainer,
        thumbnailBackground = Color(0xFFE8E9F7),
        isVideo = true,
        videoDuration = "0:24",
    ),
    Incident(
        id = 3,
        title = "Material escolar dañado",
        date = "Lun 6 oct · 09:40",
        severity = Severity.Leve,
        location = "Taller de arte · Verificada",
        seen = true,
        icon = Icons.Outlined.Palette,
        iconTint = Color(0xFFB45309),
        thumbnailBackground = Color(0xFFFDF2E9),
    ),
    Incident(
        id = 4,
        title = "Mochila extraviada",
        date = "Lun 29 sep · 10:10",
        severity = Severity.Leve,
        location = "Comedor · Verificada",
        seen = true,
        icon = Icons.Outlined.Backpack,
        iconTint = Color(0xFF464651),
        thumbnailBackground = Color(0xFFF0EDE9),
    ),
)

@Composable
fun ParentIncidentsScreen(
    onBack: () -> Unit,
    onOpenIncident: () -> Unit,
) {
    var seenIds by remember { mutableStateOf(emptySet<Int>()) }
    val incidents = remember { sampleIncidents() }

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
            Reveal(0) { IncidentsTopBar(onBack = onBack) }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Reveal(60) { IncidentStats() }
                Reveal(120) { VerifiedNote() }
                Reveal(180) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        incidents.forEach { incident ->
                            IncidentCard(
                                incident = incident,
                                seen = incident.seen || incident.id in seenIds,
                                onClick = {
                                    seenIds = seenIds + incident.id
                                    onOpenIncident()
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun IncidentsTopBar(onBack: () -> Unit) {
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
            StudentPill(name = "Mariana", grade = "3° B")
            Text(
                text = "4 registros",
                color = InkSecondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun IncidentStats() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            value = "4",
            label = "Registrados",
            valueColor = InkPrimary,
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value = "1",
            label = "Nuevo",
            valueColor = Coral,
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value = "0",
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
            text = "Evidencia, fecha y ubicación verificadas por la escuela.",
            color = InkSecondary,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun IncidentCard(
    incident: Incident,
    seen: Boolean,
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
                        text = incident.date,
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
            if (seen) {
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
                    text = incident.location,
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
private fun SeverityChip(severity: Severity) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(severity.background)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) {
        Text(
            text = severity.label,
            color = severity.foreground,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun IncidentThumbnail(incident: Incident) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(incident.thumbnailBackground),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color.Transparent, incident.iconTint.copy(alpha = 0.10f)),
                    ),
                ),
        )
        Icon(
            imageVector = incident.icon,
            contentDescription = null,
            tint = incident.iconTint.copy(alpha = 0.85f),
            modifier = Modifier.size(30.dp),
        )

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
