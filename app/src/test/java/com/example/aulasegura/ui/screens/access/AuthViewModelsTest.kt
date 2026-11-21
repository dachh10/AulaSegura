package com.example.aulasegura.ui.screens.access

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AccountRegistrationInput
import com.example.aulasegura.domain.repository.AccountRegistrationResult
import com.example.aulasegura.domain.repository.AuthFailure
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.domain.repository.PushTokenRepository
import com.example.aulasegura.domain.repository.RegistrationKind
import com.example.aulasegura.domain.repository.TutorRegistrationInput
import com.example.aulasegura.domain.repository.TutorRegistrationResult
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
import org.junit.Assert.assertTrue
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
    var lastPhotoUrl: String? = null
    var lastPhotoPublicId: String? = null
    var photoResult: ProfilePhotoResult = ProfilePhotoResult.Saved
    var lastName: String? = null
    var lastSubjects: List<String> = emptyList()
    var profileResult: ProfileUpdateResult = ProfileUpdateResult.Saved
    var currentPassword: String? = null
    var newPassword: String? = null
    var passwordResult: PasswordChangeResult = PasswordChangeResult.Changed
    var registerResult: TutorRegistrationResult =
        TutorRegistrationResult.Success(user(UserRole.TUTOR))
    var lastRegistration: TutorRegistrationInput? = null
    var accountRegisterResult: AccountRegistrationResult =
        AccountRegistrationResult.Success(user(UserRole.TUTOR))
    var lastAccountRegistration: AccountRegistrationInput? = null

    override val currentUserId: String? = null

    override var currentUser: AppUser? = null

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls++
        lastEmail = email
        return signInResult
    }

    override suspend fun registerTutor(input: TutorRegistrationInput): TutorRegistrationResult {
        lastRegistration = input
        return registerResult
    }

    override suspend fun registerAccount(
        input: AccountRegistrationInput,
    ): AccountRegistrationResult {
        lastAccountRegistration = input
        return accountRegisterResult
    }

    override suspend fun restoreSession(): AuthResult = restoreResult

    override fun observeAccountActive(): Flow<Boolean> = accountActive

    override suspend fun sendPasswordReset(email: String): PasswordResetResult =
        PasswordResetResult.Sent

    override suspend fun updateProfilePhoto(
        photoUrl: String,
        photoPublicId: String,
    ): ProfilePhotoResult {
        lastPhotoUrl = photoUrl
        lastPhotoPublicId = photoPublicId
        return photoResult
    }

    override suspend fun updateProfile(
        fullName: String,
        subjects: List<String>,
    ): ProfileUpdateResult {
        lastName = fullName
        lastSubjects = subjects
        return profileResult
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): PasswordChangeResult {
        this.currentPassword = currentPassword
        this.newPassword = newPassword
        return passwordResult
    }

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

/**
 * Counts the token work so the logout contract can be asserted without Firebase.
 * The real repository deletes `users/{uid}/fcmTokens/{token}`.
 */
private class FakePushTokenRepository : PushTokenRepository {

    var syncCalls = 0
    var deleteCalls = 0

    override suspend fun syncToken(): String? {
        syncCalls++
        return "token-1"
    }

    override suspend fun registerToken(token: String) = Unit

    override suspend fun deleteCurrentToken() {
        deleteCalls++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private fun session(
        repo: FakeAuthRepository,
        push: FakePushTokenRepository = FakePushTokenRepository(),
    ) = SessionViewModel(repo, push)

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
        val viewModel = session(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn(UserRole.ADMIN), viewModel.state.value)
    }

    @Test
    fun `restoring without session signs out`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(restoreResult = AuthResult.Failure(AuthFailure.NO_SESSION))
        val viewModel = session(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `logout calls the repository and clears the state`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(restoreResult = AuthResult.Success(user(UserRole.TEACHER)))
        val viewModel = session(repo)

        viewModel.resolveSession()
        advanceUntilIdle()
        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(1, repo.signOutCalls)
        assertEquals(SessionState.SignedOut, viewModel.state.value)
    }

