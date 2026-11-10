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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 4.9 — Más (Administración). Ported 1:1 from the Stitch hub mockup. */

private val CardShape = RoundedCornerShape(28.dp)
private val CardBorder = OutlineVariant.copy(alpha = 0.50f)

private data class MoreEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val badge: String?,
    val onClick: () -> Unit,
)

@Composable
fun AdminMoreScreen(
    onCampus: () -> Unit = {},
    onIncidents: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onProfile: () -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
) {
    val entries = listOf(
        MoreEntry(
            icon = Icons.Filled.Tune,
            title = "Configuración del plantel",
            subtitle = "Ubicación, radio GPS y horario",
            badge = null,
            onClick = onCampus,
        ),
        MoreEntry(
            icon = Icons.Filled.Warning,
            title = "Incidentes",
            subtitle = "Revisa los reportes del plantel",
            badge = "2",
            onClick = onIncidents,
        ),
        MoreEntry(
            icon = Icons.Filled.Notifications,
            title = "Notificaciones",
            subtitle = "Alertas y avisos del sistema",
            badge = "4",
            onClick = onNotifications,
        ),
        MoreEntry(
            icon = Icons.Filled.Person,
            title = "Perfil",
            subtitle = "Tu cuenta y sesión",
            badge = null,
            onClick = onProfile,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        ) {
            Reveal(0) { MoreHeader() }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .revealOnEnter(60),
                verticalArrangement = Arrangement.Center,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    entries.forEach { entry -> MoreCard(entry = entry) }
                }

                Text(
                    text = "AULASEGURA · VERSIÓN 1.0",
                    color = Outline,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
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
private fun MoreHeader() {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Más",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.56).sp,
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = "Administración del plantel",
            color = Outline,
            fontFamily = DmSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.14.sp,
        )
    }
}

@Composable
private fun MoreCard(entry: MoreEntry) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "moreCardScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(2.dp, CardShape)
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, CardBorder, CardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = entry.onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PrimaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.subtitle,
                color = Outline,
                fontFamily = DmSans,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (entry.badge != null) {
            Box(
                modifier = Modifier
                    .shadow(1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = entry.badge,
                    color = Color.White,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = OutlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
