package com.example.aulasegura.ui.screens.parent

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.aulasegura.ui.components.ParentBottomBar
import com.example.aulasegura.ui.components.ParentTab
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.profile.ChangePasswordScreen
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import java.io.File

/** 3.10 — Perfil del tutor y foto. Ported from Stitch screen
 *  `7ad5b00011fe438390ac923e9e0d28e6` "Perfil - AulaSegura" (780x1768 mobile).
 *
 *  The Stitch mock is a fixed 800dp canvas: its content stops short of the
 *  bottom, leaving a dead gap above the nav, the header is a hard 180dp, and
 *  the shield watermark reads as a cut-off polygon. This port keeps every
 *  element of the design but rebuilds the layout on the project shell so the
 *  screen scrolls, sizes itself to its content, and reuses [ParentBottomBar].
 *
 *  The data is the tutor's own: the name, the email and the avatar come from
 *  the session profile, the children come from the roster, and the notification
 *  switch reads the Android permission instead of a flag nobody can see.
 */

private val InkPrimary = Color(0xFF1B1D3A)
private val WarmBorder = Color(0xFFE6E1D8)
private val IndigoTint = Color(0xFFE8E9F7)
private val MintTint = Color(0xFFE6FAF4)
private val MintDark = Color(0xFF15803D)
private val SyncGreen = Color(0xFF006A4E)
private val DangerRed = Color(0xFFE5484D)
private val MessageBorder = Color(0xFFC9C6E8)

private const val HEADER_HEIGHT_DP = 220

