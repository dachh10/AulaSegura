package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aulasegura.ui.components.rememberPressScale
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnPrimary
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed
import com.example.aulasegura.ui.theme.OnTertiaryContainer

/**
 * The two account forms, ported from the Stitch screens of the same names.
 *
 * Layout, colour, spacing, icons and the staggered Reveal entrance follow the
 * markup exactly. Only the data is adapted to what this app owns: name and
 * subjects are editable, email and school are read-only, groups come from the
 * roster, and the password checklist is driven by the enforced rules.
 */

private val AmberSegment = Color(0xFFF59E0B)
private val FieldShape = RoundedCornerShape(16.dp)
private val CardShape = RoundedCornerShape(24.dp)

// ---------------------------------------------------------------------------
// Editar perfil — Stitch "Editar Perfil (Docente)"
// ---------------------------------------------------------------------------

@Composable
fun TeacherEditProfileScreen(
    ui: TeacherProfileUi,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onSubjectsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) { StitchTopBar(title = "Editar perfil", onBack = onBack, trailingBadge = true) }
            Reveal(60) { PhotoBlock(ui, onChangePhoto) }
            Reveal(120) { EditableFieldsCard(ui, onNameChange, onSubjectsChange) }
            Reveal(180) { SchoolAssignedCard(ui) }
            Reveal(240) { OfflineNotice() }
        }

        StitchDock(
            modifier = Modifier.align(Alignment.BottomCenter).revealOnEnter(300),
            label = if (ui.saving) "Guardando…" else "Guardar cambios",
            icon = Icons.Filled.CheckCircle,
            saving = ui.saving,
            saved = false,
            onClick = onSave,
            onCancel = onBack,
        )
        if (ui.saved) {
            StitchToast(
                message = "Cambios guardados con éxito",
                onDismiss = onDismissMessage,
                modifier = Modifier.align(Alignment.TopCenter).revealOnEnter(0),
            )
        }
    }
}

@Composable
private fun PhotoBlock(ui: TeacherProfileUi, onChangePhoto: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Surface(
                modifier = Modifier.size(104.dp),
                shape = CircleShape,
                color = SurfaceContainerLowest,
                shadowElevation = 4.dp,
            ) {
                Box(Modifier.padding(4.dp)) {
                    if (ui.photoUrl != null) {
                        AsyncImage(
                            model = ui.photoUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(CircleShape).background(PrimaryFixed),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = profileInitials(ui.fullName),
                                color = Primary,
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                            )
                        }
                    }
                }
            }
            val (_, press) = rememberPressScale(0.90f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .then(press)
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .clickable(onClick = onChangePhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Cambiar foto",
                    tint = OnPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        TextButton(onClick = onChangePhoto) {
            Text(
                text = "Cambiar foto",
                color = SecondaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun EditableFieldsCard(
    ui: TeacherProfileUi,
    onNameChange: (String) -> Unit,
    onSubjectsChange: (String) -> Unit,
) {
    Surface(
        shape = CardShape,
        color = SurfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StitchField(
                label = "Nombre completo",
                value = ui.editName,
                onChange = onNameChange,
                icon = Icons.Filled.AccountCircle,
                imeAction = ImeAction.Next,
            )
            StitchField(
                label = "Materias",
                value = ui.editSubjects,
                onChange = onSubjectsChange,
                icon = Icons.Filled.Person,
                placeholder = "Ej. Matemáticas, Ciencias",
                imeAction = ImeAction.Done,
            )
            LockedField(label = "Correo electrónico", value = ui.email)
        }
    }
}

@Composable
private fun SchoolAssignedCard(ui: TeacherProfileUi) {
    Surface(
        shape = CardShape,
        color = SurfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), CardShape),
    ) {
        // No watermark here: the rows are opaque and the card keeps a clean
        // silhouette instead of a shield bleeding past the rounded corners.
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Datos asignados por la escuela",
                    color = Primary,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
            AssignedRow(
                icon = Icons.Filled.School,
                label = "Materia",
                value = ui.subjects.firstOrNull() ?: "Sin asignar",
            )
            AssignedGroupsRow(groups = ui.groups)
        }
    }
}

