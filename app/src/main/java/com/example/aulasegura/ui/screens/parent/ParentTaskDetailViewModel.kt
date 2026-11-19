package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.ParentTaskResult
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One task, read by its document id.
 *
 * The group name is resolved against the children the account already has: a task
 * carries `groupIds` and a tutor only ever sees the groups of their own children,
 * so the label is a lookup and never a guess.
 */
class ParentTaskDetailViewModel(
    private val parents: ParentRepository,
    private val taskId: String,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentTaskDetailUiState>(ParentTaskDetailUiState.Loading)
    val state: StateFlow<ParentTaskDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = ParentTaskDetailUiState.Loading

            _state.value = when (val result = parents.loadTask(taskId)) {
                is ParentTaskResult.Loaded -> {
                    val task = result.task
                    result.task.toDetailUi(
                        groupLabel = groupLabel(task.groupIds),
                        now = now(),
                        zone = zone,
                    )
                }

                ParentTaskResult.NotFound -> ParentTaskDetailUiState.NotFound
                ParentTaskResult.SignedOut ->
                    ParentTaskDetailUiState.Failed("Vuelve a iniciar sesión.")

                is ParentTaskResult.Failed ->
                    ParentTaskDetailUiState.Failed(taskDetailFailureMessage(result.reason))
            }
        }
    }

    /**
     * A failed children read costs the label, not the task: the detail is still the
     * document the school published, and the screen simply omits the group line.
     */
    private suspend fun groupLabel(groupIds: List<String>): String =
        when (val result = parents.loadChildren()) {
            is ParentChildrenResult.Loaded -> result.children
                .firstOrNull { it.groupId in groupIds }
                ?.groupLabel
                .orEmpty()

            ParentChildrenResult.SignedOut, is ParentChildrenResult.Failed -> ""
        }
}
