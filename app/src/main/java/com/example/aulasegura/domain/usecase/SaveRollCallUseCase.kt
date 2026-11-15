package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.RollCallDraft
import com.example.aulasegura.domain.model.RollCallEntry
import com.example.aulasegura.domain.model.RollCallStudent
import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.SaveRollCallOutcome

/**
 * Closes a roll call: validates it, turns the rows into records and hands them
 * to the repository.
 *
 * The completeness check lives here and not in the screen because it is a
 * business rule, not a button state: the Firestore schema has exactly four
 * statuses and none of them means "not marked". Saving a session with untouched
 * rows would force us to invent an "absent" that the teacher never chose, and
 * the tutor would see a lie.
 *
 * Entries are sorted by [RollCallStudent.listNumber] so the order written to
 * Firestore does not depend on how the roster flow happened to emit rows.
 */
class SaveRollCallUseCase(
    private val attendance: AttendanceRepository,
) {

    suspend operator fun invoke(
        draft: RollCallDraft,
        students: List<RollCallStudent>,
    ): SaveRollCallOutcome {
        if (students.isEmpty()) return SaveRollCallOutcome.EmptyRoster

        val (marked, unmarked) = students.partition { it.mark != null }
        if (unmarked.isNotEmpty()) {
            return SaveRollCallOutcome.Incomplete(unmarked = unmarked.size)
        }

        val entries = marked
            .sortedBy { it.listNumber }
            .map { student ->
                RollCallEntry(
                    studentId = student.id,
                    listNumber = student.listNumber,
                    mark = requireNotNull(student.mark) { "Partitioned as marked without a mark" },
                )
            }

        return attendance.save(draft, entries)
    }
}
