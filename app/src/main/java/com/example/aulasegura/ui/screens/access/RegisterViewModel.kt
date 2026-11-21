package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AccountRegistrationInput
import com.example.aulasegura.domain.repository.AccountRegistrationResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.RegistrationKind
import com.example.aulasegura.domain.repository.accountRegistrationError
import com.example.aulasegura.domain.repository.accountRegistrationMessage
import com.example.aulasegura.domain.repository.passwordStrength
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val kind: RegistrationKind = RegistrationKind.TUTOR,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val childName: String = "",
    val subject: String = "",
    val accepted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set on success; the caller navigates to the email-verification screen. */
    val succeeded: Boolean = false,
) {
    val strength: Int get() = passwordStrength(password)
}

/**
 * Registration for both new roles. No student code and no role picker: the
 * form has two tabs and writes `rol = tutor | docente` with its initial state.
 */
class RegisterViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun setKind(value: RegistrationKind) = _uiState.update { it.copy(kind = value, errorMessage = null) }
    fun setFullName(value: String) = _uiState.update { it.copy(fullName = value) }
    fun setEmail(value: String) = _uiState.update { it.copy(email = value) }
    fun setPhone(value: String) = _uiState.update { it.copy(phone = value) }
    fun setPassword(value: String) = _uiState.update { it.copy(password = value) }
    fun setConfirmPassword(value: String) = _uiState.update { it.copy(confirmPassword = value) }
    fun setChildName(value: String) = _uiState.update { it.copy(childName = value) }
    fun setSubject(value: String) = _uiState.update { it.copy(subject = value) }
    fun setAccepted(value: Boolean) = _uiState.update { it.copy(accepted = value) }

    fun submit() {
        if (_uiState.value.isLoading) return
        val state = _uiState.value
        val input = AccountRegistrationInput(
            kind = state.kind,
            fullName = state.fullName.trim(),
            email = state.email.trim(),
            phone = state.phone.trim(),
            password = state.password,
            confirmPassword = state.confirmPassword,
            childName = state.childName.trim(),
            subject = state.subject.trim(),
            accepted = state.accepted,
        )
        val error = accountRegistrationError(input)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = auth.registerAccount(input)
            _uiState.update {
                when (result) {
                    is AccountRegistrationResult.Success ->
                        it.copy(isLoading = false, succeeded = true)
                    else -> it.copy(
                        isLoading = false,
                        errorMessage = accountRegistrationMessage(result),
                    )
                }
            }
        }
    }

    fun consumeNavigation() = _uiState.update { it.copy(succeeded = false) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
