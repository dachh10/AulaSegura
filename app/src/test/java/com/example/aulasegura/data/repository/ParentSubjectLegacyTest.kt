package com.example.aulasegura.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/** Old sessions must be readable without rewriting a single Firestore document. */
class ParentSubjectLegacyTest {

    @Test
    fun `a canonical id from the document wins`() {
        assertEquals(
            "g3-matematicas-iii",
            parentSubjectId("g3-matematicas-iii", "Matemáticas III", 3),
        )
    }

    @Test
    fun `a legacy label is derived using the group grade`() {
        assertEquals(
            "g3-matematicas-iii",
            parentSubjectId("", "Matemáticas", 3),
        )
        assertEquals(
            "g5-matematicas-v",
            parentSubjectId("", "Matemáticas", 5),
        )
    }

    @Test
    fun `an unknown label keeps an empty id so it only appears under all subjects`() {
        assertEquals("", parentSubjectId("", "Materia desconocida", 3))
        assertEquals("", parentSubjectId("", "", null))
    }
}
