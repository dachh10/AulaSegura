package com.example.aulasegura.ui.screens.access

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** A verified pending teacher must land on the review screen, not stay stuck. */
@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationFlowTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a verified pending teacher proceeds to account review`() = runTest(dispatcher) {
        val teacher = AppUser(
            uid = "docente-1",
            email = "docente@escuela.mx",
            role = UserRole.TEACHER,
            fullName = "Docente Nuevo",
            schoolId = "sch-1",
            active = true,
            estado = "pendiente",
        )
        val vm = AccessStateViewModel(VerifiedAuth(teacher))
        advanceUntilIdle()

        vm.checkEmail()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.proceed)
        assertEquals(
            Routes.ACCOUNT_REVIEW,
            vm.uiState.value.copy(proceed = false).nextRoute(),
        )
    }
}

/** Delegating stub: checkEmail only uses isEmailVerified, markEmailVerified and refreshProfile. */
private open class BaseAuth : AuthRepository by object : AuthRepository {
    override val currentUserId: String? = null
    override var currentUser: AppUser? = null
    override suspend fun signIn(email: String, password: String): com.example.aulasegura.domain.repository.AuthResult =
        com.example.aulasegura.domain.repository.AuthResult.Failure(
            com.example.aulasegura.domain.repository.AuthFailure.UNKNOWN)
    override suspend fun registerTutor(input: com.example.aulasegura.domain.repository.TutorRegistrationInput) =
        com.example.aulasegura.domain.repository.TutorRegistrationResult.Failed
    override suspend fun restoreSession(): com.example.aulasegura.domain.repository.AuthResult =
        com.example.aulasegura.domain.repository.AuthResult.Failure(
            com.example.aulasegura.domain.repository.AuthFailure.NO_SESSION)
    override fun observeAccountActive(): kotlinx.coroutines.flow.Flow<Boolean> =
        kotlinx.coroutines.flow.emptyFlow()
    override suspend fun updateProfilePhoto(photoUrl: String, photoPublicId: String) =
        com.example.aulasegura.domain.repository.ProfilePhotoResult.Failed
    override suspend fun updateProfile(fullName: String, subjects: List<String>) =
        com.example.aulasegura.domain.repository.ProfileUpdateResult.Failed
    override suspend fun changePassword(currentPassword: String, newPassword: String) =
        com.example.aulasegura.domain.repository.PasswordChangeResult.WrongPassword
    override suspend fun sendPasswordReset(email: String) =
        com.example.aulasegura.domain.repository.PasswordResetResult.Sent
    override fun signOut() = Unit
}

private class VerifiedAuth(private val teacher: AppUser) : BaseAuth() {
    override var currentUser: AppUser? = teacher
    override suspend fun isEmailVerified(): Boolean = true
    override suspend fun markEmailVerified(): Result<Unit> = Result.success(Unit)
    override suspend fun refreshProfile(): com.example.aulasegura.domain.repository.AuthResult =
        com.example.aulasegura.domain.repository.AuthResult.Success(teacher)
}
