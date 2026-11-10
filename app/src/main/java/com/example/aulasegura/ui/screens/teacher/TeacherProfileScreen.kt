package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.TeacherBottomBar
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.components.shieldPattern
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant

/**
 * Teacher — Perfil. Ported from Stitch screen `6e82dee0cb5245af83656dbbd9073ce9`
 * ("Perfil del Docente - AulaSegura", project 7677408035297541086).
 *
 * Visual shell only: the notification toggle is UI state and the version footer
 * is static.
 */

private val WarmBorder = Color(0xFFEAE6DF)
private val RowDivider = Color(0xFFF0ECE4)
private val LogoutRed = Color(0xFFE5484D)
private val SyncChipBackground = Color(0xFFECFDF5)
private val SyncChipBorder = Color(0xFFA7F3D0)
private val SyncChipText = Color(0xFF059669)

private val ProfileCardShape = RoundedCornerShape(24.dp)
private val GroupCardShape = RoundedCornerShape(20.dp)
private val IconTileShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(percent = 50)

private data class TeacherGroupSlot(val name: String, val students: String)

private val teacherGroupSlots = listOf(
    TeacherGroupSlot("3° B", "28 alumnos"),
    TeacherGroupSlot("3° A", "28 alumnos"),
    TeacherGroupSlot("2° C", "28 alumnos"),
)

@Composable
fun TeacherProfileScreen(
    onSelectTab: (TeacherTab) -> Unit,
    onLogout: () -> Unit = {},
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 112.dp),
        ) {
            Reveal(0) { ProfileHeader() }

            Spacer(Modifier.height(16.dp))
            Reveal(40) { MyGroupsSection() }

            Spacer(Modifier.height(14.dp))
            Reveal(100) {
                SettingsCard(
                    notificationsEnabled = notificationsEnabled,
                    onNotificationsChange = { notificationsEnabled = it },
                )
            }

            Spacer(Modifier.height(16.dp))
            Reveal(160) { LogoutSection(onLogout = onLogout) }
        }

        TeacherBottomBar(
            active = TeacherTab.Perfil,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer)
            .shieldPattern(alpha = 0.07f, color = Color.White)
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(80.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainer)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "RS",
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                )
            }
            val (cameraInteraction, cameraPress) = rememberPressScale(0.9f)
            Box(
                modifier = cameraPress
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandCoral)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(interactionSource = cameraInteraction, indication = null, onClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Ricardo Salinas Ortega",
            color = Color.White,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.33).sp,
        )

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Docente",
                    color = Color.White,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.24.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ricardo.salinas@escuela.mx",
                color = Color.White.copy(alpha = 0.75f),
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.24.sp,
            )
        }
    }
}

@Composable
private fun MyGroupsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "MIS GRUPOS",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.55.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            teacherGroupSlots.forEach { slot ->
                GroupSlotCard(slot = slot, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GroupSlotCard(
    slot: TeacherGroupSlot,
    modifier: Modifier = Modifier,
) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)
    Column(
        modifier = modifier
            .clip(GroupCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, GroupCardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = {})
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = slot.name,
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = slot.students,
            color = Outline,
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.24.sp,
        )
    }
}

@Composable
private fun SettingsCard(
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ProfileCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, ProfileCardShape),
    ) {
        SettingsRow(
            icon = Icons.Filled.Person,
            label = "Editar perfil",
            onClick = {},
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Lock,
            label = "Cambiar contraseña",
            onClick = {},
        )
        SettingsDivider()
        NotificationToggleRow(
            enabled = notificationsEnabled,
            onChange = onNotificationsChange,
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Shield,
            label = "Permisos de la app",
            onClick = {},
        )
        SettingsDivider()
        SyncStatusRow()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = icon)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NotificationToggleRow(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = Icons.Filled.Notifications)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Notificaciones",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        NotificationToggle(checked = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun NotificationToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) PrimaryContainer else SurfaceVariant,
        label = "toggleTrack",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        label = "toggleKnob",
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(PillShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { translationX = knobOffset.toPx() }
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SyncStatusRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = Icons.Filled.Sync)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Estado de sincronización",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(SyncChipBackground)
                .border(1.dp, SyncChipBorder, PillShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = SyncChipText,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Sincronizado",
                color = SyncChipText,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun SettingsIconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(IconTileShape)
            .background(PrimaryContainer.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RowDivider),
    )
}

@Composable
private fun LogoutSection(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (interaction, pressModifier) = rememberPressScale(0.98f)
        Row(
            modifier = pressModifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(PillShape)
                .border(1.dp, LogoutRed, PillShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onLogout),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = LogoutRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Cerrar sesión",
                color = LogoutRed,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Aviso de privacidad · AulaSegura v1.0",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}
