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
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
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
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/** Destinations of the teacher shell, as drawn in every teacher mockup. */
enum class TeacherTab(val label: String) {
    Inicio("Inicio"),
    Grupos("Grupos"),
    Chat("Chat"),
    Historial("Historial"),
    Perfil("Perfil"),
}

/** Hairline borders used across the teacher mockups (#E6E1D8). */
private val NavHairline = Color(0xFFE6E1D8)

/** Active pill behind the selected destination (#E8E9F7). */
private val NavActivePill = Color(0xFFE8E9F7)

/**
 * Floating island navigation shared by the teacher block:
 * 328dp wide, 64dp tall, 28dp radius, active destination in an indigo pill.
 */
@Composable
fun TeacherBottomBar(
    active: TeacherTab,
    onSelect: (TeacherTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
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
            TeacherTab.entries.forEach { tab ->
                TeacherTabItem(
                    tab = tab,
                    selected = tab == active,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}

@Composable
private fun TeacherTabItem(
    tab: TeacherTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) PrimaryContainer else OnSurfaceVariant,
        label = "teacherTabColor",
    )
    val background by animateColorAsState(
        targetValue = if (selected) NavActivePill else Color.Transparent,
        label = "teacherTabBackground",
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

private fun TeacherTab.icon(selected: Boolean): ImageVector = when (this) {
    TeacherTab.Inicio -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    TeacherTab.Grupos -> if (selected) Icons.Filled.Groups else Icons.Outlined.Groups
    TeacherTab.Chat -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubble
    TeacherTab.Historial -> if (selected) Icons.Filled.History else Icons.Outlined.History
    TeacherTab.Perfil -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}