@Composable
private fun AssignedRow(icon: ImageVector, label: String, value: String) {
    Surface(
        shape = FieldShape,
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = label, color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                color = OnSurface,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun AssignedGroupsRow(groups: List<String>) {
    Surface(
        shape = FieldShape,
        color = SurfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "Grupos", color = OnSurfaceVariant, fontFamily = DmSans, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            if (groups.isEmpty()) {
                Text(
                    text = "Sin asignar",
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                )
            } else {
                groups.take(3).forEach { group ->
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(end = 6.dp),
                    ) {
                        Text(
                            text = group,
                            color = Primary,
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineNotice() {
    val pulse by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "cloudPulse",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.CloudSync,
            contentDescription = null,
            tint = PrimaryContainer.copy(alpha = 0.5f + pulse * 0.5f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Los cambios se guardan en tu cuenta.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 12.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Cambiar contraseña — Stitch "Cambiar contraseña"
// ---------------------------------------------------------------------------

@Composable
fun TeacherChangePasswordScreen(
    ui: TeacherProfileUi,
    onBack: () -> Unit,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Surface)) {
        ShieldWatermark(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Reveal(0) { StitchTopBar(title = "Cambiar contraseña", onBack = onBack, trailingBadge = false) }
            Reveal(60) { PasswordIntro() }
            Reveal(120) {
                PasswordFormCard(
                    ui = ui,
                    currentVisible = currentVisible,
                    newVisible = newVisible,
                    confirmVisible = confirmVisible,
                    onCurrentVisibility = { currentVisible = !currentVisible },
                    onNewVisibility = { newVisible = !newVisible },
                    onConfirmVisibility = { confirmVisible = !confirmVisible },
                    onCurrentChange = onCurrentChange,
                    onNewChange = onNewChange,
                    onConfirmChange = onConfirmChange,
                )
            }
            Reveal(180) { RequirementsChecklist(ui.newPassword) }
        }

        StitchDock(
            modifier = Modifier.align(Alignment.BottomCenter).revealOnEnter(240),
            label = when {
                ui.saving -> "Actualizando…"
                ui.saved -> "Contraseña actualizada"
                else -> "Actualizar contraseña"
            },
            icon = Icons.Filled.LockReset,
            saving = ui.saving,
            saved = ui.saved,
            onClick = onSave,
            onCancel = onBack,
        )
        if (ui.message != null && !ui.saving) {
            StitchToast(
                message = ui.message.orEmpty(),
                onDismiss = onDismissMessage,
                modifier = Modifier.align(Alignment.TopCenter).revealOnEnter(0),
            )
        }
    }
}

@Composable
private fun ShieldWatermark(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val step = 72.dp.toPx()
        val tint = Primary.copy(alpha = 0.04f)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x + 36f, y + 6f)
                    lineTo(x + 60f, y + 16f)
                    lineTo(x + 60f, y + 38f)
                    quadraticTo(x + 60f, y + 52f, x + 36f, y + 66f)
                    quadraticTo(x + 12f, y + 52f, x + 12f, y + 38f)
                    lineTo(x + 12f, y + 16f)
                    close()
                }
                drawPath(path, color = tint, style = stroke)
                val check = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x + 26f, y + 36f)
                    lineTo(x + 33f, y + 43f)
                    lineTo(x + 47f, y + 28f)
                }
                drawPath(check, color = tint, style = stroke)
                x += step
            }
            y += step
        }
    }
}

