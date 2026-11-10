package com.example.aulasegura.ui.screens.parent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 2.12 — Notificaciones. Ported from Stitch screen `95a5d9030ce548b890a8491db6e9161f`
 *  "Notificaciones - AulaSegura" (780x1768 mobile). */

private val InkPrimary = Color(0xFF141761)
private val InkMuted = Color(0xFF8A8EA8)
private val WarmBorder = Color(0xFFE6E1D8)
private val Amber500 = Color(0xFFED8936)
private val Sky500 = Color(0xFF3B82F6)
private val Emerald500 = Color(0xFF10B981)

private enum class NotificationFilter(val label: String) {
    Todas("Todas"),
    Asistencia("Asistencia"),
    Tareas("Tareas"),
    Avisos("Avisos"),
    Mensajes("Mensajes"),
}

private enum class NotificationCategory {
    Asistencia,
    Tarea,
    Aviso,
    Mensaje,
}

private data class Notification(
    val id: Int,
    val title: String,
    val body: String,
    val time: String,
    val category: NotificationCategory,
    val section: String,
    val urgent: Boolean,
    val initiallyUnread: Boolean,
)

private fun sampleNotifications(): List<Notification> = listOf(
    Notification(
        id = 1,
        title = "Falta registrada",
        body = "Mariana no asistió hoy. Puedes justificarla.",
        time = "09:15",
        category = NotificationCategory.Asistencia,
        section = "HOY",
        urgent = true,
        initiallyUnread = true,
    ),
    Notification(
        id = 2,
        title = "Nueva tarea de Matemáticas",
        body = "Fracciones, ejercicios 1 al 10.",
        time = "08:02",
        category = NotificationCategory.Tarea,
        section = "HOY",
        urgent = false,
        initiallyUnread = true,
    ),
    Notification(
        id = 3,
        title = "Mensaje del Prof. Salinas",
        body = "Mariana tendrá examen el jueves, prepárate con las fracciones.",
        time = "07:40",
        category = NotificationCategory.Mensaje,
        section = "HOY",
        urgent = false,
        initiallyUnread = true,
    ),
    Notification(
        id = 4,
        title = "Nuevo aviso de Dirección",
        body = "Suspensión de clases el 17 de noviembre por junta de consejo técnico.",
        time = "Lun",
        category = NotificationCategory.Aviso,
        section = "ANTERIORES",
        urgent = false,
        initiallyUnread = true,
    ),
    Notification(
        id = 5,
        title = "Falta justificada",
        body = "El justificante del 31 de octubre fue aceptado.",
        time = "Lun",
        category = NotificationCategory.Asistencia,
        section = "ANTERIORES",
        urgent = false,
        initiallyUnread = false,
    ),
    Notification(
        id = 6,
        title = "Taller de arte el viernes",
        body = "Traer cartulina y plumones para la feria de ciencias.",
        time = "Vie 31 oct",
        category = NotificationCategory.Aviso,
        section = "ANTERIORES",
        urgent = false,
        initiallyUnread = false,
    ),
)

private fun notificationBadgeColor(notification: Notification): Color = when {
    notification.urgent -> BrandCoral
    notification.category == NotificationCategory.Asistencia -> BrandCoral
    notification.category == NotificationCategory.Tarea -> Amber500
    notification.category == NotificationCategory.Mensaje -> Sky500
    else -> PrimaryContainer
}

private fun notificationBadgeIcon(notification: Notification): ImageVector = when {
    notification.title.contains("justificada", ignoreCase = true) -> Icons.Filled.VerifiedUser
    notification.category == NotificationCategory.Asistencia -> Icons.Filled.Warning
    notification.category == NotificationCategory.Tarea -> Icons.AutoMirrored.Outlined.Assignment
    notification.category == NotificationCategory.Mensaje -> Icons.AutoMirrored.Outlined.Chat
    else -> Icons.Filled.Campaign
}

