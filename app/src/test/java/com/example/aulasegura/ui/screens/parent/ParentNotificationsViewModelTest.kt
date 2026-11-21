package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import com.example.aulasegura.domain.repository.NotificationRepository
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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
 * The tray shows what the cloud holds, marks read what the tutor opens and asks
 * the navigation to open the document's `deepLink`.
 *
 * A tap that never fires, a badge that counts read rows or a failed listener
 * painted as "no notifications" are all silent lies, so each one is pinned here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentNotificationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = at(2026, Calendar.NOVEMBER, 17, 12, 0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the tray exposes the real rows, the chips and the unread badge`() = runTest(dispatcher) {
        val repository = FakeNotifications(
            MutableStateFlow(
                listOf(
                    notification("hoy-tarea", at(2026, Calendar.NOVEMBER, 17, 10), TAREA),
                    notification("hoy-aviso", at(2026, Calendar.NOVEMBER, 17, 8), AVISO, read = true),
                    notification("vieja", at(2026, Calendar.NOVEMBER, 10, 9), ASISTENCIA),
                ),
            ),
        )
        val viewModel = ParentNotificationsViewModel(repository, now = { now }, zone = utc)
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf("HOY", "ANTERIORES"), state.groups.map { it.label })
        assertEquals(2, state.unread)
        assertFalse(state.loading)
        assertEquals(
            listOf("Todas", "Asistencia", "Tareas", "Avisos"),
            state.filters.map { it.label },
        )
    }

    @Test
    fun `a chip narrows the visible rows and stays selected`() = runTest(dispatcher) {
        val repository = FakeNotifications(
            MutableStateFlow(
                listOf(
                    notification("mensaje", now, MENSAJE),
                    notification("tarea", now, TAREA),
                ),
            ),
        )
        val viewModel = ParentNotificationsViewModel(repository, now = { now }, zone = utc)
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.setFilter(ParentNotificationFilter.Mensajes)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(ParentNotificationFilter.Mensajes, state.activeFilter)
        assertEquals(
            listOf("mensaje"),
            state.groups.flatMap { it.rows }.map { it.id },
        )
    }

    @Test
    fun `opening a row marks it read and hands the deep link to the navigation`() =
        runTest(dispatcher) {
            val repository = FakeNotifications(
                MutableStateFlow(
                    listOf(notification("tarea", now, TAREA, deepLink = "parent/tasks/t-9")),
                ),
            )
            val viewModel = ParentNotificationsViewModel(repository, now = { now }, zone = utc)
            backgroundScope.launch { viewModel.state.collect { } }
            advanceUntilIdle()

            val row = viewModel.state.value.groups.single().rows.single()
            viewModel.open(row)
            advanceUntilIdle()

            assertEquals(listOf("tarea"), repository.markedRead)
            assertEquals("parent/tasks/t-9", viewModel.pendingDeepLink.value)

            viewModel.deepLinkHandled()
            assertNull(viewModel.pendingDeepLink.value)
        }

    @Test
    fun `a refused mark all read is explained instead of silently kept`() = runTest(dispatcher) {
        val repository = FakeNotifications(
            MutableStateFlow(listOf(notification("aviso", now, AVISO))),
            failsMarkAll = true,
        )
        val viewModel = ParentNotificationsViewModel(repository, now = { now }, zone = utc)
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        viewModel.markAllRead()
        advanceUntilIdle()

        assertEquals(1, repository.markAllCalls)
        assertEquals(
            "No se pudieron marcar como leídas.",
            viewModel.state.value.notice,
        )
    }

    @Test
    fun `a failed listener is an error, not an empty tray`() = runTest(dispatcher) {
        val repository = FakeNotifications(flow { throw IllegalStateException("boom") })
        val viewModel = ParentNotificationsViewModel(repository, now = { now }, zone = utc)
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals("No se pudieron cargar las notificaciones.", state.notice)
        assertTrue(state.groups.isEmpty())
    }

    @Test
    fun `an empty tray still offers the Todas chip and no badge`() = runTest(dispatcher) {
        val viewModel = ParentNotificationsViewModel(
            FakeNotifications(),
            now = { now },
            zone = utc,
        )
        backgroundScope.launch { viewModel.state.collect { } }
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf(ParentNotificationFilter.Todas), state.filters)
        assertTrue(state.groups.isEmpty())
        assertEquals(0, state.unread)
        assertFalse(state.loading)
    }

    private class FakeNotifications(
        private val source: Flow<List<AppNotification>> = MutableStateFlow(emptyList()),
        private val failsMarkAll: Boolean = false,
    ) : NotificationRepository {

        val markedRead: MutableList<String> = mutableListOf()
        var markAllCalls: Int = 0

        override fun observeMyNotifications(): Flow<List<AppNotification>> = source

        override suspend fun markRead(notificationId: String) {
            markedRead += notificationId
        }

        override suspend fun markAllRead() {
            markAllCalls += 1
            if (failsMarkAll) error("mark all failed")
        }
    }

    private fun notification(
        id: String,
        createdAt: Long,
        type: NotificationType,
        read: Boolean = false,
        deepLink: String? = "parent/notifications",
    ) = AppNotification(
        id = id,
        title = "Aviso",
        body = "Cuerpo del aviso",
        type = type,
        deepLink = deepLink,
        read = read,
        createdAt = createdAt,
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis

    private companion object {
        val TAREA = NotificationType.TAREA
        val AVISO = NotificationType.AVISO
        val MENSAJE = NotificationType.MENSAJE
        val ASISTENCIA = NotificationType.ASISTENCIA
    }
}
