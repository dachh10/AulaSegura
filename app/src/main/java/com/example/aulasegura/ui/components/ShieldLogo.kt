package com.example.aulasegura.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ShieldSurfaceTop = Color(0xFFFFFFFF)
private val ShieldSurfaceBottom = Color(0xFFF4F5FD)
private val CheckShort = Color(0xFFFF6B57)
private val CheckLong = Color(0xFF2B2F77)

/**
 * The AulaSegura brand shield: white rounded shield with a coral short stroke
 * and an indigo long stroke forming the check mark.
 *
 * Geometry taken 1:1 from the Stitch splash mockup (viewBox 108x120).
 *
 * @param checkProgress 0f..1f draw-on progress of the check mark.
 */
@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    width: Dp = 108.dp,
    height: Dp = 120.dp,
    checkProgress: Float = 1f,
) {
    Canvas(modifier = modifier.size(width, height)) {
        val sx = size.width / 108f
        val sy = size.height / 120f
        val stroke = 10f * minOf(sx, sy)

        val shield = Path().apply {
            moveTo(54f * sx, 4f * sy)
            cubicTo(77f * sx, 4f * sy, 98f * sx, 13.5f * sy, 98f * sx, 29.5f * sy)
            cubicTo(98f * sx, 64f * sy, 71f * sx, 96f * sy, 54f * sx, 114f * sy)
            cubicTo(37f * sx, 96f * sy, 10f * sx, 64f * sy, 10f * sx, 29.5f * sy)
            cubicTo(10f * sx, 13.5f * sy, 31f * sx, 4f * sy, 54f * sx, 4f * sy)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.verticalGradient(listOf(ShieldSurfaceTop, ShieldSurfaceBottom)),
        )

        val p0 = Offset(32f * sx, 56f * sy)
        val p1 = Offset(48f * sx, 72f * sy)
        val p2 = Offset(77f * sx, 39f * sy)
        val split = 0.45f

        if (checkProgress > 0f) {
            val first = (checkProgress / split).coerceIn(0f, 1f)
            drawLine(
                color = CheckShort,
                start = p0,
                end = lerp(p0, p1, first),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        if (checkProgress > split) {
            val second = ((checkProgress - split) / (1f - split)).coerceIn(0f, 1f)
            drawLine(
                color = CheckLong,
                start = p1,
                end = lerp(p1, p2, second),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Repeating shield-with-check watermark used behind headers and empty states.
 */
@Composable
fun ShieldPattern(
    modifier: Modifier = Modifier,
    alpha: Float = 0.06f,
    color: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        drawShieldTiles(alpha = alpha, color = color)
    }
}

/**
 * Same watermark, painted as the background of the node itself. Use it on a
 * container that already has its own background (dark headers) so the pattern
 * sits behind its children without adding an extra Box layer.
 */
fun Modifier.shieldPattern(
    alpha: Float = 0.06f,
    color: Color = Color.White,
): Modifier = drawBehind {
    drawShieldTiles(alpha = alpha, color = color)
}

private fun DrawScope.drawShieldTiles(alpha: Float, color: Color) {
    val tileW = 48.dp.toPx()
    val tileH = 56.dp.toPx()
    val sx = tileW / 48f
    val sy = tileH / 56f
    val strokeWidth = 1.5f * minOf(sx, sy)
    val paint = color.copy(alpha = alpha)

    val cols = (size.width / tileW).toInt() + 2
    val rows = (size.height / tileH).toInt() + 2

    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val ox = col * tileW
            val oy = row * tileH

            val shield = Path().apply {
                moveTo(ox + 24f * sx, oy + 6f * sy)
                cubicTo(ox + 32f * sx, oy + 6f * sy, ox + 38f * sx, oy + 9f * sy, ox + 38f * sx, oy + 15f * sy)
                cubicTo(ox + 38f * sx, oy + 27f * sy, ox + 28f * sx, oy + 39f * sy, ox + 24f * sx, oy + 43f * sy)
                cubicTo(ox + 20f * sx, oy + 39f * sy, ox + 10f * sx, oy + 27f * sy, ox + 10f * sx, oy + 15f * sy)
                cubicTo(ox + 10f * sx, oy + 9f * sy, ox + 16f * sx, oy + 6f * sy, ox + 24f * sx, oy + 6f * sy)
                close()
            }
            drawPath(shield, paint, style = Stroke(width = strokeWidth))

            val check = Path().apply {
                moveTo(ox + 19f * sx, oy + 22f * sy)
                lineTo(ox + 22.5f * sx, oy + 25.5f * sy)
                lineTo(ox + 29f * sx, oy + 18f * sy)
            }
            drawPath(
                path = check,
                color = paint,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
