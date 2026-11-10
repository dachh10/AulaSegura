package com.example.aulasegura.ui.screens.access

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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.NotificationsActive
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
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SurfaceBright
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceVariant
import com.example.aulasegura.ui.theme.TertiaryContainer
import com.example.aulasegura.ui.theme.TertiaryFixed
import com.example.aulasegura.ui.theme.TertiaryFixedDim

private val NotebookLine = Color(0xFFE5E2DD)
private val MintSoft = Color(0xFFE5FAF3)
private val MintDeep = Color(0xFF1B8361)
private val MintInk = Color(0xFF004031)

/**
 * Hero illustration for onboarding page 2: paper notebook being replaced by
 * instant family notifications on a phone.
 */
@Composable
fun IllustrationFamilyNotices(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(208.dp)
                .background(Color.White.copy(alpha = 0.55f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = (-44).dp)
                .size(112.dp)
                .background(TertiaryFixed.copy(alpha = 0.3f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-48).dp, y = 44.dp)
                .size(128.dp)
                .background(BrandCoral.copy(alpha = 0.1f), CircleShape),
        )

        Notebook(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-4).dp, y = (-16).dp)
                .rotate(-6f),
        )

        CalendarPad(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-10).dp, y = 8.dp)
                .rotate(3f),
        )

        NoticesPhone(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp),
        )

        BellBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = 112.dp),
        )

        ChatChip(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-6).dp, y = 74.dp),
        )
    }
}

@Composable
private fun Notebook(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(128.dp)
            .height(176.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-10).dp, y = (-42).dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(6) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(6.dp)
                        .background(OutlineVariant, RoundedCornerShape(percent = 50))
                        .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(percent = 50)),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.width(48.dp).height(8.dp).background(Primary.copy(alpha = 0.2f), RoundedCornerShape(percent = 50)))
            Box(Modifier.fillMaxWidth().height(6.dp).background(SurfaceVariant, RoundedCornerShape(percent = 50)))
            Box(Modifier.fillMaxWidth(0.8f).height(6.dp).background(SurfaceVariant, RoundedCornerShape(percent = 50)))
            Box(Modifier.fillMaxWidth().height(6.dp).background(SurfaceVariant, RoundedCornerShape(percent = 50)))
            Box(Modifier.fillMaxWidth(0.66f).height(6.dp).background(SurfaceVariant, RoundedCornerShape(percent = 50)))

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(SurfaceContainerHigh, RoundedCornerShape(percent = 50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Ayer",
                    fontFamily = DmSans,
                    fontSize = 8.sp,
                    color = Color(0xFF777682),
                )
            }
        }
    }
}

@Composable
private fun CalendarPad(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(112.dp)
            .height(112.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(TertiaryContainer, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.size(5.dp).background(TertiaryFixed, CircleShape))
            Text(
                text = "MARZO",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 7.sp,
                letterSpacing = 0.6.sp,
                color = TertiaryFixed,
            )
            Box(Modifier.size(5.dp).background(TertiaryFixed, CircleShape))
        }

        Spacer(Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { col ->
                        val isMint = row == 0 && col == 2
                        val isCoral = row == 1 && col == 1
                        Box(
                            modifier = Modifier
                                .size(17.dp)
                                .background(
                                    color = when {
                                        isMint -> TertiaryFixedDim.copy(alpha = 0.5f)
                                        isCoral -> BrandCoral.copy(alpha = 0.4f)
                                        else -> SurfaceContainerLow
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isMint) {
                                Box(Modifier.size(5.dp).background(TertiaryContainer, CircleShape))
                            }
                            if (isCoral) {
                                Box(Modifier.size(5.dp).background(BrandCoral, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticesPhone(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(176.dp)
            .height(256.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .background(Color.White, RoundedCornerShape(28.dp))
            .border(3.dp, PrimaryContainer, RoundedCornerShape(28.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(44.dp).height(4.dp).background(Primary.copy(alpha = 0.2f), RoundedCornerShape(percent = 50)))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(PrimaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("A", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Colegio Madrid",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = Primary,
                    )
                }
                Text(
                    text = "08:15",
                    fontFamily = JetBrainsMono,
                    fontSize = 8.sp,
                    color = Color(0xFF464651),
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceVariant))

            Spacer(Modifier.height(8.dp))

            NoticeCard(
                icon = { Icon(Icons.Filled.CheckCircle, null, tint = TertiaryContainer, modifier = Modifier.size(13.dp)) },
                iconBackground = TertiaryFixed.copy(alpha = 0.4f),
                title = "Ingreso registrado",
                subtitle = "Mateo · Puerta Principal",
                borderColor = TertiaryFixedDim.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(7.dp))
            NoticeCard(
                icon = { Icon(Icons.AutoMirrored.Filled.Assignment, null, tint = Primary, modifier = Modifier.size(13.dp)) },
                iconBackground = PrimaryFixed,
                title = "Nueva Tarea: Historia",
                subtitle = "Para el viernes 22",
                borderColor = OutlineVariant.copy(alpha = 0.4f),
            )

            Spacer(Modifier.weight(1f))

            SyncPill(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun NoticeCard(
    icon: @Composable () -> Unit,
    iconBackground: Color,
    title: String,
    subtitle: String,
    borderColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(iconBackground, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column {
            Text(
                text = title,
                fontFamily = DmSans,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = Primary,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                fontFamily = DmSans,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = Color(0xFF464651),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SyncPill(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "syncPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "syncAlpha",
    )

    Row(
        modifier = modifier
            .background(TertiaryFixed.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer { alpha = pulse }
                .background(Color(0xFF3AB492), CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Sincronizado",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 7.sp,
            color = TertiaryContainer,
        )
    }
}

@Composable
private fun BellBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bellPing")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bellScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bellAlpha",
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(2.dp, BrandCoral.copy(alpha = 0.5f), CircleShape),
            )
            Box(Modifier.size(16.dp).border(1.dp, BrandCoral.copy(alpha = 0.6f), CircleShape))
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp))
                .background(BrandCoral, RoundedCornerShape(16.dp))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                .rotate(-12f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ChatChip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(6.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .rotate(3f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.MarkChatUnread,
            contentDescription = null,
            tint = MintDeep,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "Sin demoras",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = (-0.2).sp,
            color = Primary,
        )
    }
}

