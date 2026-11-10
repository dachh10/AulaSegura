package com.example.aulasegura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.OnPrimaryFixed
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** Destinations of the administrator shell, as drawn in every admin mockup. */
enum class AdminTab(val label: String) {
    Panel("Panel"),
    Usuarios("Usuarios"),
    Grupos("Grupos"),
    Reportes("Reportes"),
    Mas("Más"),
}

/** Hairline borders used across the admin mockups (#E6E1D8). */
private val NavHairline = Color(0xFFE6E1D8)

/**
 * Floating island navigation shared by the admin block:
 * 344dp wide, 64dp tall, full radius, active destination in a pale indigo pill.
 */
@Composable
fun AdminBottomBar(
    active: AdminTab,
    onSelect: (AdminTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .height(64.dp),
        shape = RoundedCornerShape(percent = 50),
        color = SurfaceContainerLowest,
        border = BorderStroke(1.dp, NavHairline),
        shadowElevation = 24.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminTab.entries.forEach { tab ->
                AdminTabItem(
                    tab = tab,
                    selected = tab == active,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun AdminTabItem(
    tab: AdminTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) OnPrimaryFixed else OnSurfaceVariant,
        label = "adminTabColor",
    )
    val background by animateColorAsState(
        targetValue = if (selected) PrimaryFixed else Color.Transparent,
        label = "adminTabBackground",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = if (selected) 12.dp else 8.dp,
                vertical = 4.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tab.icon(selected),
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = tab.label,
            color = contentColor,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 12.sp,
        )
    }
}

private fun AdminTab.icon(selected: Boolean): ImageVector = when (this) {
    AdminTab.Panel -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
    AdminTab.Usuarios -> if (selected) Icons.Filled.ManageAccounts else Icons.Outlined.ManageAccounts
    AdminTab.Grupos -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
    AdminTab.Reportes -> if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics
    AdminTab.Mas -> if (selected) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz
}
