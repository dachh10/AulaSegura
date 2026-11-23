package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.PhotoCompressor
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.IncidentSeenResult
import com.example.aulasegura.domain.repository.NoticeReadResult
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentIncidentResult
import com.example.aulasegura.domain.repository.ParentIncidentsResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentNoticesResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.ParentTaskResult
import com.example.aulasegura.domain.repository.ParentTasksResult
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.domain.repository.RosterFailure
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The tutor's profile owns three real things: the children of the account, the
 * name in `users/{uid}` and the password of the session.
 *
 * Every test here pins a promise the screen makes to the family: the children
 * are the ones the roster returns, the name is written without touching
 * anything else, and a password the rules would reject never reaches Firebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentProfileViewModelTest {

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
    fun `the children of the account are the ones the roster returns`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                children = ParentChildrenResult.Loaded(
                    listOf(child(studentId = "stu-1", firstName = "Mariana")),
                ),
            )
            advanceUntilIdle()

            assertEquals(1, viewModel.ui.value.children.size)
            assertEquals("Mariana", viewModel.ui.value.children.first().name)
            assertNull(viewModel.ui.value.childrenMessage)
        }

    @Test
    fun `an account with no linked student says so instead of showing a child`() =
        runTest(dispatcher) {
            val viewModel = viewModel(children = ParentChildrenResult.Loaded(emptyList()))
            advanceUntilIdle()

            assertTrue(viewModel.ui.value.children.isEmpty())
            assertNull(viewModel.ui.value.childrenMessage)
        }

    @Test
    fun `a failed roster leaves an honest message instead of an empty card`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                children = ParentChildrenResult.Failed(RosterFailure.OFFLINE),
            )
            advanceUntilIdle()

            assertTrue(viewModel.ui.value.children.isEmpty())
            assertEquals(
                "Sin conexión. Revisa tu red e intenta de nuevo.",
                viewModel.ui.value.childrenMessage,
            )
        }

    @Test
    fun `saving the name writes it without touching the subjects`() =
        runTest(dispatcher) {
            val auth = FakeAuth()
            val viewModel = viewModel(auth = auth)
            advanceUntilIdle()

            viewModel.openEditProfile()
            viewModel.setEditName("  Ana Ruiz  ")
            viewModel.saveProfile()
            advanceUntilIdle()

            assertEquals(listOf("Ana Ruiz" to emptyList<String>()), auth.profileWrites)
            assertEquals("Ana Ruiz", viewModel.ui.value.fullName)
            assertEquals(ParentProfileSection.None, viewModel.ui.value.screen)
            assertTrue(viewModel.ui.value.saved)
        }

    @Test
    fun `a blank name never reaches the repository`() = runTest(dispatcher) {
        val auth = FakeAuth()
        val viewModel = viewModel(auth = auth)
        advanceUntilIdle()

        viewModel.openEditProfile()
        viewModel.setEditName("   ")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(auth.profileWrites.isEmpty())
        assertEquals("Escribe tu nombre.", viewModel.ui.value.message)
    }

    @Test
    fun `a rejected write keeps the form open and explains the refusal`() =
        runTest(dispatcher) {
            val auth = FakeAuth(profileUpdateResult = ProfileUpdateResult.Rejected)
            val viewModel = viewModel(auth = auth)
            advanceUntilIdle()

            viewModel.openEditProfile()
            viewModel.setEditName("Ana Ruiz")
            viewModel.saveProfile()
            advanceUntilIdle()

            assertEquals(ParentProfileSection.EditProfile, viewModel.ui.value.screen)
            assertEquals(
                "No se pudo guardar el perfil. Revisa tu cuenta.",
                viewModel.ui.value.message,
            )
        }

    @Test
    fun `a weak password never reaches the repository`() = runTest(dispatcher) {
        val auth = FakeAuth()
        val viewModel = viewModel(auth = auth)
        advanceUntilIdle()

        viewModel.openChangePassword()
        viewModel.setCurrentPassword("vieja123")
        viewModel.setNewPassword("abc")
        viewModel.setConfirmPassword("abc")
        viewModel.savePassword()
        advanceUntilIdle()

        assertTrue(auth.passwordWrites.isEmpty())
        assertNotNull(viewModel.ui.value.message)
    }

    @Test
    fun `a changed password closes the form and says it happened`() =
        runTest(dispatcher) {
            val auth = FakeAuth()
            val viewModel = viewModel(auth = auth)
            advanceUntilIdle()

            viewModel.openChangePassword()
            viewModel.setCurrentPassword("vieja123")
            viewModel.setNewPassword("Nueva1234")
            viewModel.setConfirmPassword("Nueva1234")
            viewModel.savePassword()
            advanceUntilIdle()

            assertEquals(listOf("vieja123" to "Nueva1234"), auth.passwordWrites)
            assertEquals(ParentProfileSection.None, viewModel.ui.value.screen)
            assertEquals("Contraseña actualizada.", viewModel.ui.value.message)
        }

    @Test
    fun `a wrong current password keeps the form open`() = runTest(dispatcher) {
        val auth = FakeAuth(passwordChangeResult = PasswordChangeResult.WrongPassword)
        val viewModel = viewModel(auth = auth)
        advanceUntilIdle()

        viewModel.openChangePassword()
        viewModel.setCurrentPassword("vieja123")
        viewModel.setNewPassword("Nueva1234")
        viewModel.setConfirmPassword("Nueva1234")
        viewModel.savePassword()
        advanceUntilIdle()

        assertEquals(ParentProfileSection.ChangePassword, viewModel.ui.value.screen)
        assertEquals(
            "La contraseña actual no es correcta.",
            viewModel.ui.value.message,
        )
    }

    // ---- helpers ------------------------------------------------------------

    private fun viewModel(
        auth: FakeAuth = FakeAuth(),
        children: ParentChildrenResult = ParentChildrenResult.Loaded(emptyList()),
        compressor: FakeCompressor = FakeCompressor(),
        uploader: FakeUploader = FakeUploader(),
    ) = ParentProfileViewModel(
        auth = auth,
        parents = FakeParents(children),
        compressor = compressor,
        uploader = uploader,
    )

    private fun child(
        studentId: String = "stu-1",
        fullName: String = "Mariana Ruiz",
        firstName: String = "Mariana",
    ) = ParentChild(
        studentId = studentId,
        fullName = fullName,
        firstName = firstName,
        listNumber = 3,
        groupId = "group-1",
        groupLabel = "4° D",
    )

    private class FakeAuth(
        private val profileUpdateResult: ProfileUpdateResult = ProfileUpdateResult.Saved,
        private val passwordChangeResult: PasswordChangeResult = PasswordChangeResult.Changed,
    ) : AuthRepository {

        var user: AppUser? = AppUser(
            uid = "tutor-1",
            email = "familia@correo.mx",
            role = UserRole.TUTOR,
            fullName = "Ana Ruiz",
            schoolId = "school-1",
            active = true,
        )

        val profileWrites = mutableListOf<Pair<String, List<String>>>()
        val passwordWrites = mutableListOf<Pair<String, String>>()
        val photoWrites = mutableListOf<Pair<String, String>>()

        override val currentUserId: String? get() = user?.uid
        override val currentUser: AppUser? get() = user

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
        ): ProfilePhotoResult {
            photoWrites += photoUrl to photoPublicId
            val current = user ?: return ProfilePhotoResult.SignedOut
            user = current.copy(photoUrl = photoUrl, photoPublicId = photoPublicId)
            return ProfilePhotoResult.Saved
        }

        override suspend fun updateProfile(
            fullName: String,
            subjects: List<String>,
        ): ProfileUpdateResult {
            profileWrites += fullName to subjects
            val current = user ?: return ProfileUpdateResult.SignedOut
            user = current.copy(fullName = fullName, subjects = subjects)
            return profileUpdateResult
        }

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult {
            passwordWrites += currentPassword to newPassword
            return passwordChangeResult
        }

        override fun signOut() {
            user = null
        }
    }

    private class FakeParents(
        private val children: ParentChildrenResult,
    ) : ParentRepository {

        override suspend fun loadChildren(): ParentChildrenResult = children

        override suspend fun loadMonth(
            child: ParentChild,
            year: Int,
            month: Int,
            subjectId: String?,
        ): ParentMonthResult = error("not used")

        override suspend fun loadHome(subjectId: String?): ParentHomeResult = error("not used")

        override suspend fun loadTasks(child: ParentChild): ParentTasksResult =
            error("not used")

        override suspend fun loadTask(taskId: String): ParentTaskResult = error("not used")

        override suspend fun loadNotices(): ParentNoticesResult = error("not used")

        override suspend fun markNoticeRead(noticeId: String): NoticeReadResult =
            error("not used")

        override suspend fun loadIncidents(): ParentIncidentsResult = error("not used")

        override suspend fun loadIncident(incidentId: String): ParentIncidentResult =
            error("not used")

        override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult =
            error("not used")
    }

    private class FakeCompressor(
        private val bytes: ByteArray? = byteArrayOf(1, 2, 3),
    ) : PhotoCompressor {

        var calls = 0

        override suspend fun compress(uri: android.net.Uri): ByteArray? {
            calls++
            return bytes
        }
    }

    private class FakeUploader : CloudinaryUploader {

        var calls = 0

        override suspend fun uploadImage(
            bytes: ByteArray,
            fileName: String,
            folder: CloudinaryFolder,
            scopeId: String,
        ): CloudinaryResult = CloudinaryResult.NotConfigured

        override suspend fun uploadDocument(
            bytes: ByteArray,
            fileName: String,
            folder: CloudinaryFolder,
            scopeId: String,
            mimeType: String,
        ): CloudinaryResult = error("not used")
    }
}
