package com.example.aulasegura.ui.screens.teacher

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.profile.CardShape
import com.example.aulasegura.ui.profile.FieldShape
import com.example.aulasegura.ui.profile.LockedField
import com.example.aulasegura.ui.profile.ProfilePhotoBlock
import com.example.aulasegura.ui.profile.StitchDock
import com.example.aulasegura.ui.profile.StitchField
import com.example.aulasegura.ui.profile.StitchToast
import com.example.aulasegura.ui.profile.StitchTopBar
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.JetBrainsMono
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * The teacher's edit-profile form, ported from the Stitch screen of the same
 * name.
 *
 * Layout, colour, spacing, icons and the staggered Reveal entrance follow the
 * markup exactly. Only the data is adapted to what this app owns: name and
 * subjects are editable, email is read-only, groups come from the roster.
 *
 * The widgets and the password form are shared with the tutor in `ui/profile`:
 * both roles see the same product, not two hand-drawn copies of it.
 */

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
            Reveal(60) {
                ProfilePhotoBlock(
                    photoUrl = ui.photoUrl,
                    initials = profileInitials(ui.fullName),
                    onChangePhoto = onChangePhoto,
                )
            }
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
