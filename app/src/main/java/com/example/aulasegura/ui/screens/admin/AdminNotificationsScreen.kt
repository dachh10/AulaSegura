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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

private enum class NotificationCategory(val label: String) {
    Todas("Todas"),
    Incidentes("Incidentes"),
    Asistencia("Asistencia"),
    Sistema("Sistema"),
}

private enum class DayGroup(val label: String) {
    Hoy("HOY"),
    Anteriores("ANTERIORES"),
}

private data class AdminNotification(
    val id: Int,
    val category: NotificationCategory,
    val day: DayGroup,
    val icon: ImageVector,
    val iconTone: Color,
    val iconInk: Color,
    val title: String,
    val body: String,
    val time: String,
    val actionLabel: String? = null,
    val initiallyUnread: Boolean = false,
)

private val notifications = listOf(
    AdminNotification(
        id = 0,
        category = NotificationCategory.Incidentes,
        day = DayGroup.Hoy,
        icon = Icons.Filled.Warning,
        iconTone = RedTone,
        iconInk = RedInk,
        title = "Incidente grave registrado",
        body = "Daño al mobiliario en 2° C, registrado por el Prof. Vega. Ubicación editada.",
        time = "10:48",
        actionLabel = "Revisar",
        initiallyUnread = true,
    ),
    AdminNotification(
        id = 1,
        category = NotificationCategory.Asistencia,
        day = DayGroup.Hoy,
        icon = Icons.Filled.Schedule,
        iconTone = AmberTone,
        iconInk = AmberInk,
        title = "Lista pendiente",
        body = "El Prof. Vega aún no pasa lista en 2° C.",
        time = "09:30",
        initiallyUnread = true,
    ),
    AdminNotification(
        id = 2,
        category = NotificationCategory.Sistema,
        day = DayGroup.Hoy,
        icon = Icons.Filled.PersonAdd,
        iconTone = CircleTone,
        iconInk = PrimaryContainer,
        title = "Nueva familia vinculada",
        body = "Carlos Gómez se registró y se vinculó con Mariana Gómez Ruiz · 3° B.",
        time = "08:55",
        initiallyUnread = true,
    ),
    AdminNotification(
        id = 3,
        category = NotificationCategory.Incidentes,
        day = DayGroup.Hoy,
        icon = Icons.Filled.Shield,
        iconTone = CoralTone,
        iconInk = SecondaryContainer,
        title = "Nuevo incidente",
        body = "Caída en el recreo en 3° B, registrado por el Prof. Salinas.",
        time = "08:40",
        initiallyUnread = true,
    ),
    AdminNotification(
        id = 4,
        category = NotificationCategory.Sistema,
        day = DayGroup.Hoy,
        icon = Icons.Filled.CloudDone,
        iconTone = GreenTone,
        iconInk = GreenInk,
        title = "Sincronización completada",
        body = "Se enviaron 3 registros guardados sin conexión.",
        time = "07:58",
    ),
    AdminNotification(
        id = 5,
        category = NotificationCategory.Asistencia,
        day = DayGroup.Anteriores,
        icon = Icons.Filled.Description,
        iconTone = PurpleTone,
        iconInk = PurpleInk,
        title = "Justificante recibido",
        body = "Laura Ríos envió un comprobante de Bruno Castillo.",
        time = "Ayer",
    ),
    AdminNotification(
        id = 6,
        category = NotificationCategory.Asistencia,
        day = DayGroup.Anteriores,
        icon = Icons.Filled.WarningAmber,
        iconTone = AmberTone,
        iconInk = AmberInk,
        title = "Alumno con 3 faltas",
        body = "Camila Domínguez · 3° B llegó a 5 faltas este mes.",
        time = "Lun",
    ),
)

@Composable
fun AdminNotificationsScreen(
    onBack: () -> Unit = {},
    onFilter: () -> Unit = {},
    onRevisar: () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(NotificationCategory.Todas) }
    // Bitmask of notifications marked as read during this visit.
    var readFlags by rememberSaveable { mutableLongStateOf(0L) }

    fun isRead(notification: AdminNotification): Boolean =
        !notification.initiallyUnread || (readFlags shr notification.id) and 1L == 1L

    val visible = notifications.filter { notification ->
        filter == NotificationCategory.Todas || notification.category == filter
    }
    val unreadCount = notifications.count { !isRead(it) }

    fun markAllRead() {
        var flags = readFlags
        notifications.forEach { notification ->
            flags = flags or (1L shl notification.id)
        }
        readFlags = flags
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
            Reveal(0) { NotificationsTopBar(onBack = onBack, onMarkAllRead = ::markAllRead) }

            Spacer(modifier = Modifier.height(10.dp))

            Reveal(60) { NotificationsSummary(unreadCount = unreadCount, onFilter = onFilter) }

            Spacer(modifier = Modifier.height(16.dp))

            Reveal(120) {
                NotificationFilters(
                    selected = filter,
                    onSelect = { filter = it },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Reveal(180) {
                Column {
                    var renderedAny = false
                    DayGroup.entries.forEach { day ->
                        val items = visible.filter { it.day == day }
                        if (items.isNotEmpty()) {
                            if (renderedAny) Spacer(modifier = Modifier.height(14.dp))
                            DaySection(
                                day = day,
                                items = items,
                                isRead = ::isRead,
                                onOpen = { notification ->
                                    readFlags = readFlags or (1L shl notification.id)
                                },
                                onRevisar = onRevisar,
                            )
                            renderedAny = true
                        }
                    }
                }
            }
        }
    }
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
private fun NotificationsSummary(unreadCount: Int, onFilter: () -> Unit) {
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
                    text = if (unreadCount == 0) "Sin pendientes" else "Última: hace 6 min",
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
    selected: NotificationCategory,
    onSelect: (NotificationCategory) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NotificationCategory.entries.forEach { option ->
            FilterPill(
                label = option.label,
                selected = option == selected,
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
    day: DayGroup,
    items: List<AdminNotification>,
    isRead: (AdminNotification) -> Boolean,
    onOpen: (AdminNotification) -> Unit,
    onRevisar: () -> Unit,
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
                text = day.label,
                color = MetaMuted,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.88.sp,
            )
            if (day == DayGroup.Hoy) {
                Text(
                    text = "${items.size} avisos",
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
            items.forEach { notification ->
                NotificationCard(
                    notification = notification,
                    read = isRead(notification),
                    onClick = { onOpen(notification) },
                    onRevisar = onRevisar,
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: AdminNotification,
    read: Boolean,
    onClick: () -> Unit,
    onRevisar: () -> Unit,
) {
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
                    .background(notification.iconTone),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = notification.icon,
                    contentDescription = null,
                    tint = notification.iconInk,
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

                if (notification.actionLabel != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(RedTone)
                            .clickable(onClick = onRevisar)
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
                            text = notification.actionLabel,
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
