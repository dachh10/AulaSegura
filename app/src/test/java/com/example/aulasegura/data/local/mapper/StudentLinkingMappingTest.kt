package com.example.aulasegura.data.local.mapper

import com.example.aulasegura.data.local.entity.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * A student approved from a QR may reach the roster cache with only the new
 * vocabulary. The cache must understand it without losing the legacy path.
 */
class StudentLinkingMappingTest {

    @Test
    fun `linking keys populate the roster cache`() {
        val entity = studentEntityFrom(
            id = "stu-new",
            data = mapOf(
                "nombre" to "Ana Pérez",
                "firstName" to "Ana",
                "lastName" to "Pérez",
                "tutores" to listOf("tutor-1"),
                "grupo" to "",
                "grado" to "3",
                "creadoPor" to "directora-1",
                "syncStatus" to "PENDIENTE",
                "schoolId" to "",
                "active" to true,
            ),
        )

        assertNotNull(entity)
        assertEquals("Ana Pérez", entity!!.fullName)
        assertEquals(listOf("tutor-1"), entity.tutorIds)
        assertEquals("", entity.grupo)
        assertEquals("3", entity.grado)
        assertEquals("directora-1", entity.creadoPor)
        assertEquals(SyncState.PENDING, entity.syncStatus)
    }

    @Test
    fun `a legacy document stays synced`() {
        val entity = studentEntityFrom(
            id = "stu-old",
            data = mapOf(
                "fullName" to "Luis García",
                "groupId" to "grp-2",
                "schoolId" to "sch-1",
            ),
        )

        assertNotNull(entity)
        assertEquals(SyncState.SYNCED, entity!!.syncStatus)
        assertEquals("grp-2", entity.grupo)
    }

    @Test
    fun `an unknown sync value never crashes the roster`() {
        val entity = studentEntityFrom(
            id = "stu-x",
            data = mapOf(
                "fullName" to "X",
                "groupId" to "",
                "schoolId" to "",
                "syncStatus" to "DESCONOCIDO",
            ),
        )

        assertEquals(SyncState.SYNCED, entity!!.syncStatus)
    }
}
