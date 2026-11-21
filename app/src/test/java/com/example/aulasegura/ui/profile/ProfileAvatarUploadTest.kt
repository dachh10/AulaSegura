package com.example.aulasegura.ui.profile

import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.MediaMetadata
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.domain.repository.TutorRegistrationInput
import com.example.aulasegura.domain.repository.TutorRegistrationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The avatar of the teacher and the tutor is the same write.
 *
 * The picture travels to Cloudinary and comes back as two coordinates that the
 * profile stores; if either of them is wrong the family sees the old picture
 * forever, or the app points at a file nobody owns. Every failure has to say
 * what happened, because the tutor cannot fix a silent one.
 */
class ProfileAvatarUploadTest {

    @Test
    fun `the avatar is uploaded to the profile folder of the signed-in user`() = runTest {
        val uploader = FakeUploader()
        val auth = FakeAuth()

        uploadProfileAvatar("tutor-1", byteArrayOf(1, 2, 3), uploader, auth)

        assertEquals(1, uploader.calls)
        assertEquals(CloudinaryFolder.PROFILES, uploader.lastFolder)
        assertEquals("tutor-1", uploader.lastScopeId)
        assertEquals("avatar-tutor-1.jpg", uploader.lastFileName)
    }

    @Test
    fun `the uploaded picture is written to the profile with its public id`() = runTest {
        val auth = FakeAuth()

        val result = uploadProfileAvatar(
            "tutor-1",
            byteArrayOf(1, 2, 3),
            FakeUploader(),
            auth,
        )

        assertEquals(
            listOf("https://cdn/avatar.jpg" to "profiles/tutor-1"),
            auth.photoWrites,
        )
        assertEquals(
            AvatarUploadResult.Uploaded("https://cdn/avatar.jpg", "profiles/tutor-1"),
            result,
        )
    }

    @Test
    fun `an unreadable picture never reaches the upload`() = runTest {
        val uploader = FakeUploader()
        val auth = FakeAuth()

        val result = uploadProfileAvatar("tutor-1", null, uploader, auth)

        assertEquals(0, uploader.calls)
        assertTrue(auth.photoWrites.isEmpty())
        assertEquals(
            AvatarUploadResult.Failed("No se pudo leer la foto. Prueba con otra imagen."),
            result,
        )
    }

    @Test
    fun `a network failure explains itself`() = runTest {
        val result = uploadProfileAvatar(
            "tutor-1",
            byteArrayOf(1),
            FakeUploader(result = CloudinaryResult.NetworkError),
            FakeAuth(),
        )

        assertEquals(
            AvatarUploadResult.Failed("Sin conexión. Vuelve a intentarlo cuando tengas red."),
            result,
        )
    }

    @Test
    fun `a refused upload explains itself`() = runTest {
        val result = uploadProfileAvatar(
            "tutor-1",
            byteArrayOf(1),
            FakeUploader(result = CloudinaryResult.Rejected(400, "bad preset")),
            FakeAuth(),
        )

        assertEquals(
            AvatarUploadResult.Failed("Ese archivo no se pudo subir. Prueba con otra foto."),
            result,
        )
    }

    @Test
    fun `a refused profile write explains itself`() = runTest {
        val result = uploadProfileAvatar(
            "tutor-1",
            byteArrayOf(1),
            FakeUploader(),
            FakeAuth(photoResult = ProfilePhotoResult.Rejected),
        )

        assertEquals(
            AvatarUploadResult.Failed("No se pudo guardar la foto en tu perfil."),
            result,
        )
    }

    private class FakeUploader(
        private val result: CloudinaryResult = CloudinaryResult.Success(
            MediaMetadata(
                publicId = "profiles/tutor-1",
                url = "https://cdn/avatar.jpg",
                thumbnailUrl = "https://cdn/avatar-thumb.jpg",
                assetFolder = "aulasegura/profiles/tutor-1",
                type = "upload",
                format = "jpg",
                bytes = 3,
                width = 1080,
                height = 1080,
            ),
        ),
    ) : CloudinaryUploader {

        var calls = 0
        var lastFolder: CloudinaryFolder? = null
        var lastScopeId: String? = null
        var lastFileName: String? = null

        override suspend fun uploadImage(
            bytes: ByteArray,
            fileName: String,
            folder: CloudinaryFolder,
            scopeId: String,
        ): CloudinaryResult {
            calls++
            lastFolder = folder
            lastScopeId = scopeId
            lastFileName = fileName
            return result
        }

        override suspend fun uploadDocument(
            bytes: ByteArray,
            fileName: String,
            folder: CloudinaryFolder,
            scopeId: String,
            mimeType: String,
        ): CloudinaryResult = error("not used")
    }

    private class FakeAuth(
        private val photoResult: ProfilePhotoResult = ProfilePhotoResult.Saved,
    ) : AuthRepository {

        val photoWrites = mutableListOf<Pair<String, String>>()

        override val currentUserId: String? get() = "tutor-1"
        override val currentUser: AppUser? get() = null

        override suspend fun signIn(email: String, password: String): AuthResult =
            error("not used")

        override suspend fun registerTutor(
            input: TutorRegistrationInput,
        ): TutorRegistrationResult = error("not used")

        override suspend fun restoreSession(): AuthResult = error("not used")

        override fun observeAccountActive(): Flow<Boolean> = flowOf(true)

        override suspend fun sendPasswordReset(email: String): PasswordResetResult =
            error("not used")

        override suspend fun updateProfilePhoto(
            photoUrl: String,
            photoPublicId: String,
        ): ProfilePhotoResult {
            photoWrites += photoUrl to photoPublicId
            return photoResult
        }

        override suspend fun updateProfile(
            fullName: String,
            subjects: List<String>,
        ): ProfileUpdateResult = error("not used")

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult = error("not used")

        override fun signOut() = Unit
    }
}
