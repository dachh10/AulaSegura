package com.example.aulasegura.ui.screens.admin

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.components.shieldPattern
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * 4.8 — Perfil del administrador. Adapted from the generic Stitch profile mockup
 * (`7ad5b00011fe438390ac923e9e0d28e6`) with the directora's data, since there is
 * no admin-specific profile mockup.
 */

private val WarmBorder = Color(0xFFE6E1D8)
private val DangerRed = Color(0xFFBA1A1A)
private val SyncGreen = Color(0xFF1B7A50)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun AdminProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onOpenSyncStatus: () -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
) {
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
            Reveal(0) { ProfileHeader(onBack = onBack) }

            Reveal(60) {
                SettingsSection(
                    notificationsEnabled = notificationsEnabled,
                    onToggleNotifications = { notificationsEnabled = !notificationsEnabled },
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onOpenPermissions = onOpenPermissions,
                    onOpenSyncStatus = onOpenSyncStatus,
                )
            }

            Reveal(120) { ProfileFooter(onLogout = onLogout) }
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
private fun ProfileHeader(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer)
            .shieldPattern(alpha = 0.07f, color = Color.White)
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = OnPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

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
                text = "PR",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Patricia Rangel Vega",
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
                text = "Directora",
                color = OnPrimary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "directora@escuela.mx",
            color = OnPrimary.copy(alpha = 0.80f),
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
                .shadow(2.dp, CardShape)
                .clip(CardShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, WarmBorder, CardShape),
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
        label = "adminSettingsRowBackground",
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
            color = OnSurface,
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
private fun ProfileToggle(checked: Boolean, onToggle: () -> Unit) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) PrimaryContainer else OutlineVariant,
        animationSpec = tween(220),
        label = "adminProfileToggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "adminProfileToggleThumb",
    )

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 20.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(onClick = onToggle)
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

@Composable
private fun ProfileFooter(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val background by animateColorAsState(
            targetValue = if (pressed) DangerRed.copy(alpha = 0.06f) else Color.Transparent,
            animationSpec = tween(160),
            label = "adminLogoutBackground",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.5.dp, DangerRed, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onLogout,
                ),
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
