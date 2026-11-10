package com.example.aulasegura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * Compact student selector pill shared by the parent shell screens
 * ("M · Mariana · 3° B ▾").
 */
@Composable
fun StudentPill(
    name: String,
    grade: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val (interaction, pressModifier) = rememberPressScale(0.95f)
    Row(
        modifier = modifier
            .then(pressModifier)
            .clip(CircleShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.4f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PrimaryFixed),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
        Text(
            text = "$name · $grade",
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = "Cambiar estudiante",
            tint = Outline,
            modifier = Modifier.size(16.dp),
        )
    }
}