    @Test
    fun `logout deletes the device token first, so a shared phone stops receiving push`() =
        runTest(dispatcher) {
            val repo = FakeAuthRepository(restoreResult = AuthResult.Success(user(UserRole.TEACHER)))
            val push = FakePushTokenRepository()
            val viewModel = session(repo, push)

            viewModel.resolveSession()
            advanceUntilIdle()
            viewModel.signOut()
            advanceUntilIdle()

            assertEquals(1, push.deleteCalls)
            assertEquals("the token is deleted before the session disappears", 1, repo.signOutCalls)
            assertEquals(SessionState.SignedOut, viewModel.state.value)
        }

    @Test
    fun `a resolved session registers this device's token`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(restoreResult = AuthResult.Success(user(UserRole.TEACHER)))
        val push = FakePushTokenRepository()
        val viewModel = session(repo, push)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(1, push.syncCalls)
    }

    @Test
    fun `a deactivated account is ejected mid-session`() = runTest(dispatcher) {
        val repo = FakeAuthRepository(
            restoreResult = AuthResult.Success(user(UserRole.TEACHER)),
            accountActive = flowOf(false),
        )
        val viewModel = session(repo)

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
        val viewModel = session(repo)

        viewModel.resolveSession()
        advanceUntilIdle()

        assertEquals(SessionState.SignedIn(UserRole.TEACHER), viewModel.state.value)
        assertEquals(0, repo.signOutCalls)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel(repo: FakeAuthRepository): RegisterViewModel {
        val vm = RegisterViewModel(repo)
        vm.setFullName("Mariana Gómez")
        vm.setEmail("mariana@correo.com")
        vm.setPhone("55 1234 5678")
        vm.setPassword("AulaSegura#2024")
        vm.setConfirmPassword("AulaSegura#2024")
        vm.setChildName("Mariana Gómez Ruiz")
        vm.setAccepted(true)
        return vm
    }

    @Test
    fun `a missing child name is rejected before any account is created`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)
        vm.setChildName("")

        vm.submit()

        assertEquals("Escribe el nombre completo de tu hijo.",
            vm.uiState.value.errorMessage)
        assertNull(repo.lastAccountRegistration)
    }

    @Test
    fun `an unaccepted form does not call the repository`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)
        vm.setAccepted(false)

        vm.submit()

        assertEquals("Acepta el aviso de privacidad y los términos de uso.",
            vm.uiState.value.errorMessage)
        assertNull(repo.lastAccountRegistration)
    }

    @Test
    fun `a successful tutor registration carries the child name`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)

        vm.submit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.succeeded)
        assertEquals(RegistrationKind.TUTOR, repo.lastAccountRegistration?.kind)
        assertEquals("Mariana Gómez Ruiz", repo.lastAccountRegistration?.childName)
        assertEquals("", repo.lastAccountRegistration?.subject)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `a teacher registration carries the subject and no child`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        val vm = viewModel(repo)
        vm.setKind(RegistrationKind.DOCENTE)
        vm.setChildName("")
        vm.setSubject("Matemáticas")

        vm.submit()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.succeeded)
        assertEquals(RegistrationKind.DOCENTE, repo.lastAccountRegistration?.kind)
        assertEquals("Matemáticas", repo.lastAccountRegistration?.subject)
        assertEquals("", repo.lastAccountRegistration?.childName)
    }

    @Test
    fun `an email already in use explains why and emits no success`() = runTest(dispatcher) {
        val repo = FakeAuthRepository()
        repo.accountRegisterResult = AccountRegistrationResult.EmailInUse
        val vm = viewModel(repo)

        vm.submit()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.succeeded)
        assertEquals("Ya existe una cuenta con ese correo. Inicia sesión.",
            vm.uiState.value.errorMessage)
    }
}
