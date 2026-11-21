package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ParentNoticeItem
import com.example.aulasegura.domain.repository.NoticeReadResult
import com.example.aulasegura.domain.repository.ParentNoticesResult
import com.example.aulasegura.data.local.ChildSelection
import com.example.aulasegura.domain.repository.ParentRepository
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The tutor's notice inbox.
 *
 * Opening a notice writes its read mark and updates the list in place: the
 * mark is optimistic because the tutor did open it, and it is reverted with a
 * message when the write fails, so the badge never claims something the cloud
 * refused.
 */
class ParentNoticesViewModel(
    private val parents: ParentRepository,
    private val selection: ChildSelection,
    private val currentUid: () -> String? = { null },
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentNoticesUiState>(ParentNoticesUiState.Loading)
    val state: StateFlow<ParentNoticesUiState> = _state.asStateFlow()

    private var notices: List<ParentNoticeItem> = emptyList()
    private var filter = ParentNoticeFilter.All
    private var error: String? = null

    init {
        load()
    }

    fun refresh() = load()

    fun selectFilter(value: ParentNoticeFilter) {
        if (value == filter) return
        filter = value
        publish()
    }

    fun markRead(noticeId: String) {
        val target = notices.firstOrNull { it.noticeId == noticeId } ?: return
        if (target.read) return

        notices = notices.markRead(noticeId, read = true)
        error = null
        publish()

        viewModelScope.launch {
            when (val result = parents.markNoticeRead(noticeId)) {
                NoticeReadResult.Marked -> Unit
                NoticeReadResult.SignedOut -> {
                    notices = notices.markRead(noticeId, read = false)
                    error = "Vuelve a iniciar sesión."
                }

                is NoticeReadResult.Failed -> {
                    notices = notices.markRead(noticeId, read = false)
                    error = "No se pudo guardar que ya lo leíste."
                }
            }
            publish()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ParentNoticesUiState.Loading
            error = null

            when (val result = parents.loadNotices()) {
                is ParentNoticesResult.Loaded -> {
                    // Filter to the selected child's group when we can resolve it.
                    val children = when (val kids = parents.loadChildren()) {
                        is com.example.aulasegura.domain.repository.ParentChildrenResult.Loaded -> kids.children
                        else -> emptyList()
                    }
                    val selectedId = selection.ensureValid(currentUid().orEmpty(), children.map { it.studentId })
                    val selectedGroup = children.firstOrNull { it.studentId == selectedId }?.groupId
                    notices = if (selectedGroup.isNullOrBlank()) {
                        result.notices
                    } else {
                        result.notices.filter { it.groupId.isBlank() || it.groupId == selectedGroup }
                    }
                    publish()
                }

                ParentNoticesResult.SignedOut ->
                    _state.value = ParentNoticesUiState.Failed("Vuelve a iniciar sesión.")

                is ParentNoticesResult.Failed ->
                    _state.value = ParentNoticesUiState.Failed(noticesFailureMessage(result.reason))
            }
        }
    }

    private fun publish() {
        val moment = now()
        val unread = notices.count { !it.read }

        _state.value = ParentNoticesUiState.Loaded(
            unreadCount = unread,
            latestLabel = noticesLatestLabel(notices, unread, moment, zone),
            filter = filter,
            sections = noticeSections(filter, notices, moment, zone),
            emptyMessage = noticesEmptyMessage(filter),
            error = error,
        )
    }

    private fun List<ParentNoticeItem>.markRead(noticeId: String, read: Boolean): List<ParentNoticeItem> =
        map { if (it.noticeId == noticeId) it.copy(read = read) else it }
}
