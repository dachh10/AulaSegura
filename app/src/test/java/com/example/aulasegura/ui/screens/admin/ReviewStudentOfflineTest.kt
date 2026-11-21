package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.data.local.entity.StudentEntity
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.sync.PendingStudentApproval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** Proves the online/offline decision of T4b without Firebase or a device. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewStudentOfflineTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val director = AppUser(
        uid = "directora-1",
        email = "directora@escuela.mx",
        role = UserRole.ADMIN,
        fullName = "Directora",
        schoolId = "sch-1",
        active = true,
    )

    @Test
    fun `with no signal the approval is queued and reported as pending`() = runTest(dispatcher) {
        var queued: Pair<StudentEntity, PendingStudentApproval>? = null
        val repo = TestStudents()
        val vm = ReviewStudentViewModel(
            students = repo,
            auth = TestAuth(),
            currentUser = { director },
            tutorUid = "tutor-42",
            alumno = "Ana Pérez",
            isOnline = { false },
            enqueueApproval = { student, approval -> queued = student to approval },
            now = { 1_700_000_000_000L },
        )
        advanceUntilIdle()

        vm.onFormChange(ReviewStudentForm(grado = "3", grupo = "B"))
        vm.approve()
        advanceUntilIdle()

        assertTrue(vm.state.value.done)
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.message.orEmpty().contains("Se vinculará"))
        assertNotNull(queued)
        assertEquals("tutor-42", queued!!.first.tutorIds.single())
        assertEquals("PENDING", queued!!.first.syncStatus.name)
        assertEquals("tutor-42", queued!!.second.tutorUid)
        assertEquals(0, repo.approveCalls)
    }

    @Test
    fun `with signal the approval goes online and is not queued`() = runTest(dispatcher) {
        var queued = false
        val repo = TestStudents()
        val vm = ReviewStudentViewModel(
            students = repo,
            auth = TestAuth(),
            currentUser = { director },
            tutorUid = "tutor-42",
            alumno = "Ana Pérez",
            isOnline = { true },
            enqueueApproval = { _, _ -> queued = true },
        )
        advanceUntilIdle()

        vm.onFormChange(ReviewStudentForm(grado = "3", grupo = "B"))
        vm.approve()
        advanceUntilIdle()

        assertTrue(vm.state.value.done)
        assertEquals(1, repo.approveCalls)
        assertEquals(false, queued)
    }
}

/**
 * Delegating stub: the ViewModel only calls setTutorLinking on the auth contract
 * (reject), and reads currentUser through the injected lambda.
 */
private open class BaseAuth : AuthRepository by object : AuthRepository {
    override val currentUserId: String? = null
    override var currentUser: com.example.aulasegura.domain.model.AppUser? = null
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

private class TestAuth : BaseAuth()

private open class BaseStudents : AdminStudentRepository by object : AdminStudentRepository {
    override suspend fun getStudents() = emptyList<com.example.aulasegura.domain.model.AdminStudent>()
    override suspend fun createStudent(input: com.example.aulasegura.domain.repository.AdminStudentInput) =
        com.example.aulasegura.domain.repository.CreateStudentResult.Failure(
            com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun setActive(studentId: String, active: Boolean) =
        com.example.aulasegura.domain.repository.AdminStudentResult.Failure(
            com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun approveStudent(input: com.example.aulasegura.domain.repository.ApprovedStudentInput) =
        com.example.aulasegura.domain.repository.ApproveStudentResult.Success("stu-online")
    override suspend fun createLinkingCode(studentId: String) =
        com.example.aulasegura.domain.repository.LinkingCodeResult.Failure(
            com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun getLinkingCode(studentId: String): String? = null
    override suspend fun assignGroup(
        studentIds: List<String>,
        groupId: String,
        grado: Int,
        grupo: String,
    ) = com.example.aulasegura.domain.repository.AdminStudentResult.Success
}

private class TestStudents : BaseStudents() {
    var approveCalls = 0
    override suspend fun approveStudent(input: com.example.aulasegura.domain.repository.ApprovedStudentInput): com.example.aulasegura.domain.repository.ApproveStudentResult {
        approveCalls++
        return com.example.aulasegura.domain.repository.ApproveStudentResult.Success("stu-online")
    }
}
