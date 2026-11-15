package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.IncidentLocation
import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.IncidentRepository
import com.example.aulasegura.domain.repository.StudentRepository
import com.example.aulasegura.domain.usecase.CreateIncidentUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for "Registrar incidente".
 *
 * The roster is the teacher's real groups, cache-first, so the selector works in
 * airplane mode — which is the whole point of the block: an incident reported in
 * the patio reaches Firestore when the signal comes back.
 *
 * Nothing is auto-selected. A wrong student on an incident is a call to the
 * family, so the footer stays disabled until the teacher names the child.
 */
class TeacherIncidentViewModel(
    groups: GroupRepository,
    students: StudentRepository,
    private val createIncident: CreateIncidentUseCase,
    private val incidents: IncidentRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)
    private val severity = MutableStateFlow(IncidentSeverity.MODERADO)
    private val description = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val saved = MutableStateFlow(false)
    private val sync = MutableStateFlow<SyncPhase?>(null)
    private val message = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roster: StateFlow<List<IncidentStudentUi>> = groups.observeMyGroups()
        .flatMapLatest { myGroups ->
            if (myGroups.isEmpty()) {
                flowOf(emptyList())
            } else {
                val perGroup: List<Flow<Pair<com.example.aulasegura.domain.model.Group, List<com.example.aulasegura.domain.model.Student>>>> =
                    myGroups.map { group ->
                        students.observeRoster(group.id).map { roster -> group to roster }
                    }
                combine(perGroup) { parts ->
                    parts.flatMap { (group, roster) ->
                        roster.map { student ->
                            IncidentStudentUi(
                                id = student.id,
                                fullName = student.fullName,
                                initials = initialsOf(student.firstName, student.lastName),
                                groupId = group.id,
                                groupLabel = group.label,
                            )
                        }
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private data class Transient(
        val saving: Boolean,
        val saved: Boolean,
        val sync: SyncPhase?,
        val message: String?,
    )

    private val transient = combine(saving, saved, sync, message) { isSaving, isSaved, phase, text ->
        Transient(isSaving, isSaved, phase, text)
    }

    val state: StateFlow<TeacherIncidentUi> = combine(
        roster,
        selectedId,
        severity,
        description,
        transient,
    ) { students, selected, level, text, flags ->
        TeacherIncidentUi(
            students = students,
            selectedId = selected,
            severity = level,
            description = text,
            loading = students.isEmpty(),
            saving = flags.saving,
            saved = flags.saved,
            sync = flags.sync,
            message = incidentSyncMessage(flags.sync) ?: flags.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TeacherIncidentUi(),
    )

    fun selectStudent(studentId: String) {
        if (saved.value) return
        selectedId.value = studentId
        message.value = null
    }

    fun setSeverity(level: IncidentSeverity) {
        if (saved.value) return
        severity.value = level
    }

    fun setDescription(text: String) {
        if (saved.value) return
        description.value = text
        message.value = null
    }

    /**
     * Queues the incident with the location the teacher placed on the map.
     *
     * The id is generated by the repository; what comes back is the Firestore
     * document id, and that is what the strip follows afterwards.
     */
    fun save(location: IncidentLocation?) {
        if (saving.value || saved.value) return

        val student = roster.value.firstOrNull { it.id == selectedId.value }
        if (student == null) {
            message.value = "Elige el alumno del incidente."
            return
        }

        saving.value = true
        message.value = null

        viewModelScope.launch {
            val incident = NewIncident(
                groupId = student.groupId,
                studentId = student.id,
                studentName = student.fullName,
                title = titleOf(description.value),
                description = description.value.trim(),
                severity = severity.value,
                occurredAt = now(),
                location = location,
            )

            when (val outcome = createIncident(incident)) {
                is IncidentSaveOutcome.Queued -> {
                    saved.value = true
                    // Same rule as the roll call: from here the honest answer is
                    // the one Room holds.
                    watchSync(outcome.incidentId)
                }

                IncidentSaveOutcome.EmptyDescription ->
                    message.value = "Describe qué pasó: el tutor lo va a leer."

                IncidentSaveOutcome.NoStudent ->
                    message.value = "Elige el alumno del incidente."

                IncidentSaveOutcome.SignedOut ->
                    message.value = "Tu sesión expiró. Vuelve a iniciar sesión."

                is IncidentSaveOutcome.NoGroup ->
                    message.value =
                        "No encontramos el grupo ${outcome.groupId} en este dispositivo."

                is IncidentSaveOutcome.Failed ->
                    message.value = "No se pudo guardar el incidente. Vuelve a intentarlo."
            }

            saving.value = false
        }
    }

    private fun watchSync(incidentId: String) {
        sync.value = SyncPhase.PENDING
        viewModelScope.launch {
            incidents.observeSync(incidentId).collect { phase -> sync.value = phase }
        }
    }

    /**
     * The screen has no title field, but the document needs one to be readable in
     * the admin list: the first line of the description is the least invented
     * title available.
     */
    private fun titleOf(text: String): String {
        val firstLine = text.lineSequence().firstOrNull()?.trim().orEmpty()
        return firstLine.take(TITLE_MAX_CHARS).ifBlank { FALLBACK_TITLE }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TITLE_MAX_CHARS = 80
        const val FALLBACK_TITLE = "Incidente sin descripción"
    }
}
