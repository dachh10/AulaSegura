package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.ui.profile.ProfilePhotoOptionsDialog
import com.example.aulasegura.ui.profile.rememberProfilePhotoPicker

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.ui.components.AdminBottomBar
import com.example.aulasegura.ui.components.AdminTab
import com.example.aulasegura.ui.components.shieldPattern
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.theme.BrandCoral
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.BrandMint
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import java.io.File

/**
 * 4.8 — Perfil del administrador. Adapted from the generic Stitch profile mockup
 * (`7ad5b00011fe438390ac923e9e0d28e6`) with the directora's data, since there is
 * no admin-specific profile mockup.
 */

private val WarmBorder = Color(0xFFE6E1D8)
private val DangerRed = Color(0xFFBA1A1A)
private val SyncGreen = Color(0xFF1B7A50)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
fun AdminProfileScreen(
    onLogout: () -> Unit,
    /** Wired to Routes.PERMISSIONS by NavHost, the app's existing permission screen. */
    onOpenAppPermissions: () -> Unit,
    onBack: () -> Unit = {},
    onOpenSyncStatus: () -> Unit = {},
    onPanel: () -> Unit = {},
    onUsuarios: () -> Unit = {},
    onGrupos: () -> Unit = {},
    onReportes: () -> Unit = {},
) {
    val viewModel: AdminProfileViewModel =
        viewModel(factory = rememberAppContainer().viewModelFactory)
    val ui by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showEditProfile by remember { mutableStateOf(false) }
    var editForm by remember { mutableStateOf(AdminProfileEditForm()) }
    var showChangePassword by remember { mutableStateOf(false) }
    var passwordForm by remember { mutableStateOf(AdminChangePasswordForm()) }

    // A successful change closes the dialog; the honest success banner stays visible.
    LaunchedEffect(ui.passwordChange.phase) {
        if (ui.passwordChange.phase == AdminPasswordChangePhase.SUCCESS) {
            showChangePassword = false
        }
    }

    var showPhotoOptions by remember { mutableStateOf(false) }
    val photoPicker = rememberProfilePhotoPicker(onPhoto = viewModel::changePhoto)

    val isUploading = ui.upload.phase in setOf(
        AdminPhotoUploadPhase.COMPRESSING,
        AdminPhotoUploadPhase.UPLOADING,
        AdminPhotoUploadPhase.SAVING,
    )

    if (showPhotoOptions) {
        ProfilePhotoOptionsDialog(
            onDismiss = { showPhotoOptions = false },
            onTakePhoto = { showPhotoOptions = false; photoPicker.takePhoto() },
            onPickFromGallery = { showPhotoOptions = false; photoPicker.pickFromGallery() },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandCream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
            Reveal(0) {
                ProfileHeader(
                    ui = ui,
                    onBack = onBack,
                    onChangePhoto = { showPhotoOptions = true },
                )
            }

            Reveal(40) {
                ProfileDataSection(
                    rows = adminProfileFieldRows(
                        fullName = ui.fullName,
                        email = ui.email,
                        schoolId = ui.schoolId,
                        schoolName = ui.schoolName,
                        phone = ui.phone,
                    ),
                    status = ui.upload.message ?: uploadPhaseLabel(ui.upload.phase),
                    showStatus = ui.upload.phase != AdminPhotoUploadPhase.IDLE,
                    isUploading = isUploading,
                    isFailure = ui.upload.phase == AdminPhotoUploadPhase.FAILURE,
                )
            }

            Reveal(80) {
                ProfileEditStatus(
                    edit = ui.edit,
                    onDismiss = { viewModel.editAcknowledged() },
                )
            }

            Reveal(60) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileEditStatus(
                        edit = ui.settings.toEditStatus(),
                        onDismiss = { viewModel.settingsAcknowledged() },
                    )
                    ProfileEditStatus(
                        edit = ui.passwordChange.toEditStatus(),
                        onDismiss = { viewModel.changePasswordAcknowledged() },
                    )
                    SettingsSection(
                        notificationsEnabled = ui.notificationsEnabled,
                        notificationsSaving = ui.settings.phase == AdminSettingsPhase.SAVING,
                        onToggleNotifications = {
                            viewModel.setNotificationsEnabled(!ui.notificationsEnabled)
                        },
                        onEditProfile = {
                            editForm = AdminProfileEditForm(
                                firstName = ui.firstName,
                                lastName = ui.lastName,
                                phone = ui.phone,
                            )
                            showEditProfile = true
                        },
                        onChangePhoto = { showPhotoOptions = true },
                        onChangePhotoEnabled = !isUploading,
                        onChangePassword = {
                            passwordForm = AdminChangePasswordForm()
                            viewModel.changePasswordAcknowledged()
                            showChangePassword = true
                        },
                        onOpenAppPermissions = onOpenAppPermissions,
                        onOpenSyncStatus = onOpenSyncStatus,
                    )
                }
            }

            Reveal(120) { ProfileFooter(onLogout = onLogout) }
        }

        AdminBottomBar(
            active = AdminTab.Mas,
            onSelect = { tab ->
                when (tab) {
                    AdminTab.Panel -> onPanel()
                    AdminTab.Usuarios -> onUsuarios()
                    AdminTab.Grupos -> onGrupos()
                    AdminTab.Reportes -> onReportes()
                    AdminTab.Mas -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showEditProfile) {
            EditProfileDialog(
                form = editForm,
                validationError = profileValidationError(editForm.firstName, editForm.lastName),
                saving = ui.edit.phase == AdminProfileEditPhase.SAVING,
                onFormChange = { editForm = it },
                onDismiss = { showEditProfile = false },
                onSave = {
                    viewModel.saveProfile(
                        firstName = editForm.firstName,
                        lastName = editForm.lastName,
                        phone = editForm.phone,
                    )
                    showEditProfile = false
                },
            )
        }

        if (showChangePassword) {
            ChangePasswordDialog(
                form = passwordForm,
                validationError = changePasswordValidationError(
                    passwordForm.current,
                    passwordForm.newPassword,
                ),
                status = ui.passwordChange.message,
                failed = ui.passwordChange.phase == AdminPasswordChangePhase.FAILURE,
                saving = ui.passwordChange.phase == AdminPasswordChangePhase.SAVING,
                onFormChange = { passwordForm = it },
                onDismiss = {
                    showChangePassword = false
                    viewModel.changePasswordAcknowledged()
                },
                onSave = {
                    viewModel.changePassword(passwordForm.current, passwordForm.newPassword)
                },
            )
        }
    }
}

