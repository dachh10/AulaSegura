package com.example.aulasegura.ui.screens.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.JustificationAttachmentDraft
import com.example.aulasegura.domain.model.JustificationDraft
import com.example.aulasegura.domain.model.ParentChild
import com.example.aulasegura.domain.model.ParentMonthAttendance
import com.example.aulasegura.domain.repository.JustificationRepository
import com.example.aulasegura.domain.repository.JustificationResult
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentMonthResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.RosterFailure
import com.example.aulasegura.util.AttendanceTimes
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The tutor's justification form.
 *
 * It points at a real absence, never at "today": [absenceIso] arrives from the
 * attendance detail when the tutor taps one record, and without it the form takes
 * the newest absence the school registered. Both paths read `attendanceSessions`,
 * so the date the principal receives is a date the school itself made.
 */
class ParentJustifyViewModel(
    private val parents: ParentRepository,
    private val justifications: JustificationRepository,
    /** The child the form is about, when the caller already knows it. */
    private val studentId: String? = null,
    /** ISO day of the absence touched in the attendance detail. */
    private val absenceIso: String? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: TimeZone = TimeZone.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<ParentJustifyUiState>(ParentJustifyUiState.Loading)
    val state: StateFlow<ParentJustifyUiState> = _state.asStateFlow()

    private var child: ParentChild? = null
    private var absence: JustifyAbsenceUi? = null
    private var emptyMessage: String? = null
    private var reason = JustificationReason.ILLNESS
    private var details = ""
    private var receipt: JustifyReceiptUi? = null
    private var sending = false
    private var sent = false
    private var error: String? = null

    init {
        load()
    }

    fun refresh() = load()

    fun selectReason(value: JustificationReason) {
        reason = value
        publish()
    }

    fun updateDetails(value: String) {
        details = value.take(DETAILS_LIMIT)
        publish()
    }

    fun attach(fileName: String, mimeType: String, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        receipt = JustifyReceiptUi(
            fileName = fileName.ifBlank { "comprobante.jpg" },
            mimeType = mimeType.ifBlank { "image/jpeg" },
            bytes = bytes,
        )
        error = null
        publish()
    }

    fun removeReceipt() {
        receipt = null
        publish()
    }

    fun send() {
        val target = absence ?: return
        val current = child ?: return
        if (sending || sent) return

        val dateMillis = AttendanceTimes.isoDateToMillis(target.isoDate, zone)
        if (dateMillis == null) {
            error = "No se pudo leer la fecha de la falta."
            publish()
            return
        }

        sending = true
        error = null
        publish()

        viewModelScope.launch {
            val draft = JustificationDraft(
                studentId = current.studentId,
                groupId = current.groupId,
                absenceDateMillis = dateMillis,
                reasonCode = reason.code,
                description = details.trim(),
                attachment = receipt?.let {
                    JustificationAttachmentDraft(
                        fileName = it.fileName,
                        bytes = it.bytes,
                        mimeType = it.mimeType,
                    )
                },
            )

            when (justifications.submit(draft)) {
                JustificationResult.Queued -> sent = true
                JustificationResult.SignedOut -> error = "Vuelve a iniciar sesión."
                JustificationResult.StorageFailed ->
                    error = "No se pudo guardar el comprobante en el teléfono. Intenta de nuevo."
            }

            sending = false
            publish()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ParentJustifyUiState.Loading
            error = null

            when (val result = parents.loadChildren()) {
                is ParentChildrenResult.Loaded -> {
                    val children = result.children
                    if (children.isEmpty()) {
                        _state.value = ParentJustifyUiState.NoChildren
                        return@launch
                    }

                    val target = children.firstOrNull { it.studentId == studentId }
                        ?: children.first()
                    child = target

                    when (val lookup = resolveAbsence(target)) {
                        is AbsenceLookup.Found -> {
                            absence = lookup.absence
                            emptyMessage = null
                            publish()
                        }

                        is AbsenceLookup.Missing -> {
                            absence = null
                            emptyMessage = lookup.message
                            publish()
                        }

                        is AbsenceLookup.Unavailable ->
                            _state.value = ParentJustifyUiState.Failed(
                                attendanceFailureMessage(lookup.reason),
                            )
                    }
                }

                ParentChildrenResult.SignedOut ->
                    _state.value = ParentJustifyUiState.Failed("Vuelve a iniciar sesión.")

                is ParentChildrenResult.Failed ->
                    _state.value = ParentJustifyUiState.Failed(
                        attendanceFailureMessage(result.reason),
                    )
            }
        }
    }

    /**
     * The absence of the route wins when it exists. Without one, the newest
     * absence of this month; if the month has none, the previous one, because a
     * tutor opening the form in November is usually answering an October absence.
     */
    private suspend fun resolveAbsence(target: ParentChild): AbsenceLookup {
        val touched = absenceIso
        if (touched != null) {
            val day = calendarOf(touched) ?: return AbsenceLookup.Missing(NO_ABSENCE)
            val month = when (val loaded = monthOf(target, day)) {
                is MonthLookup.Loaded -> loaded.month
                is MonthLookup.Failed -> return AbsenceLookup.Unavailable(loaded.reason)
            }

            val record = month.days.firstOrNull {
                it.date == touched && it.mark == AttendanceMark.ABSENT
            } ?: return AbsenceLookup.Missing(ROUTE_ABSENCE_GONE)
            return AbsenceLookup.Found(record.toAbsenceUi(zone))
        }

        val current = Calendar.getInstance(zone).apply { timeInMillis = now() }
        val thisMonth = when (val loaded = monthOf(target, current)) {
            is MonthLookup.Loaded -> loaded.month
            is MonthLookup.Failed -> return AbsenceLookup.Unavailable(loaded.reason)
        }
        newestAbsence(thisMonth.days)?.let { return AbsenceLookup.Found(it.toAbsenceUi(zone)) }

        val previous = (current.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val lastMonth = when (val loaded = monthOf(target, previous)) {
            is MonthLookup.Loaded -> loaded.month
            is MonthLookup.Failed -> return AbsenceLookup.Missing(NO_ABSENCE)
        }
        val record = newestAbsence(lastMonth.days)
            ?: return AbsenceLookup.Missing(NO_ABSENCE)
        return AbsenceLookup.Found(record.toAbsenceUi(zone))
    }

    private suspend fun monthOf(target: ParentChild, day: Calendar): MonthLookup =
        when (
            val result = parents.loadMonth(
                target,
                day.get(Calendar.YEAR),
                day.get(Calendar.MONTH) + 1,
            )
        ) {
            is ParentMonthResult.Loaded -> MonthLookup.Loaded(result.month)
            ParentMonthResult.SignedOut -> MonthLookup.Failed(RosterFailure.DENIED)
            is ParentMonthResult.Failed -> MonthLookup.Failed(result.reason)
        }

    private fun calendarOf(iso: String): Calendar? {
        val millis = AttendanceTimes.isoDateToMillis(iso, zone) ?: return null
        return Calendar.getInstance(zone).apply { timeInMillis = millis }
    }

    private fun publish() {
        val target = child ?: return
        _state.value = ParentJustifyUiState.Loaded(
            childName = target.fullName,
            groupLabel = target.groupLabel,
            absence = absence,
            reason = reason,
            details = details,
            receipt = receipt,
            sending = sending,
            sent = sent,
            error = error,
            emptyMessage = emptyMessage,
        )
    }

    private sealed interface AbsenceLookup {

        data class Found(val absence: JustifyAbsenceUi) : AbsenceLookup

        data class Missing(val message: String) : AbsenceLookup

        /** The month could not be read: the screen offers a retry, not a lie. */
        data class Unavailable(val reason: RosterFailure) : AbsenceLookup
    }

    private sealed interface MonthLookup {

        data class Loaded(val month: ParentMonthAttendance) : MonthLookup

        data class Failed(val reason: RosterFailure) : MonthLookup
    }

    private companion object {
        const val DETAILS_LIMIT = 200
        const val NO_ABSENCE = "No hay faltas por justificar en los dos últimos meses."
        const val ROUTE_ABSENCE_GONE = "Esa falta ya no está registrada en la asistencia."
    }
}
