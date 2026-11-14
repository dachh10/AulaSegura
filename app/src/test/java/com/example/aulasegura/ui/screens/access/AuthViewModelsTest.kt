package com.example.aulasegura.ui.screens.access

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthFailure
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Fake repository. The ViewModels only see the domain contract, which is exactly
 * why they are testable without Firebase.
 */
private class FakeAuthRepository(
    var signInResult: AuthResult = AuthResult.Failure(AuthFailure.UNKNOWN),
    var restoreResult: AuthResult = AuthResult.Failure(AuthFailure.NO_SESSION),
    var accountActive: Flow<Boolean> = emptyFlow(),
) : AuthRepository {

    var signInCalls = 0
    var signOutCalls = 0
    var lastEmail: String? = null

    override val currentUserId: String? = null

    override var currentUser: AppUser? = null

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls++
        lastEmail = email
        return signInResult
    }

    override suspend fun restoreSession(): AuthResult = restoreResult

    override fun observeAccountActive(): Flow<Boolean> = accountActive

    override suspend fun sendPasswordReset(email: String): PasswordResetResult =
        PasswordResetResult.Sent

    override fun signOut() {
        signOutCalls++
    }
}

private fun user(role: UserRole) = AppUser(
    uid = "uid-1",
    email = "test@aulasegura.com",
    role = role,
    fullName = "Test User",
    schoolId = "sch-test",
    active = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank fields fail without calling the repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo)

        viewModel.signIn("", "")
        advanceUntilIdle()

        assertEquals("Completa correo y contraseña", viewModel.uiState.value.errorMessage)
        assertEquals(0, repo.signInCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `invalid credentials surface a message and no role`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            signInResult = AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS),
        )
        val viewModel = LoginViewModel(repo)

        viewModel.signIn("docente@aulasegura.com", "wrong")
        advanceUntilIdle()

        assertEquals("Correo o contraseña incorrectos", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.authenticatedRole)
    }

    @Test
    fun `successful login emits the role and trims the email`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(signInResult = AuthResult.Success(user(UserRole.TEACHER)))
        val viewModel = LoginViewModel(repo)

        viewModel.signIn("  docente@aulasegura.com  ", "123456")
        advanceUntilIdle()

        assertEquals("docente@aulasegura.com", repo.lastEmail)
        assertEquals(UserRole.TEACHER, viewModel.uiState.value.authenticatedRole)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `consuming navigation clears the emitted role`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(signInResult = AuthResult.Success(user(UserRole.TUTOR)))
        val viewModel = LoginViewModel(repo)

        viewModel.signIn("carlos.gomez@familia.mx", "AulaSegura#2024")
        advanceUntilIdle()
        viewModel.consumeNavigation()

        assertNull(viewModel.uiState.value.authenticatedRole)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restoring a session publishes the role`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(restoreResult = AuthResult.Success(user(UserRole.ADMIN)))
        val viewModel = SessionViewModel(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn(UserRole.ADMIN), viewModel.state.value)
    }

    @Test
    fun `restoring without session signs out`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(restoreResult = AuthResult.Failure(AuthFailure.NO_SESSION))
        val viewModel = SessionViewModel(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `logout calls the repository and clears the state`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(restoreResult = AuthResult.Success(user(UserRole.TEACHER)))
        val viewModel = SessionViewModel(repo)

        viewModel.resolveSession()
        advanceUntilIdle()
        viewModel.signOut()

        assertEquals(1, repo.signOutCalls)
        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `a deactivated account is ejected mid-session`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            restoreResult = AuthResult.Success(user(UserRole.TEACHER)),
            accountActive = flowOf(false),
        )
        val viewModel = SessionViewModel(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.state.value)
        assertEquals(1, repo.signOutCalls)
    }

    @Test
    fun `an active account stays signed in`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            restoreResult = AuthResult.Success(user(UserRole.TEACHER)),
            accountActive = flowOf(true, true),
        )
        val viewModel = SessionViewModel(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn(UserRole.TEACHER), viewModel.state.value)
        assertEquals(0, repo.signOutCalls)
    }
}
