package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminGroup
import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.repository.AdminGroupRepository
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AdminStudentResult
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

@OptIn(ExperimentalCoroutinesApi::class)
class UngroupedStudentsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun student(id: String, groupId: String = "") = AdminStudent(
        id = id,
        firstName = "Alumno", lastName = id, fullName = "Alumno $id", curp = "",
        listNumber = 0, groupId = groupId, schoolId = "sch-1",
        tutorIds = listOf("tutor-$id"), active = true,
    )

    private val groups = listOf(
        AdminGroup(
            id = "grp-3b", name = "3° B", grade = 3, section = "B", schoolId = "sch-1",
            teacherIds = emptyList(), titularTeacherId = "", studentCount = 0, cycle = "2026-2027",
        ),
    )

    @Test
    fun `only unassigned students are selectable and toggleAll selects them`() = runTest(dispatcher) {
        val vm = UngroupedStudentsViewModel(UngroupedTestStudents(listOf(student("a"), student("b", "grp-3b"))), UngroupedTestGroups(groups))
        advanceUntilIdle()

        assertEquals(listOf("a"), vm.state.value.ungrouped.map { it.id })

        vm.toggleAll()
        assertEquals(setOf("a"), vm.state.value.selected)

        vm.toggleAll()
        assertEquals(emptySet<String>(), vm.state.value.selected)
    }

    @Test
    fun `assigning writes the batch for the selected students`() = runTest(dispatcher) {
        val repo = UngroupedTestStudents(listOf(student("a"), student("c")))
        val vm = UngroupedStudentsViewModel(repo, UngroupedTestGroups(groups))
        advanceUntilIdle()

        vm.toggleAll()
        vm.onGradoChange(3)
        vm.onGrupoChange("B")
        vm.assign()
        advanceUntilIdle()

        assertEquals(listOf("a", "c"), repo.assignedIds)
        assertEquals("grp-3b", repo.assignedGroupId)
        assertTrue(vm.state.value.message.orEmpty().contains("2"))
    }
}

private open class UngroupedBaseStudents : AdminStudentRepository by object : AdminStudentRepository {
    override suspend fun getStudents() = emptyList<AdminStudent>()
    override suspend fun createStudent(input: com.example.aulasegura.domain.repository.AdminStudentInput) =
        com.example.aulasegura.domain.repository.CreateStudentResult.Failure(
            com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun setActive(studentId: String, active: Boolean) =
        AdminStudentResult.Failure(com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun approveStudent(input: com.example.aulasegura.domain.repository.ApprovedStudentInput) =
        com.example.aulasegura.domain.repository.ApproveStudentResult.Failure(
            com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun createLinkingCode(studentId: String) =
        com.example.aulasegura.domain.repository.LinkingCodeResult.Failure(
            com.example.aulasegura.domain.repository.AdminStudentError.UNKNOWN)
    override suspend fun getLinkingCode(studentId: String): String? = null
    override suspend fun assignGroup(studentIds: List<String>, groupId: String, grado: Int, grupo: String) =
        AdminStudentResult.Success
}

private class UngroupedTestStudents(private val list: List<AdminStudent>) : UngroupedBaseStudents() {
    var assignedIds: List<String> = emptyList()
    var assignedGroupId: String = ""
    override suspend fun getStudents() = list
    override suspend fun assignGroup(studentIds: List<String>, groupId: String, grado: Int, grupo: String): AdminStudentResult {
        assignedIds = studentIds
        assignedGroupId = groupId
        return AdminStudentResult.Success
    }
}

private open class UngroupedBaseGroups : AdminGroupRepository by object : AdminGroupRepository {
    override suspend fun getGroups() = emptyList<AdminGroup>()
    override suspend fun getGroup(groupId: String): AdminGroup? = null
    override suspend fun createGroup(name: String, grade: Int, section: String) =
        com.example.aulasegura.domain.repository.AdminGroupResult.Success
    override suspend fun updateGroup(groupId: String, name: String, grade: Int, section: String) =
        com.example.aulasegura.domain.repository.AdminGroupResult.Success
    override suspend fun deleteGroup(groupId: String, destinationGroupId: String?) =
        com.example.aulasegura.domain.repository.AdminGroupResult.Success
    override suspend fun assignTeacher(groupId: String, teacherUid: String, assign: Boolean) =
        com.example.aulasegura.domain.repository.AdminGroupResult.Success
    override suspend fun saveSchedule(groupId: String, slots: List<com.example.aulasegura.domain.model.AdminScheduleSlot>) =
        com.example.aulasegura.domain.repository.AdminGroupResult.Success
}

private class UngroupedTestGroups(private val list: List<AdminGroup>) : UngroupedBaseGroups() {
    override suspend fun getGroups() = list
}
