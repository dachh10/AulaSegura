package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.repository.AuthRepositoryImpl
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Resolving : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val role: UserRole) : SessionState
}

/**
 * Resolves the persisted Firebase session at startup and exposes logout.
 *
 * Lives at the navigation level so every role can sign out through the same
 * instance.
 */
class SessionViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Resolving)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun resolveSession() {
        if (_state.value is SessionState.SignedIn) return
        viewModelScope.launch {
            _state.value = when (val result = authRepository.restoreSession()) {
                is AuthResult.Success -> SessionState.SignedIn(result.user.role)
                is AuthResult.Failure -> SessionState.SignedOut
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        authRepository.signOut()
        _state.value = SessionState.SignedOut
        onSignedOut()
    }
}
