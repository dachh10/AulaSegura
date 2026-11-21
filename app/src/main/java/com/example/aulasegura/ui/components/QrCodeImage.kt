package com.example.aulasegura.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Renders the linking payload as a QR entirely on the device: no network, no
 * firebase call and no data beyond the uid and the child's name.
 */
@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(content) {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.MARGIN, 2)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        }
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
        Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Código QR para vincular a tu hijo",
        modifier = modifier,
    )
}
