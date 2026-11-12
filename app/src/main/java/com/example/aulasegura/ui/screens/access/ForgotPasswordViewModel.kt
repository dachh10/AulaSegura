package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.PasswordResetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sentTo: String? = null,
)

/**
 * Sends the password reset email through the auth repository.
 *
 * Security note: the repository never reveals whether the address is registered,
 * so an unknown email still lands on the "check your email" screen. Only a
 * missing/invalid address or a network failure surfaces an error.
 *
 * No Firebase type appears here on purpose: the screen depends on the domain
 * contract, which is what makes it constructible by the DI container and
 * testable without an emulator.
 */
class ForgotPasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun sendResetEmail(email: String) {
        if (_uiState.value.isLoading) return

        val trimmed = email.trim()
        if (!trimmed.contains("@") || trimmed.length < 5) {
            _uiState.update { it.copy(errorMessage = "Escribe un correo válido") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            when (authRepository.sendPasswordReset(trimmed)) {
                PasswordResetResult.Sent ->
                    _uiState.update { it.copy(isLoading = false, sentTo = trimmed) }

                PasswordResetResult.InvalidEmail ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Escribe un correo válido")
                    }

                PasswordResetResult.NetworkError,
                PasswordResetResult.Unknown,
                ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No pudimos enviar el enlace. Revisa tu conexión",
                        )
                    }
            }
        }
    }

    fun reset() {
        _uiState.value = ForgotPasswordUiState()
    }
}
