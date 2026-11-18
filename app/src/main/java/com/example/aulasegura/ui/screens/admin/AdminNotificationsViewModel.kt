package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AppNotification
import com.example.aulasegura.domain.repository.NotificationRepository
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The principal's notification tray.
 *
 * Read-only except for `read`: the rules forbid clients from creating documents.
 * A tap marks the row read and exposes its `deepLink` as state so the screen can
 * open it once; a rotation must not reopen the same screen.
 */
class AdminNotificationsViewModel(
    private val notifications: NotificationRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val filter = MutableStateFlow(ADMIN_NOTIFICATIONS_ALL_FILTER)
    private val loading = MutableStateFlow(true)
    private val notice = MutableStateFlow<String?>(null)
    private val _pendingDeepLink = MutableStateFlow<String?>(null)
    val pendingDeepLink: StateFlow<String?> = _pendingDeepLink

    private val items: StateFlow<List<AppNotification>> = notifications.observeMyNotifications()
        .onEach { loading.value = false }
        .catch {
            loading.value = false
            notice.value = "No se pudieron cargar las notificaciones."
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    val state: StateFlow<AdminNotificationsUi> =
        combine(items, filter, loading, notice) { list, chip, isLoading, message ->
            val groups = adminNotificationGroups(list, now(), zone)
            val allRows = groups.flatMap { it.rows }
            val chips = adminNotificationFiltersFor(allRows)
            val effective = if (chip in chips) chip else ADMIN_NOTIFICATIONS_ALL_FILTER
            val visible = groups.mapNotNull { group ->
                filterAdminNotifications(group.rows, effective)
                    .takeIf { it.isNotEmpty() }
                    ?.let { AdminNotificationGroupUi(label = group.label, rows = it) }
            }

            AdminNotificationsUi(
                groups = visible,
                filters = chips,
                activeFilter = effective,
                unread = unreadAdminNotifications(list),
                loading = isLoading,
                notice = message,
                lastNotificationTime = list.firstOrNull()?.createdAt
                    ?.let { adminRelativeTime(it, now()) },
                pendingDeepLink = _pendingDeepLink.value,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_MILLIS),
            initialValue = AdminNotificationsUi(),
        )

    fun setFilter(value: String) {
        filter.value = value
    }

    fun open(row: AdminNotificationRowUi) {
        viewModelScope.launch { notifications.markRead(row.id) }
        _pendingDeepLink.value = row.deepLink
    }

    fun markAllRead() {
        viewModelScope.launch {
            val result = runCatching { notifications.markAllRead() }
            if (result.isFailure) {
                notice.value = "No se pudieron marcar como leídas."
            }
        }
    }

    fun deepLinkHandled() {
        _pendingDeepLink.value = null
    }

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
