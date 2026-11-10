package com.example.aulasegura.ui.screens.access

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer

private val CloudIndigo = Color(0xFF2B2F77)
private val ScreenGrey = Color(0xFFF0EDE9)
private val TintGrey = Color(0xFFC5C8E6)
private val MintSync = Color(0xFF5ED3B0)

/**
 * Hero illustration for onboarding page 3: offline-first cloud sync.
 * Translated 1:1 from the mockup SVG (viewBox 260x260).
 */
@Composable
fun IllustrationOffline(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(256.dp)
                .border(1.dp, PrimaryContainer.copy(alpha = 0.10f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(192.dp)
                .border(1.dp, PrimaryContainer.copy(alpha = 0.15f), CircleShape),
        )

        CloudSyncCanvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OfflinePill()
            Text(
                text = "v2.4",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = Primary.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun OfflinePill() {
    val pulse by rememberInfiniteTransition(label = "offlinePulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "offlineAlpha",
    )

    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.8f), CircleShape)
            .border(1.dp, TintGrey, CircleShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer { alpha = pulse }
                .background(Color(0xFF3AB492), CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "Modo Sin Conexión Listo",
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
            fontSize = 8.sp,
            letterSpacing = (-0.2).sp,
            color = Primary,
        )
    }
}

@Composable
private fun CloudSyncCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scaleFactor = minOf(size.width, size.height) / 260f
        val dx = (size.width - 260f * scaleFactor) / 2f
        val dy = (size.height - 260f * scaleFactor) / 2f

        withTransform({
            translate(dx, dy)
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
        }) {
            // Backdrop glow
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = 75f,
                center = Offset(130f, 115f),
            )

            // Cloud body
            val cloud = Path().apply {
                moveTo(78f, 128f)
                cubicTo(72f, 128f, 66f, 123f, 66f, 116f)
                cubicTo(66f, 109f, 71f, 104f, 78f, 104f)
                cubicTo(79f, 104f, 80f, 104f, 81f, 104f)
                cubicTo(84f, 94f, 94f, 86f, 106f, 86f)
                cubicTo(112f, 86f, 118f, 89f, 122f, 93f)
                cubicTo(126f, 87f, 134f, 83f, 143f, 83f)
                cubicTo(157f, 83f, 169f, 94f, 171f, 108f)
                cubicTo(176f, 109f, 180f, 114f, 180f, 120f)
                cubicTo(180f, 126f, 175f, 131f, 169f, 131f)
                lineTo(78f, 131f)
                close()
            }
            drawPath(cloud, CloudIndigo)

            val highlight = Path().apply {
                moveTo(106f, 91f)
                cubicTo(111f, 91f, 116f, 93f, 120f, 97f)
                cubicTo(124f, 92f, 131f, 88f, 139f, 88f)
                cubicTo(151f, 88f, 161f, 97f, 164f, 109f)
                cubicTo(168f, 110f, 171f, 114f, 171f, 118f)
                cubicTo(171f, 123f, 167f, 126f, 162f, 126f)
                lineTo(80f, 126f)
                cubicTo(75f, 126f, 71f, 122f, 71f, 117f)
                cubicTo(71f, 112f, 75f, 108f, 80f, 108f)
            }
            drawPath(highlight, Color.White.copy(alpha = 0.15f))

            // Bidirectional sync ring
            drawCircle(
                color = MintSync,
                radius = 28f,
                center = Offset(130f, 115f),
                style = Stroke(
                    width = 4.5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(32f, 14f)),
                ),
            )
            drawPath(
                path = Path().apply {
                    moveTo(148f, 94f)
                    lineTo(157f, 93f)
                    lineTo(155f, 103f)
                    close()
                },
                color = MintSync,
            )
            drawPath(
                path = Path().apply {
                    moveTo(112f, 136f)
                    lineTo(103f, 137f)
                    lineTo(105f, 127f)
                    close()
                },
                color = MintSync,
            )

            // Data stream connector
            drawLine(
                color = CloudIndigo,
                start = Offset(130f, 148f),
                end = Offset(130f, 170f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
            )
            drawCircle(color = MintSync, radius = 3f, center = Offset(130f, 155f))

            // Phone base
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(94f, 162f),
                size = Size(72f, 66f),
                cornerRadius = CornerRadius(16f, 16f),
            )
            drawRoundRect(
                color = CloudIndigo,
                topLeft = Offset(94f, 162f),
                size = Size(72f, 66f),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3.5f),
            )
            drawRoundRect(
                color = ScreenGrey,
                topLeft = Offset(103f, 172f),
                size = Size(54f, 42f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawRoundRect(
                color = CloudIndigo,
                topLeft = Offset(122f, 166f),
                size = Size(16f, 2.5f),
                cornerRadius = CornerRadius(1.25f, 1.25f),
            )
            drawRoundRect(
                color = MintSync,
                topLeft = Offset(109f, 178f),
                size = Size(42f, 4f),
                cornerRadius = CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = CloudIndigo,
                topLeft = Offset(109f, 186f),
                size = Size(32f, 4f),
                cornerRadius = CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = TintGrey,
                topLeft = Offset(109f, 194f),
                size = Size(22f, 4f),
                cornerRadius = CornerRadius(2f, 2f),
            )
            drawCircle(color = BrandCoral, radius = 3f, center = Offset(145f, 196f))

            // Security shield (top left)
            withTransform({ translate(36f, 68f) }) {
                val shieldOuter = Path().apply {
                    moveTo(26f, 4f)
                    cubicTo(14f, 4f, 4f, 10f, 4f, 23f)
                    cubicTo(4f, 41f, 26f, 54f, 26f, 54f)
                    cubicTo(26f, 54f, 48f, 41f, 48f, 23f)
                    cubicTo(48f, 10f, 38f, 4f, 26f, 4f)
                    close()
                }
                drawPath(shieldOuter, Color.White)
                drawPath(shieldOuter, CloudIndigo, style = Stroke(width = 3f))

                val shieldInner = Path().apply {
                    moveTo(26f, 8f)
                    cubicTo(17f, 8f, 8f, 13f, 8f, 23f)
                    cubicTo(8f, 38f, 26f, 49f, 26f, 49f)
                    cubicTo(26f, 49f, 44f, 38f, 44f, 23f)
                    cubicTo(44f, 13f, 35f, 8f, 26f, 8f)
                    close()
                }
                drawPath(shieldInner, CloudIndigo)

                drawPath(
                    path = Path().apply {
                        moveTo(18f, 24f)
                        lineTo(23f, 29f)
                        lineTo(34f, 18f)
                    },
                    color = MintSync,
                    style = Stroke(
                        width = 3.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }

            // Camera badge (top right)
            withTransform({ translate(178f, 64f) }) {
                drawCircle(color = BrandCoral, radius = 24f, center = Offset(28f, 28f))
                drawCircle(
                    color = Color.White,
                    radius = 24f,
                    center = Offset(28f, 28f),
                    style = Stroke(width = 3f),
                )
                val camera = Path().apply {
                    moveTo(19f, 22f)
                    lineTo(23f, 22f)
                    lineTo(25f, 19f)
                    lineTo(31f, 19f)
                    lineTo(33f, 22f)
                    lineTo(37f, 22f)
                    cubicTo(38.6f, 22f, 40f, 23.4f, 40f, 25f)
                    lineTo(40f, 35f)
                    cubicTo(40f, 36.6f, 38.6f, 38f, 37f, 38f)
                    lineTo(19f, 38f)
                    cubicTo(17.4f, 38f, 16f, 36.6f, 16f, 35f)
                    lineTo(16f, 25f)
                    cubicTo(16f, 23.4f, 17.4f, 22f, 19f, 22f)
                    close()
                }
                drawPath(camera, Color.White)
                drawCircle(color = BrandCoral, radius = 5f, center = Offset(28f, 30f))
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(28f, 30f),
                    style = Stroke(width = 1.5f),
                )
            }

            // Sparkles
            drawCircle(color = MintSync, radius = 4f, center = Offset(56f, 155f))
            drawCircle(color = CloudIndigo, radius = 5f, center = Offset(208f, 160f))
            drawCircle(color = BrandCoral, radius = 3f, center = Offset(196f, 135f))
            drawPath(
                path = Path().apply {
                    moveTo(68f, 62f)
                    lineTo(70f, 56f)
                    lineTo(76f, 58f)
                    lineTo(70f, 60f)
                    close()
                },
                color = BrandCoral,
            )
        }
    }
}
