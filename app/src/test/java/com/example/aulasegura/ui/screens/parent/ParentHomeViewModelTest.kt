package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.model.NotificationType
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.repository.IncidentSeenResult
import com.example.aulasegura.domain.repository.NoticeReadResult
import com.example.aulasegura.domain.repository.NotificationRepository
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentHomeResult
import com.example.aulasegura.domain.repository.ParentIncidentResult
import com.example.aulasegura.domain.repository.ParentIncidentsResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentNoticesResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.ParentTaskResult
import com.example.aulasegura.domain.repository.ParentTasksResult
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
import org.junit.Before
import org.junit.Test

/**
 * The bell on the home screen must count the same rows the tray shows.
 *
 * A hardcoded badge is the same lie as a mock tray: the family sees "3" with an
 * empty inbox and stops trusting the counter.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ParentHomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val now: Long = Calendar.getInstance(utc).apply {
        clear()
        set(2026, Calendar.NOVEMBER, 17, 12, 0)
    }.timeInMillis

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the bell counts only the unread notifications`() = runTest(dispatcher) {
        val viewModel = viewModel(
            notifications = MutableStateFlow(
                listOf(
                    notification("unread-1", read = false),
                    notification("read", read = true),
                    notification("unread-2", read = false),
                ),
            ),
        )
        backgroundScope.launch { viewModel.unreadNotifications.collect { } }
        advanceUntilIdle()

        assertEquals(2, viewModel.unreadNotifications.value)
    }

    @Test
    fun `a failed listener leaves the bell at zero instead of a stale number`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                notifications = flow { throw IllegalStateException("boom") },
            )
            backgroundScope.launch { viewModel.unreadNotifications.collect { } }
            advanceUntilIdle()

            assertEquals(0, viewModel.unreadNotifications.value)
        }

    private fun viewModel(notifications: Flow<List<AppNotification>>): ParentHomeViewModel =
        ParentHomeViewModel(
            selection = TestChildSelection(),
            parents = NoChildrenParents,
            notifications = FakeNotifications(notifications),
            now = { now },
            zone = utc,
        )

    private fun notification(id: String, read: Boolean) = AppNotification(
        id = id,
        title = "Aviso",
        body = "Cuerpo",
        type = NotificationType.AVISO,
        deepLink = "parent/notifications",
        read = read,
        createdAt = now,
    )

    private class FakeNotifications(
        private val source: Flow<List<AppNotification>>,
    ) : NotificationRepository {
        override fun observeMyNotifications(): Flow<List<AppNotification>> = source
        override suspend fun markRead(notificationId: String) = Unit
        override suspend fun markAllRead() = Unit
    }

    /** The badge does not depend on the home payload; this fake keeps it empty. */
    private object NoChildrenParents : ParentRepository {
        override suspend fun loadChildren(): ParentChildrenResult =
            ParentChildrenResult.Loaded(emptyList())

        override suspend fun loadMonth(
            child: ParentChild,
            year: Int,
            month: Int,
        ): ParentMonthResult = error("not used")

        override suspend fun loadHome(): ParentHomeResult =
            ParentHomeResult.Loaded(emptyList())

        override suspend fun loadTasks(child: ParentChild): ParentTasksResult =
            error("not used")

        override suspend fun loadTask(taskId: String): ParentTaskResult =
            error("not used")

        override suspend fun loadNotices(): ParentNoticesResult = error("not used")

        override suspend fun markNoticeRead(noticeId: String): NoticeReadResult =
            error("not used")

        override suspend fun loadIncidents(): ParentIncidentsResult = error("not used")

        override suspend fun loadIncident(incidentId: String): ParentIncidentResult =
            error("not used")

        override suspend fun markIncidentSeen(incidentId: String): IncidentSeenResult =
            error("not used")
    }
}
