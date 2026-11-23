package com.example.aulasegura.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Gate every system camera intent behind the runtime CAMERA permission.
 *
 * The manifest declares `android.permission.CAMERA` because the QR scanners use
 * CameraX. Android refuses to start `ACTION_IMAGE_CAPTURE` from an app that
 * declares CAMERA without holding it: the launcher throws `SecurityException`
 * and, when the caller swallows that failure, tapping "Tomar foto" looks like a
 * dead button. Asking here keeps the system camera usable in every role while
 * the scans keep working the same way.
 *
 * The returned function runs [onGranted] immediately when the permission is
 * already held, or after the system dialog is accepted. [onDenied] runs when the
 * answer is no, including the permanent "don't ask again" state.
 */
@Composable
fun rememberCameraPermissionGate(onDenied: () -> Unit): (onGranted: () -> Unit) -> Unit {
    val context = LocalContext.current
    val currentOnDenied by rememberUpdatedState(onDenied)
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = pendingAction
        pendingAction = null
        if (granted) action?.invoke() else currentOnDenied()
    }

    return { onGranted ->
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            onGranted()
        } else {
            pendingAction = onGranted
            runCatching { permissionLauncher.launch(Manifest.permission.CAMERA) }
                .onFailure {
                    pendingAction = null
                    currentOnDenied()
                }
        }
    }
}
