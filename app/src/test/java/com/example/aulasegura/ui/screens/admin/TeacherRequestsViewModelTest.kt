package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AdminUserUpdateResult
import com.example.aulasegura.domain.repository.BatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherRequestsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun teacher(uid: String, estado: String) = AdminUser(
        uid = uid,
        email = "$uid@escuela.mx",
        role = UserRole.TEACHER,
        firstName = uid,
        lastName = "Apellido",
        fullName = "$uid Apellido",
        phone = "",
        schoolId = "sch-1",
        subjects = emptyList(),
        groupIds = emptyList(),
        studentIds = emptyList(),
        active = true,
        emailVerified = true,
        fechaSolicitud = 100L,
        estado = estado,
    )

    private val users = listOf(teacher("a", "pendiente"), teacher("b", "pendiente"))

    @Test
    fun `the queue only shows verified pending teachers`() = runTest(dispatcher) {
        val vm = TeacherRequestsViewModel(TestUsers(users))
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), vm.state.value.visible.map { it.uid })
        assertEquals(2, vm.state.value.pendingCount)
    }

    @Test
    fun `accept all reports how many teachers were approved`() = runTest(dispatcher) {
        val repo = TestUsers(users)
        val vm = TeacherRequestsViewModel(repo)
        advanceUntilIdle()

        vm.acceptAll()
        advanceUntilIdle()

        assertEquals(1, repo.acceptAllCalls)
        assertEquals(true, vm.state.value.message.orEmpty().contains("2"))
    }

    @Test
    fun `deciding one teacher writes the right state`() = runTest(dispatcher) {
        val repo = TestUsers(users)
        val vm = TeacherRequestsViewModel(repo)
        advanceUntilIdle()

        vm.decide("a", approve = false)
        advanceUntilIdle()

        assertEquals("rechazado", repo.lastEstado)
        assertEquals("a", repo.lastUid)
    }
}

/** Delegating stub: only three methods of AdminUserRepository are exercised. */
private open class BaseUsers : AdminUserRepository by object : AdminUserRepository {
    override suspend fun getUsers() = emptyList<AdminUser>()
    override suspend fun getSelf(): AdminUser? = null
    override suspend fun updateProfile(uid: String, changes: Map<String, Any>) =
        AdminUserUpdateResult.Success
    override suspend fun updatePhoto(uid: String, photoUrl: String, photoPublicId: String) =
        AdminUserUpdateResult.Success
    override suspend fun setTeacherStatus(uid: String, estado: String) = AdminUserUpdateResult.Success
    override suspend fun acceptAllPendingTeachers() = BatchResult.Success(0)
    override suspend fun createAccount(
        email: String, password: String, firstName: String, lastName: String,
        role: com.example.aulasegura.domain.repository.AccountRole, phone: String,
        subjects: List<String>, groupIds: List<String>,
    ) = com.example.aulasegura.domain.repository.CreateAccountResult.Success("uid")
}

private class TestUsers(private val list: List<AdminUser>) : BaseUsers() {
    var acceptAllCalls = 0
    var lastUid: String? = null
    var lastEstado: String? = null
    override suspend fun getUsers() = list
    override suspend fun acceptAllPendingTeachers(): BatchResult {
        acceptAllCalls++
        return BatchResult.Success(list.size)
    }
    override suspend fun setTeacherStatus(uid: String, estado: String): AdminUserUpdateResult {
        lastUid = uid
        lastEstado = estado
        return AdminUserUpdateResult.Success
    }
}
