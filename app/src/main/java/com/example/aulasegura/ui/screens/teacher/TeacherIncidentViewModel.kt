package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.local.media.IncidentMediaStore
import com.example.aulasegura.domain.model.Group
import com.example.aulasegura.domain.model.IncidentCategory
import com.example.aulasegura.domain.model.IncidentLocation
import com.example.aulasegura.domain.model.IncidentSaveOutcome
import com.example.aulasegura.domain.model.IncidentSeverity
import com.example.aulasegura.domain.model.MediaUploadPhase
import com.example.aulasegura.domain.model.NewIncident
import com.example.aulasegura.domain.model.NewIncidentMedia
import com.example.aulasegura.domain.model.Student
import com.example.aulasegura.domain.model.SyncPhase
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.IncidentRepository
import com.example.aulasegura.domain.repository.StudentRepository
import com.example.aulasegura.domain.usecase.CreateIncidentUseCase
import java.io.File
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
    private val mediaStore: IncidentMediaStore,
    readMediaLimit: () -> Int,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val selectedId = MutableStateFlow<String?>(null)

    /**
     * No default: the teacher answers the severity question, and the footer stays
     * locked until they do, because that field is what the tutor triages by.
     */
    private val severity = MutableStateFlow<IncidentSeverity?>(null)
    private val category = MutableStateFlow(IncidentCategory.OTRO)
    private val description = MutableStateFlow("")
    private val saving = MutableStateFlow(false)
    private val saved = MutableStateFlow(false)
    private val sync = MutableStateFlow<SyncPhase?>(null)
    private val message = MutableStateFlow<String?>(null)

    /**
     * Read once, when the screen opens: Remote Config is a console value, not
     * something that changes while a teacher is filling a form.
     */
    private val mediaLimit: Int = readMediaLimit()

    /** The photos this form is carrying, in the order the teacher took them. */
    private val captured = MutableStateFlow<List<IncidentEvidenceUi>>(emptyList())

    /** A capture is being compressed, so the footer waits for it. */
    private val preparing = MutableStateFlow(false)

    /** Upload phases, keyed by the file path the media row points at. */
    private val mediaPhases = MutableStateFlow<Map<String, MediaUploadPhase>>(emptyMap())

    /** Captured once, when the form opens: the card and the document share it. */
    private val occurredAt = now()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val roster: StateFlow<List<IncidentStudentUi>> = groups.observeMyGroups()
        .flatMapLatest { myGroups ->
            if (myGroups.isEmpty()) {
                flowOf(emptyList())
            } else {
                val perGroup: List<Flow<Pair<Group, List<Student>>>> =
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

    /** What the form holds while the teacher fills it. */
    private data class Form(
        val severity: IncidentSeverity?,
        val category: IncidentCategory,
        val description: String,
    )

    private val form = combine(severity, category, description) { level, kind, text ->
        Form(level, kind, text)
    }

    private data class Transient(
        val saving: Boolean,
        val saved: Boolean,
        val sync: SyncPhase?,
        val message: String?,
    )

    private val transient = combine(saving, saved, sync, message) { isSaving, isSaved, phase, text ->
        Transient(isSaving, isSaved, phase, text)
    }

    private data class EvidenceState(
        val files: List<IncidentEvidenceUi>,
        val limit: Int,
        val preparing: Boolean,
    )

    /**
     * The tiles the grid paints: what the form holds, with the phase Room reports
     * for the same file once the incident exists.
     *
     * Joined by `localPath` and not by index: the outbox rewrites the rows, so an
     * index would paint the state of one photo on another.
     */
    private val evidence = combine(captured, mediaPhases, preparing) { files, phases, isPreparing ->
        EvidenceState(
            files = files.map { file ->
                file.copy(phase = phases[file.localPath] ?: file.phase)
            },
            limit = mediaLimit,
            preparing = isPreparing,
        )
    }

    val state: StateFlow<TeacherIncidentUi> = combine(
        roster,
        selectedId,
        form,
        transient,
        evidence,
    ) { students, selected, fields, flags, files ->
        TeacherIncidentUi(
            students = students,
            selectedId = selected,
            severity = fields.severity,
            category = fields.category,
            description = fields.description,
            occurredAt = occurredAt,
            saving = flags.saving,
            saved = flags.saved,
            sync = flags.sync,
            message = incidentSyncMessage(flags.sync) ?: flags.message,
            evidence = files.files,
            mediaLimit = files.limit,
            preparing = files.preparing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = TeacherIncidentUi(occurredAt = occurredAt),
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

    fun setCategory(kind: IncidentCategory) {
        if (saved.value) return
        category.value = kind
    }

    fun setDescription(text: String) {
        if (saved.value) return
        description.value = text
        message.value = null
    }

    /**
     * Takes the file the camera just wrote, compresses it and keeps it on disk.
     *
     * Compression happens here and not in the worker (`FASE1.md` 7.2): the teacher
     * sees the photo appear in the grid while the form is still open, and the sync
     * layer only ever reads bytes that are already final.
     *
     * [source] is the camera's own file, deleted by the store once it has been
     * shrunk.
     */
    fun addPhoto(source: File) {
        if (saved.value || preparing.value) {
            source.delete()
            return
        }
        if (captured.value.size >= mediaLimit) {
            source.delete()
            message.value = "Máximo $mediaLimit archivos por incidente."
            return
        }

        preparing.value = true
        viewModelScope.launch {
            val kept = mediaStore.keep(source)
            preparing.value = false

            if (kept == null) {
                message.value = "No se pudo preparar la foto. Vuelve a intentarlo."
                return@launch
            }

            captured.value = captured.value +
                IncidentEvidenceUi(localPath = kept.localPath, thumbnail = kept.thumbnail)
        }
    }

    /** Drops one photo from the form and its file from the phone. */
    fun removeEvidence(localPath: String) {
        if (saved.value) return
        captured.value = captured.value.filterNot { it.localPath == localPath }
        mediaStore.discard(localPath)
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

        val level = severity.value
        if (level == null) {
            message.value = "Elige la gravedad del incidente."
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
                severity = level,
                category = category.value,
                occurredAt = occurredAt,
                location = location,
                media = captured.value.map { NewIncidentMedia(localPath = it.localPath) },
            )

            when (val outcome = createIncident(incident, mediaCount = captured.value.size)) {
                is IncidentSaveOutcome.Queued -> {
                    saved.value = true
                    // Same rule as the roll call: from here the honest answer is
                    // the one Room holds.
                    watchSync(outcome.incidentId)
                    watchMedia(outcome.incidentId)
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
     * Follows each file's upload, not just the incident's.
     *
     * 7.4 is per file: the document is written only after every photo is terminal,
     * so a single "syncing" flag would hide which photo is still on its way.
     */
    private fun watchMedia(incidentId: String) {
        viewModelScope.launch {
            incidents.observeMedia(incidentId).collect { files ->
                mediaPhases.value = files.mapNotNull { file ->
                    file.localPath?.let { path -> path to file.phase }
                }.toMap()
            }
        }
    }

    override fun onCleared() {
        // A form closed without saving leaves photos no draft points at. The
        // saved ones belong to the queued incident and must survive.
        if (!saved.value) {
            captured.value.forEach { mediaStore.discard(it.localPath) }
        }
        super.onCleared()
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
