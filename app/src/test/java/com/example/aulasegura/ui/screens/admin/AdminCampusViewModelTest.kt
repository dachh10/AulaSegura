package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminCampusConfig
import com.example.aulasegura.domain.model.AdminSchool
import com.example.aulasegura.domain.repository.AdminSchoolRepository
import com.example.aulasegura.domain.repository.AdminSchoolResult
import com.example.aulasegura.domain.repository.SchoolNameResolver
import com.example.aulasegura.domain.repository.resolvedSchoolName
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminCampusViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(resolver: SchoolNameResolver) = AdminCampusViewModel(
        schools = object : AdminSchoolRepository {
            override suspend fun getSchool() = AdminSchoolResult.Success(AdminSchool())
            override suspend fun updateCampus(config: AdminCampusConfig) = AdminSchoolResult.Success(Unit)
        },
        nameResolver = resolver,
    )

    @Test
    fun `a school with no document yet opens a blank configurable form`() = runTest(dispatcher) {
        val vm = AdminCampusViewModel(
            schools = object : AdminSchoolRepository {
                override suspend fun getSchool() =
                    AdminSchoolResult.Success(AdminSchool())
                override suspend fun updateCampus(config: AdminCampusConfig) =
                    AdminSchoolResult.Success(Unit)
            },
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.school)
        assertEquals("", state.school?.name)
        assertEquals(null, state.error)
    }

    @Test
    fun `coordinates fill the school name automatically`() = runTest(dispatcher) {
        val vm = viewModel(SchoolNameResolver { _, _ -> "Escuela Benito Juárez" })
        advanceUntilIdle()

        vm.setLatitude("19.2826")
        vm.setLongitude("-99.6557")
        advanceUntilIdle()

        assertEquals("Escuela Benito Juárez", vm.state.value.nameText)
    }

    @Test
    fun `a name typed by the director is never overwritten`() = runTest(dispatcher) {
        val vm = viewModel(SchoolNameResolver { _, _ -> "Nombre del geocoder" })
        advanceUntilIdle()

        vm.setName("Mi escuela")
        vm.setLocation(19.2826, -99.6557)
        advanceUntilIdle()

        assertEquals("Mi escuela", vm.state.value.nameText)
    }

    @Test
    fun `a failed lookup leaves the name untouched`() = runTest(dispatcher) {
        val vm = viewModel(SchoolNameResolver { _, _ -> null })
        advanceUntilIdle()

        vm.setLocation(19.2826, -99.6557)
        advanceUntilIdle()

        assertEquals("", vm.state.value.nameText)
    }

    @Test
    fun `the address line preference is city first`() {
        assertEquals(
            "Aguascalientes",
            resolvedSchoolName("Aguascalientes", "Ags", "Aguascalientes", "C. José Ma. Arteaga"),
        )
        assertNull(resolvedSchoolName(null, null, null, null))
    }
}
