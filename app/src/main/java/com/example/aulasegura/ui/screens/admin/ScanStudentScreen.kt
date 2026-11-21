package com.example.aulasegura.ui.screens.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aulasegura.domain.repository.ParentLinkQr
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.google.zxing.BinaryBitmap
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val FRAME_SIZE_DP = 260

/**
 * Full-screen camera with the Stitch frame and scan line. The decoded payload is
 * validated before leaving: a foreign QR produces a message and writes nothing.
 */
@Composable
fun ScanStudentScreen(
    onBack: () -> Unit,
    onFound: (ParentLinkQr) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val handled = remember { AtomicBoolean(false) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    var message by remember { mutableStateOf<String?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            // The composable can be recreated (rotation, back stack). Release
            // the camera before the next instance binds, otherwise CameraX sees
            // two Preview+Analysis use cases and throws "No supported surface
            // combination" on the main thread.
            val release = ProcessCameraProvider.getInstance(context)
            release.addListener(
                { runCatching { release.get().unbindAll() } },
                ContextCompat.getMainExecutor(context),
            )
            analyzerExecutor.shutdownNow()
        }
    }

    // The dashboard asks for permission before navigating. If anything else
    // opens this route, leave instead of binding a camera we cannot use.
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(top = 28.dp)) {
        AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewView ->
                        // Bound once when the view is created, not on every
                        // recomposition: re-binding on a state change would
                        // restart the preview and drop frames mid-scan.
                        val future = ProcessCameraProvider.getInstance(context)
                        future.addListener(
                            {
                                val provider = future.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val analyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                analyzer.setAnalyzer(analyzerExecutor) { image ->
                                    runCatching {
                                        val raw = decodeQr(image)
                                        if (raw != null && handled.compareAndSet(false, true)) {
                                            val result = ScanStudentUi.from(raw)
                                            mainHandler.post {
                                                if (result.found != null) {
                                                    onFound(result.found)
                                                } else {
                                                    message = result.message
                                                    handled.set(false)
                                                }
                                            }
                                        }
                                    }.also { image.close() }
                                }
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analyzer,
                                ).also { cameraControl = it.cameraControl }
                            },
                            ContextCompat.getMainExecutor(context),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            val transition = rememberInfiniteTransition(label = "scanLine")
            val lineY by transition.animateFloat(
                initialValue = 0f,
                targetValue = FRAME_SIZE_DP.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(1800),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scanLineY",
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(FRAME_SIZE_DP.dp)
                        .border(2.dp, SurfaceContainerLowest, RoundedCornerShape(20.dp))
                        .drawBehind {
                            drawLine(
                                color = Color(0xFFFC6955),
                                start = androidx.compose.ui.geometry.Offset(0f, lineY),
                                end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                                strokeWidth = 3f,
                            )
                        },
                )
            }
            if (message != null) {
                Text(
                    text = message.orEmpty(),
                    color = Error,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 110.dp),
                )
            }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Regresar",
                tint = SurfaceContainerLowest,
                modifier = Modifier.size(28.dp).clickable(onClick = onBack),
            )
            Text(
                text = "Escanear alumno",
                color = SurfaceContainerLowest,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .size(40.dp)
                .border(1.dp, SurfaceContainerLowest, RoundedCornerShape(20.dp))
                .padding(8.dp)
                .then(
                    Modifier.clickableTorch {
                        torchOn = !torchOn
                        cameraControl?.enableTorch(torchOn)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.FlashlightOn,
                contentDescription = "Linterna",
                tint = SurfaceContainerLowest,
            )
        }
        Text(
            text = "Apunta al código QR del tutor",
            color = SurfaceContainerLowest,
            fontFamily = DmSans,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
        )
    }
}

private fun Modifier.clickableTorch(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

/**
 * Decodes one camera frame. CameraX's [ImageProxy.toBitmap] already applies the
 * sensor rotation, so no manual LuminanceSource rotation is needed - a
 * PlanarYUVLuminanceSource does not support it and threw off the analyzer
 * thread. Everything is contained: a bad frame must never kill the process.
 */
private fun decodeQr(image: ImageProxy): String? = try {
    val bitmap = image.toBitmap()
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val luma = luminanceFromArgb(pixels)
    val source = object : com.google.zxing.LuminanceSource(width, height) {
        override fun getRow(y: Int, row: ByteArray?): ByteArray {
            val out = row ?: ByteArray(width)
            System.arraycopy(luma, y * width, out, 0, width)
            return out
        }

        override fun getMatrix(): ByteArray = luma
    }
    QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
} catch (_: Exception) {
    null
}
