package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentTaskItem
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.data.local.ChildSelection
import com.example.aulasegura.data.local.indexForSelection
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.ParentTasksResult
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The tutor's task list for one child.
 *
 * The tasks are kept once loaded so switching filter is instant; switching child
 * reloads, because the tasks belong to the group and not to the account.
 */
class ParentTasksViewModel(
    private val parents: ParentRepository,
    private val selection: ChildSelection,
    private val currentUid: () -> String? = { null },
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentTasksUiState>(ParentTasksUiState.Loading)
    val state: StateFlow<ParentTasksUiState> = _state.asStateFlow()

    private var children: List<ParentChild> = emptyList()
    private var selected = 0
    private var filter = ParentTaskFilter.Pending
    private var tasks: List<ParentTaskItem> = emptyList()

    init {
        loadChildren()
    }

    fun refresh() {
        if (children.isEmpty()) loadChildren() else loadTasks()
    }

    fun selectFilter(value: ParentTaskFilter) {
        if (value == filter) return
        filter = value
        publish()
    }

    /** One pill and no selector, exactly like the attendance screen. */
    fun cycleChild() {
        if (children.size <= 1) return
        selected = (selected + 1) % children.size
        selection.select(currentUid().orEmpty(), children[selected].studentId)
        loadTasks()
    }

    private fun loadChildren() {
        viewModelScope.launch {
            _state.value = ParentTasksUiState.Loading
            when (val result = parents.loadChildren()) {
                is ParentChildrenResult.Loaded -> {
                    children = result.children
                    if (children.isEmpty()) {
                        _state.value = ParentTasksUiState.NoChildren
                    } else {
                        selected = indexForSelection(selection.ensureValid(currentUid().orEmpty(), children.map { it.studentId }), children.map { it.studentId })
                        loadTasks()
                    }
                }

                ParentChildrenResult.SignedOut ->
                    _state.value = ParentTasksUiState.Failed("Vuelve a iniciar sesión.")

                is ParentChildrenResult.Failed ->
                    _state.value = ParentTasksUiState.Failed(tasksFailureMessage(result.reason))
            }
        }
    }

    private fun loadTasks() {
        val child = children.getOrNull(selected) ?: return
        viewModelScope.launch {
            _state.value = ParentTasksUiState.Loading
            when (val result = parents.loadTasks(child)) {
                is ParentTasksResult.Loaded -> {
                    tasks = result.tasks
                    publish()
                }

                ParentTasksResult.SignedOut ->
                    _state.value = ParentTasksUiState.Failed("Vuelve a iniciar sesión.")

                is ParentTasksResult.Failed ->
                    _state.value = ParentTasksUiState.Failed(tasksFailureMessage(result.reason))
            }
        }
    }

    private fun publish() {
        val child = children.getOrNull(selected) ?: return
        _state.value = ParentTasksUiState.Loaded(
            childName = child.firstName.ifBlank { child.fullName },
            groupLabel = child.groupLabel,
            childCount = children.size,
            filter = filter,
            sections = taskSections(filter, tasks, now(), zone),
            emptyMessage = tasksEmptyMessage(filter),
        )
    }
}
