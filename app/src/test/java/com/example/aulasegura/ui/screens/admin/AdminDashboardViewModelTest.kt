package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminDashboard
import com.example.aulasegura.domain.model.AdminReportRangeSummary
import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AccountRole
import com.example.aulasegura.domain.repository.AdminDashboardRepository
import com.example.aulasegura.domain.repository.AdminDashboardResult
import com.example.aulasegura.domain.repository.AdminReportRepository
import com.example.aulasegura.domain.repository.AdminReportResult
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AdminUserUpdateResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.BatchResult
import com.example.aulasegura.domain.repository.CreateAccountResult
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.domain.repository.TutorRegistrationInput
import com.example.aulasegura.domain.repository.TutorRegistrationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * The dashboard's top bar greets the directora with her photo and name.
 *
 * `auth.currentUser` is the profile resolved at sign-in, so it keeps the old
 * picture after the profile screen saves a new one. These tests pin that the
 * greeting comes from the fresh `users/{uid}` document instead, and that reading
 * it never disturbs the metrics' own loading state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdminDashboardViewModelTest {

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
    fun `the greeting shows the photo saved in the profile, not the login snapshot`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                session = user(photoUrl = "https://cdn/old.jpg", fullName = "Directora vieja"),
                self = adminUser(photoUrl = "https://cdn/new.jpg", fullName = "Ana Directora"),
            )
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals("https://cdn/new.jpg", state.photoUrl)
            assertEquals("Ana Directora", state.fullName)
        }

    @Test
    fun `a failed server read keeps the photo the session already had`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                session = user(photoUrl = "https://cdn/old.jpg", fullName = "Directora"),
                self = null,
            )
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals("https://cdn/old.jpg", viewModel.state.value.photoUrl)
            assertEquals("Directora", viewModel.state.value.fullName)
        }

    @Test
    fun `refreshing the greeting leaves the metrics loading state untouched`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                session = user(photoUrl = "https://cdn/old.jpg", fullName = "Directora"),
                self = adminUser(photoUrl = "https://cdn/new.jpg", fullName = "Directora"),
            )
            advanceUntilIdle()
            assertFalse(viewModel.state.value.loading)
            assertFalse(viewModel.state.value.todayLoading)

            viewModel.refreshIdentity()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.loading)
            assertFalse(viewModel.state.value.todayLoading)
        }

    private fun viewModel(
        session: AppUser?,
        self: AdminUser?,
    ): AdminDashboardViewModel = AdminDashboardViewModel(
        dashboard = FakeDashboard(),
        reports = FakeReports(),
        users = FakeUsers(self),
        auth = FakeAuth(session),
    )

    private fun user(photoUrl: String?, fullName: String): AppUser = AppUser(
        uid = "directora-1",
        email = "directora@escuela.mx",
        role = UserRole.ADMIN,
        fullName = fullName,
        schoolId = "school-1",
        active = true,
        photoUrl = photoUrl,
    )

    private fun adminUser(photoUrl: String?, fullName: String): AdminUser = AdminUser(
        uid = "directora-1",
        email = "directora@escuela.mx",
        role = UserRole.ADMIN,
        firstName = fullName.substringBefore(' '),
        lastName = fullName.substringAfter(' ', ""),
        fullName = fullName,
        phone = "",
        subjects = emptyList(),
        groupIds = emptyList(),
        studentIds = emptyList(),
        active = true,
        photoUrl = photoUrl,
    )

    private class FakeDashboard : AdminDashboardRepository {
        override suspend fun getMetrics(): AdminDashboardResult =
            AdminDashboardResult.Success(AdminDashboard())
    }

    private class FakeReports : AdminReportRepository {
        override suspend fun getRangeSummary(
            fromDate: String,
            toDate: String,
        ): AdminReportResult<AdminReportRangeSummary> = summary()

        override suspend fun getGroupBreakdown(
            fromDate: String,
            toDate: String,
        ): AdminReportResult<List<com.example.aulasegura.domain.model.AdminGroupBreakdown>> =
            AdminReportResult.Success(emptyList())

        override suspend fun getTopAbsenceDays(
            fromDate: String,
            toDate: String,
        ): AdminReportResult<List<com.example.aulasegura.domain.model.AdminTopAbsenceDay>> =
            AdminReportResult.Success(emptyList())

        override suspend fun getTodaySummary(): AdminReportResult<AdminReportRangeSummary> =
            summary()

        private fun summary(): AdminReportResult<AdminReportRangeSummary> =
            AdminReportResult.Success(AdminReportRangeSummary())
    }

    private class FakeUsers(private val self: AdminUser?) : AdminUserRepository {
        override suspend fun getUsers(): List<AdminUser> = emptyList()

        override suspend fun getSelf(): AdminUser? = self

        override suspend fun updateProfile(
            uid: String,
            changes: Map<String, Any>,
        ): AdminUserUpdateResult = error("not used")

        override suspend fun updatePhoto(
            uid: String,
            photoUrl: String,
            photoPublicId: String,
        ): AdminUserUpdateResult = error("not used")

        override suspend fun setTeacherStatus(
            uid: String,
            estado: String,
        ): AdminUserUpdateResult = error("not used")

        override suspend fun acceptAllPendingTeachers(): BatchResult = error("not used")

        override suspend fun createAccount(
            email: String,
            password: String,
            firstName: String,
            lastName: String,
            role: AccountRole,
            phone: String,
            subjects: List<String>,
            groupIds: List<String>,
        ): CreateAccountResult = error("not used")
    }

    private class FakeAuth(private val session: AppUser?) : AuthRepository {
        override val currentUserId: String? get() = session?.uid
        override val currentUser: AppUser? get() = session

        override suspend fun signIn(email: String, password: String): AuthResult =
            error("not used")

        override suspend fun registerTutor(
            input: TutorRegistrationInput,
        ): TutorRegistrationResult = error("not used")

        override suspend fun restoreSession(): AuthResult = error("not used")

        override fun observeAccountActive(): Flow<Boolean> = flowOf(true)

        override suspend fun sendPasswordReset(email: String): PasswordResetResult =
            error("not used")

        override suspend fun updateProfilePhoto(
            photoUrl: String,
            photoPublicId: String,
        ): ProfilePhotoResult = error("not used")

        override suspend fun updateProfile(
            fullName: String,
            subjects: List<String>,
        ): ProfileUpdateResult = error("not used")

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult = error("not used")

        override fun signOut() = Unit
    }
}
