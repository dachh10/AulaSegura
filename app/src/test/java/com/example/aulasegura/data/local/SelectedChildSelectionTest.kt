package com.example.aulasegura.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure rules of the persisted child selection: it belongs to exactly one tutor. */
class SelectedChildSelectionTest {

    private data class Saved(val uid: String, val studentId: String)

    private fun resolve(saved: Saved?, currentUid: String, knownIds: List<String>): String? =
        SelectedChildStore.resolve(saved?.uid, saved?.studentId, currentUid, knownIds)

    @Test
    fun `a saved selection of the current tutor is kept`() {
        val saved = Saved("tutor-1", "stu-b")

        assertEquals("stu-b", resolve(saved, "tutor-1", listOf("stu-a", "stu-b")))
    }

    @Test
    fun `a selection saved by another tutor is ignored`() {
        val saved = Saved("tutor-OLD", "stu-b")

        assertEquals("stu-a", resolve(saved, "tutor-1", listOf("stu-a", "stu-b")))
    }

    @Test
    fun `a selection pointing to a removed student falls back to the first`() {
        val saved = Saved("tutor-1", "stu-deleted")

        assertEquals("stu-a", resolve(saved, "tutor-1", listOf("stu-a", "stu-b")))
    }

    @Test
    fun `no known children means no selection`() {
        assertNull(resolve(null, "tutor-1", emptyList()))
    }
}
