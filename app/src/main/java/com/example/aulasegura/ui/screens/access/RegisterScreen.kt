package com.example.aulasegura.ui.screens.access

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.aulasegura.ui.profile.ProfilePhotoOptionsDialog
import com.example.aulasegura.ui.profile.rememberProfilePhotoPicker
import java.io.File

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.domain.model.SchoolCatalog
import com.example.aulasegura.domain.model.groupNameFor
import com.example.aulasegura.domain.model.subjectsForGrade
import com.example.aulasegura.domain.repository.RegistrationKind
import com.example.aulasegura.ui.components.AulaField
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.AulaTextStyles
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSecondaryFixed
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.PillShape
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.Secondary
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SecondaryFixed
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.SurfaceVariant
import com.example.aulasegura.ui.theme.TertiaryContainer

/**
 * Registro (Tutor / Docente), ported from Stitch screens `88ef2e7c` and
 * `8325cdd3`. There is no student code: the role is chosen with a segmented
 * control and the extra field depends on it.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onCreated: (Uri?) -> Unit,
    onLogin: () -> Unit,
    ui: RegisterUiState = RegisterUiState(),
    onKindChange: (RegistrationKind) -> Unit = {},
    onFullNameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPhoneChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onConfirmPasswordChange: (String) -> Unit = {},
    onChildNameChange: (String) -> Unit = {},
    onSubjectChange: (String) -> Unit = {},
    onSubjectIdChange: (String) -> Unit = {},
    onGradeChange: (Int) -> Unit = {},
    onSectionChange: (String) -> Unit = {},
    onSelectionChanged: () -> Unit = {},
    onAcceptedChange: (Boolean) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val isTutor = ui.kind == RegistrationKind.TUTOR
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    val photoPicker = rememberProfilePhotoPicker(onPhoto = { photoUri = it })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.05f,
            color = PrimaryContainer,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            RegisterTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 132.dp),
            ) {
                Reveal(delayMillis = 40) { RegisterHeading() }

                Reveal(delayMillis = 70) {
                    KindSelector(
                        kind = ui.kind,
                        onKindChange = onKindChange,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                Reveal(delayMillis = 90) {
                    AvatarPicker(
                        photoUri = photoUri,
                        onClick = { showPhotoOptions = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 28.dp),
                    )
                }

                Reveal(delayMillis = 140) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AulaField(
                            value = ui.fullName,
                            onValueChange = onFullNameChange,
                            placeholder = "Nombre y apellidos",
                            leadingIcon = Icons.Filled.Person,
                            label = "Nombre completo",
                            keyboardType = KeyboardType.Text,
                        )

                        Column {
                            AulaField(
                                value = ui.email,
                                onValueChange = onEmailChange,
                                placeholder = "ejemplo@correo.com",
                                leadingIcon = Icons.Outlined.Mail,
                                label = "Correo electrónico",
                                keyboardType = KeyboardType.Email,
                            )
                            Text(
                                text = "Usa un correo real: te enviaremos un enlace de verificación.",
                                color = OnSurfaceVariant,
                                fontFamily = DmSans,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                            )
                        }

                        AulaField(
                            value = ui.phone,
                            onValueChange = onPhoneChange,
                            placeholder = "55 1234 5678",
                            leadingIcon = Icons.Outlined.Call,
                            label = "Teléfono de contacto",
                            keyboardType = KeyboardType.Phone,
                            prefixText = "+52",
                        )

                        Column {
                            AulaField(
                                value = ui.password,
                                onValueChange = onPasswordChange,
                                placeholder = "••••••••",
                                leadingIcon = Icons.Outlined.Lock,
                                label = "Contraseña",
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                textStyle = AulaTextStyles.dataMonoMd,
                                trailing = {
                                    PasswordToggle(
                                        visible = passwordVisible,
                                        onToggle = { passwordVisible = !passwordVisible },
                                    )
                                },
                            )
                            PasswordStrength(
                                strength = ui.strength,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        AulaField(
                            value = ui.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            placeholder = "Repite tu contraseña",
                            leadingIcon = Icons.Outlined.LockClock,
                            label = "Confirmar contraseña",
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            passwordVisible = confirmVisible,
                            trailing = {
                                PasswordToggle(
                                    visible = confirmVisible,
                                    onToggle = { confirmVisible = !confirmVisible },
                                )
                            },
                        )

                        if (isTutor) {
                            AulaField(
                                value = ui.childName,
                                onValueChange = onChildNameChange,
                                placeholder = "Nombre completo de tu hijo",
                                leadingIcon = Icons.Outlined.Badge,
                                label = "Nombre completo de tu hijo",
                                keyboardType = KeyboardType.Text,
                                labelTrailing = {
                                    Text(
                                        text = "Para tu código QR",
                                        color = PrimaryContainer,
                                        fontFamily = DmSans,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                    )
                                },
                            )
                        } else {
                            TeacherAssignmentFields(
                                grade = ui.grade,
                                section = ui.section,
                                subjectId = ui.subjectId,
                                takenSubjects = ui.takenSubjects,
                                onGradeChange = onGradeChange,
                                onSectionChange = onSectionChange,
                                onSubjectIdChange = onSubjectIdChange,
                                onSelectionChanged = onSelectionChanged,
                            )
                        }

                        TermsRow(
                            accepted = ui.accepted,
                            onToggle = { onAcceptedChange(!ui.accepted) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        RegisterBottomBar(
            label = if (isTutor) "Crear cuenta" else "Enviar solicitud",
            photoUri = photoUri,
            onCreated = onCreated,
            onLogin = onLogin,
            isLoading = isLoading,
            errorMessage = errorMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showPhotoOptions) {
            ProfilePhotoOptionsDialog(
                onDismiss = { showPhotoOptions = false },
                onTakePhoto = { showPhotoOptions = false; photoPicker.takePhoto() },
                onPickFromGallery = { showPhotoOptions = false; photoPicker.pickFromGallery() },
            )
        }
    }
}

@Composable
private fun RegisterTopBar(onBack: () -> Unit) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = statusBarTop + 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Regresar",
                tint = PrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.30f), PillShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = TertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "AulaSegura",
                color = Primary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = (-0.2).sp,
            )
        }
    }
}

@Composable
private fun RegisterHeading() {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(SecondaryFixed)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = OnSecondaryFixed,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Comunidad Escolar",
                color = OnSecondaryFixed,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Crea tu cuenta",
            color = Primary,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Regístrate como tutor o docente para formar parte de la comunidad escolar.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun KindSelector(
    kind: RegistrationKind,
    onKindChange: (RegistrationKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.40f), PillShape)
            .padding(4.dp),
    ) {
        RegistrationKind.entries.forEach { option ->
            val selected = option == kind
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(if (selected) PrimaryContainer else Color.Transparent)
                    .clickable { onKindChange(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (option == RegistrationKind.TUTOR) "Tutor" else "Docente",
                    color = if (selected) SurfaceContainerLowest else OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun AvatarPicker(
    photoUri: Uri? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(88.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(PrimaryFixed.copy(alpha = 0.60f))
                    .border(2.dp, SurfaceContainerLowest, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Foto de perfil elegida",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = PrimaryContainer.copy(alpha = 0.70f),
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .border(2.dp, SurfaceContainerLowest, CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = "Subir foto de perfil",
                    tint = SurfaceContainerLowest,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Agregar foto (opcional)",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun PasswordToggle(visible: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
            tint = Outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun PasswordStrength(strength: Int, modifier: Modifier = Modifier) {
    val label = when (strength) {
        0 -> "SIN CONTRASEÑA"
        1 -> "DÉBIL"
        2 -> "MEDIA"
        3 -> "FUERTE"
        else -> "MUY FUERTE"
    }
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(PillShape)
                        .background(
                            if (index < strength) {
                                if (strength <= 2) SecondaryContainer else TertiaryContainer
                            } else {
                                SurfaceVariant
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mínimo 8 caracteres",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.3.sp,
            )
            Text(
                text = label,
                color = Secondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SecondaryFixed.copy(alpha = 0.50f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun TermsRow(
    accepted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (accepted) PrimaryContainer else Color.Transparent)
                .border(
                    width = if (accepted) 0.dp else 1.dp,
                    color = OutlineVariant.copy(alpha = 0.60f),
                    shape = RoundedCornerShape(6.dp),
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (accepted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = buildAnnotatedString {
                append("Acepto el ")
                withStyle(SpanStyle(color = Secondary, fontWeight = FontWeight.SemiBold)) {
                    append("aviso de privacidad")
                }
                append(" y los ")
                withStyle(SpanStyle(color = Secondary, fontWeight = FontWeight.SemiBold)) {
                    append("términos de uso")
                }
                append(" de la plataforma escolar.")
            },
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun RegisterBottomBar(
    label: String,
    photoUri: Uri? = null,
    onCreated: (Uri?) -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Background.copy(alpha = 0.95f),
                        Background,
                    ),
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Error,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        PillButton(
            text = if (isLoading) "Enviando…" else label,
            onClick = { if (!isLoading) onCreated(photoUri) },
            containerColor = SecondaryContainer,
            contentColor = SurfaceContainerLowest,
            fontSize = 16.sp,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            shadowColor = SecondaryContainer.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "¿Ya tienes cuenta? ",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 13.sp,
            )
            Text(
                text = "Inicia sesión",
                color = Secondary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onLogin),
            )
        }
    }
}


/**
 * A teacher picks a classroom (grade + section) and one of the six subjects.
 * A subject already owned in that classroom cannot be selected: the same
 * invariant the approval transaction enforces atomically.
 */
