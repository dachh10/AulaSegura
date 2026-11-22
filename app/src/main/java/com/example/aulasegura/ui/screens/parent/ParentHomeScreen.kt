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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.aulasegura.domain.model.AttendanceMark
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
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed

/** 2.1 — Inicio Familias. Ported from Stitch screen `54371f57ccfe4427870eb7e7092542ff`. */

private val AmberBg = Color(0xFFFEF3C7)
private val AmberText = Color(0xFFB45309)

@Composable
fun ParentHomeScreen(
    ui: ParentHomeUiState,
    fullName: String,
    onNotifications: () -> Unit,
    onQuickAvisos: () -> Unit,
    onQuickIncidentes: () -> Unit,
    onQuickJustificar: () -> Unit,
    onQuickAsistente: () -> Unit,
    onVerTareas: () -> Unit,
    onSelectChild: (Int) -> Unit,
    onAddChild: () -> Unit = {},
    onSelectTab: (ParentTab) -> Unit,
    unreadNotifications: Int = 0,
) {
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
            Reveal(0) {
                HomeTopBar(
                    fullName = fullName,
                    onNotifications = onNotifications,
                    unreadNotifications = unreadNotifications,
                )
            }
            when (ui) {
                is ParentHomeUiState.Loaded -> {
                    Reveal(60) {
                        ChildSelector(
                            children = ui.children,
                            selected = ui.selected,
                            onSelect = onSelectChild,
                            onAddChild = onAddChild,
                        )
                    }
                    Reveal(120) {
                        TodaySummary(child = ui.child, todayLabel = ui.todayLabel)
                    }
                    Reveal(180) {
                        QuickAccess(
                            unreadNotices = unreadNotifications,
                            onAvisos = onQuickAvisos,
                            onIncidentes = onQuickIncidentes,
                            onJustificar = onQuickJustificar,
                            onAsistente = onQuickAsistente,
                        )
                    }
                    Reveal(240) {
                        UpcomingTasks(tasks = ui.tasks, onVerTareas = onVerTareas)
                    }
                }

                else -> Reveal(60) { HomePlaceholder(ui) }
            }
        }

        ParentBottomBar(
            active = ParentTab.Inicio,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Loading, no linked child, or a failed read.
 *
 * A tutor who just redeemed a code and has no child is not an error: the screen
 * says what to do instead of showing an empty selector.
 */
@Composable
private fun HomePlaceholder(ui: ParentHomeUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        when (ui) {
            ParentHomeUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryContainer)
            }

            ParentHomeUiState.NoChildren -> HomeMessage(
                title = "Aún no hay hijos vinculados",
                body = "Cuando la escuela escanee tu QR con el alumno, aparecerá aquí. También puedes usar «Agregar hijo».",
            )

            is ParentHomeUiState.Failed -> HomeMessage(
                title = "No se pudo cargar",
                body = ui.message,
            )

            is ParentHomeUiState.Loaded -> Unit
        }
    }
}

@Composable
private fun HomeMessage(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Text(
            text = title,
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun HomeTopBar(
    fullName: String,
    onNotifications: () -> Unit,
    unreadNotifications: Int,
) {
    val firstName = fullName.trim().substringBefore(' ')

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
                text = initialsOf(fullName),
                color = OnPrimary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (firstName.isBlank()) "Hola" else "Hola, $firstName",
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
        NotificationBell(onClick = onNotifications, unread = unreadNotifications)
    }
}

@Composable
private fun NotificationBell(onClick: () -> Unit, unread: Int) {
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
        // No badge at zero: an empty tray with a "0" reads as a broken counter.
        if (unread > 0) {
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
                    text = if (unread > 9) "9+" else unread.toString(),
                    color = OnPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ChildSelector(
    children: List<ParentChildUi>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAddChild: () -> Unit,
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
        // Always available: a tutor with one child can add another by generating
        // a fresh QR (the name is corrected on the linking screen).
        val (addInteraction, addPress) = rememberPressScale(0.95f)
        Row(
            modifier = addPress
                .clip(CircleShape)
                .border(1.dp, OutlineVariant.copy(alpha = 0.7f), CircleShape)
                .clickable(interactionSource = addInteraction, indication = null) { onAddChild() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Agregar hijo",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun TodaySummary(child: ParentChildUi, todayLabel: String) {
    val pillBackground = when (child.mark) {
        AttendanceMark.ABSENT -> SecondaryFixed
        AttendanceMark.LATE -> AmberBg
        else -> TertiaryFixed
    }
    val pillForeground = when (child.mark) {
        AttendanceMark.ABSENT -> Secondary
        AttendanceMark.LATE -> AmberText
        else -> OnTertiaryFixed
    }
    val pillIcon = when (child.mark) {
        AttendanceMark.ABSENT -> Icons.Filled.Cancel
        AttendanceMark.LATE -> Icons.Outlined.Timer
        else -> Icons.Filled.Check
    }

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
                    text = todayLabel,
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
                        .background(pillBackground)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = pillIcon,
                        contentDescription = null,
                        tint = pillForeground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = child.status,
                        color = pillForeground,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ProgressRing(
                    progress = (child.percent ?: 0) / 100f,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    progressColor = BrandMint,
                    size = 64.dp,
                    strokeWidth = 5.dp,
                ) {
                    Text(
                        text = childAttendanceStatus(child.percent),
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
    unreadNotices: Int = 0,
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
                subtitle = if (unreadNotices > 0) "$unreadNotices nuevos" else "Sin nuevos",
                subtitleColor = if (unreadNotices > 0) Secondary else Outline,
                onClick = onAvisos,
            )
            QuickCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Shield,
                title = "Incidentes",
                subtitle = "Ver reportes",
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
private fun UpcomingTasks(tasks: List<ParentTaskUi>, onVerTareas: () -> Unit) {
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
        if (tasks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLowest)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = "Sin tareas próximas",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                )
            }
        } else {
            tasks.forEachIndexed { index, task ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                TaskRow(
                    dotColor = if (index % 2 == 0) SecondaryContainer else Primary,
                    title = if (task.subject.isBlank()) {
                        task.title
                    } else {
                        "${task.subject} · ${task.title}"
                    },
                    pill = task.dueLabel,
                    pillBackground = if (task.dueSoon) AmberBg else PrimaryFixed,
                    pillForeground = if (task.dueSoon) AmberText else Primary,
                )
            }
        }
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
