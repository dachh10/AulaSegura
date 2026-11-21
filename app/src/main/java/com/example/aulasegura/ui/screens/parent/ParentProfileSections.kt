package com.example.aulasegura.ui.screens.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.profile.CardShape
import com.example.aulasegura.ui.profile.LockedField
import com.example.aulasegura.ui.profile.ProfilePhotoBlock
import com.example.aulasegura.ui.profile.StitchDock
import com.example.aulasegura.ui.profile.StitchField
import com.example.aulasegura.ui.profile.StitchToast
import com.example.aulasegura.ui.profile.StitchTopBar
import com.example.aulasegura.ui.profile.profileInitials
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.BrandCream
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLowest

/**
 * The tutor's edit-profile form.
 *
 * It is the teacher's form with the subjects removed: a tutor owns no subject,
 * so the only editable field is the name. The widgets come from `ui/profile`,
 * which is what makes both roles look like the same product instead of two
 * hand-drawn copies of the same screen.
 */
@Composable
fun ParentEditProfileScreen(
    ui: ParentProfileUi,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(BrandCream)) {
        ShieldPattern(
            modifier = Modifier.fillMaxSize(),
            alpha = 0.04f,
            color = PrimaryContainer,
        )
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
            Reveal(120) { EditableFieldsCard(ui, onNameChange) }
        }

        StitchDock(
            modifier = Modifier.align(Alignment.BottomCenter).revealOnEnter(180),
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
    ui: ParentProfileUi,
    onNameChange: (String) -> Unit,
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
                imeAction = ImeAction.Done,
            )
            LockedField(label = "Correo electrónico", value = ui.email)
        }
    }
}
