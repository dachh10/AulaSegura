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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.aulasegura.domain.repository.ParentLinkQr
import com.example.aulasegura.ui.components.PillButton
import com.example.aulasegura.ui.components.QrCodeImage
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.Background
import com.example.aulasegura.ui.theme.BrandCoral
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccessStateUiState(
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.TUTOR,
    val estado: String = "pendiente",
    val vinculacion: String = "pendiente",
    val childName: String = "",
    val message: String? = null,
    val isLoading: Boolean = false,
    /** Set once the app may move on; the NavHost resolves the exact route. */
    val proceed: Boolean = false,
    val knownChildIds: Set<String> = emptySet(),
) {
    val isRejected: Boolean get() = estado == "rechazado"
}

/** Shared state for the "check your email", "account in review" and "link child" steps. */
class AccessStateViewModel(
    private val auth: AuthRepository,
    private val parents: com.example.aulasegura.domain.repository.ParentRepository? = null,
    /** "Agregar otro hijo" must never auto-navigate on entry. */
    private val addMode: Boolean = false,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessStateUiState())
    val uiState: StateFlow<AccessStateUiState> = _uiState.asStateFlow()

    /**
     * The first Firestore linking snapshot decides whether this visit started
     * already linked. auth.currentUser is stale right after the first approval
     * (the listener updates the UI, not the cached profile).
     */
    private var linkingBaseline: String? = null

    /** Cancelled and replaced every time a new QR is generated in ADD mode. */
    private var watcherJob: Job? = null

    init {
        val current = auth.currentUser
        // In ADD mode the watcher created at QR generation is the only live
        // roster listener; keeping this one open made the baseline move while
        // the QR was being generated.
        if (!addMode) {
            parents?.observeMyChildren()?.let { flow ->
                viewModelScope.launch {
                    flow.collect { ids -> _uiState.update { it.copy(knownChildIds = ids.toSet()) } }
                }
            }
        }
        _uiState.update {
            it.copy(
                uid = current?.uid.orEmpty(),
                email = current?.email.orEmpty(),
                role = current?.role ?: UserRole.TUTOR,
                estado = current?.estado?.ifBlank { "pendiente" } ?: "pendiente",
                vinculacion = current?.vinculacion?.ifBlank { "pendiente" } ?: "pendiente",
                childName = current?.hijoSolicitado.orEmpty(),
            )
        }
        // The director approves on another device. A live listener is the only
        // way the tutor sees it without having to guess when to tap "refresh".
        viewModelScope.launch {
            auth.observeLinkingState().collect { link ->
                val baseline = linkingBaseline
                val startedLinked = if (baseline == null) {
                    linkingBaseline = link.vinculacion
                    link.vinculacion == "vinculado"
                } else {
                    baseline == "vinculado"
                }
                _uiState.update { ui ->
                    // ADD mode never auto-navigates here: the only exit is the
                    // roster watcher after a QR has actually been generated.
                    // Never clear a proceed already granted by that watcher.
                    val proceed = when (ui.role) {
                        UserRole.TUTOR -> if (addMode) {
                            ui.proceed
                        } else {
                            ui.proceed || shouldAutoProceed(startedLinked, link.vinculacion)
                        }
                        UserRole.TEACHER ->
                            ui.proceed || link.estado == "aprobado" || link.estado == "rechazado"
                        else -> ui.proceed
                    }
                    ui.copy(
                        vinculacion = link.vinculacion.ifBlank { ui.vinculacion },
                        estado = link.estado.ifBlank { ui.estado },
                        childName = link.hijoSolicitado.ifBlank { ui.childName },
                        proceed = proceed,
                    )
                }
            }
        }
    }

    /**
     * After generating a QR for another child, watch the tutor's roster. When a
     * NEW student id appears (the director scanned and approved), go back home.
     * Only called in ADD mode, so it cannot cause the first-linking bounce.
     */
    fun watchForNewChild(knownIds: Set<String>) {
        val repo = parents ?: return
        // A previous QR generation must not leave a second listener behind.
        watcherJob?.cancel()
        watcherJob = viewModelScope.launch {
            val watcher = NewChildWatcher(knownIds)
            repo.observeMyChildren().collect { ids ->
                if (watcher.hasNewChild(ids)) {
                    _uiState.update { it.copy(proceed = true) }
                    return@collect
                }
            }
        }
    }

    fun setChildName(value: String) = _uiState.update { it.copy(childName = value, message = null) }

    fun saveChildName() {
        if (_uiState.value.isLoading) return
        val name = _uiState.value.childName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(message = "Escribe el nombre del alumno.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            val result = auth.updateChildName(name)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    childName = name,
                    message = if (result.isSuccess) {
                        "Nombre actualizado. Muestra el código QR a la directora."
                    } else {
                        "No pudimos guardar el nombre. Inténtalo de nuevo."
                    },
                )
            }
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
            // Persist the confirmation so the director's queue can tell verified
            // teachers apart; the flag cannot be read from another user's Auth.
            runCatching { auth.markEmailVerified() }
            val refreshed = auth.refreshProfile()
            val user = (refreshed as? AuthResult.Success)?.user ?: auth.currentUser
            // Every verified account has somewhere to go. A pending teacher goes
            // to "Cuenta en revisión" (nextRoute already maps estado), not back to
            // login and not stuck here. Only an unknown role blocks navigation.
            val current = _uiState.value
            val target = current.copy(
                role = user?.role ?: current.role,
                estado = user?.estado ?: current.estado,
                vinculacion = user?.vinculacion ?: current.vinculacion,
            ).nextRoute()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    role = user?.role ?: it.role,
                    estado = user?.estado ?: it.estado,
                    vinculacion = user?.vinculacion ?: it.vinculacion,
                    proceed = target != "login",
                )
            }
        }
    }

    fun refreshState() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, message = null) }
        viewModelScope.launch {
            // Self-heal accounts that verified before the flag existed: the rule
            // only accepts this write while Auth itself says the email is verified.
            if (runCatching { auth.isEmailVerified() }.getOrDefault(false)) {
                runCatching { auth.markEmailVerified() }
            }
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
    // On entry, self-heal the verified-email flag: a teacher may confirm the
    // link while the app is already on this screen. refreshState() re-persists
    // the flag via markEmailVerified() (fresh token), so the director sees them
    // without the user having to discover the "Actualizar estado" button.
    LaunchedEffect(Unit) { onRefresh() }

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
    uid: String = "",
    mode: LinkChildMode = LinkChildMode.PENDING,
    childName: String,
    rejected: Boolean = false,
    isLoading: Boolean = false,
    message: String? = null,
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onChildNameChange: (String) -> Unit = {},
    onSaveChildName: () -> Unit = {},
    onQrGenerated: (Set<String>) -> Unit = {},
    knownChildIds: Set<String> = emptySet(),
) {
    // ADD mode keeps the new child's name LOCAL: the QR carries it and no profile
    // field is written (hijoSolicitado is not self-editable by the rules).
    var localName by remember { mutableStateOf("") }
    var localQrReady by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(mode == LinkChildMode.ADD) }

    val isAdd = mode == LinkChildMode.ADD
    val shownName = if (isAdd) localName else childName
    val showQr = if (isAdd) localQrReady && localName.isNotBlank() else childName.isNotBlank()
    val payload = remember(uid, shownName) { ParentLinkQr.encode(uid, shownName) }

    AccessScaffold(
        title = mode.title,
        subtitle = if (isAdd) {
            "Escribe el nombre del nuevo alumno y muestra el código a la directora."
        } else if (rejected) {
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
                text = if (isAdd) "Nuevo hijo" else if (rejected) "Vinculación rechazada" else "Pendiente de vinculación",
                color = PrimaryContainer,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(24.dp))

        if (isAdd && !localQrReady) {
            OutlinedTextField(
                value = localName,
                onValueChange = { localName = it },
                label = { Text("Nombre completo del nuevo alumno") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                text = "Generar QR",
                onClick = {
                    if (localName.isNotBlank()) {
                        localQrReady = true
                        editingName = false
                        onQrGenerated(knownChildIds)
                    }
                },
                containerColor = Primary,
            )
        } else if (showQr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainerLowest)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                QrCodeImage(content = payload, modifier = Modifier.size(240.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Alumno: $shownName",
                color = Primary,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            if (isAdd) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Cambiar nombre",
                    color = BrandCoral,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { localQrReady = false },
                )
            }
        }

        if (!isAdd) {
            Spacer(Modifier.height(24.dp))
            Text(text = "1. Preséntate con la directora", color = OnSurfaceVariant,
                fontFamily = DmSans, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = "2. Muestra este código QR", color = OnSurfaceVariant,
                fontFamily = DmSans, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = "3. Ella lo escaneará y vincularemos a tu hijo", color = OnSurfaceVariant,
                fontFamily = DmSans, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Corregir nombre del alumno",
                color = BrandCoral,
                fontFamily = DmSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { editingName = !editingName },
            )
            if (editingName) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = childName,
                    onValueChange = onChildNameChange,
                    label = { Text("Nombre completo de tu hijo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    text = if (isLoading) "Guardando." else "Guardar nombre",
                    onClick = { if (!isLoading) onSaveChildName() },
                )
            }
        }

        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = message, color = Error, fontFamily = DmSans, fontSize = 13.sp)
        }
        if (!isAdd) {
            Spacer(Modifier.height(24.dp))
            PillButton(
                text = if (isLoading) "Actualizando." else "Ya fui vinculado",
                onClick = { if (!isLoading) onRefresh() },
                containerColor = SecondaryContainer,
                contentColor = SurfaceContainerLowest,
            )
        }
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

/**
 * Auto-navigation is for the FIRST linking only: a pending tutor is taken home
 * the moment the director approves. A tutor who was already linked came back to
 * look at/refresh their QR (e.g. "+ Agregar hijo") and must stay on this screen.
 */
internal fun shouldAutoProceed(wasLinkedOnEntry: Boolean, vinculacion: String): Boolean =
    !wasLinkedOnEntry && vinculacion == "vinculado"

/** Resolves where to go once the current account is allowed in. */
fun AccessStateUiState.nextRoute(): String = when (role) {
    UserRole.ADMIN -> "admin/dashboard"
    UserRole.TEACHER -> if (estado == "aprobado") "teacher/home" else "account_review"
    UserRole.TUTOR -> if (vinculacion == "vinculado") "parent/home" else "link_child"
    UserRole.UNKNOWN -> "login"
}
