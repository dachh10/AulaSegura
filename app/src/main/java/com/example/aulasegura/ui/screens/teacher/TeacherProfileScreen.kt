package com.example.aulasegura.ui.screens.teacher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.aulasegura.ui.components.TeacherBottomBar
import com.example.aulasegura.ui.components.TeacherTab
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.components.shieldPattern
import com.example.aulasegura.ui.profile.ChangePasswordScreen
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant
import java.io.File

/**
 * Teacher — Perfil. Ported from Stitch screen `6e82dee0cb5245af83656dbbd9073ce9`
 * ("Perfil del Docente - AulaSegura", project 7677408035297541086).
 *
 * The identity block is the session profile: name, email, school and subjects come
 * from `users/{uid}` through the ViewModel. The avatar is uploaded to Cloudinary
 * and its URL written back to Firestore, so a cold start reads the same picture
 * from the local profile cache.
 *
 * The notification switch stays inert on purpose: Android 13's permission is
 * requested when the app opens, not from this row.
 */

private val WarmBorder = Color(0xFFEAE6DF)
private val RowDivider = Color(0xFFF0ECE4)
private val LogoutRed = Color(0xFFE5484D)
private val SyncChipBackground = Color(0xFFECFDF5)
private val SyncChipBorder = Color(0xFFA7F3D0)
private val SyncChipText = Color(0xFF059669)