@Composable
fun ParentNotificationsScreen(onBack: () -> Unit) {
    var filter by remember { mutableStateOf(NotificationFilter.Todas) }
    var readIds by remember { mutableStateOf(emptySet<Int>()) }
    val notifications = remember { sampleNotifications() }

    val unreadCount = notifications.count { it.initiallyUnread && it.id !in readIds }

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
            Reveal(0) {
                NotificationsTopBar(
                    unreadCount = unreadCount,
                    onMarkAllRead = { readIds = readIds + notifications.map { it.id } },
                    onBack = onBack,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Reveal(60) {
                    NotificationFilters(
                        selected = filter,
                        onSelect = { filter = it },
                    )
                }
                Reveal(120) {
                    NotificationList(
                        filter = filter,
                        notifications = notifications,
                        readIds = readIds,
                        onOpen = { id -> readIds = readIds + id },
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    unreadCount: Int,
    onMarkAllRead: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (backInteraction, backPress) = rememberPressScale(0.9f)
            Box(
                modifier = backPress
                    .size(40.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, WarmBorder, CircleShape)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Regresar",
                    tint = InkPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            MarkAllReadButton(
                unreadCount = unreadCount,
                onClick = onMarkAllRead,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Notificaciones",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
private fun MarkAllReadButton(
    unreadCount: Int,
    onClick: () -> Unit,
) {
    val enabled = unreadCount > 0
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    val background by animateColorAsState(
        targetValue = if (enabled) PrimaryContainer else OutlineVariant.copy(alpha = 0.35f),
        animationSpec = tween(200),
        label = "markAllBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (enabled) Color.White else OnSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "markAllForeground",
    )

    Row(
        modifier = pressModifier
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Marcar todo como leído",
            color = foreground,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun NotificationFilters(
    selected: NotificationFilter,
    onSelect: (NotificationFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NotificationFilter.entries.forEach { filter ->
            NotificationFilterChip(
                label = filter.label,
                selected = filter == selected,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun NotificationFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    val background by animateColorAsState(
        targetValue = if (selected) PrimaryContainer else SurfaceContainerLowest,
        animationSpec = tween(200),
        label = "notificationFilterBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) Color.White else OnSurfaceVariant,
        animationSpec = tween(200),
        label = "notificationFilterForeground",
    )

    Box(
        modifier = pressModifier
            .height(34.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, OutlineVariant.copy(alpha = 0.6f), CircleShape)
                },
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun NotificationList(
    filter: NotificationFilter,
    notifications: List<Notification>,
    readIds: Set<Int>,
    onOpen: (Int) -> Unit,
) {
    val visible = when (filter) {
        NotificationFilter.Todas -> notifications
        NotificationFilter.Asistencia ->
            notifications.filter { it.category == NotificationCategory.Asistencia }
        NotificationFilter.Tareas ->
            notifications.filter { it.category == NotificationCategory.Tarea }
        NotificationFilter.Avisos ->
            notifications.filter { it.category == NotificationCategory.Aviso }
        NotificationFilter.Mensajes ->
            notifications.filter { it.category == NotificationCategory.Mensaje }
    }

    if (visible.isEmpty()) {
        EmptyNotifications()
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf("HOY", "ANTERIORES").forEach { section ->
            val sectionNotifications = visible.filter { it.section == section }
            if (sectionNotifications.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = section,
                        color = OnSurfaceVariant,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    sectionNotifications.forEach { notification ->
                        NotificationCard(
                            notification = notification,
                            unread = notification.initiallyUnread && notification.id !in readIds,
                            onClick = { onOpen(notification.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    unread: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(notificationBadgeColor(notification)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notificationBadgeIcon(notification),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notification.title,
                    color = InkPrimary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = notification.time,
                    color = if (unread) OnSurfaceVariant else InkMuted,
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                )
                if (unread) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SecondaryContainer),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = notification.body,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (notification.urgent) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandCoral.copy(alpha = 0.12f))
                        .border(1.dp, BrandCoral.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = BrandCoral,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Importante",
                        color = BrandCoral,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(PrimaryContainer.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sin notificaciones",
            color = InkPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Cuando haya novedades aparecerán aquí.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}
