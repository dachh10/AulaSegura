package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AppUser
import kotlinx.coroutines.flow.Flow

/**
 * Authentication contract.
 *
 * Implementations live in `data/`; the UI only ever sees this interface and the
 * domain models it returns.
 */
interface AuthRepository {

    /** `uid` of the persisted session, or `null` when nobody is signed in. */
    val currentUserId: String?

    /** Signs in and resolves the role from `users/{uid}`. */
    suspend fun signIn(email: String, password: String): AuthResult

    /** Resolves the persisted session, if any. Used by Splash. */
    suspend fun restoreSession(): AuthResult

    /**
     * Watches `users/{uid}.active` while a session is open.
     *
     * Emits only on an explicit document value; a read failure emits nothing so
     * a flaky network never signs anybody out by mistake.
     */
    fun observeAccountActive(): Flow<Boolean>

    fun signOut()
}

sealed interface AuthResult {

    data class Success(val user: AppUser) : AuthResult

    data class Failure(val reason: AuthFailure) : AuthResult
}

enum class AuthFailure {
    INVALID_CREDENTIALS,
    USER_DISABLED,
    NO_SESSION,
    PROFILE_MISSING,
    PROFILE_INACTIVE,
    ROLE_UNKNOWN,
    NETWORK,
    UNKNOWN,
}