private val ProfileCardShape = RoundedCornerShape(24.dp)
private val GroupCardShape = RoundedCornerShape(20.dp)
private val IconTileShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun TeacherProfileScreen(
    ui: TeacherProfileUi,
    onSelectTab: (TeacherTab) -> Unit,
    onLogout: () -> Unit = {},
    onChangePhoto: (Uri) -> Unit = {},
    onDismissMessage: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onEditNameChange: (String) -> Unit = {},
    onEditSubjectsChange: (String) -> Unit = {},
    onSaveProfile: () -> Unit = {},
    onCurrentPasswordChange: (String) -> Unit = {},
    onNewPasswordChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onSavePassword: () -> Unit = {},
    onCloseDialog: () -> Unit = {},
) {
    val context = LocalContext.current
    var showPhotoOptions by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    // The notification switch reflects the Android permission, not a local flag:
    // the OS is what decides whether the phone rings, and it can only be changed
    // from the system dialog or Settings. OnResume keeps the row honest after the
    // teacher comes back from either one.
    var notificationsGranted by remember { mutableStateOf(notificationsEnabled(context)) }
    val requestNotifications = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = notificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
            runCatching { requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            runCatching { context.startActivity(intent) }
        }
    }

    // The camera writes into a cache file the FileProvider exposes to the camera
    // app; the ViewModel only sees the resulting Uri and compresses it. The same
    // pattern the incident form uses, with its own directory so the two never mix.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val captured = pendingCapture
        pendingCapture = null
        if (saved && captured != null) {
            onChangePhoto(Uri.fromFile(captured))
        } else {
            captured?.delete()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) onChangePhoto(uri)
    }

    fun capturePhoto() {
        val directory = File(context.cacheDir, "perfil").apply { mkdirs() }
        val file = File(directory, "perfil_${System.currentTimeMillis()}.jpg")
        pendingCapture = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        runCatching { cameraLauncher.launch(uri) }.onFailure {
            pendingCapture = null
            file.delete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 112.dp),
        ) {
            Reveal(0) {
                ProfileHeader(
                    ui = ui,
                    onChangePhoto = { showPhotoOptions = true },
                )
            }

            if (ui.message != null) {
                Spacer(Modifier.height(10.dp))
                Reveal(20) {
                    ProfileMessage(
                        message = ui.message,
                        onDismiss = onDismissMessage,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Reveal(40) { MySubjectsSection(subjects = ui.subjects) }

            Spacer(Modifier.height(14.dp))
            Reveal(100) {
                SettingsCard(
                    schoolId = ui.schoolId,
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onOpenPermissions = onOpenPermissions,
                    notificationsGranted = notificationsGranted,
                    onNotificationsClick = ::openNotificationSettings,
                )
            }

            Spacer(Modifier.height(16.dp))
            Reveal(160) { LogoutSection(onLogout = onLogout) }
        }

        TeacherBottomBar(
            active = TeacherTab.Perfil,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Full-screen account forms cover the profile and its bottom bar. They
        // share this screen's ViewModel, so saving keeps `saving`/`message` and
        // returning shows a profile refreshed from the same session profile.
        when (ui.screen) {
            ProfileSection.EditProfile -> TeacherEditProfileScreen(
                ui = ui,
                onBack = onCloseDialog,
                onChangePhoto = { showPhotoOptions = true },
                onNameChange = onEditNameChange,
                onSubjectsChange = onEditSubjectsChange,
                onSave = onSaveProfile,
                onDismissMessage = onDismissMessage,
            )
            ProfileSection.ChangePassword -> ChangePasswordScreen(
                current = ui.currentPassword,
                new = ui.newPassword,
                confirmation = ui.confirmPassword,
                saving = ui.saving,
                saved = ui.saved,
                message = ui.message,
                onBack = onCloseDialog,
                onCurrentChange = onCurrentPasswordChange,
                onNewChange = onNewPasswordChange,
                onConfirmChange = onConfirmPasswordChange,
                onSave = onSavePassword,
                onDismissMessage = onDismissMessage,
            )
            ProfileSection.None -> Unit
        }
    }

    if (showPhotoOptions) {
        // A Dialog + Surface instead of AlertDialog: the overload that lets us
        // place three options does not draw Material's dialog container, so the
        // options ended up floating over the page. Here the card is explicit and
        // opaque, and each option is a full-width button with its own hit target.
        Dialog(onDismissRequest = { showPhotoOptions = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceContainerLowest,
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
                        color = PrimaryContainer,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                    Text(
                        text = "Elige una foto reciente o toma una ahora.",
                        color = Outline,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    val (cameraOptionInteraction, cameraOptionPress) = rememberPressScale(0.98f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .then(cameraOptionPress)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(BrandCoral)
                            .clickable(
                                interactionSource = cameraOptionInteraction,
                                indication = null,
                                onClick = {
                                    showPhotoOptions = false
                                    capturePhoto()
                                },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Tomar foto",
                            color = Color.White,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }

                    val (galleryOptionInteraction, galleryOptionPress) = rememberPressScale(0.98f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .then(galleryOptionPress)
                            .clip(RoundedCornerShape(percent = 50))
                            .border(1.dp, PrimaryContainer, RoundedCornerShape(percent = 50))
                            .clickable(
                                interactionSource = galleryOptionInteraction,
                                indication = null,
                                onClick = {
                                    showPhotoOptions = false
                                    galleryLauncher.launch("image/*")
                                },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = PrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Elegir de la galería",
                            color = PrimaryContainer,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }

                    TextButton(
                        onClick = { showPhotoOptions = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Outline,
                            fontFamily = DmSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }

}
@Composable
private fun ProfileMessage(
    message: String,
    onDismiss: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.98f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BrandCoral.copy(alpha = 0.10f))
            .border(1.dp, BrandCoral.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onDismiss,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ProfileHeader(
    ui: TeacherProfileUi,
    onChangePhoto: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer)
            .shieldPattern(alpha = 0.07f, color = Color.White)
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(80.dp)) {
            val avatarModifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceContainer)
                .border(3.dp, Color.White, CircleShape)

            if (ui.photoUrl == null) {
                Box(modifier = avatarModifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = profileInitials(ui.fullName),
                        color = PrimaryContainer,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                    )
                }
            } else {
                AsyncImage(
                    model = ui.photoUrl,
                    contentDescription = "Foto de perfil",
                    modifier = avatarModifier,
                    contentScale = ContentScale.Crop,
                )
            }

            val (cameraInteraction, cameraPress) = rememberPressScale(0.9f)
            Box(
                modifier = cameraPress
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandCoral)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(
                        enabled = !ui.uploading,
                        interactionSource = cameraInteraction,
                        indication = null,
                        onClick = onChangePhoto,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (ui.uploading) {
            Text(
                text = "Subiendo foto…",
                color = Color.White.copy(alpha = 0.85f),
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
        }

        Text(
            text = ui.fullName.ifBlank { "Perfil del docente" },
            color = Color.White,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.33).sp,
        )

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text = profileSubtitle(ui.subjects),
                    color = Color.White,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.24.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = ui.email.ifBlank { "Sin correo registrado" },
                color = Color.White.copy(alpha = 0.75f),
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.24.sp,
            )
        }

        if (ui.schoolId.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Escuela · ${ui.schoolId}",
                color = Color.White.copy(alpha = 0.65f),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

/**
 * Subjects come from the profile itself. The mockup showed three invented groups
 * with "28 alumnos" each; the real groups live in "Mis grupos", and printing fake
 * ones here would be a lie the teacher cannot correct from this screen.
 */
@Composable
private fun MySubjectsSection(subjects: List<String>) {
    val labels = subjects.filter { it.isNotBlank() }.ifEmpty { listOf("General") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "MIS MATERIAS",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.55.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            labels.forEach { label ->
                SubjectSlotCard(label = label, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SubjectSlotCard(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(GroupCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, GroupCardShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            maxLines = 2,
        )
    }
}

@Composable
private fun SettingsCard(
    schoolId: String,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenPermissions: () -> Unit,
    notificationsGranted: Boolean,
    onNotificationsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ProfileCardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, ProfileCardShape),
    ) {
        SettingsRow(
            icon = Icons.Filled.Person,
            label = "Editar perfil",
            onClick = onEditProfile,
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Lock,
            label = "Cambiar contraseña",
            onClick = onChangePassword,
        )
        SettingsDivider()
        SettingsValueRow(label = "Escuela", value = schoolId.ifBlank { "Sin asignar" })
        SettingsDivider()
        NotificationToggleRow(
            granted = notificationsGranted,
            onClick = onNotificationsClick,
        )
        SettingsDivider()
        SettingsRow(
            icon = Icons.Filled.Shield,
            label = "Permisos de la app",
            onClick = onOpenPermissions,
        )
        SettingsDivider()
        SyncStatusRow()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = icon)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = Icons.Filled.School)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Text(
            text = value,
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

/**
 * The switch is a readout of the Android permission. Tapping the row either asks
 * for it (Android 13+) or opens the app's notification settings, where the teacher
 * can turn it off. Writing a flag in Firestore would be cosmetic: without Cloud
 * Functions the push is sent manually from the console.
 */
@Composable
private fun NotificationToggleRow(
    granted: Boolean,
    onClick: () -> Unit,
) {
    val (interaction, pressModifier) = rememberPressScale(0.99f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .then(pressModifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = Icons.Filled.Notifications)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Notificaciones",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            Text(
                text = if (granted) {
                    "Activadas en este teléfono · toca para cambiar"
                } else {
                    "Pendiente de permiso · toca para activar"
                },
                color = Outline,
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        NotificationToggle(checked = granted, interactive = false)
    }
}

/** Current state of the Android notification permission. */
private fun notificationsEnabled(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

@Composable
private fun NotificationToggle(
    checked: Boolean,
    enabled: Boolean = true,
    interactive: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) PrimaryContainer else SurfaceVariant,
        label = "toggleTrack",
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        label = "toggleKnob",
    )

    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(PillShape)
            .background(trackColor)
            .clickable(
                enabled = enabled && interactive,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { translationX = knobOffset.toPx() }
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SyncStatusRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconTile(icon = Icons.Filled.Sync)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Estado de sincronización",
            modifier = Modifier.weight(1f),
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(SyncChipBackground)
                .border(1.dp, SyncChipBorder, PillShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = SyncChipText,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Sincronizado",
                color = SyncChipText,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun SettingsIconTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(IconTileShape)
            .background(PrimaryContainer.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryContainer,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RowDivider),
    )
}

@Composable
private fun LogoutSection(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (interaction, pressModifier) = rememberPressScale(0.98f)
        Row(
            modifier = pressModifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(PillShape)
                .border(1.dp, LogoutRed, PillShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onLogout),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = LogoutRed,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Cerrar sesión",
                color = LogoutRed,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Aviso de privacidad · AulaSegura v1.0",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}
