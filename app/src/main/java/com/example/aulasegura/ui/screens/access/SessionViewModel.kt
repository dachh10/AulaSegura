package com.example.aulasegura.ui.screens.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.PushTokenRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SessionState {
    data object Resolving : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val role: UserRole) : SessionState
}

/**
 * Resolves the persisted Firebase session at startup, keeps watching whether the
 * account is still active, and exposes logout.
 *
 * Lives at the navigation level so every role can sign out through the same
 * instance, and so a deactivated account is ejected from any screen.
 */
class SessionViewModel(
    private val authRepository: AuthRepository,
    private val pushTokens: PushTokenRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Resolving)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var accountWatch: Job? = null

    fun resolveSession() {
        if (_state.value is SessionState.SignedIn) return
        viewModelScope.launch {
            when (val result = authRepository.restoreSession()) {
                is AuthResult.Success -> {
                    _state.value = SessionState.SignedIn(result.user.role)
                    syncPushToken()
                    watchAccountStatus()
                }

                is AuthResult.Failure -> _state.value = SessionState.SignedOut
            }
        }
    }

    fun signOut() {
        accountWatch?.cancel()
        accountWatch = null

        viewModelScope.launch {
            // The token belongs to the account, not to the phone: a shared device
            // must stop receiving the previous teacher's push. Bounded, because a
            // logout cannot wait on a dying network.
            withTimeoutOrNull(SIGNOUT_RPC_TIMEOUT) { pushTokens.deleteCurrentToken() }
            authRepository.signOut()
            _state.value = SessionState.SignedOut
        }
    }

    /** Login succeeded: publish the role and start watching the account. */
    fun onSignedIn(role: UserRole) {
        _state.value = SessionState.SignedIn(role)
        syncPushToken()
        watchAccountStatus()
    }

    /**
     * Registers this device's FCM token under the signed-in user.
     *
     * Fire and forget: without push the app still works, and a failure here must
     * not make the login look broken.
     */
    private fun syncPushToken() {
        viewModelScope.launch { pushTokens.syncToken() }
    }

    /**
     * A directivo can set `active = false` while the affected user is already
     * inside the app. Without this listener they would keep working on cached
     * data until they restarted.
     */
    private fun watchAccountStatus() {
        accountWatch?.cancel()
        accountWatch = viewModelScope.launch {
            // firstOrNull closes the snapshot listener as soon as a deactivation
            // arrives, so the watch does not outlive its usefulness.
            val deactivated = authRepository.observeAccountActive().firstOrNull { !it } != null
            if (deactivated) {
                accountWatch = null
                withTimeoutOrNull(SIGNOUT_RPC_TIMEOUT) { pushTokens.deleteCurrentToken() }
                authRepository.signOut()
                _state.value = SessionState.SignedOut
            }
        }
    }

    private companion object {
        const val SIGNOUT_RPC_TIMEOUT = 2_500L
    }
}
