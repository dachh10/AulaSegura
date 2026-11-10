package com.example.aulasegura.ui.screens.access

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.components.ShieldLogo
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.AulaTextStyles
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import kotlinx.coroutines.delay

private val SplashIndigo = Color(0xFF2B2F77)
private val SplashIndigoDeep = Color(0xFF1B1D4F)
private val SplashCoral = Color(0xFFFF6B57)
private val SplashMint = Color(0xFF5ED3B0)

/**
 * 1.1 Splash — ported from Stitch screen 365fa467756743fb9ae312b7472eccc9.
 *
 * The mockup draws a dummy Android status bar; here we let the system draw the
 * real one and keep the gradient full-bleed behind it.
 *
 * Temporary review behaviour: tap anywhere to continue, or wait 4 seconds.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val checkProgress = remember { Animatable(0f) }
    val glow by rememberInfiniteTransition(label = "splashGlow").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val progress by rememberInfiniteTransition(label = "splashProgress").animateFloat(
        initialValue = 0.12f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2800
                0.12f at 0 using LinearEasing
                0.72f at 1400 using FastOutSlowInEasing
                1f at 2800 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "progressFill",
    )

    LaunchedEffect(Unit) {
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, delayMillis = 250, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(Unit) {
        delay(4000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SplashIndigo, SplashIndigoDeep)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
    ) {
        ShieldPattern(modifier = Modifier.fillMaxSize(), alpha = 0.06f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGlow(
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                radius = 110.dp.toPx(),
                color = SplashMint,
                peakAlpha = 0.28f * glow,
            )
            drawGlow(
                center = Offset(size.width * 0.6f, size.height * 0.36f),
                radius = 70.dp.toPx(),
                color = SplashCoral,
                peakAlpha = 0.16f,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 32.dp),
                ) {
                    ShieldLogo(checkProgress = checkProgress.value)

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.White)) { append("Aula") }
                            withStyle(SpanStyle(color = SplashCoral)) { append("Segura") }
                        },
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-1).sp,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Tu escuela, siempre contigo",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        letterSpacing = 0.3.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(percent = 50)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(SplashCoral, RoundedCornerShape(percent = 50)),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "VERSIÓN 1.0",
                    style = AulaTextStyles.dataMonoSm,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.1.sp,
                )
            }
        }
    }
}

private fun DrawScope.drawGlow(
    center: Offset,
    radius: Float,
    color: Color,
    peakAlpha: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = peakAlpha),
                color.copy(alpha = peakAlpha * 0.3f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
