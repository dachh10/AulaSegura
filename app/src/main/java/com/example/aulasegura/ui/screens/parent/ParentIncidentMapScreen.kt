package com.example.aulasegura.ui.screens.parent

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.CampusMap
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
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.TertiaryFixedDim
import com.google.android.gms.maps.model.LatLng

/**
 * 2.8 — Detalle del incidente con mapa.
 * Ported from Stitch screen `65e5f2a843054caa8acc844ef4d74681`.
 *
 * The map draws the coordinate the teacher's phone recorded
 * (`incidents.location`), not a decorative neighbourhood: [CampusMap] centres on
 * the pin when there is no campus to show, and no pin means no map at all.
 */
@Composable
fun ParentIncidentMapScreen(
    ui: ParentIncidentDetailUiState,
    onBack: () -> Unit,
    onTalkToTeacher: () -> Unit,
    onAcknowledge: () -> Unit,
    onRetry: () -> Unit,
) {
    val loaded = ui as? ParentIncidentDetailUiState.Loaded
    val pin = loaded?.let { item ->
        if (item.latitude != null && item.longitude != null) {
            LatLng(item.latitude, item.longitude)
        } else {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        MapTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
                    Reveal(60) { IncidentHeading(ui) }
                    Reveal(120) { LocationSection(ui = ui, pin = pin) }
                    Reveal(180) { DateCard(ui) }
                    Reveal(240) { RegistrarCard(ui) }
                    ui.error?.let { message ->
                        Reveal(280) { MapError(message) }
                    }
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
                    MapMessage(
                        title = "Este incidente ya no está disponible",
                        message = "La escuela lo retiró o el enlace está incompleto.",
                    )
                }

                is ParentIncidentDetailUiState.Failed -> Reveal(0) {
                    MapMessage(
                        title = "No se pudo cargar el incidente",
                        message = ui.message,
                        onRetry = onRetry,
                    )
                }
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
private fun MapTopBar(onBack: () -> Unit) {
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

        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun IncidentHeading(ui: ParentIncidentDetailUiState.Loaded) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SecondaryFixed)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = ui.severity.label,
                    color = Secondary,
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
                letterSpacing = 0.3.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
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

@Composable
private fun LocationSection(ui: ParentIncidentDetailUiState.Loaded, pin: LatLng?) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Lugar del incidente",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.weight(1f))
            if (ui.locationVerified) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TertiaryFixedDim),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Georreferencia verificada",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE6E1D8), RoundedCornerShape(24.dp))
                .padding(6.dp),
        ) {
            if (pin != null) {
                CampusMap(
                    teacherFix = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                    // No campus circle: the tutor's screen does not load the
                    // school profile, and a geofence drawn on a guessed centre
                    // would be a false claim about where the building is.
                    campus = null,
                    pin = pin,
                    pinTitle = ui.title,
                    scrollGesturesEnabled = false,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "La escuela no registró la ubicación",
                            color = OnSurfaceVariant,
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = ui.locationLabel,
                    modifier = Modifier.weight(1f),
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.width(6.dp))
                if (ui.locationVerified) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE6FAF4))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16866F),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Verificada",
                            color = Color(0xFF16866F),
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            Text(
                text = listOfNotNull(ui.coordinatesLabel, ui.locationDetail)
                    .joinToString(" · "),
                modifier = Modifier.padding(start = 33.dp, top = 3.dp, bottom = 8.dp),
                color = OnSurfaceVariant,
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun DateCard(ui: ParentIncidentDetailUiState.Loaded) {
    InfoCard(
        icon = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = PrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        label = "Fecha y hora",
        value = ui.dateLabel,
        mono = true,
    )
}

@Composable
private fun RegistrarCard(ui: ParentIncidentDetailUiState.Loaded) {
    InfoCard(
        icon = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ui.reporterInitials,
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        },
        label = "Registrado por",
        value = ui.reporterName.ifBlank { "Sin nombre registrado" },
    )
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    mono: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE6E1D8), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = OnSurface,
                fontFamily = if (mono) JetBrainsMono else DmSans,
                fontWeight = if (mono) FontWeight.Medium else FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MapError(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFEF3C7))
            .border(1.dp, Color(0xFF78350F).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = Color(0xFF78350F),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            color = Color(0xFF78350F),
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun MapMessage(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE6E1D8), RoundedCornerShape(24.dp))
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
