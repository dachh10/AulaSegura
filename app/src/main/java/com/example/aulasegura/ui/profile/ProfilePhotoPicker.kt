package com.example.aulasegura.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

/** Camera and gallery actions, identical for tutor, teacher, director and sign-up. */
data class ProfilePhotoPicker(
    val takePhoto: () -> Unit,
    val pickFromGallery: () -> Unit,
)

/**
 * Uses TakePicturePreview instead of TakePicture: some camera apps never write
 * the file behind the supplied content:// URI, so TakePicture returns
 * `saved=false` on those devices. The preview Bitmap is written by us into the
 * same FileProvider file, so the rest of the app still receives a normal Uri.
 *
 * Errors are reported through [onError]. Screens that do not pass one get a
 * Toast, so a denied permission or an unavailable camera app never looks like a
 * button that does nothing.
 */
@Composable
fun rememberProfilePhotoPicker(
    onPhoto: (Uri) -> Unit,
    onError: ((String) -> Unit)? = null,
): ProfilePhotoPicker {
    val context = LocalContext.current
    val reportError: (String) -> Unit = onError ?: { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    fun newCaptureFile(): File {
        val directory = File(context.cacheDir, "perfil").apply { mkdirs() }
        return File(directory, "perfil_${System.currentTimeMillis()}.jpg")
    }

    fun deliver(file: File?) {
        if (file == null) return
        runCatching {
            onPhoto(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }.onFailure { reportError("No se pudo abrir la foto. Intenta con la galería.") }
    }

    val previewLauncher: ActivityResultLauncher<Void?> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        val file = pendingFile
        pendingFile = null
        if (bitmap == null) {
            file?.delete()
            reportError("La cámara no devolvió la foto. Prueba con la galería.")
            return@rememberLauncherForActivityResult
        }
        val saved = runCatching {
            file?.outputStream()?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            } != null
        }.getOrDefault(false)
        if (saved) deliver(file) else {
            file?.delete()
            reportError("No se pudo guardar la foto. Prueba con la galería.")
        }
    }

    val galleryLauncher: ActivityResultLauncher<String> = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) onPhoto(uri) }

    val requireCameraPermission = rememberCameraPermissionGate(
        onDenied = {
            reportError("Permite el acceso a la cámara para tomar la foto.")
        },
    )

    return ProfilePhotoPicker(
        takePhoto = {
            requireCameraPermission {
                val file = newCaptureFile()
                pendingFile = file
                runCatching { previewLauncher.launch(null) }.onFailure {
                    pendingFile = null
                    file.delete()
                    reportError("No se pudo abrir la cámara. Prueba con la galería.")
                }
            }
        },
        pickFromGallery = { galleryLauncher.launch("image/*") },
    )
}


/** The same "Cambiar foto" dialog for tutor, teacher, director and sign-up. */
@Composable
fun ProfilePhotoOptionsDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            color = com.example.aulasegura.ui.theme.SurfaceContainerLowest,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Cambiar foto",
                    color = com.example.aulasegura.ui.theme.PrimaryContainer,
                    fontFamily = com.example.aulasegura.ui.theme.BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Text(
                    text = "Elige una foto reciente o toma una ahora.",
                    color = com.example.aulasegura.ui.theme.Outline,
                    fontFamily = com.example.aulasegura.ui.theme.DmSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    text = "Tomar foto",
                    icon = Icons.Filled.CameraAlt,
                    filled = true,
                    onClick = onTakePhoto,
                )
                Button(
                    text = "Elegir de la galería",
                    icon = Icons.Filled.PhotoLibrary,
                    filled = false,
                    onClick = onPickFromGallery,
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = "Cancelar",
                        color = com.example.aulasegura.ui.theme.Outline,
                        fontFamily = com.example.aulasegura.ui.theme.DmSans,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Button(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
            .background(
                if (filled) com.example.aulasegura.ui.theme.PrimaryContainer
                else Color.Transparent,
            )
            .then(
                if (filled) Modifier else Modifier.border(
                    1.dp,
                    com.example.aulasegura.ui.theme.PrimaryContainer,
                    androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) Color.White
            else com.example.aulasegura.ui.theme.PrimaryContainer,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = if (filled) Color.White
            else com.example.aulasegura.ui.theme.PrimaryContainer,
            fontFamily = com.example.aulasegura.ui.theme.DmSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}
