package com.example.aulasegura.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.RollCallDraft
import com.example.aulasegura.domain.model.RollCallStudent
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.domain.model.SessionLocation
import com.example.aulasegura.domain.model.Student
import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.AttendanceStart
import com.example.aulasegura.domain.repository.SaveRollCallOutcome
import com.example.aulasegura.domain.repository.StudentRepository
import com.example.aulasegura.domain.usecase.SaveRollCallUseCase
import com.example.aulasegura.domain.usecase.StartRollCallUseCase
import com.example.aulasegura.util.AttendanceTimes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for one roll call.
 *
 * The roster is a cache `Flow`, so the list survives a rotation and redraws if
 * the sync worker touches it; the marks live here, in memory, until the teacher
 * saves. Leaving the screen without saving loses the marks on purpose — a
 * session that was never closed should not exist in Firestore.
 *
 * Saving is idempotent twice over: [saving] and [saved] stop a double tap from
 * writing twice in this process, and the client-generated session id makes a
 * retry of the same session overwrite itself instead of creating a second one.
 */
class RollCallViewModel(
    private val groupId: String,
    private val subjectId: String,
    private val startRollCall: StartRollCallUseCase,
    private val saveRollCall: SaveRollCallUseCase,
    students: StudentRepository,
    private val attendance: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RollCallUi())
    val state: StateFlow<RollCallUi> = _state.asStateFlow()

    private var draft: RollCallDraft? = null
    private var roster: List<RollCallStudent> = emptyList()

    /**
     * The fix and verdict the gate produced, if any.
     *
     * Kept here instead of inside [draft] because the gate and the list are
     * different screens: the location arrives after the draft is open, and a
     * late arrival must still reach the save. A `null` value is a real answer —
     * the session is saved unverified, which is exactly what happens when the
     * teacher denied the permission.
     */
    private var sessionLocation: SessionLocation? = null

    private var groupLabel: String = ""
    private var subject: String = ""
    private var dateLabel: String = ""
    private var message: String? = null
    private var saving: Boolean = false
    private var saved: Boolean = false
    private var blocked: Boolean = false
    private var sync: RollCallSync? = null
    private var rosterLoaded: Boolean = false

    init {
        viewModelScope.launch {
            openSession()
            // Collected after the draft so the first emission already has the
            // header; a roster that arrives first would paint an empty subtitle.
            students.observeRoster(groupId).collect { cached -> onRoster(cached) }
        }
    }

    fun mark(studentId: String, mark: AttendanceMark) {
        if (blocked) return
        updateMarks { current -> current.map { if (it.id == studentId) it.copy(mark = mark) else it } }
    }

    /**
     * Hands the gate's result to the session being taken.
     *
     * Called from the screen that owns the GPS read; the ViewModel deliberately
     * does not read location itself, so a ViewModel test never needs Play
     * Services or a permission prompt.
     */
    fun setLocation(location: SessionLocation?) {
        sessionLocation = location
    }

    fun markAllPresent() {
        if (blocked) return
        updateMarks { current -> current.map { it.copy(mark = AttendanceMark.PRESENT) } }
    }

    fun save() {
        if (blocked || saving || saved) return
        val session = draft
        if (session == null) {
            message = NO_SESSION_MESSAGE
            render()
            return
        }
        if (roster.isEmpty()) {
            message = EMPTY_ROSTER_MESSAGE
            render()
            return
        }

        saving = true
        message = null
        render()

        viewModelScope.launch {
            val outcome = try {
                saveRollCall(session.copy(location = sessionLocation), roster)
            } catch (failed: Exception) {
                // Room is the source of truth; a thrown write means nothing was
                // queued, so the honest result is the same as a reported failure.
                SaveRollCallOutcome.Failed(groupId)
            }

            saving = false
            when (outcome) {
                SaveRollCallOutcome.Queued -> {
                    saved = true
                    // The local write is done; from here the honest answer is the
                    // one Room holds, so the strip follows the outbox instead of
                    // repeating a promise.
                    watchSync(session.sessionId)
                }

                is SaveRollCallOutcome.AlreadyTaken -> {
                    blocked = true
                    message = alreadyTakenMessage()
                }

                is SaveRollCallOutcome.Incomplete ->
                    message = "Faltan ${outcome.unmarked} alumnos por marcar."

                SaveRollCallOutcome.EmptyRoster -> message = EMPTY_ROSTER_MESSAGE
                SaveRollCallOutcome.SignedOut -> message = SIGNED_OUT_MESSAGE
                is SaveRollCallOutcome.Failed ->
                    message = "No se pudo guardar el pase de lista del grupo ${outcome.groupId}."
            }
            render()
        }
    }

    /**
     * Follows the saved session while it waits for Firestore.
     *
     * `PENDING` is set before collecting so the strip switches immediately instead
     * of showing the previous message until the first database emission arrives.
     * The flow outlives the network: it reads the cache, so it keeps reporting
     * after the teacher leaves the classroom.
     */
    private fun watchSync(sessionId: String) {
        sync = RollCallSync.PENDING
        viewModelScope.launch {
            attendance.observeSync(sessionId).collect { phase ->
                sync = phase
                render()
            }
        }
    }

    private suspend fun openSession() {
        when (val start = startRollCall(groupId, subjectId)) {
            is AttendanceStart.Ready -> {
                draft = start.draft
                groupLabel = start.draft.groupLabel
                subject = start.draft.subject
                dateLabel = AttendanceTimes.dayLabel(start.draft.startedAt)
            }

            AttendanceStart.SignedOut -> message = SIGNED_OUT_MESSAGE
            is AttendanceStart.NoGroup -> message =
                "No encontramos el grupo ${start.groupId} en este dispositivo. " +
                    "Vuelve a Mis grupos y reintenta."

            is AttendanceStart.InvalidSubject -> message =
                "La materia seleccionada no corresponde a este grupo. " +
                    "Vuelve a elegir la materia desde el horario."

            is AttendanceStart.AlreadyTaken -> {
                groupLabel = start.groupLabel
                subject = start.subject
                blocked = true
                message = alreadyTakenMessage()
            }
        }
        render()
    }

    private fun alreadyTakenMessage(): String {
        val slot = listOf(groupLabel, subject).filter { it.isNotBlank() }.joinToString(" · ")
        return "Ya se pasó lista en $slot hoy. Cada grupo y materia permite un solo " +
            "pase de lista por día. Consultá el historial para ver la asistencia guardada."
    }

    private fun onRoster(cached: List<Student>) {
        rosterLoaded = true
        // Marks survive a cache refresh: the roster flow can emit again while the
        // teacher is marking, and the rows must not lose their answer.
        val marks = roster.associate { it.id to it.mark }
        roster = cached.map { student ->
            RollCallStudent(
                id = student.id,
                listNumber = student.listNumber,
                fullName = student.fullName,
                initials = initialsOf(student.firstName, student.lastName),
                mark = marks[student.id],
            )
        }
        render()
    }

    private fun updateMarks(transform: (List<RollCallStudent>) -> List<RollCallStudent>) {
        if (blocked || saved) return
        roster = transform(roster)
        message = null
        render()
    }

    private fun render() {
        _state.value = RollCallUi(
            subtitle = rollCallSubtitle(groupLabel, subject, dateLabel),
            students = roster.map { it.toRowUi() },
            counts = AttendanceCounts.of(roster.map { it.mark }),
            unmarked = roster.count { it.mark == null },
            loading = !rosterLoaded,
            saving = saving,
            saved = saved,
            blocked = blocked,
            sync = sync,
            message = rollCallSyncMessage(sync) ?: message,
        )
    }

    private companion object {
        const val EMPTY_ROSTER_MESSAGE = "No hay alumnos en este grupo."
        const val SIGNED_OUT_MESSAGE = "Tu sesión expiró. Vuelve a iniciar sesión."
        const val NO_SESSION_MESSAGE = "No se pudo abrir el pase de lista. Vuelve a intentarlo."
    }
}

/** Builds a [RollCallViewModel] for the group being marked. */
class RollCallViewModelFactory(
    private val groupId: String,
    private val subjectId: String,
    private val startRollCall: StartRollCallUseCase,
    private val saveRollCall: SaveRollCallUseCase,
    private val students: StudentRepository,
    private val attendance: AttendanceRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RollCallViewModel(groupId, subjectId, startRollCall, saveRollCall, students, attendance) as T
}