@Composable
private fun PasswordIntro() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Surface(
                modifier = Modifier.size(112.dp),
                shape = CircleShape,
                color = PrimaryFixed,
                shadowElevation = 2.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp),
                shape = CircleShape,
                color = SecondaryContainer,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = TertiaryFixed, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Elige una contraseña segura que no uses en otras cuentas.",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun PasswordFormCard(
    ui: TeacherProfileUi,
    currentVisible: Boolean,
    newVisible: Boolean,
    confirmVisible: Boolean,
    onCurrentVisibility: () -> Unit,
    onNewVisibility: () -> Unit,
    onConfirmVisibility: () -> Unit,
    onCurrentChange: (String) -> Unit,
    onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
) {
    Surface(
        shape = FieldShape,
        color = SurfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StitchPasswordField(
                label = "Contraseña actual",
                value = ui.currentPassword,
                onChange = onCurrentChange,
                visible = currentVisible,
                onToggle = onCurrentVisibility,
                imeAction = ImeAction.Next,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StitchPasswordField(
                    label = "Nueva contraseña",
                    value = ui.newPassword,
                    onChange = onNewChange,
                    visible = newVisible,
                    onToggle = onNewVisibility,
                    imeAction = ImeAction.Next,
                )
                val steps = passwordStrengthSteps(ui.newPassword)
                if (ui.newPassword.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .background(
                                        color = if (index < steps) {
                                            when (index) {
                                                0, 1 -> AmberSegment
                                                else -> OnTertiaryContainer
                                            }
                                        } else {
                                            SurfaceContainerHigh
                                        },
                                        shape = RoundedCornerShape(percent = 50),
                                    ),
                            )
                        }
                    }
                    val strength = passwordStrengthLabel(ui.newPassword)
                    Text(
                        text = strength,
                        color = if (strength == "Buena") OnTertiaryContainer else OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Confirmar nueva contraseña",
                        color = OnSurfaceVariant,
                        fontFamily = DmSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (passwordsMatch(ui.newPassword, ui.confirmPassword)) {
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = TertiaryFixed.copy(alpha = 0.3f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = OnTertiaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Coinciden",
                                    color = OnTertiaryContainer,
                                    fontFamily = DmSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
                StitchPasswordField(
                    label = "",
                    value = ui.confirmPassword,
                    onChange = onConfirmChange,
                    visible = confirmVisible,
                    onToggle = onConfirmVisibility,
                    imeAction = ImeAction.Done,
                )
            }
        }
    }
}

@Composable
private fun RequirementsChecklist(newPassword: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        passwordRules(newPassword).forEach { rule ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (rule.met) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (rule.met) OnTertiaryContainer else Outline,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = rule.label,
                    color = if (rule.met) OnSurface else OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared Stitch pieces
// ---------------------------------------------------------------------------

@Composable
private fun StitchTopBar(
    title: String,
    onBack: () -> Unit,
    trailingBadge: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (_, press) = rememberPressScale(0.95f)
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(press)
                .clip(CircleShape)
                .background(SurfaceContainerLowest)
                .border(1.dp, OutlineVariant.copy(alpha = 0.4f), CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = OnSurface,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.weight(1f),
        )
        if (trailingBadge) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = SurfaceContainerLow) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Badge, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StitchField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String? = null,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = { Icon(icon, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp)) },
            singleLine = true,
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = PrimaryContainer,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Primary,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = DmSans,
                fontSize = 16.sp,
                color = OnSurface,
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = imeAction),
        )
    }
}

@Composable
private fun LockedField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(SurfaceContainerHigh.copy(alpha = 0.6f), FieldShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = OutlineVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = value.ifBlank { "—" },
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Outline, modifier = Modifier.size(18.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Outline, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Para cambiarlo, contacta a la administración.",
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun StitchPasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    visible: Boolean,
    onToggle: () -> Unit,
    imeAction: ImeAction,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = OnSurfaceVariant,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Outline, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = FieldShape,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                focusedBorderColor = PrimaryContainer,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Primary,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
                color = OnSurface,
                letterSpacing = 0.5.sp,
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction,
            ),
        )
    }
}

@Composable
private fun StitchDock(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    saving: Boolean,
    saved: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest)
            .border(1.dp, OutlineVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val (interaction, press) = rememberPressScale(0.98f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .then(press)
                .background(
                    color = if (saved) OnTertiaryContainer else SecondaryContainer,
                    shape = RoundedCornerShape(percent = 50),
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = OnPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(icon, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = OnPrimary,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
        }
        TextButton(onClick = onCancel) {
            Text(text = "Cancelar", color = Primary, fontFamily = DmSans, fontSize = 14.sp)
        }
        Spacer(
            Modifier
                .size(width = 128.dp, height = 4.dp)
                .background(OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(percent = 50)),
        )
    }
}

@Composable
private fun StitchToast(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = Primary,
        shadowElevation = 8.dp,
        modifier = modifier.padding(top = 64.dp).fillMaxWidth(0.9f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                color = OnPrimary,
                fontFamily = DmSans,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text("Listo", color = OnPrimary, fontFamily = DmSans, fontSize = 12.sp)
            }
        }
    }
}
