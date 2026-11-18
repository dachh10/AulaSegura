package com.example.aulasegura.data.local.mapper

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The offline profile cache is the difference between "abre la app en el aula sin
 * señal" and "te manda al login". These tests pin the two ways it can go wrong
 * silently: storing the enum name instead of the Firestore value, and handing the
 * app a role it cannot route.
 */
class ProfileMappersTest {

    private val teacher = AppUser(
        uid = "uid-docente",
        email = "docente@aulasegura.com",
        role = UserRole.TEACHER,
        fullName = "Ricardo Salinas",
        schoolId = "sch-benito-juarez",
        active = true,
        photoUrl = null,
        subjects = listOf("Matemáticas"),
    )

    @Test
    fun `the cached role is the firestore value not the enum name`() {
        val entity = teacher.toCachedEntity(now = 1_700_000_000_000L)

        assertEquals("docente", entity.role)
        assertEquals(1_700_000_000_000L, entity.updatedAt)
        assertEquals(listOf("Matemáticas"), entity.subjects)
    }

    @Test
    fun `a cached teacher round trips back to the domain model`() {
        val cached = teacher.toCachedEntity(now = 0L).toCachedUser()

        assertEquals(teacher, cached)
    }

    @Test
    fun `a role the app cannot route yields no cached user`() {
        val entity = teacher.toCachedEntity(now = 0L).copy(role = "prefecto")

        assertNull(entity.toCachedUser())
    }

    @Test
    fun `the avatar url survives the offline cache without caching its public id`() {
        val withPhoto = teacher.copy(
            photoUrl = "https://res.cloudinary.com/demo/image/upload/aulasegura/profiles/uid-docente.jpg",
            photoPublicId = "aulasegura/profiles/uid-docente",
        )

        val cached = withPhoto.toCachedEntity(now = 0L).toCachedUser()

        assertEquals(withPhoto.photoUrl, cached?.photoUrl)
        // Room has no photoPublicId column: it is only needed to replace the
        // picture in Cloudinary, and a new upload always mints a new id.
        assertNull(cached?.photoPublicId)
    }
}
