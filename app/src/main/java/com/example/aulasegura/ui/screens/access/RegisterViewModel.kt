package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.TutorRegistrationInput
import com.example.aulasegura.domain.repository.registrationError
import com.example.aulasegura.domain.repository.registrationMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val code: String = "",
    val accepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registeredRole: UserRole? = null,
)

/**
 * The family registration form. Only a tutor can be created here: there is no
 * role picker and no code-free path. Navigation is emitted through
 * [RegisterUiState.registeredRole], consumed by the caller.
 */
class RegisterViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun setFullName(value: String) = _uiState.update { it.copy(fullName = value) }
    fun setEmail(value: String) = _uiState.update { it.copy(email = value) }
    fun setPhone(value: String) = _uiState.update { it.copy(phone = value) }
    fun setPassword(value: String) = _uiState.update { it.copy(password = value) }
    fun setConfirmPassword(value: String) = _uiState.update { it.copy(confirmPassword = value) }
    fun setCode(value: String) = _uiState.update { it.copy(code = value) }
    fun setAccepted(value: Boolean) = _uiState.update { it.copy(accepted = value) }

    fun submit() {
        if (_uiState.value.isLoading) return
        val state = _uiState.value

        val invalid = registrationError(
            TutorRegistrationInput(
                fullName = state.fullName,
                email = state.email,
                phone = state.phone,
                password = state.password,
                code = state.code,
            ),
        )
        val error = invalid ?: when {
            state.password != state.confirmPassword -> "Las contraseñas no coinciden."
            !state.accepted -> "Acepta el aviso de privacidad y los términos de uso."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = auth.registerTutor(
                TutorRegistrationInput(
                    fullName = state.fullName.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    password = state.password,
                    code = state.code,
                ),
            )
            _uiState.update {
                when (result) {
                    is com.example.aulasegura.domain.repository.TutorRegistrationResult.Success ->
                        it.copy(isLoading = false, registeredRole = result.user.role)
                    else -> it.copy(
                        isLoading = false,
                        errorMessage = registrationMessage(result),
                    )
                }
            }
        }
    }

    fun consumeNavigation() = _uiState.update { it.copy(registeredRole = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
