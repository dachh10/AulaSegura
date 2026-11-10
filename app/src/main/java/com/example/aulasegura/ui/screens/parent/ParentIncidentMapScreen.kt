package com.example.aulasegura.ui.screens.parent

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.IncidentMap
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
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.TertiaryFixedDim

/**
 * 2.8 — Detalle del incidente con mapa.
 * Ported from Stitch screen `65e5f2a843054caa8acc844ef4d74681`.
 */
@Composable
fun ParentIncidentMapScreen(
    onBack: () -> Unit,
    onTalkToTeacher: () -> Unit,
) {
    var acknowledged by remember { mutableStateOf(false) }

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
            Reveal(0) { MapEvidence() }
            Reveal(60) { IncidentHeading() }
            Reveal(120) { LocationSection() }
            Reveal(180) { DateCard() }
            Reveal(240) { RegistrarCard() }
        }

        MapFooter(
            acknowledged = acknowledged,
            onTalkToTeacher = onTalkToTeacher,
            onAcknowledge = { acknowledged = !acknowledged },
        )
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

        val (moreInteraction, morePress) = rememberPressScale(0.9f)
        Box(
            modifier = morePress
                .size(40.dp)
                .clip(CircleShape)
                .clickable(interactionSource = moreInteraction, indication = null, onClick = {}),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "Más opciones",
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MapEvidence() {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFDCE5E1), Color(0xFFB8C7C2)),
                        ),
                    ),
            )
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = PrimaryContainer.copy(alpha = 0.18f),
                modifier = Modifier.size(72.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, PrimaryContainer.copy(alpha = 0.5f)),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.94f))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = SecondaryContainer,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "00:18",
                    color = OnSurface,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }

            val (playInteraction, playPress) = rememberPressScale(0.92f)
            Box(
                modifier = playPress
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    .clickable(interactionSource = playInteraction, indication = null, onClick = {}),
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer),
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC7C5D2)),
            )
        }
    }
}

@Composable
private fun IncidentHeading() {
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
                    text = "Moderado",
                    color = Secondary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "FOLIO #INC-4829",
                color = Color(0xFF8A8EA8),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Discusión con compañero",
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
private fun LocationSection() {
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
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TertiaryFixedDim),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Georreferencia activa",
                color = Color(0xFF8A8EA8),
                fontFamily = DmSans,
                fontSize = 11.sp,
            )
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
            IncidentMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF6B6E8F),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Pasillo 2° piso · Dentro del plantel",
                    modifier = Modifier.weight(1f),
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.width(6.dp))
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

            Text(
                text = "19.2926, -99.6557",
                modifier = Modifier.padding(start = 33.dp, top = 3.dp, bottom = 8.dp),
                color = Color(0xFF8A8EA8),
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun DateCard() {
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
        value = "Mar 21 oct 2026 · 13:05",
        mono = true,
        trailing = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainerLow)
                    .border(1.dp, Color(0xFFE6E1D8), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "Turno Mat.",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 10.sp,
                )
            }
        },
    )
}

@Composable
private fun RegistrarCard() {
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
                    text = "RS",
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                )
            }
        },
        label = "Registrado por",
        value = "Prof. Ricardo Salinas",
        trailing = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PrimaryContainer)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "Docente titular",
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                )
            }
        },
    )
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    mono: Boolean = false,
    trailing: @Composable () -> Unit,
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
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun MapFooter(
    acknowledged: Boolean,
    onTalkToTeacher: () -> Unit,
    onAcknowledge: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (acknowledged) PrimaryContainer else SecondaryContainer,
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
                    text = "Enterado",
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
