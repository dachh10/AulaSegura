package com.example.aulasegura.ui.screens.parent

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** 2.13 — Perfil. Ported from Stitch screen `7ad5b00011fe438390ac923e9e0d28e6`
 *  "Perfil - AulaSegura" (780x1768 mobile).
 *
 *  The Stitch mock is a fixed 800dp canvas: its content stops short of the
 *  bottom, leaving a dead gap above the nav, the header is a hard 180dp, and
 *  the shield watermark reads as a cut-off polygon. This port keeps every
 *  element of the design but rebuilds the layout on the project shell so the
 *  screen scrolls, sizes itself to its content, and reuses [ParentBottomBar].
 */

private val InkPrimary = Color(0xFF1B1D3A)
private val WarmBorder = Color(0xFFE6E1D8)
private val IndigoTint = Color(0xFFE8E9F7)
private val MintTint = Color(0xFFE6FAF4)
private val MintDark = Color(0xFF15803D)
private val SyncGreen = Color(0xFF006A4E)
private val DangerRed = Color(0xFFE5484D)

private const val HEADER_HEIGHT_DP = 220

private data class ProfileChild(
    val name: String,
    val grade: String,
    val initial: String,
    val tint: Color,
    val tintForeground: Color,
)

private val profileChildren = listOf(
    ProfileChild("Mariana", "3° B · Primaria", "M", IndigoTint, PrimaryContainer),
    ProfileChild("Diego", "1° A · Primaria", "D", MintTint, MintDark),
)

@Composable
fun ParentProfileScreen(
    onSelectTab: (ParentTab) -> Unit,
    onLogout: () -> Unit,
    onOpenPermissions: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onChangePhoto: () -> Unit = {},
    onOpenSyncStatus: () -> Unit = {},
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    // The indigo header sits under the status bar, so its icons must be light
    // while the header is on screen and dark once the content scrolls under.
    val headerOnScreen = scrollState.value < HEADER_HEIGHT_DP
    LightStatusBarWhile(headerOnScreen)

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
                .verticalScroll(scrollState)
                .padding(bottom = 104.dp),
        ) {
            Reveal(0) {
                ProfileHeader(onChangePhoto = onChangePhoto)
            }
            Reveal(80) {
                MyChildrenSection()
            }
            Reveal(160) {
                SettingsSection(
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = { notificationsEnabled = !notificationsEnabled },
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onOpenPermissions = onOpenPermissions,
                    onOpenSyncStatus = onOpenSyncStatus,
                )
            }
            Reveal(240) {
                ProfileFooter(onLogout = onLogout)
            }
        }

        ParentBottomBar(
            active = ParentTab.Perfil,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ---------- ZONE 1: header ----------

@Composable
private fun ProfileHeader(onChangePhoto: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer)
            .statusBarsPadding()
            .padding(top = 14.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ShieldPattern(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.07f,
                color = Color.White,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(onChangePhoto = onChangePhoto)

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Carlos Gómez Pérez",
                    color = OnPrimary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.3).sp,
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OnPrimary.copy(alpha = 0.20f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "Padre de familia",
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "carlos.gomez@familia.mx",
                    color = OnPrimary.copy(alpha = 0.80f),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(onChangePhoto: () -> Unit) {
    val (interaction, press) = rememberPressScale(0.9f)
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(PrimaryFixed)
                .border(3.dp, OnPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "C",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
            )
        }
        Box(
            modifier = press
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(22.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(SecondaryContainer)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onChangePhoto,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Cambiar foto de perfil",
                tint = OnPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

// ---------- ZONE 2: mis hijos ----------

@Composable
private fun MyChildrenSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
    ) {
        Text(
            text = "MIS HIJOS",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            profileChildren.forEach { child ->
                ChildCard(child = child, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChildCard(child: ProfileChild, modifier: Modifier = Modifier) {
    val (interaction, press) = rememberPressScale(0.97f)
    Row(
        modifier = modifier
            .then(press)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = {})
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(child.tint)
                .border(1.dp, WarmBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = child.initial,
                color = child.tintForeground,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = child.name,
                color = InkPrimary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = child.grade,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------- ZONE 3: settings ----------

@Composable
private fun SettingsSection(
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSyncStatus: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, WarmBorder, RoundedCornerShape(24.dp)),
        ) {
            SettingsRow(
                icon = Icons.Outlined.Edit,
                label = "Editar perfil",
                onClick = onEditProfile,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Lock,
                label = "Cambiar contraseña",
                onClick = onChangePassword,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                label = "Notificaciones",
                onClick = null,
                trailing = {
                    ProfileToggle(
                        checked = notificationsEnabled,
                        onToggle = onToggleNotifications,
                    )
                },
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Security,
                label = "Permisos de la app",
                onClick = onOpenPermissions,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Sync,
                label = "Estado de sincronización",
                onClick = onOpenSyncStatus,
                trailing = {
                    SyncBadge()
                    Spacer(Modifier.width(6.dp))
                    SettingsChevron()
                },
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = WarmBorder.copy(alpha = 0.7f),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit = { SettingsChevron() },
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) SurfaceContainerLow else Color.Transparent,
        animationSpec = tween(160),
        label = "settingsRowBackground",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .background(background)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryContainer.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = InkPrimary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun SettingsChevron() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = OutlineVariant,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun ProfileToggle(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) PrimaryContainer else OutlineVariant,
        animationSpec = tween(220),
        label = "profileToggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "profileToggleThumb",
    )
    val (interaction, press) = rememberPressScale(0.92f)

    Box(
        modifier = press
            .size(width = 40.dp, height = 20.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(OnPrimary),
        )
    }
}

@Composable
private fun SyncBadge() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(BrandMint.copy(alpha = 0.25f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = SyncGreen,
            modifier = Modifier.size(10.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "Sincronizado",
            color = SyncGreen,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            lineHeight = 12.sp,
        )
    }
}

// ---------- ZONE 4: logout + version ----------

@Composable
private fun ProfileFooter(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (interaction, press) = rememberPressScale(0.95f)
        val pressed by interaction.collectIsPressedAsState()
        val background by animateColorAsState(
            targetValue = if (pressed) DangerRed.copy(alpha = 0.06f) else Color.Transparent,
            animationSpec = tween(160),
            label = "logoutBackground",
        )

        Row(
            modifier = press
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.5.dp, DangerRed, CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onLogout),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Cerrar sesión",
                color = DangerRed,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Aviso de privacidad · AulaSegura v1.0",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ---------- system bar ----------

/**
 * Keeps the status bar icons readable over the indigo header: light icons while
 * the header covers the status bar, dark icons once the content scrolls under.
 */
@Composable
private fun LightStatusBarWhile(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val activity = view.context.findActivity() ?: return
    val controller = remember(activity, view) {
        WindowInsetsControllerCompat(activity.window, view)
    }
    val previous = remember { controller.isAppearanceLightStatusBars }

    SideEffect {
        controller.isAppearanceLightStatusBars = !lightIcons
    }
    DisposableEffect(controller) {
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
