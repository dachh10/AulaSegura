package com.example.aulasegura.ui.screens.teacher

import com.example.aulasegura.domain.model.AttendanceCounts
import com.example.aulasegura.domain.model.AttendanceMark
import com.example.aulasegura.domain.model.AttendanceSessionDetail
import com.example.aulasegura.domain.model.AttendanceSessionRecord
import com.example.aulasegura.domain.model.AttendanceSessionSummary
import com.example.aulasegura.domain.model.RollCallSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The history is grouped by date, and that grouping is the one piece of the
 * screen that can silently be wrong: a saved attendance in the wrong bucket looks
 * like a missing attendance. These tests fix the buckets and the order.
 */
class AttendanceHistoryUiTest {

    private val today = "2026-11-15"

    private fun session(
        id: String,
        date: String,
        sync: RollCallSync = RollCallSync.SYNCED,
        present: Int = 27,
        absent: Int = 1,
        late: Int = 0,
        justified: Int = 0,
    ) = AttendanceSessionSummary(
        sessionId = id,
        groupId = "grp-3b",
        groupLabel = "3° B",
        subject = "Matemáticas",
        date = date,
        startedAt = 1_762_000_000_000L,
        counts = AttendanceCounts(
            present = present,
            absent = absent,
            late = late,
            justified = justified,
        ),
        verified = true,
        sync = sync,
    )

    @Test
    fun `sessions fall into today, this week, last week and older, in that order`() {
        val sections = historySections(
            sessions = listOf(
                session("hoy", "2026-11-15"),
                session("semana", "2026-11-11"),
                session("anterior", "2026-11-06"),
                session("vieja", "2026-10-20"),
            ),
            todayIso = today,
        )

        assertEquals(listOf(TODAY, THIS_WEEK, LAST_WEEK, OLDER), sections.map { it.label })
        assertEquals("hoy", sections.first().sessions.single().id)
    }

    @Test
    fun `a section only exists when it has sessions`() {
        val sections = historySections(
            sessions = listOf(session("hoy", "2026-11-15")),
            todayIso = today,
        )

        assertEquals(1, sections.size)
        assertEquals(TODAY, sections.single().label)
    }

    @Test
    fun `no sessions means no sections`() {
        assertTrue(historySections(emptyList(), today).isEmpty())
    }

    @Test
    fun `a date that cannot be parsed is kept, not hidden`() {
        val sections = historySections(
            sessions = listOf(session("rota", "sin-fecha")),
            todayIso = today,
        )

        assertEquals(OLDER, sections.single().label)
        assertEquals("rota", sections.single().sessions.single().id)
    }

    @Test
    fun `the expanded session carries its records and the others do not`() {
        val expanded = session("abierta", "2026-11-15")
        val detail = AttendanceSessionDetail(
            summary = expanded,
            records = listOf(
                AttendanceSessionRecord(
                    studentId = "stu-1",
                    listNumber = 1,
                    fullName = "Ana Sofía Aguilar",
                    mark = AttendanceMark.PRESENT,
                    time = "08:12",
                ),
                AttendanceSessionRecord(
                    studentId = "stu-2",
                    listNumber = 2,
                    fullName = "Bruno Cárdenas",
                    mark = AttendanceMark.ABSENT,
                    time = null,
                ),
            ),
        )

        val sections = historySections(
            sessions = listOf(expanded, session("cerrada", "2026-11-14")),
            todayIso = today,
            expandedId = "abierta",
            detail = detail,
        )

        val open = sections.flatMap { it.sessions }.first { it.id == "abierta" }
        val closed = sections.flatMap { it.sessions }.first { it.id == "cerrada" }

        assertTrue(open.expanded)
        assertEquals(listOf("01", "02"), open.records.map { it.number })
        assertEquals(AttendanceMark.PRESENT, open.records.first().mark)
        assertEquals("08:12", open.records.first().time)
        assertTrue(closed.records.isEmpty())
    }

    @Test
    fun `the card title joins the group and the subject and skips an empty subject`() {
        assertEquals("3° B · Matemáticas", session("a", today).toUi(false, emptyList(), false).title)

        val withoutSubject = session("b", today).copy(subject = "")
        assertEquals("3° B", withoutSubject.toUi(false, emptyList(), false).title)
    }

    @Test
    fun `a session still in the outbox does not read as synced`() {
        val pending = session("p", today, sync = RollCallSync.PENDING)
            .toUi(false, emptyList(), false)

        assertEquals(RollCallSync.PENDING, pending.sync)
        assertEquals("27", pending.present.toString())
    }
}
