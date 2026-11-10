package com.example.aulasegura.ui.screens.access

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow

private val MintSoft = Color(0xFFE5FAF3)
private val MintDeep = Color(0xFF1B8361)
private val MintInk = Color(0xFF004031)
private val MintInkSoft = Color(0xFF00513F)
private val CoralSoft = Color(0xFFFFEAE6)
private val CoralInk = Color(0xFF0B664B)

/**
 * Hero illustration for onboarding page 1: a roll-call phone with a GPS pin.
 */
@Composable
fun IllustrationRollCall(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-56).dp)
                .size(176.dp)
                .background(BrandMint.copy(alpha = 0.15f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-56).dp, y = 56.dp)
                .size(160.dp)
                .background(BrandCoral.copy(alpha = 0.15f), CircleShape),
        )

        RollCallPhone(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-22).dp, y = 4.dp)
                .rotate(-1f),
        )

        RadarPin(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = 22.dp),
        )

        LocationChip(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = (-14).dp),
        )
    }
}

@Composable
private fun RollCallPhone(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(190.dp)
            .height(290.dp)
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .background(Color.White, RoundedCornerShape(28.dp))
            .border(3.dp, PrimaryContainer, RoundedCornerShape(28.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(BrandMint, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "3° Primaria B",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.sp,
                        color = Primary,
                    )
                }
                Text(
                    text = "08:15 AM",
                    fontFamily = JetBrainsMono,
                    fontSize = 8.sp,
                    color = Outline,
                )
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = SurfaceContainerHigh, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                RollRow("SM", "Sofía Morales", present = true, PrimaryContainer, Color.White)
                RollRow("LG", "Lucas Garza", present = false, Color(0xFFFFDAD4), Secondary)
                RollRow("VR", "Valeria Ríos", present = true, PrimaryFixed, Primary)
                RollRow("MT", "Mateo Torres", present = true, Color(0xFFD7E3FF), Primary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandMint.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .border(1.dp, BrandMint.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.TaskAlt,
                contentDescription = null,
                tint = CoralInk,
                modifier = Modifier.size(16.dp),
            )
            Column {
                Text(
                    text = "GPS COLEGIO OK",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    color = MintInk,
                )
                Text(
                    text = "Radio seguro 50m",
                    fontFamily = DmSans,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    color = MintInkSoft,
                )
            }
        }
    }
}

@Composable
private fun RollRow(
    initials: String,
    name: String,
    present: Boolean,
    avatarBackground: Color,
    avatarForeground: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(12.dp))
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(avatarBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarForeground,
                )
            }
            Text(
                text = name,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = OnSurface,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(if (present) MintSoft else CoralSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (present) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
                tint = if (present) MintDeep else BrandCoral,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun RadarPin(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "radarPin")
    val pingScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pingScale",
    )
    val pingAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pingAlpha",
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer {
                        scaleX = pingScale
                        scaleY = pingScale
                        alpha = pingAlpha
                    }
                    .border(2.dp, BrandMint.copy(alpha = 0.5f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(1.dp, PrimaryContainer.copy(alpha = 0.2f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .background(BrandCoral, RoundedCornerShape(16.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .background(PrimaryContainer, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                tint = BrandMint,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "Campus Norte",
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.2.sp,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun LocationChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE6E1D8), RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MintSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = null,
                tint = MintDeep,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = "Ubicación escolar",
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = Primary,
            )
            Text(
                text = "Precisión ±3m",
                fontFamily = JetBrainsMono,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = Outline,
            )
        }
    }
}
