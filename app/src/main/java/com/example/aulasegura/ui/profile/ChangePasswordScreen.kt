package com.example.aulasegura.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aulasegura.ui.screens.access.Reveal
import com.example.aulasegura.ui.screens.access.revealOnEnter
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.OnSurface
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OnTertiaryContainer
import com.example.aulasegura.ui.theme.Outline
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryFixed
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.Surface
import com.example.aulasegura.ui.theme.SurfaceContainerHigh
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryFixed

/**
 * The Stitch "Cambiar contraseña" form, shared by every role.
 *
 * Changing a password is the same operation for a teacher and a tutor: same
 * fields, same checklist, same Firebase errors. A copy per role would only be a
 * second place to forget one of the rules.
 */
@Composable
fun ChangePasswordScreen(
    current: String,
    new: String,
    confirmation: String,
    saving: Boolean,
    saved: Boolean,
    message: String?,
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
            Reveal(0) {
                StitchTopBar(title = "Cambiar contraseña", onBack = onBack, trailingBadge = false)
            }
            Reveal(60) { PasswordIntro() }
            Reveal(120) {
                PasswordFormCard(
                    current = current,
                    new = new,
                    confirmation = confirmation,
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
            Reveal(180) { RequirementsChecklist(new) }
        }

        StitchDock(
            modifier = Modifier.align(Alignment.BottomCenter).revealOnEnter(240),
            label = when {
                saving -> "Actualizando…"
                saved -> "Contraseña actualizada"
                else -> "Actualizar contraseña"
            },
            icon = Icons.Filled.LockReset,
            saving = saving,
            saved = saved,
            onClick = onSave,
            onCancel = onBack,
        )
        if (message != null && !saving) {
            StitchToast(
                message = message,
                onDismiss = onDismissMessage,
                modifier = Modifier.align(Alignment.TopCenter).revealOnEnter(0),
            )
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
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = TertiaryFixed,
                        modifier = Modifier.size(20.dp),
                    )
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
    current: String,
    new: String,
    confirmation: String,
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
                value = current,
                onChange = onCurrentChange,
                visible = currentVisible,
                onToggle = onCurrentVisibility,
                imeAction = ImeAction.Next,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StitchPasswordField(
                    label = "Nueva contraseña",
                    value = new,
                    onChange = onNewChange,
                    visible = newVisible,
                    onToggle = onNewVisibility,
                    imeAction = ImeAction.Next,
                )
                val steps = passwordStrengthSteps(new)
                if (new.isNotBlank()) {
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
                    val strength = passwordStrengthLabel(new)
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
                    if (passwordsMatch(new, confirmation)) {
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
                    value = confirmation,
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
                    imageVector = if (rule.met) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.RadioButtonUnchecked
                    },
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

private val AmberSegment = Color(0xFFF59E0B)
