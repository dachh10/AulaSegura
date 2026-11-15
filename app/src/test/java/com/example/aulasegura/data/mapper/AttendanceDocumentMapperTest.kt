package com.example.aulasegura.data.mapper

import com.example.aulasegura.data.local.entity.SyncState
import com.example.aulasegura.data.local.mapper.toTimestamp
import com.example.aulasegura.domain.model.RollCallSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The read side of attendance has to survive what Firestore actually sends:
 * numbers as `Long`, `counts` as a map, `closedAt` and `location` missing on
 * sessions created before those fields existed. A mapping bug here does not
 * crash — it paints a session with zero students present.
 */
class AttendanceDocumentMapperTest {

    private val startedAt = 1_762_000_000_000L

    private fun document(overrides: Map<String, Any?> = emptyMap()): Map<String, Any?> =
        mapOf(
            "groupId" to "grp-3b",
            "schoolId" to "sch-benito-juarez",
            "teacherId" to "uid-docente",
            "subject" to "Matemáticas",
            "date" to "2026-11-15",
            "startedAt" to startedAt.toTimestamp(),
            "closedAt" to (startedAt + 600_000).toTimestamp(),
            "status" to "closed",
            "counts" to mapOf("present" to 26L, "absent" to 1L, "late" to 1L, "justified" to 0L),
            "location" to mapOf(
                "lat" to 19.2926,
                "lng" to -99.6557,
                "accuracy" to 13.5,
                "distanceMeters" to 431.16,
                "verified" to true,
            ),
            "createdAt" to startedAt.toTimestamp(),
            "updatedAt" to startedAt.toTimestamp(),
        ) + overrides

    @Test
    fun `reads counts, location and timestamps from a real document`() {
        val session = requireNotNull(attendanceSessionEntityFrom("ses-1", document()))

        assertEquals("ses-1", session.sessionId)
        assertEquals("grp-3b", session.groupId)
        assertEquals(26, session.presentCount)
        assertEquals(1, session.absentCount)
        assertEquals(1, session.lateCount)
        assertEquals(0, session.justifiedCount)
        assertEquals(startedAt, session.startedAt)
        assertEquals(startedAt + 600_000, session.closedAt)
        assertEquals(true, session.locationVerified)
        assertEquals(431.16, session.locationDistanceMeters!!, 0.001)
        // A document that came from the server is already there.
        assertEquals(SyncState.SYNCED, session.syncState)
    }

    @Test
    fun `a session saved without a fix has no location and no closedAt is allowed`() {
        val session = requireNotNull(
            attendanceSessionEntityFrom(
                "ses-2",
                document(mapOf("location" to null, "closedAt" to null)),
            ),
        )

        assertNull(session.locationLat)
        assertNull(session.locationVerified)
        assertNull(session.closedAt)
    }

    @Test
    fun `a document without the fields the cache is keyed by is skipped`() {
        assertNull(attendanceSessionEntityFrom("ses-3", document(mapOf("groupId" to null))))
        assertNull(attendanceSessionEntityFrom("ses-4", document(mapOf("teacherId" to null))))
        assertNull(attendanceSessionEntityFrom("ses-5", document(mapOf("date" to null))))
    }

    @Test
    fun `counts that arrive as ints or missing default to zero instead of breaking`() {
        val session = requireNotNull(
            attendanceSessionEntityFrom(
                "ses-6",
                document(mapOf("counts" to mapOf("present" to 5))),
            ),
        )

        assertEquals(5, session.presentCount)
        assertEquals(0, session.absentCount)
        assertEquals(0, session.justifiedCount)
    }

    @Test
    fun `a record keeps its status and only present or late carry a time`() {
        val present = attendanceRecordEntityFrom(
            sessionId = "ses-1",
            studentId = "stu-1",
            data = mapOf("listNumber" to 1L, "status" to "present", "time" to "08:12"),
        )
        val absent = attendanceRecordEntityFrom(
            sessionId = "ses-1",
            studentId = "stu-2",
            data = mapOf("listNumber" to 2L, "status" to "absent"),
        )

        assertEquals("present", present.status)
        assertEquals("08:12", present.time)
        assertEquals(1, present.listNumber)
        assertEquals("absent", absent.status)
        assertNull(absent.time)
    }

    @Test
    fun `an unreadable status is not turned into a present student`() {
        val record = attendanceRecordEntityFrom(
            sessionId = "ses-1",
            studentId = "stu-3",
            data = mapOf("listNumber" to 3L),
        )

        // Empty, not "present": the UI has to say "sin registrar" rather than
        // invent an attendance that nobody marked.
        assertEquals("", record.status)
    }

    @Test
    fun `every room sync state maps to exactly one domain phase`() {
        assertEquals(RollCallSync.PENDING, SyncState.PENDING.toRollCallSync())
        assertEquals(RollCallSync.SYNCING, SyncState.SYNCING.toRollCallSync())
        assertEquals(RollCallSync.SYNCED, SyncState.SYNCED.toRollCallSync())
        assertEquals(RollCallSync.FAILED, SyncState.FAILED.toRollCallSync())
    }
}
