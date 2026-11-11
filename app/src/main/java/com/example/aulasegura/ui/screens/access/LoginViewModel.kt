package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.repository.AuthRepositoryImpl
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthFailure
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedRole: UserRole? = null,
)

/**
 * Owns the login request. Navigation is emitted through [LoginUiState.authenticatedRole]
 * and consumed by the caller, so the ViewModel never touches a NavController.
 */
class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_uiState.value.isLoading) return

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Completa correo y contraseña") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoading = false, authenticatedRole = result.user.role)
                }

                is AuthResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.reason.toMessage())
                }
            }
        }
    }

    fun consumeNavigation() {
        _uiState.update { it.copy(authenticatedRole = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

private fun AuthFailure.toMessage(): String = when (this) {
    AuthFailure.INVALID_CREDENTIALS -> "Correo o contraseña incorrectos"
    AuthFailure.USER_DISABLED -> "Esta cuenta fue deshabilitada"
    AuthFailure.NO_SESSION -> "No hay una sesión activa"
    AuthFailure.PROFILE_MISSING -> "La cuenta no tiene perfil en el plantel"
    AuthFailure.PROFILE_INACTIVE -> "Tu cuenta está inactiva. Contacta a la dirección"
    AuthFailure.ROLE_UNKNOWN -> "La cuenta tiene un rol no reconocido"
    AuthFailure.NETWORK -> "Sin conexión. Revisa tu red e intenta de nuevo"
    AuthFailure.UNKNOWN -> "No pudimos iniciar sesión. Intenta de nuevo"
}