@Composable
internal fun TeacherAssignmentFields(
    grade: Int?,
    section: String,
    subjectId: String,
    takenSubjects: Set<String>,
    onGradeChange: (Int) -> Unit,
    onSectionChange: (String) -> Unit,
    onSubjectIdChange: (String) -> Unit,
    onSelectionChanged: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Grupo que impartes",
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SchoolCatalog.GRADES.forEach { value ->
                SelectionChip(
                    label = "$value°",
                    selected = grade == value,
                    onClick = {
                        onGradeChange(value)
                        onSelectionChanged()
                    },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SchoolCatalog.SECTIONS.forEach { value ->
                SelectionChip(
                    label = value,
                    selected = section == value,
                    onClick = {
                        onSectionChange(value)
                        onSelectionChanged()
                    },
                )
            }
        }
        if (grade != null && section.isNotBlank()) {
            Text(
                text = "Grupo: ${groupNameFor(grade, section)}",
                color = OnSurfaceVariant,
                style = AulaTextStyles.dataMonoSm,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Materia que impartes",
            color = PrimaryContainer,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
        if (grade == null) {
            Text(
                text = "Elige primero el grado para ver sus materias.",
                color = OnSurfaceVariant,
                style = AulaTextStyles.dataMonoSm,
            )
        } else {
            val gradeSubjects = subjectsForGrade(grade)
            listOf(gradeSubjects.take(3), gradeSubjects.drop(3)).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { subject ->
                        val taken = subject.id in takenSubjects && subject.id != subjectId
                        SelectionChip(
                            label = subject.label,
                            selected = subject.id == subjectId,
                            enabled = !taken,
                            onClick = { onSubjectIdChange(subject.id) },
                        )
                    }
                }
            }
        }
        if (takenSubjects.isNotEmpty()) {
            Text(
                text = "Las materias marcadas ya tienen docente en este grupo.",
                color = OnSurfaceVariant,
                style = AulaTextStyles.dataMonoSm,
            )
        }
        Row(
            modifier = Modifier.padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = TertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Tu cuenta será revisada por la directora; el grupo y la materia se asignan solos.",
                color = OnSurfaceVariant,
                style = AulaTextStyles.dataMonoSm,
            )
        }
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(PillShape)
            .border(
                width = 1.dp,
                color = if (selected) Primary else Outline,
                shape = PillShape,
            )
            .background(if (selected) PrimaryContainer else SurfaceContainerLowest)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) OnSecondaryFixed else OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        )
    }
}
