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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.aulasegura.domain.model.NotificationType
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

@Composable
fun ParentNotificationsScreen(
    onBack: () -> Unit,
    ui: ParentNotificationsUi = ParentNotificationsUi(),
    onFilterChange: (ParentNotificationFilter) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onOpen: (ParentNotificationRowUi) -> Unit = {},
) {
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
                    unreadCount = ui.unread,
                    onMarkAllRead = onMarkAllRead,
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
                        filters = ui.filters,
                        selected = ui.activeFilter,
                        onSelect = onFilterChange,
                    )
                }
                Reveal(120) {
                    if (ui.groups.isEmpty()) {
                        // Empty while loading is silence, not "no notifications":
                        // the second one is a claim we cannot make yet.
                        if (!ui.loading) {
                            EmptyNotifications(notice = ui.notice)
                        }
                    } else {
                        NotificationList(
                            groups = ui.groups,
                            onOpen = onOpen,
                        )
                    }
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
    filters: List<ParentNotificationFilter>,
    selected: ParentNotificationFilter,
    onSelect: (ParentNotificationFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
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
    groups: List<ParentNotificationGroupUi>,
    onOpen: (ParentNotificationRowUi) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = group.label,
                    color = OnSurfaceVariant,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                group.rows.forEach { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { onOpen(notification) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: ParentNotificationRowUi,
    onClick: () -> Unit,
) {
    val unread = !notification.read
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
                .background(notificationBadgeColor(notification.type)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notificationBadgeIcon(notification.type),
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
private fun EmptyNotifications(notice: String?) {
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
            text = if (notice == null) "Sin notificaciones" else "No se pudieron cargar",
            color = InkPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = notice ?: "Cuando haya novedades aparecerán aquí.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

private fun notificationBadgeColor(type: NotificationType): Color = when (type) {
    NotificationType.ASISTENCIA, NotificationType.INCIDENTE -> BrandCoral
    NotificationType.TAREA -> Amber500
    NotificationType.MENSAJE -> Sky500
    NotificationType.AVISO, NotificationType.SISTEMA, NotificationType.OTRO -> PrimaryContainer
}

private fun notificationBadgeIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.ASISTENCIA, NotificationType.INCIDENTE -> Icons.Filled.Warning
    NotificationType.TAREA -> Icons.AutoMirrored.Outlined.Assignment
    NotificationType.MENSAJE -> Icons.AutoMirrored.Outlined.Chat
    NotificationType.AVISO, NotificationType.SISTEMA, NotificationType.OTRO -> Icons.Filled.Campaign
}
