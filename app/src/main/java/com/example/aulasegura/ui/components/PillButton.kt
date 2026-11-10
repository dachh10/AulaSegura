package com.example.aulasegura.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.PillShape
import com.example.aulasegura.ui.theme.PrimaryContainer

/**
 * Full-width pill CTA used by the access screens.
 * Press feedback is a 0.98 scale, matching the mockups' `active:scale-[0.98]`.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BrandCoral,
    contentColor: Color = Color.White,
    height: Dp = 48.dp,
    fontSize: TextUnit = 15.sp,
    trailingIcon: ImageVector? = null,
    shadowColor: Color = BrandCoral.copy(alpha = 0.25f),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "pillScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 4.dp,
                shape = PillShape,
                ambientColor = shadowColor,
                spotColor = shadowColor,
            )
            .clip(PillShape)
            .background(containerColor)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                color = contentColor,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Outlined pill used for the "Continuar con Google" action. */
@Composable
fun GooglePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "googleScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PillShape)
            .background(Color.White)
            .border(1.5.dp, PrimaryContainer, PillShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoogleLogo(size = 16.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

/** Multicolored Google "G", traced from the mockup SVG (viewBox 24x24). */
@Composable
fun GoogleLogo(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f

        val blue = Path().apply {
            moveTo(22.56f * s, 12.25f * s)
            cubicTo(22.56f * s, 11.47f * s, 22.49f * s, 10.72f * s, 22.36f * s, 10.00f * s)
            lineTo(12f * s, 10f * s)
            lineTo(12f * s, 14.26f * s)
            lineTo(17.92f * s, 14.26f * s)
            cubicTo(17.66f * s, 15.63f * s, 16.88f * s, 16.79f * s, 15.71f * s, 17.57f * s)
            lineTo(15.71f * s, 20.34f * s)
            lineTo(19.28f * s, 20.34f * s)
            cubicTo(21.36f * s, 18.42f * s, 22.56f * s, 15.60f * s, 22.56f * s, 12.25f * s)
            close()
        }
        val green = Path().apply {
            moveTo(12f * s, 23f * s)
            cubicTo(14.97f * s, 23f * s, 17.46f * s, 22.02f * s, 19.28f * s, 20.34f * s)
            lineTo(15.71f * s, 17.57f * s)
            cubicTo(14.73f * s, 18.23f * s, 13.48f * s, 18.63f * s, 12f * s, 18.63f * s)
            cubicTo(9.14f * s, 18.63f * s, 6.71f * s, 16.70f * s, 5.84f * s, 14.10f * s)
            lineTo(2.18f * s, 14.10f * s)
            lineTo(2.18f * s, 16.94f * s)
            cubicTo(3.99f * s, 20.53f * s, 7.70f * s, 23f * s, 12f * s, 23f * s)
            close()
        }
        val yellow = Path().apply {
            moveTo(5.84f * s, 14.09f * s)
            cubicTo(5.62f * s, 13.43f * s, 5.49f * s, 12.73f * s, 5.49f * s, 12.00f * s)
            cubicTo(5.49f * s, 11.27f * s, 5.62f * s, 10.57f * s, 5.84f * s, 9.91f * s)
            lineTo(5.84f * s, 7.06f * s)
            lineTo(2.18f * s, 7.06f * s)
            cubicTo(1.43f * s, 8.55f * s, 1.00f * s, 10.22f * s, 1.00f * s, 12.00f * s)
            cubicTo(1.00f * s, 13.78f * s, 1.43f * s, 15.45f * s, 2.18f * s, 16.94f * s)
            lineTo(5.03f * s, 16.72f * s)
            lineTo(5.84f * s, 16.09f * s)
            close()
        }
        val red = Path().apply {
            moveTo(12f * s, 5.38f * s)
            cubicTo(13.62f * s, 5.38f * s, 15.06f * s, 5.94f * s, 16.21f * s, 7.02f * s)
            lineTo(19.36f * s, 3.87f * s)
            cubicTo(17.45f * s, 2.09f * s, 14.97f * s, 1.00f * s, 12f * s, 1.00f * s)
            cubicTo(7.70f * s, 1.00f * s, 3.99f * s, 3.47f * s, 2.18f * s, 7.06f * s)
            lineTo(5.84f * s, 9.90f * s)
            cubicTo(6.71f * s, 7.30f * s, 9.14f * s, 5.38f * s, 12f * s, 5.38f * s)
            close()
        }

        drawPath(yellow, Color(0xFFFBBC05))
        drawPath(blue, Color(0xFF4285F4))
        drawPath(green, Color(0xFF34A853))
        drawPath(red, Color(0xFFEA4335))
    }
}
