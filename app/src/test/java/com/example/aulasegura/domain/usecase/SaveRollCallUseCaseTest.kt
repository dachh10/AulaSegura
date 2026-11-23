package com.example.aulasegura.domain.usecase

import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.RollCallDraft
import com.example.aulasegura.domain.model.RollCallEntry
import com.example.aulasegura.domain.model.RollCallStudent
import com.example.aulasegura.domain.model.RollCallSync
import com.example.aulasegura.domain.repository.AttendanceRepository
import com.example.aulasegura.domain.repository.AttendanceStart
import com.example.aulasegura.domain.repository.SaveRollCallOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The completeness rule is the one that protects the tutor's data: the schema
 * has no "unmarked" status, so saving a half-marked session would have to invent
 * absences nobody chose.
 */
class SaveRollCallUseCaseTest {

    private class FakeAttendanceRepository : AttendanceRepository {

        var savedEntries: List<RollCallEntry>? = null
        var outcome: SaveRollCallOutcome = SaveRollCallOutcome.Queued

        override suspend fun startDraft(groupId: String, subjectId: String): AttendanceStart =
            AttendanceStart.NoGroup(groupId)

        override suspend fun save(
            draft: RollCallDraft,
            entries: List<RollCallEntry>,
        ): SaveRollCallOutcome {
            savedEntries = entries
            return outcome
        }

        override fun observeSync(sessionId: String): Flow<RollCallSync> = emptyFlow()
    }

    private val draft = RollCallDraft(
        sessionId = "session-uuid",
        groupId = "grp-3b",
        groupLabel = "3° B",
        schoolId = "sch-benito-juarez",
        teacherId = "uid-docente",
        subject = "Matemáticas",
        date = "2026-11-14",
        startedAt = 1_763_136_000_000L,
    )

    private fun student(id: String, number: Int, mark: AttendanceMark?) =
        RollCallStudent(
            id = id,
            listNumber = number,
            fullName = "Alumno $number",
            initials = "A$number",
            mark = mark,
        )

    @Test
    fun `one unmarked student stops the save before the cache is touched`() = runBlocking {
        val repository = FakeAttendanceRepository()
        val useCase = SaveRollCallUseCase(repository)

        val outcome = useCase(
            draft,
            listOf(
                student("s1", 1, AttendanceMark.PRESENT),
                student("s2", 2, AttendanceMark.PRESENT),
                student("s3", 3, null),
                student("s4", 4, null),
            ),
        )

        assertEquals(SaveRollCallOutcome.Incomplete(unmarked = 2), outcome)
        assertNull(repository.savedEntries)
    }

    @Test
    fun `a complete session is written in roster order with the chosen marks`() = runBlocking {
        val repository = FakeAttendanceRepository()
        val useCase = SaveRollCallUseCase(repository)

        val outcome = useCase(
            draft,
            listOf(
                student("s3", 3, AttendanceMark.ABSENT),
                student("s1", 1, AttendanceMark.PRESENT),
                student("s2", 2, AttendanceMark.LATE),
            ),
        )

        assertEquals(SaveRollCallOutcome.Queued, outcome)
        val entries = requireNotNull(repository.savedEntries)
        assertEquals(listOf("s1", "s2", "s3"), entries.map { it.studentId })
        assertEquals(
            listOf(AttendanceMark.PRESENT, AttendanceMark.LATE, AttendanceMark.ABSENT),
            entries.map { it.mark },
        )
        assertEquals(listOf(1, 2, 3), entries.map { it.listNumber })
    }

    @Test
    fun `an empty roster is not a session`() = runBlocking {
        val repository = FakeAttendanceRepository()
        val useCase = SaveRollCallUseCase(repository)

        assertEquals(SaveRollCallOutcome.EmptyRoster, useCase(draft, emptyList()))
        assertNull(repository.savedEntries)
    }

    @Test
    fun `a local write failure comes back with the group name`() = runBlocking {
        val repository = FakeAttendanceRepository().apply {
            outcome = SaveRollCallOutcome.Failed("grp-3b")
        }
        val useCase = SaveRollCallUseCase(repository)

        val outcome = useCase(draft, listOf(student("s1", 1, AttendanceMark.PRESENT)))

        assertTrue(outcome is SaveRollCallOutcome.Failed)
        assertEquals("grp-3b", (outcome as SaveRollCallOutcome.Failed).groupId)
    }
}
