package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sentTo: String? = null,
)

/**
 * Sends the password reset email through Firebase Auth.
 *
 * Security note: Firebase deliberately does not reveal whether the address is
 * registered, so an unknown email still lands on the "check your email" screen.
 * Only a missing/invalid address or a network failure surfaces an error.
 */
class ForgotPasswordViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
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
            try {
                auth.sendPasswordResetEmail(trimmed).await()
                _uiState.update { it.copy(isLoading = false, sentTo = trimmed) }
            } catch (e: FirebaseAuthInvalidUserException) {
                // Do not leak account existence: behave exactly like success.
                _uiState.update { it.copy(isLoading = false, sentTo = trimmed) }
            } catch (e: Exception) {
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