@Composable
fun ParentProfileScreen(
    ui: ParentProfileUi,
    onSelectTab: (ParentTab) -> Unit,
    onLogout: () -> Unit = {},
    onChangePhoto: (Uri) -> Unit = {},
    onDismissMessage: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onOpenPermissions: () -> Unit = {},
    onEditNameChange: (String) -> Unit = {},
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
    val scrollState = rememberScrollState()

    // The indigo header sits under the status bar, so its icons must be light
    // while the header is on screen and dark once the content scrolls under.
    val headerOnScreen = scrollState.value < HEADER_HEIGHT_DP
    LightStatusBarWhile(headerOnScreen)

    // The notification switch reflects the Android permission, not a local flag:
    // the OS is what decides whether the phone rings, and it can only be changed
    // from the system dialog or Settings. OnResume keeps the row honest after
    // the tutor comes back from either one.
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
    // app; the ViewModel only sees the resulting Uri and compresses it. Its own
    // directory, so the profile photo never mixes with the incident evidence.
    var cameraFailed by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        val captured = pendingCapture
        pendingCapture = null
        if (saved && captured != null) {
            cameraFailed = false
            onChangePhoto(Uri.fromFile(captured))
        } else {
            captured?.delete()
            cameraFailed = true
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
            .background(BrandCream),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f,
            color = PrimaryContainer,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 104.dp),
        ) {
            Reveal(0) {
                ProfileHeader(
                    ui = ui,
                    onChangePhoto = { showPhotoOptions = true },
                )
            }

            if (cameraFailed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "No se pudo tomar la foto. Intenta con otra cámara o elige una de la galería.",
                    color = Error,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
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

            Reveal(80) {
                MyChildrenSection(
                    children = ui.children,
                    loading = ui.childrenLoading,
                    message = ui.childrenMessage,
                )
            }
            Reveal(160) {
                SettingsSection(
                    notificationsEnabled = notificationsGranted,
                    onNotificationsClick = ::openNotificationSettings,
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onOpenPermissions = onOpenPermissions,
                )
            }
            Reveal(240) {
                ProfileFooter(onLogout = onLogout)
            }
        }

        ParentBottomBar(
            active = ParentTab.Perfil,
            onSelect = onSelectTab,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // The full-screen account forms cover the profile and its bottom bar.
        // They share this screen's ViewModel, so saving keeps `saving`/`message`
        // and returning shows a profile refreshed from the same session profile.
        when (ui.screen) {
            ParentProfileSection.EditProfile -> ParentEditProfileScreen(
                ui = ui,
                onBack = onCloseDialog,
                onChangePhoto = { showPhotoOptions = true },
                onNameChange = onEditNameChange,
                onSave = onSaveProfile,
                onDismissMessage = onDismissMessage,
            )
            ParentProfileSection.ChangePassword -> ChangePasswordScreen(
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
            ParentProfileSection.None -> Unit
        }
    }

    if (showPhotoOptions) {
        PhotoOptionsDialog(
            onDismiss = { showPhotoOptions = false },
            onTakePhoto = {
                showPhotoOptions = false
                capturePhoto()
            },
            onPickFromGallery = {
                showPhotoOptions = false
                galleryLauncher.launch("image/*")
            },
        )
    }
}

// ---------- ZONE 1: header ----------

@Composable
private fun ProfileHeader(
    ui: ParentProfileUi,
    onChangePhoto: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer)
            .statusBarsPadding()
            .padding(top = 14.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ShieldPattern(
                modifier = Modifier.fillMaxSize(),
                alpha = 0.07f,
                color = Color.White,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(
                    photoUrl = ui.photoUrl,
                    fullName = ui.fullName,
                    uploading = ui.uploading,
                    onChangePhoto = onChangePhoto,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = ui.fullName.ifBlank { "Perfil del tutor" },
                    color = OnPrimary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(OnPrimary.copy(alpha = 0.20f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "Padre de familia",
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = ui.email.ifBlank { "Sin correo registrado" },
                    color = OnPrimary.copy(alpha = 0.80f),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    fullName: String,
    uploading: Boolean,
    onChangePhoto: () -> Unit,
) {
    val (interaction, press) = rememberPressScale(0.9f)
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(PrimaryFixed)
                .border(3.dp, OnPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Text(
                    text = profileInitials(fullName),
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                )
            }
        }
        Box(
            modifier = press
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(22.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(SecondaryContainer)
                .clickable(
                    enabled = !uploading,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onChangePhoto,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = "Cambiar foto de perfil",
                tint = OnPrimary,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

// ---------- ZONE 2: mis hijos ----------

/**
 * The children of the tutor, as the roster knows them.
 *
 * The mock showed two invented children; the real ones come from the same
 * roster the home screen reads. An account with no linked student says so
 * instead of showing a child that does not exist.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyChildrenSection(
    children: List<ParentChildCardUi>,
    loading: Boolean,
    message: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp),
    ) {
        Text(
            text = "MIS HIJOS",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(10.dp))

        when {
            loading -> ChildrenNotice(text = "Cargando hijos…")
            message != null -> ChildrenNotice(text = message)
            children.isEmpty() ->
                ChildrenNotice(text = "Esta cuenta todavía no tiene hijos vinculados.")
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                children.forEachIndexed { index, child ->
                    ChildCard(
                        child = child,
                        tint = if (index % 2 == 0) IndigoTint else MintTint,
                        tintForeground = if (index % 2 == 0) PrimaryContainer else MintDark,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** The honest line under "MIS HIJOS" when there is no card to show. */
@Composable
private fun ChildrenNotice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ChildCard(
    child: ParentChildCardUi,
    tint: Color,
    tintForeground: Color,
    modifier: Modifier = Modifier,
) {
    // Informational, not a control: there is no per-child destination from the
    // profile (the attendance screen owns its own child selector), so the card
    // does not pretend to be tappable.
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint)
                .border(1.dp, WarmBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = child.initial,
                color = tintForeground,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = child.name,
                color = InkPrimary,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = child.grade,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------- ZONE 3: settings ----------

@Composable
private fun SettingsSection(
    notificationsEnabled: Boolean,
    onNotificationsClick: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onOpenPermissions: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLowest)
                .border(1.dp, WarmBorder, RoundedCornerShape(24.dp)),
        ) {
            SettingsRow(
                icon = Icons.Outlined.Edit,
                label = "Editar perfil",
                onClick = onEditProfile,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Lock,
                label = "Cambiar contraseña",
                onClick = onChangePassword,
            )
            SettingsDivider()
            NotificationRow(
                granted = notificationsEnabled,
                onClick = onNotificationsClick,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Security,
                label = "Permisos de la app",
                onClick = onOpenPermissions,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Sync,
                label = "Estado de sincronización",
                onClick = null,
                trailing = { SyncBadge() },
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = WarmBorder.copy(alpha = 0.7f),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit = { SettingsChevron() },
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) SurfaceContainerLow else Color.Transparent,
        animationSpec = tween(160),
        label = "settingsRowBackground",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .background(background)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryContainer.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = InkPrimary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * The switch is a readout of the Android permission. Tapping the row either asks
 * for it (Android 13+) or opens the app's notification settings, where the tutor
 * can turn it off. A flag in Firestore would be cosmetic: without Cloud
 * Functions the push is sent manually from the console.
 */
@Composable
private fun NotificationRow(
    granted: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) SurfaceContainerLow else Color.Transparent,
        animationSpec = tween(160),
        label = "notificationRowBackground",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryContainer.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Notificaciones",
                color = InkPrimary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
            Text(
                text = if (granted) {
                    "Activadas en este teléfono · toca para cambiar"
                } else {
                    "Pendiente de permiso · toca para activar"
                },
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
        }
        ProfileToggle(checked = granted)
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
private fun SettingsChevron() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = OutlineVariant,
        modifier = Modifier.size(18.dp),
    )
}

/**
 * The switch of the Stitch design, wired to the permission instead of to a
 * flag: it shows the state of the phone and the row opens the system dialog.
 */
@Composable
private fun ProfileToggle(checked: Boolean) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) PrimaryContainer else OutlineVariant,
        animationSpec = tween(220),
        label = "profileToggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "profileToggleThumb",
    )

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 20.dp)
            .clip(CircleShape)
            .background(trackColor)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(OnPrimary),
        )
    }
}

@Composable
private fun SyncBadge() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(BrandMint.copy(alpha = 0.25f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = SyncGreen,
            modifier = Modifier.size(10.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "Sincronizado",
            color = SyncGreen,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp,
            lineHeight = 12.sp,
        )
    }
}

// ---------- ZONE 4: logout + version ----------

@Composable
private fun ProfileFooter(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val (interaction, press) = rememberPressScale(0.95f)
        val pressed by interaction.collectIsPressedAsState()
        val background by animateColorAsState(
            targetValue = if (pressed) DangerRed.copy(alpha = 0.06f) else Color.Transparent,
            animationSpec = tween(160),
            label = "logoutBackground",
        )

        Row(
            modifier = press
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.5.dp, DangerRed, CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onLogout),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Cerrar sesión",
                color = DangerRed,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Aviso de privacidad · AulaSegura v1.0",
            color = Outline,
            fontFamily = JetBrainsMono,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ---------- result banner ----------

/** The outcome of the last action: a photo, a name, a password. */
@Composable
private fun ProfileMessage(
    message: String,
    onDismiss: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(IndigoTint)
            .border(1.dp, MessageBorder, RoundedCornerShape(12.dp))
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
            color = InkPrimary,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

// ---------- photo options ----------

/**
 * The two ways to get a photo. A Dialog + Surface instead of AlertDialog: the
 * overload that lets us place the options does not draw Material's dialog
 * container, so the options ended up floating over the page.
 */
@Composable
private fun PhotoOptionsDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
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

                val (cameraInteraction, cameraPress) = rememberPressScale(0.98f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .then(cameraPress)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(PrimaryContainer)
                        .clickable(
                            interactionSource = cameraInteraction,
                            indication = null,
                            onClick = onTakePhoto,
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Tomar foto",
                        color = OnPrimary,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }

                val (galleryInteraction, galleryPress) = rememberPressScale(0.98f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .then(galleryPress)
                        .clip(RoundedCornerShape(percent = 50))
                        .border(1.dp, PrimaryContainer, RoundedCornerShape(percent = 50))
                        .clickable(
                            interactionSource = galleryInteraction,
                            indication = null,
                            onClick = onPickFromGallery,
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
                    onClick = onDismiss,
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

// ---------- system bar ----------

/**
 * Keeps the status bar icons readable over the indigo header: light icons while
 * the header covers the status bar, dark icons once the content scrolls under.
 */
@Composable
private fun LightStatusBarWhile(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val activity = view.context.findActivity() ?: return
    val controller = remember(activity, view) {
        WindowInsetsControllerCompat(activity.window, view)
    }
    val previous = remember { controller.isAppearanceLightStatusBars }

    SideEffect {
        controller.isAppearanceLightStatusBars = !lightIcons
    }
    DisposableEffect(controller) {
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
