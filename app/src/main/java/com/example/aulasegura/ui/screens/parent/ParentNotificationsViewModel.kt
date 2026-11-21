package com.example.aulasegura.ui.screens.parent

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
 * The tutor's notification tray.
 *
 * A tap does two things in order: marks the document read and asks the screen to
 * open the `deepLink`. The second is exposed as state and consumed by the
 * navigation, so a rotation does not reopen the same screen.
 *
 * The repository is the same one the teacher and the principal use: it already
 * filters `notifications` by the signed-in `userId`, so a role-specific copy
 * would only duplicate the query.
 */
class ParentNotificationsViewModel(
    private val notifications: NotificationRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val filter = MutableStateFlow(ParentNotificationFilter.Todas)
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

    val state: StateFlow<ParentNotificationsUi> =
        combine(items, filter, loading, notice) { list, chip, isLoading, message ->
            val groups = parentNotificationGroups(list, now(), zone)
            val allRows = groups.flatMap { it.rows }
            val chips = parentNotificationFiltersFor(allRows)
            val effective = if (chip in chips) chip else ParentNotificationFilter.Todas
            val visible = groups.mapNotNull { group ->
                filterParentNotifications(group.rows, effective)
                    .takeIf { it.isNotEmpty() }
                    ?.let { ParentNotificationGroupUi(label = group.label, rows = it) }
            }

            ParentNotificationsUi(
                groups = visible,
                filters = chips,
                activeFilter = effective,
                unread = unreadParentNotifications(list),
                loading = isLoading,
                notice = message,
                pendingDeepLink = _pendingDeepLink.value,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_MILLIS),
            initialValue = ParentNotificationsUi(),
        )

    fun setFilter(value: ParentNotificationFilter) {
        filter.value = value
    }

    fun open(row: ParentNotificationRowUi) {
        viewModelScope.launch { notifications.markRead(row.id) }
        _pendingDeepLink.value = row.deepLink
    }

    fun markAllRead() {
        viewModelScope.launch {
            notice.value = null
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
