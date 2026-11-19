package com.example.aulasegura.domain.model

import java.text.Collator
import java.util.Locale

/**
 * One roster row while list numbers are being recomputed.
 *
 * [listNumber] is the number currently stored (0 for a student not saved yet); the
 * pure function below replaces it with the alphabetical position. No new Firestore
 * field is introduced: this entry is only an in-memory view of the document.
 */
data class StudentListEntry(
    val id: String,
    val lastName: String,
    val firstName: String,
    val listNumber: Int,
)

/**
 * Spanish collation at PRIMARY strength: accents and case are ignored and `ñ` sorts
 * after `n`. That makes "Pérez" come before "Pineda" by the second letter of the
 * primer apellido, exactly like a hand-written class roster.
 *
 * One instance is shared because [Collator] keeps internal state.
 */
private val spanishCollator: Collator = Collator.getInstance(Locale("es", "ES")).apply {
    strength = Collator.PRIMARY
}

/**
 * Roster order: apellidos, then nombre with the same collation, then the document id
 * so two identical names still get a deterministic numbering.
 */
val studentListComparator: Comparator<StudentListEntry> =
    compareBy<StudentListEntry, String>(spanishCollator) { it.lastName }
        .then(compareBy<StudentListEntry, String>(spanishCollator) { it.firstName })
        .thenBy { it.id }

/** Maps a stored student to the in-memory entry used by the ordering function. */
fun AdminStudent.toStudentListEntry(): StudentListEntry = StudentListEntry(
    id = id,
    lastName = lastName,
    firstName = firstName,
    listNumber = listNumber,
)

/**
 * Sorts the existing roster plus one new student and assigns 1..N by position.
 *
 * Pure: no Firestore, no side effects. The caller writes the returned numbers inside
 * a single transaction, so the roster always reflects alphabetical order instead of
 * appending a new student at the end.
 */
fun orderStudentList(
    existing: List<StudentListEntry>,
    newStudent: StudentListEntry,
): List<StudentListEntry> = (existing + newStudent)
    .sortedWith(studentListComparator)
    .mapIndexed { index, entry -> entry.copy(listNumber = index + 1) }
