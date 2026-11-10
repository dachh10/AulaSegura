package com.example.aulasegura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * Destinations of the parent/tutor shell.
 *
 * The Stitch mockups are not fully consistent: "Inicio" shows
 * Inicio / Asistencia / Tareas / Chat / Perfil, while "Asistencia" shows
 * Inicio / Avisos / Asistencia / Chat / Asistente. A single shell is used
 * across the whole block so navigation stays predictable; Avisos and the
 * AI assistant remain reachable from the quick-access grid on Inicio.
 */
enum class ParentTab(val label: String) {
    Inicio("Inicio"),
    Asistencia("Asistencia"),
    Tareas("Tareas"),
    Chat("Chat"),
    Perfil("Perfil"),
}

@Composable
fun ParentBottomBar(
    active: ParentTab,
    onSelect: (ParentTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        color = SurfaceContainerLowest.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 14.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParentTab.entries.forEach { tab ->
                ParentTabItem(
                    tab = tab,
                    selected = tab == active,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun ParentTabItem(
    tab: ParentTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) Primary else OnSurfaceVariant,
        label = "parentTabColor",
    )
    val background by animateColorAsState(
        targetValue = if (selected) PrimaryFixed else Color.Transparent,
        label = "parentTabBackground",
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
                horizontal = if (selected) 14.dp else 10.dp,
                vertical = 6.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tab.icon(selected),
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = contentColor,
            fontFamily = DmSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 10.sp,
        )
    }
}

private fun ParentTab.icon(selected: Boolean): ImageVector = when (this) {
    ParentTab.Inicio -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    ParentTab.Asistencia -> if (selected) Icons.AutoMirrored.Filled.FactCheck else Icons.AutoMirrored.Outlined.FactCheck
    ParentTab.Tareas ->
        if (selected) Icons.AutoMirrored.Filled.Assignment
        else Icons.AutoMirrored.Outlined.Assignment
    ParentTab.Chat -> if (selected) Icons.AutoMirrored.Filled.Chat else Icons.AutoMirrored.Outlined.Chat
    ParentTab.Perfil -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}