@Composable
private fun ProfileEditStatus(edit: AdminProfileEditUi, onDismiss: () -> Unit) {
    val message = edit.message
    if (message != null) {
        val failure = edit.phase == AdminProfileEditPhase.FAILURE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (failure) DangerRed.copy(alpha = 0.08f)
                    else BrandMint.copy(alpha = 0.20f),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                color = if (failure) DangerRed else SyncGreen,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Aceptar",
                    color = if (failure) DangerRed else SyncGreen,
                    fontFamily = DmSans,
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    form: AdminProfileEditForm,
    validationError: String?,
    saving: Boolean,
    onFormChange: (AdminProfileEditForm) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar perfil",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.firstName,
                    onValueChange = { onFormChange(form.copy(firstName = it)) },
                    label = { Text("Nombre", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.lastName,
                    onValueChange = { onFormChange(form.copy(lastName = it)) },
                    label = { Text("Apellidos", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.phone,
                    onValueChange = { onFormChange(form.copy(phone = it)) },
                    label = { Text("Teléfono", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                validationError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = validationError == null && !saving,
            ) {
                Text(text = if (saving) "Guardando…" else "Guardar", fontFamily = DmSans)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar", fontFamily = DmSans)
            }
        },
    )
}

@Composable
private fun ChangePasswordDialog(
    form: AdminChangePasswordForm,
    validationError: String?,
    status: String?,
    failed: Boolean,
    saving: Boolean,
    onFormChange: (AdminChangePasswordForm) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Text(
                text = "Cambiar contraseña",
                color = PrimaryContainer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.current,
                    onValueChange = { onFormChange(form.copy(current = it)) },
                    label = { Text("Contraseña actual", fontFamily = DmSans) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.newPassword,
                    onValueChange = { onFormChange(form.copy(newPassword = it)) },
                    label = { Text("Nueva contraseña (mínimo 6)", fontFamily = DmSans) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                validationError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                    )
                }
                status?.let {
                    Text(
                        text = it,
                        color = if (failed) DangerRed else SyncGreen,
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = validationError == null && !saving,
            ) {
                Text(
                    text = if (saving) "Guardando…" else "Cambiar contraseña",
                    fontFamily = DmSans,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(text = "Cancelar", fontFamily = DmSans)
            }
        },
    )
}

@Composable
private fun ProfileHeader(
    ui: AdminProfileUi,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit = {},
) {
    val avatarUrl = adminAvatarUrl(ui.photoUrl)
    val initials = adminInitials(ui.fullName)
    val displayName = when {
        ui.fullName.isNotBlank() -> ui.fullName
        ui.loading -> "Cargando…"
        else -> "Sin nombre"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
            .background(PrimaryContainer)
            .shieldPattern(alpha = 0.07f, color = Color.White)
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = OnPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Box {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(PrimaryFixed)
                    .border(3.dp, OnPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
            if (avatarUrl != null) {
                // The URL carries the 512/fill/face transform: Coil never downloads
                // the original just to paint a 74 dp avatar.
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = initials,
                    color = PrimaryContainer,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                )
            }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(BrandCoral)
                    .border(2.dp, OnPrimary, CircleShape)
                    .clickable(onClick = onChangePhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Cambiar foto",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = displayName,
            color = OnPrimary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.3).sp,
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(OnPrimary.copy(alpha = 0.20f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = ADMIN_PROFILE_ROLE_LABEL,
                color = OnPrimary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = ui.email.ifBlank { "—" },
            color = OnPrimary.copy(alpha = 0.80f),
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProfileDataSection(
    rows: List<AdminProfileFieldRow>,
    status: String,
    showStatus: Boolean,
    isUploading: Boolean,
    isFailure: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .shadow(2.dp, CardShape)
            .clip(CardShape)
            .background(SurfaceContainerLowest)
            .border(1.dp, WarmBorder, CardShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Datos del perfil",
            color = PrimaryContainer,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(6.dp))

        rows.forEachIndexed { index, row ->
            if (index > 0) SettingsDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.label,
                    color = Outline,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    modifier = Modifier.width(76.dp),
                )
                Text(
                    text = row.value,
                    color = OnSurface,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (showStatus) {
            SettingsDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = status,
                    color = if (isFailure) DangerRed else SyncGreen,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    notificationsEnabled: Boolean,
    notificationsSaving: Boolean,
    onToggleNotifications: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePhoto: () -> Unit,
    onChangePhotoEnabled: Boolean,
    onChangePassword: () -> Unit,
    onOpenAppPermissions: () -> Unit,
    onOpenSyncStatus: () -> Unit,
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
                .shadow(2.dp, CardShape)
                .clip(CardShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, WarmBorder, CardShape),
        ) {
            SettingsRow(
                icon = Icons.Outlined.Edit,
                label = "Editar perfil",
                onClick = onEditProfile,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.PhotoCamera,
                label = "Cambiar foto",
                onClick = if (onChangePhotoEnabled) onChangePhoto else null,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Lock,
                label = "Cambiar contraseña",
                onClick = onChangePassword,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Notifications,
                label = "Notificaciones",
                onClick = null,
                trailing = {
                    ProfileToggle(
                        checked = notificationsEnabled,
                        enabled = !notificationsSaving,
                        onToggle = onToggleNotifications,
                    )
                },
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Security,
                label = "Permisos de la app",
                onClick = onOpenAppPermissions,
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Outlined.Sync,
                label = "Estado de sincronización",
                onClick = onOpenSyncStatus,
                trailing = {
                    SyncBadge()
                    Spacer(Modifier.width(6.dp))
                    SettingsChevron()
                },
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
        label = "adminSettingsRowBackground",
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
            color = OnSurface,
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

@Composable
private fun SettingsChevron() {
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = OutlineVariant,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun ProfileToggle(checked: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) PrimaryContainer else OutlineVariant,
        animationSpec = tween(220),
        label = "adminProfileToggleTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(220),
        label = "adminProfileToggleThumb",
    )

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 20.dp)
            .clip(CircleShape)
            .background(trackColor)
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier)
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

@Composable
private fun ProfileFooter(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val background by animateColorAsState(
            targetValue = if (pressed) DangerRed.copy(alpha = 0.06f) else Color.Transparent,
            animationSpec = tween(160),
            label = "adminLogoutBackground",
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .background(background)
                .border(1.5.dp, DangerRed, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onLogout,
                ),
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
