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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OutlineVariant
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

@Composable
fun ParentIncidentDetailScreen(
    onBack: () -> Unit,
    onTalkToTeacher: () -> Unit,
    onOpenMap: () -> Unit,
) {
    var acknowledged by remember { mutableStateOf(false) }

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
            Reveal(0) { EvidenceCarousel() }
            Reveal(70) { IncidentHeader() }
            Reveal(140) { IncidentDataCard(onOpenMap = onOpenMap) }
            Reveal(210) { DescriptionSection() }
        }

        DetailFooter(
            acknowledged = acknowledged,
            onTalkToTeacher = onTalkToTeacher,
            onAcknowledge = { acknowledged = !acknowledged },
        )
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
private fun EvidenceCarousel() {
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
                    .background(Brush.linearGradient(listOf(ThumbTop, ThumbBottom))),
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
                            listOf(Color.Transparent, PrimaryContainer.copy(alpha = 0.55f)),
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
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Evidencia 1",
                    color = OnPrimary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                )
            }

            val (playInteraction, playPress) = rememberPressScale(0.92f)
            Box(
                modifier = playPress
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f))
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
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(OutlineVariant),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(OutlineVariant),
            )
        }
    }
}

@Composable
private fun IncidentHeader() {
    Column {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SecondaryFixed)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Secondary),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Moderado",
                color = Secondary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
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
private fun IncidentDataCard(onOpenMap: () -> Unit) {
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
            value = "Mar 21 oct 2026 · 13:05",
            mono = true,
        )
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0EDE6))
        DataRow(
            icon = Icons.Outlined.LocationOn,
            label = "Ubicación",
            value = "Patio principal · Dentro del plantel",
            verified = true,
            onClick = onOpenMap,
        )
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF0EDE6))
        DataRow(
            icon = Icons.Outlined.Person,
            label = "Registrado por",
            value = "Prof. Ricardo Salinas",
        )
    }
}

@Composable
private fun DataRow(
    icon: ImageVector,
    label: String,
    value: String,
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
        }
    }
}

@Composable
private fun DescriptionSection() {
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
            text = "Durante el receso, Mariana y un compañero de su grupo tuvieron una " +
                "discusión por el uso de la cancha. La docente de guardia intervino de " +
                "inmediato y ambos estudiantes se encuentran bien. Se registró la " +
                "evidencia del pasillo y se notificó a las familias para dar seguimiento " +
                "conjunto.",
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
private fun DetailFooter(
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
