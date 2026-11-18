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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.NotificationType
import com.example.aulasegura.navigation.resolveDeepLink
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 4.10 — Notificaciones (Administración). Ported 1:1 from the Stitch mockup. */

private val Ink = Color(0xFF1B1D3A)
private val MetaMuted = Color(0xFF6B6E8F)
private val BodyMuted = Color(0xFF595C7A)
private val UnreadBg = Color(0xFFF3F4FB)
private val UnreadBorder = Color(0xFFD9DCF0)
private val ReadBorder = Color(0xFFE6E1D8)
private val CardShape = RoundedCornerShape(20.dp)

private val CircleTone = Color(0xFFE7E9F9)
private val CoralTone = Color(0xFFFFE6E1)
private val AmberTone = Color(0xFFFDF0D5)
private val AmberInk = Color(0xFFB45309)
private val GreenTone = Color(0xFFE3F6EC)
private val GreenInk = Color(0xFF157F52)
private val PurpleTone = Color(0xFFEFE7FA)
private val PurpleInk = Color(0xFF6D3FA8)
private val RedTone = Color(0xFFFFE2E0)
private val RedInk = Color(0xFFC2412F)

/** The mockup's icon and tints, chosen by `type` instead of by a hard-coded row. */
private data class CardStyle(val icon: ImageVector, val tone: Color, val ink: Color)

private fun NotificationType.cardStyle(): CardStyle = when (this) {
    NotificationType.INCIDENTE -> CardStyle(Icons.Filled.Warning, RedTone, RedInk)
    NotificationType.ASISTENCIA -> CardStyle(Icons.Filled.Schedule, AmberTone, AmberInk)
    NotificationType.MENSAJE -> CardStyle(Icons.Filled.ChatBubble, CircleTone, PrimaryContainer)
    NotificationType.AVISO -> CardStyle(Icons.Filled.PersonAdd, CoralTone, SecondaryContainer)
    NotificationType.TAREA -> CardStyle(Icons.Filled.Description, PurpleTone, PurpleInk)
    NotificationType.SISTEMA -> CardStyle(Icons.Filled.CloudDone, GreenTone, GreenInk)
    NotificationType.OTRO -> CardStyle(Icons.Filled.WarningAmber, CircleTone, MetaMuted)
}

/**
 * The rows come from `notifications` through [AdminNotificationsViewModel]: the
 * tray is read-only except for the `read` flag, the chips map to a `type`, and a
 * tap marks the document read and follows its whitelisted `deepLink`.
 */
@Composable
fun AdminNotificationsScreen(
    onBack: () -> Unit = {},
    onFilter: () -> Unit = {},
    onDeepLink: (String) -> Unit = {},
) {
    val viewModel: AdminNotificationsViewModel =
        viewModel(factory = rememberAppContainer().viewModelFactory)
    val ui by viewModel.state.collectAsState()
    val pendingDeepLink by viewModel.pendingDeepLink.collectAsState()

    // The document's deepLink is untrusted: only whitelisted routes may open, and
    // the value is consumed once so a rotation does not reopen the same screen.
    LaunchedEffect(pendingDeepLink) {
        if (pendingDeepLink != null) {
            resolveDeepLink(pendingDeepLink)?.let(onDeepLink)
            viewModel.deepLinkHandled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        ) {
            Reveal(0) { NotificationsTopBar(onBack = onBack, onMarkAllRead = viewModel::markAllRead) }

            Spacer(modifier = Modifier.height(10.dp))

            Reveal(60) {
                NotificationsSummary(
                    unreadCount = ui.unread,
                    lastTime = ui.lastNotificationTime,
                    onFilter = onFilter,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Reveal(120) {
                NotificationFilters(
                    filters = ui.filters,
                    activeFilter = ui.activeFilter,
                    onSelect = viewModel::setFilter,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Reveal(180) {
                Column {
                    ui.groups.forEachIndexed { index, group ->
                        if (index > 0) Spacer(modifier = Modifier.height(14.dp))
                        DaySection(group = group, onOpen = viewModel::open)
                    }

                    if (!ui.notice.isNullOrBlank()) {
                        if (ui.groups.isNotEmpty()) Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = ui.notice.orEmpty(),
                            color = RedInk,
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (ui.loading) {
                        Placeholder(text = "Cargando notificaciones…")
                    } else if (ui.groups.isEmpty() && ui.notice.isNullOrBlank()) {
                        Placeholder(text = "No tienes notificaciones todavía.")
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Text(
        text = text,
        color = MetaMuted,
        fontFamily = DmSans,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    )
}

@Composable
private fun NotificationsTopBar(onBack: () -> Unit, onMarkAllRead: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(1.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
                    .border(1.dp, ReadBorder, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Ink,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Notificaciones",
                color = Ink,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 25.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.5).sp,
            )
        }

        Text(
            text = "Marcar todo leído",
            color = SecondaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onMarkAllRead)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun NotificationsSummary(unreadCount: Int, lastTime: String?, onFilter: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(PrimaryContainer)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(SecondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (unreadCount == 0) "Todo leído" else "$unreadCount sin leer",
                    color = Color.White,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = if (unreadCount == 0) {
                        "Sin pendientes"
                    } else {
                        lastTime?.let { "Última: $it" }.orEmpty()
                    },
                    color = Color.White.copy(alpha = 0.70f),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(onClick = onFilter),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Filtrar",
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NotificationFilters(
    filters: List<String>,
    activeFilter: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { option ->
            FilterPill(
                label = option,
                selected = option == activeFilter,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "notificationChipScale",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .border(
                width = 1.dp,
                color = if (selected) PrimaryContainer else ReadBorder,
                shape = RoundedCornerShape(percent = 50),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (selected) 14.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Ink,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun DaySection(
    group: AdminNotificationGroupUi,
    onOpen: (AdminNotificationRowUi) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.label,
                color = MetaMuted,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.88.sp,
            )
            if (group.label == ADMIN_NOTIFICATIONS_TODAY_LABEL) {
                Text(
                    text = "${group.rows.size} avisos",
                    color = MetaMuted.copy(alpha = 0.70f),
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            group.rows.forEach { row ->
                NotificationCard(
                    notification = row,
                    onClick = { onOpen(row) },
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AdminNotificationRowUi,
    onClick: () -> Unit,
) {
    val style = notification.type.cardStyle()
    val read = notification.read

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (read) SurfaceContainerLowest else UnreadBg)
            .border(1.dp, if (read) ReadBorder else UnreadBorder, CardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(style.tone),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = null,
                    tint = style.ink,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = notification.title,
                        color = Ink,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notification.time,
                            color = MetaMuted,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                        if (!read) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryContainer),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.body,
                    color = BodyMuted,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )

                if (notification.deepLink != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(RedTone)
                            .clickable(onClick = onClick)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = RedInk,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Revisar",
                            color = RedInk,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}
