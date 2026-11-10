package com.example.aulasegura.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.PrimaryContainer

/**
 * Stylized school-campus map used by the incident detail variant.
 *
 * This is a vector rendering of the Stitch mockup, not a live map. It exists so the
 * screen is reviewable with zero setup (no API key, no Google Play Services).
 *
 * To swap in the real Google Maps SDK later, keep this composable's signature and
 * replace only its body with:
 * ```
 * GoogleMap(modifier = modifier, cameraPositionState = rememberCameraPositionState {
 *     position = CameraPosition.fromLatLngZoom(LatLng(19.2926, -99.6557), 17f)
 * })
 * ```
 * after adding `com.google.maps.android:maps-compose` + `play-services-maps` and the
 * `MAPS_API_KEY` meta-data in AndroidManifest.xml.
 */
@Composable
fun IncidentMap(
    modifier: Modifier = Modifier,
    onRecenter: () -> Unit = {},
) {
    val measurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MapBase),
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            fun px(fraction: Float) = w * fraction
            fun py(fraction: Float) = h * fraction

            fun block(
                left: Float,
                top: Float,
                right: Float,
                bottom: Float,
                fill: Color,
                border: Color,
                radius: Float = 6f,
            ) {
                val topLeft = Offset(px(left), py(top))
                val blockSize = Size(px(right - left), py(bottom - top))
                val corner = CornerRadius(radius.dp.toPx(), radius.dp.toPx())
                drawRoundRect(color = fill, topLeft = topLeft, size = blockSize, cornerRadius = corner)
                drawRoundRect(
                    color = border,
                    topLeft = topLeft,
                    size = blockSize,
                    cornerRadius = corner,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            fun label(
                text: String,
                centerX: Float,
                centerY: Float,
                style: TextStyle,
                color: Color = Color(0xFF6B6E5F),
            ) {
                val layout = measurer.measure(AnnotatedString(text), style = style)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        centerX - layout.size.width / 2f,
                        centerY - layout.size.height / 2f,
                    ),
                    color = color,
                )
            }

            // Streets (drawn first so blocks sit on top).
            drawRect(StreetFill, topLeft = Offset(0f, py(0.26f)), size = Size(w, py(0.10f)))
            drawRect(StreetFill, topLeft = Offset(0f, py(0.74f)), size = Size(w, py(0.10f)))
            drawRect(StreetFill, topLeft = Offset(px(0.16f), 0f), size = Size(px(0.10f), h))
            drawRect(StreetFill, topLeft = Offset(px(0.70f), 0f), size = Size(px(0.09f), h))
            drawLine(
                color = StreetBorder,
                start = Offset(0f, py(0.26f)),
                end = Offset(w, py(0.26f)),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = StreetBorder,
                start = Offset(0f, py(0.84f)),
                end = Offset(w, py(0.84f)),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = StreetBorder,
                start = Offset(px(0.16f), 0f),
                end = Offset(px(0.16f), h),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = StreetBorder,
                start = Offset(px(0.70f), 0f),
                end = Offset(px(0.70f), h),
                strokeWidth = 1.dp.toPx(),
            )

            // Green area.
            drawRect(ParkFill, topLeft = Offset(0f, 0f), size = Size(px(0.16f), py(0.26f)))

            // Buildings.
            block(0.62f, 0.08f, 0.92f, 0.23f, BeigeFill, BeigeBorder, radius = 5f)
            block(0.05f, 0.56f, 0.31f, 0.72f, BeigeFill, BeigeBorder, radius = 5f)
            block(0.28f, 0.40f, 0.50f, 0.60f, BlockFill, BlockBorder, radius = 6f)
            block(0.80f, 0.42f, 0.98f, 0.60f, BlockFill, BlockBorder, radius = 6f)
            block(0.30f, 0.86f, 0.62f, 0.98f, BlockFill, BlockBorder, radius = 6f)

            val microStyle = TextStyle(
                fontFamily = DmSans,
                fontSize = 6.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp,
            )
            label("CANCHA", px(0.77f), py(0.155f), microStyle)
            label("AUDITORIO", px(0.18f), py(0.64f), microStyle)

            // Edificio B: white pill label inside the block.
            val pillLeft = px(0.315f)
            val pillTop = py(0.44f)
            val pillWidth = px(0.15f)
            val pillHeight = 14.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(pillLeft, pillTop),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
            )
            drawRoundRect(
                color = BlockBorder,
                topLeft = Offset(pillLeft, pillTop),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f),
                style = Stroke(width = 1.dp.toPx()),
            )
            label(
                text = "EDIFICIO B",
                centerX = pillLeft + pillWidth / 2f,
                centerY = pillTop + pillHeight / 2f,
                style = TextStyle(
                    fontFamily = DmSans,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color(0xFF4A4B57),
            )

            // Safe-school perimeter.
            val perimeterCenter = Offset(px(0.46f), py(0.47f))
            val perimeterRadius = px(0.27f)
            drawCircle(
                color = Teal.copy(alpha = 0.85f),
                radius = perimeterRadius,
                center = perimeterCenter,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                    ),
                ),
            )
            label(
                text = "PERÍMETRO ESCOLAR SEGURO",
                centerX = perimeterCenter.x,
                centerY = perimeterCenter.y + perimeterRadius + 9.dp.toPx(),
                style = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                ),
                color = Teal,
            )

            // Incident pin.
            val pin = Offset(px(0.46f), py(0.44f))
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 13.dp.toPx(), center = pin)
            drawCircle(Coral, radius = 10.dp.toPx(), center = pin)
            drawCircle(Color.White, radius = 3.5.dp.toPx(), center = pin)

            // Pin label pill.
            val labelLeft = pin.x + 16.dp.toPx()
            val labelTop = pin.y - 8.dp.toPx()
            val labelWidth = 48.dp.toPx()
            val labelHeight = 16.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelHeight / 2f, labelHeight / 2f),
            )
            drawRoundRect(
                color = Coral.copy(alpha = 0.45f),
                topLeft = Offset(labelLeft, labelTop),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(labelHeight / 2f, labelHeight / 2f),
                style = Stroke(width = 1.dp.toPx()),
            )
            label(
                text = "Incidente",
                centerX = labelLeft + labelWidth / 2f,
                centerY = labelTop + labelHeight / 2f,
                style = TextStyle(
                    fontFamily = DmSans,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = CoralDark,
            )
        }

        // Recenter control.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE6E1D8), CircleShape)
                .clickable(onClick = onRecenter),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "Centrar en mi ubicación",
                tint = PrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private val MapBase = Color(0xFFE9F0E8)
private val StreetFill = Color(0xFFFAFAF5)
private val StreetBorder = Color(0xFFE3E7DB)
private val BlockFill = Color(0xFFFCFCF8)
private val BlockBorder = Color(0xFFE3E7DB)
private val BeigeFill = Color(0xFFF4EFE4)
private val BeigeBorder = Color(0xFFE7DFCE)
private val ParkFill = Color(0xFFDCEAD8)
private val Teal = Color(0xFF4FAE9C)
private val Coral = Color(0xFFFF6B57)
private val CoralDark = Color(0xFFAE3123)
