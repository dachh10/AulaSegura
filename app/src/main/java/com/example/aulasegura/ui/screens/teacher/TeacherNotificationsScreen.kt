package com.example.aulasegura.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.NotificationType
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * The mockup's icon and tints, chosen by `type`.
 *
 * One mapping instead of a field per row: the notification document carries a
 * type, and the color is presentation.
 */
private fun NotificationRowUi.toRender(): TeacherNotification = TeacherNotification(
    id = id,
    title = title,
    body = body,
    time = time,
    icon = when (type) {
        NotificationType.MENSAJE -> Icons.Filled.ChatBubble
        NotificationType.AVISO, NotificationType.INCIDENTE -> Icons.Filled.Campaign
        NotificationType.TAREA -> Icons.Filled.Description
        NotificationType.ASISTENCIA -> Icons.Filled.Schedule
        NotificationType.SISTEMA, NotificationType.OTRO -> Icons.Filled.Sync
    },
    iconTint = when (type) {
        NotificationType.MENSAJE -> BrandCoral
        NotificationType.AVISO, NotificationType.INCIDENTE -> PrimaryContainer
        NotificationType.TAREA -> VioletTint
        NotificationType.ASISTENCIA -> AmberTint
        NotificationType.SISTEMA, NotificationType.OTRO -> MintTint
    },
    iconBackground = when (type) {
        NotificationType.MENSAJE -> BrandCoral.copy(alpha = 0.15f)
        NotificationType.AVISO, NotificationType.INCIDENTE -> PrimaryContainer.copy(alpha = 0.10f)
        NotificationType.TAREA -> VioletBackground.copy(alpha = 0.15f)
        NotificationType.ASISTENCIA -> AmberBackground.copy(alpha = 0.15f)
        NotificationType.SISTEMA, NotificationType.OTRO -> BrandMint.copy(alpha = 0.20f)
    },
    unread = !read,
)

/**
 * Teacher — Notificaciones. Ported from Stitch screen `cd4bc66529b14e1a9aed9bfa3bcc4208`
 * ("Notificaciones - AulaSegura", project 7677408035297541086).
 *
 * The rows come from `notifications` through `TeacherNotificationsViewModel`: the
 * filters map to a `type`, a tap marks the document read and follows its
 * `deepLink`, and "marcar todo leído" commits one batch.
 */

private val WarmBorder = Color(0xFFE6E1D8)
private val NotificationBody = Color(0xFF595761)
private val NotificationCardShape = RoundedCornerShape(20.dp)
private val NotificationPillShape = RoundedCornerShape(percent = 50)

private val MintTint = Color(0xFF1F7B60)
private val AmberTint = Color(0xFFD97706)
private val AmberBackground = Color(0xFFF59E0B)
private val VioletTint = Color(0xFF7C3AED)
private val VioletBackground = Color(0xFF8B5CF6)

private data class TeacherNotification(
    val id: String,
    val title: String,
    val body: String,
    val time: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val unread: Boolean,
)


@Composable
fun TeacherNotificationsScreen(
    onBack: () -> Unit,
    ui: TeacherNotificationsUi = TeacherNotificationsUi(),
    onFilterChange: (String) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onOpen: (NotificationRowUi) -> Unit = {},
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
                .padding(bottom = 24.dp),
        ) {
            Reveal(0) {
                NotificationsTopBar(
                    onBack = onBack,
                    onMarkAllRead = onMarkAllRead,
                )
            }
            Reveal(60) {
                NotificationFilters(
                    filters = ui.filters,
                    activeFilter = ui.activeFilter,
                    onFilterChange = onFilterChange,
                )
            }
            ui.groups.forEachIndexed { index, group ->
                Reveal(120 + index * 60) {
                    NotificationGroup(
                        label = group.label,
                        rows = group.rows,
                        onOpen = onOpen,
                    )
                }
            }
            if (!ui.loading && ui.groups.isEmpty()) {
                Reveal(120) {
                    Text(
                        text = ui.notice ?: "No tienes notificaciones todavía.",
                        color = Outline,
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (backInteraction, backPress) = rememberPressScale(0.92f)
        Box(
            modifier = backPress
                .size(44.dp)
                .clip(NotificationPillShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, WarmBorder, NotificationPillShape)
                .clickable(
                    interactionSource = backInteraction,
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = PrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = "Notificaciones",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "Marcar todo como leído",
            modifier = Modifier
                .width(104.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onMarkAllRead)
                .padding(vertical = 4.dp),
            color = SecondaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.End,
            maxLines = 2,
        )
    }
}

@Composable
private fun NotificationFilters(
    filters: List<String>,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        filters.forEach { filter ->
            NotificationFilterChip(
                label = filter,
                selected = filter == activeFilter,
                onClick = { onFilterChange(filter) },
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
    Box(
        modifier = pressModifier
            .clip(NotificationPillShape)
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, WarmBorder, NotificationPillShape)
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(
                horizontal = if (selected) 16.dp else 12.dp,
                vertical = 6.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) SurfaceContainerLowest else PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun NotificationGroup(
    label: String,
    rows: List<NotificationRowUi>,
    onOpen: (NotificationRowUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 2.dp),
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.55.sp,
        )

        rows.forEach { row ->
            NotificationCard(
                notification = row.toRender(),
                showUnreadDot = !row.read,
                onClick = { onOpen(row) },
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: TeacherNotification,
    showUnreadDot: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = pressModifier
            .fillMaxWidth()
            .clip(NotificationCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, NotificationCardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(notification.iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notification.icon,
                contentDescription = null,
                tint = notification.iconTint,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = notification.body,
                color = NotificationBody,
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = notification.time,
                color = Outline,
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        if (showUnreadDot) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrandCoral),
            )
        } else {
            Spacer(Modifier.width(16.dp))
        }
    }
}
