package com.example.aulasegura.ui.screens.teacher

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.ImageCompressor
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Profile and avatar of the signed-in teacher.
 *
 * Everything starts from `auth.currentUser`: the profile was resolved at sign-in
 * (or restored from the Room cache on a cold start), so the screen can render real
 * data before a single network call. The avatar is the one field the teacher can
 * change here; it goes through the same Cloudinary stack as the incident evidence,
 * but straight to Firestore instead of the outbox because it is one small write and
 * the next cold start reads it back from the cached profile.
 */
class TeacherProfileViewModel(
    private val auth: AuthRepository,
    private val compressor: ImageCompressor,
    private val uploader: CloudinaryUploader,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        auth.currentUser?.toProfileUi() ?: TeacherProfileUi(),
    )
    val ui: StateFlow<TeacherProfileUi> = _ui.asStateFlow()

    /** Compresses, uploads and stores the new avatar, in that order. */
    fun changePhoto(uri: Uri) {
        if (_ui.value.uploading) return

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _ui.value = _ui.value.copy(message = "Tu sesión expiró. Vuelve a iniciar sesión.")
            return
        }

        _ui.value = _ui.value.copy(uploading = true, message = null, saved = false)

        viewModelScope.launch {
            val bytes = compressor.compress(uri)
            if (bytes == null) {
                finish(message = "No se pudo leer la foto. Prueba con otra imagen.")
                return@launch
            }

            val uploaded = uploader.uploadImage(
                bytes = bytes,
                fileName = "avatar-$uid.jpg",
                folder = CloudinaryFolder.PROFILES,
                scopeId = uid,
            )

            when (uploaded) {
                is CloudinaryResult.Success -> {
                    when (auth.updateProfilePhoto(uploaded.asset.url, uploaded.asset.publicId)) {
                        ProfilePhotoResult.Saved -> {
                            _ui.value = auth.currentUser?.toProfileUi()?.copy(
                                saved = true,
                                message = "Foto actualizada.",
                            ) ?: _ui.value.copy(message = "Tu sesión expiró.")
                        }
                        ProfilePhotoResult.SignedOut ->
                            finish(message = "Tu sesión expiró. Vuelve a iniciar sesión.")
                        ProfilePhotoResult.Rejected ->
                            finish(message = "No se pudo guardar la foto en tu perfil.")
                        ProfilePhotoResult.Failed ->
                            finish(message = "No se pudo guardar la foto. Vuelve a intentarlo.")
                    }
                }
                CloudinaryResult.NetworkError ->
                    finish(message = "Sin conexión. Vuelve a intentarlo cuando tengas red.")
                CloudinaryResult.NotConfigured ->
                    finish(message = "La subida de fotos no está configurada en esta app.")
                is CloudinaryResult.Rejected ->
                    finish(message = "Ese archivo no se pudo subir. Prueba con otra foto.")
                is CloudinaryResult.ServerError ->
                    finish(message = "El servicio de fotos no respondió. Vuelve a intentarlo.")
            }
        }
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    private fun finish(message: String) {
        _ui.value = _ui.value.copy(uploading = false, message = message)
    }
}
