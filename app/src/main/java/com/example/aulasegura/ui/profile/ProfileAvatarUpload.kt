package com.example.aulasegura.ui.profile

import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ProfilePhotoResult

/** The outcome of the avatar flow, once the picture is already compressed. */
sealed interface AvatarUploadResult {

    /** Cloudinary accepted the picture and the profile points at it. */
    data class Uploaded(val photoUrl: String, val photoPublicId: String) : AvatarUploadResult

    /** The picture did not reach the profile; [message] says why, in Spanish. */
    data class Failed(val message: String) : AvatarUploadResult
}

/**
 * Uploads the avatar of the signed-in user and points the profile at it.
 *
 * The teacher and the tutor own the same avatar: same folder, same file name,
 * same write to `users/{uid}`. Two copies would let one of them drift, and the
 * family would end up with a picture in a different place than the teacher
 * expects.
 *
 * The bytes arrive already compressed: the compression needs an Android
 * `Context`, so it stays in the caller and this function owns everything that
 * does not.
 */
suspend fun uploadProfileAvatar(
    uid: String,
    bytes: ByteArray?,
    uploader: CloudinaryUploader,
    auth: AuthRepository,
): AvatarUploadResult {
    if (bytes == null) {
        return AvatarUploadResult.Failed("No se pudo leer la foto. Prueba con otra imagen.")
    }

    val uploaded = uploader.uploadImage(
        bytes = bytes,
        fileName = "avatar-$uid.jpg",
        folder = CloudinaryFolder.PROFILES,
        scopeId = uid,
    )

    return when (uploaded) {
        is CloudinaryResult.Success -> when (
            auth.updateProfilePhoto(uploaded.asset.url, uploaded.asset.publicId)
        ) {
            ProfilePhotoResult.Saved ->
                AvatarUploadResult.Uploaded(uploaded.asset.url, uploaded.asset.publicId)
            ProfilePhotoResult.SignedOut ->
                AvatarUploadResult.Failed("Tu sesión expiró. Vuelve a iniciar sesión.")
            ProfilePhotoResult.Rejected ->
                AvatarUploadResult.Failed("No se pudo guardar la foto en tu perfil.")
            ProfilePhotoResult.Failed ->
                AvatarUploadResult.Failed("No se pudo guardar la foto. Vuelve a intentarlo.")
        }
        CloudinaryResult.NetworkError ->
            AvatarUploadResult.Failed("Sin conexión. Vuelve a intentarlo cuando tengas red.")
        CloudinaryResult.NotConfigured ->
            AvatarUploadResult.Failed("La subida de fotos no está configurada en esta app.")
        is CloudinaryResult.Rejected ->
            AvatarUploadResult.Failed("Ese archivo no se pudo subir. Prueba con otra foto.")
        is CloudinaryResult.ServerError ->
            AvatarUploadResult.Failed("El servicio de fotos no respondió. Vuelve a intentarlo.")
    }
}
