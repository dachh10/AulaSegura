package com.example.aulasegura.domain.model

/** A student plus the roster number assigned to them in a bulk renumber. */
data class NumberedStudent(
    val studentId: String,
    val listNumber: Int,
)

val MAX_ROSTER_SIZE = 30

/** Next free roster number, or null when the group is full. */
fun nextListNumber(roster: List<AdminStudent>): Int? {
    val max = roster.maxOfOrNull { it.listNumber } ?: 0
    val next = max + 1
    return next.takeIf { it <= MAX_ROSTER_SIZE }
}

/**
 * Orders the roster by paternal surname, then first name, then full name as a
 * stable tiebreaker, and assigns 1..N. Never exceeds [MAX_ROSTER_SIZE].
 */
fun renumberRoster(roster: List<AdminStudent>): List<NumberedStudent> =
    roster.sortedWith(
        compareBy<AdminStudent>(
            { it.lastName.trim().substringBefore(' ').lowercase() },
            { it.firstName.trim().lowercase() },
            { it.fullName.trim().lowercase() },
            { it.id },
        ),
    )
        .take(MAX_ROSTER_SIZE)
        .mapIndexed { index, student -> NumberedStudent(student.id, index + 1) }
