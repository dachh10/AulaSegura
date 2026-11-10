package com.example.aulasegura.ui.screens.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.ProgressRing
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OnTertiaryFixed
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed

/** 2.1 — Inicio Familias. Ported from Stitch screen `54371f57ccfe4427870eb7e7092542ff`. */

private val AmberBg = Color(0xFFFEF3C7)
private val AmberText = Color(0xFFB45309)

private data class Child(
    val name: String,
    val grade: String,
    val initials: String,
    val percent: Int,
    val headline: String,
    val status: String,
)

@Composable
fun ParentHomeScreen(
    onNotifications: () -> Unit,
    onQuickAvisos: () -> Unit,
    onQuickIncidentes: () -> Unit,
    onQuickJustificar: () -> Unit,
    onQuickAsistente: () -> Unit,
    onVerTareas: () -> Unit,
    onSelectTab: (ParentTab) -> Unit,
) {
    val children = remember {
        listOf(
            Child("Mariana", "3° B", "M", 94, "Mariana asistió a clase", "Presente · 07:52"),
            Child("Diego", "1° A", "D", 97, "Diego asistió a clase", "Presente · 07:48"),
        )
    }
    var selected by remember { mutableIntStateOf(0) }
    val child = children[selected]

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
                .padding(bottom = 104.dp),
        ) {
            Reveal(0) { HomeTopBar(onNotifications = onNotifications) }
            Reveal(60) {
                ChildSelector(
                    children = children,
                    selected = selected,
                    onSelect = { selected = it },
                )
            }
            Reveal(120) { TodaySummary(child = child) }
            Reveal(180) {
                QuickAccess(
                    onAvisos = onQuickAvisos,
                    onIncidentes = onQuickIncidentes,
                    onJustificar = onQuickJustificar,
                    onAsistente = onQuickAsistente,
                )
            }
            Reveal(240) { UpcomingTasks(onVerTareas = onVerTareas) }
        }

        ParentBottomBar(
            active = ParentTab.Inicio,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HomeTopBar(onNotifications: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PrimaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "CG",
                color = OnPrimary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Hola, Carlos",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.4).sp,
            )
            Text(
                text = "Padre de familia",
                color = Outline,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
        NotificationBell(onClick = onNotifications)
    }
}

@Composable
private fun NotificationBell(onClick: () -> Unit) {
    val (interaction, pressModifier) = rememberPressScale(0.92f)
    Box(
        modifier = pressModifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notificaciones",
            tint = Primary,
            modifier = Modifier.size(22.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-1).dp, y = 1.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(SecondaryContainer)
                .border(2.dp, SurfaceContainerLowest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "3",
                color = OnPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun ChildSelector(
    children: List<Child>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        children.forEachIndexed { index, child ->
            val isSelected = index == selected
            val (interaction, pressModifier) = rememberPressScale(0.95f)
            Row(
                modifier = pressModifier
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryContainer else SurfaceContainerLowest)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else OutlineVariant.copy(alpha = 0.7f),
                        shape = CircleShape,
                    )
                    .clickable(interactionSource = interaction, indication = null) { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) OnPrimary.copy(alpha = 0.2f) else SurfaceContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = child.initials,
                        color = if (isSelected) OnPrimary else OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
                Text(
                    text = "${child.name} · ${child.grade}",
                    color = if (isSelected) OnPrimary else Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun TodaySummary(child: Child) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PrimaryContainer)
            .padding(20.dp),
    ) {
        SummaryShieldMotif(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 24.dp, y = 24.dp)
                .size(144.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                Text(
                    text = "HOY · MARTES 3 DE NOVIEMBRE",
                    color = OnPrimary.copy(alpha = 0.7f),
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = child.headline,
                    color = OnPrimary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(TertiaryFixed)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = OnTertiaryFixed,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = child.status,
                        color = OnTertiaryFixed,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ProgressRing(
                    progress = child.percent / 100f,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    progressColor = BrandMint,
                    size = 64.dp,
                    strokeWidth = 5.dp,
                ) {
                    Text(
                        text = "${child.percent}%",
                        color = OnPrimary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "asistencia\ndel mes",
                    color = OnPrimary.copy(alpha = 0.7f),
                    fontFamily = DmSans,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SummaryShieldMotif(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 100f
        val sy = h / 120f
        val paint = Color.White.copy(alpha = 0.06f)

        val shield = Path().apply {
            moveTo(50f * sx, 0f)
            lineTo(0f, 20f * sy)
            lineTo(0f, 60f * sy)
            cubicTo(0f, 92f * sy, 50f * sx, 120f * sy, 50f * sx, 120f * sy)
            cubicTo(50f * sx, 120f * sy, 100f * sx, 92f * sy, 100f * sx, 60f * sy)
            lineTo(100f * sx, 20f * sy)
            close()
        }
        drawPath(shield, paint)

        val check = Path().apply {
            moveTo(42f * sx, 85f * sy)
            lineTo(20f * sx, 63f * sy)
            lineTo(28f * sx, 55f * sy)
            lineTo(42f * sx, 69f * sy)
            lineTo(72f * sx, 39f * sy)
            lineTo(80f * sx, 47f * sy)
            close()
        }
        drawPath(check, paint)
    }
}

@Composable
private fun QuickAccess(
    onAvisos: () -> Unit,
    onIncidentes: () -> Unit,
    onJustificar: () -> Unit,
    onAsistente: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Accesos rápidos",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Campaign,
                title = "Avisos",
                subtitle = "2 nuevos",
                subtitleColor = Secondary,
                onClick = onAvisos,
            )
            QuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Shield,
                title = "Incidentes",
                subtitle = "Sin pendientes",
                subtitleColor = Outline,
                onClick = onIncidentes,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.DocumentScanner,
                title = "Justificar falta",
                subtitle = "Enviar comprobante",
                subtitleColor = Outline,
                onClick = onJustificar,
            )
            QuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.AutoAwesome,
                title = "Asistente IA",
                subtitle = "Pregúntame lo que quieras",
                subtitleColor = Outline,
                onClick = onAsistente,
            )
        }
    }
}

@Composable
private fun QuickCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)
    Column(
        modifier = modifier
            .then(pressModifier)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = subtitleColor,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UpcomingTasks(onVerTareas: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Próximas tareas",
                color = Primary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                text = "Ver todas",
                color = SecondaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onVerTareas)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        TaskRow(
            dotColor = SecondaryContainer,
            title = "Matemáticas · Fracciones, ejercicios 1-10",
            pill = "Mañana",
            pillBackground = AmberBg,
            pillForeground = AmberText,
        )
        Spacer(Modifier.height(10.dp))
        TaskRow(
            dotColor = Primary,
            title = "Español · Resumen del cuento",
            pill = "Vie 7 nov",
            pillBackground = PrimaryFixed,
            pillForeground = Primary,
        )
    }
}

@Composable
private fun TaskRow(
    dotColor: Color,
    title: String,
    pill: String,
    pillBackground: Color,
    pillForeground: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(pillBackground)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = pill,
                color = pillForeground,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            )
        }
    }
}
