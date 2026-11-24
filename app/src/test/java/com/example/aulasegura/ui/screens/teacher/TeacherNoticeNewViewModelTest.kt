package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.NewNotice
import com.example.aulasegura.domain.model.PublishOutcome
import com.example.aulasegura.domain.model.PublishedNotice
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.NoticeRepository
import com.example.aulasegura.domain.repository.RosterRefreshResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The notice form must preselect only a group the teacher actually teaches:
 * otherwise publishing a single group fails while "all my groups" works.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TeacherNoticeNewViewModelTest {

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
    fun `a preselected id that is not the teacher's group is ignored`() = runTest(dispatcher) {
        val notices = NoticeStubRepo()
        val viewModel = TeacherNoticeNewViewModel(
            groups = NoticeStubGroups(listOf(group("grp-1a", "1° A"))),
            auth = NoticeStubAuth(),
            notices = notices,
        )
        advanceUntilIdle()

        viewModel.preselectGroup("grp-viejo")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(emptySet<String>(), state.selectedGroupIds)
        assertTrue("no stale id may be published", notices.published.isEmpty())
    }

    @Test
    fun `a real group can be selected and published`() = runTest(dispatcher) {
        val notices = NoticeStubRepo()
        val viewModel = TeacherNoticeNewViewModel(
            groups = NoticeStubGroups(listOf(group("grp-1a", "1° A"))),
            auth = NoticeStubAuth(),
            notices = notices,
        )
        advanceUntilIdle()

        viewModel.preselectGroup("grp-1a")
        viewModel.setTitle("Traer cartulina")
        viewModel.publish()
        advanceUntilIdle()

        assertEquals(1, notices.published.size)
        assertEquals(listOf("grp-1a"), notices.published.single().groupIds)
    }

    private fun group(id: String, label: String) = Group(
        id = id,
        name = label,
        grade = 1,
        section = "A",
        schoolId = "sch-benito-juarez",
        titularTeacherId = "teacher-1",
        teacherIds = listOf("teacher-1"),
        studentCount = 4,
        cycle = "2026-2027",
    )
}

private class NoticeStubGroups(private val groups: List<Group>) : GroupRepository {
    override fun observeMyGroups(): Flow<List<Group>> = flowOf(groups)
    override suspend fun refreshMyGroups(): RosterRefreshResult = RosterRefreshResult.Updated(groups.size)
}

private open class NoticeStubBaseAuth : AuthRepository by object : AuthRepository {
    override val currentUserId: String? = null
    override var currentUser: AppUser? = null
    override suspend fun signIn(email: String, password: String) =
        com.example.aulasegura.domain.repository.AuthResult.Failure(
            com.example.aulasegura.domain.repository.AuthFailure.UNKNOWN,
        )
    override suspend fun registerTutor(input: com.example.aulasegura.domain.repository.TutorRegistrationInput) =
        com.example.aulasegura.domain.repository.TutorRegistrationResult.Failed
    override suspend fun restoreSession() =
        com.example.aulasegura.domain.repository.AuthResult.Failure(
            com.example.aulasegura.domain.repository.AuthFailure.NO_SESSION,
        )
    override fun observeAccountActive(): Flow<Boolean> = flowOf(true)
    override suspend fun sendPasswordReset(email: String) =
        com.example.aulasegura.domain.repository.PasswordResetResult.Unknown
    override suspend fun updateProfilePhoto(photoUrl: String, photoPublicId: String) =
        com.example.aulasegura.domain.repository.ProfilePhotoResult.Failed
    override suspend fun updateProfile(fullName: String, subjects: List<String>) =
        com.example.aulasegura.domain.repository.ProfileUpdateResult.Failed
    override suspend fun changePassword(currentPassword: String, newPassword: String) =
        com.example.aulasegura.domain.repository.PasswordChangeResult.Changed
    override fun signOut() = Unit
}

private class NoticeStubAuth : NoticeStubBaseAuth() {
    override val currentUserId: String? = "teacher-1"
    override var currentUser: AppUser? = AppUser(
        uid = "teacher-1",
        email = "docente@escuela.mx",
        role = UserRole.TEACHER,
        fullName = "Docente",
        schoolId = "sch-benito-juarez",
        active = true,
    )
}

private class NoticeStubRepo : NoticeRepository {
    var published: List<NewNotice> = emptyList()
        private set

    override suspend fun publish(notice: NewNotice): PublishOutcome {
        published = published + notice
        return PublishOutcome.Queued("notice-${published.size}")
    }

    override fun observeMyNotices(): Flow<List<PublishedNotice>> = flowOf(emptyList())
}
