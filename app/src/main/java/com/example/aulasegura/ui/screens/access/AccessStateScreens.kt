package com.example.aulasegura.ui.screens.access

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BricolageGrotesque
import com.example.aulasegura.ui.theme.DmSans
import com.example.aulasegura.ui.theme.Error
import com.example.aulasegura.ui.theme.OnSurfaceVariant
import com.example.aulasegura.ui.theme.OutlineVariant
import com.example.aulasegura.ui.theme.Primary
import com.example.aulasegura.ui.theme.PrimaryContainer
import com.example.aulasegura.ui.theme.SecondaryContainer
import com.example.aulasegura.ui.theme.SurfaceContainerLow
import com.example.aulasegura.ui.theme.SurfaceContainerLowest
import com.example.aulasegura.ui.theme.TertiaryContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccessStateUiState(
    val email: String = "",
    val role: UserRole = UserRole.TUTOR,
    val estado: String = "pendiente",
    val vinculacion: String = "pendiente",
    val childName: String = "",
    val message: String? = null,
    val isLoading: Boolean = false,
    /** Set once the app may move on; the NavHost resolves the exact route. */
    val proceed: Boolean = false,
) {
    val isRejected: Boolean get() = estado == "rechazado"
}

/** Shared state for the "check your email", "account in review" and "link child" steps. */
class AccessStateViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessStateUiState())
    val uiState: StateFlow<AccessStateUiState> = _uiState.asStateFlow()

    init {
        val current = auth.currentUser
        _uiState.update {
            it.copy(
                email = current?.email.orEmpty(),
                role = current?.role ?: UserRole.TUTOR,
                estado = current?.estado?.ifBlank { "pendiente" } ?: "pendiente",
                vinculacion = current?.vinculacion?.ifBlank { "pendiente" } ?: "pendiente",
                childName = current?.hijoSolicitado.orEmpty(),
            )
        }
    }

    fun checkEmail() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            if (!auth.isEmailVerified()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Todavía no verificamos tu correo. Revisa tu bandeja y vuelve a intentar.",
                    )
                }
                return@launch
            }
            val refreshed = auth.refreshProfile()
            val user = (refreshed as? AuthResult.Success)?.user ?: auth.currentUser
            val canProceed = when (user?.role ?: UserRole.TUTOR) {
                UserRole.TEACHER -> user?.estado == "aprobado" || user?.estado == "rechazado"
                UserRole.TUTOR -> true
                UserRole.ADMIN -> true
                UserRole.UNKNOWN -> false
            }
            _uiState.update { it.copy(isLoading = false, proceed = canProceed) }
        }
    }

    fun refreshState() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val refreshed = auth.refreshProfile()
            val user = (refreshed as? AuthResult.Success)?.user ?: auth.currentUser
            _uiState.update {
                it.copy(
                    isLoading = false,
                    email = user?.email ?: _uiState.value.email,
                    childName = user?.hijoSolicitado ?: _uiState.value.childName,
                    estado = user?.estado ?: "pendiente",
                    vinculacion = user?.vinculacion ?: "pendiente",
                    proceed = when (user?.role) {
                        UserRole.TEACHER -> user.estado == "aprobado" || user.estado == "rechazado"
                        UserRole.TUTOR -> user.vinculacion == "vinculado"
                        UserRole.ADMIN -> true
                        else -> false
                    },
                )
            }
        }
    }

    fun resend() {
        viewModelScope.launch {
            val ok = auth.resendVerificationEmail().isSuccess
            _uiState.update {
                it.copy(
                    message = if (ok) {
                        "Te enviamos el correo otra vez."
                    } else {
                        "No pudimos reenviar el correo. Inténtalo de nuevo."
                    },
                )
            }
        }
    }

    fun consumeProceed() = _uiState.update { it.copy(proceed = false) }
}

@Composable
private fun AccessScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        ShieldPattern(modifier = Modifier.fillMaxSize(), alpha = 0.05f, color = PrimaryContainer)
        Column(modifier = Modifier.fillMaxSize()) {
            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = top + 16.dp, bottom = 8.dp),
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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Regresar",
                        tint = PrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceContainerLowest)
                        .border(1.dp, OutlineVariant.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
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
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SecondaryContainer.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = title,
                    color = Primary,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = subtitle,
                    color = OnSurfaceVariant,
                    fontFamily = DmSans,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(28.dp))
                content()
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(
    email: String,
    message: String? = null,
    isLoading: Boolean = false,
    onBack: () -> Unit = {},
    onChecked: () -> Unit = {},
    onResend: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    AccessScaffold(
        title = "Revisa tu correo",
        subtitle = "Enviamos un enlace de verificación a $email. Ábrelo para continuar.",
        onBack = onBack,
    ) {
        if (message != null) {
            Text(
                text = message,
                color = if (message.startsWith("No pudimos")) Error else PrimaryContainer,
                fontFamily = DmSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        PillButton(
            text = if (isLoading) "Verificando…" else "Ya verifiqué mi correo",
            onClick = { if (!isLoading) onChecked() },
            containerColor = SecondaryContainer,
            contentColor = SurfaceContainerLowest,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Reenviar correo",
            color = Primary,
            fontFamily = DmSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.clickable(onClick = onResend),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Cerrar sesión",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onSignOut),
        )
    }
}

@Composable
fun AccountReviewScreen(
    rejected: Boolean = false,
    isLoading: Boolean = false,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    AccessScaffold(
        title = if (rejected) "Solicitud rechazada" else "Tu cuenta está en revisión",
        subtitle = if (rejected) {
            "Contacta a la escuela para más información."
        } else {
            "Tu solicitud fue enviada. La directora la revisará y te dará acceso cuando la apruebe."
        },
        onBack = onBack,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (rejected) Error.copy(alpha = 0.12f) else Color(0xFFFFD8A8))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = if (rejected) "Solicitud rechazada" else "Pendiente de aprobación",
                color = if (rejected) Error else PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        PillButton(
            text = if (isLoading) "Actualizando…" else "Actualizar estado",
            onClick = { if (!isLoading) onRefresh() },
            containerColor = SecondaryContainer,
            contentColor = SurfaceContainerLowest,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Cerrar sesión",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onSignOut),
        )
    }
}

@Composable
fun LinkChildScreen(
    childName: String,
    rejected: Boolean = false,
    isLoading: Boolean = false,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    AccessScaffold(
        title = "Pendiente de vinculación",
        subtitle = if (rejected) {
            "La directora rechazó el nombre «$childName». Corrígelo y genera un código QR nuevo."
        } else {
            "Preséntate con la directora para vincular a $childName."
        },
        onBack = onBack,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFD8A8))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = if (rejected) "Vinculación rechazada" else "Pendiente de vinculación",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        PillButton(
            text = if (isLoading) "Actualizando…" else "Ya fui vinculado",
            onClick = { if (!isLoading) onRefresh() },
            containerColor = SecondaryContainer,
            contentColor = SurfaceContainerLowest,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Cerrar sesión",
            color = OnSurfaceVariant,
            fontFamily = DmSans,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onSignOut),
        )
    }
}

/** Resolves where to go once the current account is allowed in. */
fun AccessStateUiState.nextRoute(): String = when (role) {
    UserRole.ADMIN -> "admin/dashboard"
    UserRole.TEACHER -> if (estado == "aprobado") "teacher/home" else "account_review"
    UserRole.TUTOR -> if (vinculacion == "vinculado") "parent/home" else "link_child"
    UserRole.UNKNOWN -> "login"
}
