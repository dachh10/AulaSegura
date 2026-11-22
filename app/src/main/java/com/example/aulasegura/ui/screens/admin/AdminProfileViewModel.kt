package com.example.aulasegura.ui.screens.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.ImageCompressor
import com.example.aulasegura.domain.repository.AdminUserFields
import com.example.aulasegura.domain.repository.AdminUserRepository
import com.example.aulasegura.domain.repository.AdminUserUpdateError
import com.example.aulasegura.domain.repository.AdminUserUpdateResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.PasswordChangeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The directora's own profile: one read on entry, and one photo change pipeline.
 *
 * The pipeline is compress → upload to Cloudinary → write only photo metadata →
 * refresh. A broken URL is never written: [AdminUserRepository.updatePhoto] is
 * called only with the publicId and URL Cloudinary actually returned on success.
 * No Firebase types here — the repository speaks the domain error type and the
 * uploader speaks [CloudinaryResult], so both steps can be reasoned about apart.
 */
class AdminProfileViewModel(
    private val users: AdminUserRepository,
    private val auth: AuthRepository,
    private val compressor: ImageCompressor,
    private val uploader: CloudinaryUploader,
    private val schools: com.example.aulasegura.domain.repository.AdminSchoolRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminProfileUi())
    val state: StateFlow<AdminProfileUi> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Reloads the profile from the server once; keeps the last photo on failure. */
    fun refresh() {
        refresh(keepPhoto = false)
    }

    private fun refresh(keepPhoto: Boolean) {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val schoolName = runCatching { schools?.getSchool() }
                .getOrNull()
                ?.let { it as? com.example.aulasegura.domain.repository.AdminSchoolResult.Success }
                ?.data?.name.orEmpty()
            _state.update { it.copy(schoolName = schoolName) }
            val user = users.getSelf()
            _state.update { current ->
                current.copy(
                    loading = false,
                    uid = user?.uid?.takeIf { it.isNotBlank() } ?: current.uid,
                    firstName = user?.firstName ?: current.firstName,
                    lastName = user?.lastName ?: current.lastName,
                    fullName = user?.fullName ?: current.fullName,
                    email = user?.email ?: current.email,
                    schoolId = user?.schoolId ?: current.schoolId,
                    phone = user?.phone ?: current.phone,
                    // The switch mirrors the server preference, never a local default.
                    notificationsEnabled = user?.notificationsEnabled
                        ?: current.notificationsEnabled,
                    // After a successful write the URL we just stored is the newest
                    // truth; a stale offline fallback must not replace it.
                    photoUrl = if (keepPhoto) current.photoUrl else user?.photoUrl ?: current.photoUrl,
                )
            }
        }
    }

    /**
     * Saves the directora's own editable fields. The derived fullName and the
     * whitelist come from pure helpers; the repository adds updatedAt and drops any
     * identity field before writing `users/{uid}`.
     */
    fun saveProfile(firstName: String, lastName: String, phone: String) {
        val invalid = profileValidationError(firstName, lastName)
        if (invalid != null) {
            _state.update {
                it.copy(edit = AdminProfileEditUi(AdminProfileEditPhase.FAILURE, invalid))
            }
            return
        }

        val uid = _state.value.uid
        if (uid.isBlank()) {
            _state.update {
                it.copy(
                    edit = AdminProfileEditUi(
                        AdminProfileEditPhase.FAILURE,
                        "No hay una sesión activa.",
                    ),
                )
            }
            return
        }

        val changes = buildProfileUpdateMap(firstName, lastName, phone)
        val derivedFullName = changes[AdminUserFields.FULL_NAME] as? String ?: ""
        val derivedPhone = changes[AdminUserFields.PHONE] as? String ?: ""
        _state.update { it.copy(edit = AdminProfileEditUi(AdminProfileEditPhase.SAVING)) }
        viewModelScope.launch {
            when (val result = users.updateProfile(uid, changes)) {
                AdminUserUpdateResult.Success -> _state.update {
                    it.copy(
                        firstName = changes[AdminUserFields.FIRST_NAME] as? String ?: "",
                        lastName = changes[AdminUserFields.LAST_NAME] as? String ?: "",
                        fullName = derivedFullName,
                        phone = derivedPhone,
                        edit = AdminProfileEditUi(
                            AdminProfileEditPhase.SUCCESS,
                            "Perfil actualizado.",
                        ),
                    )
                }

                is AdminUserUpdateResult.Failure -> _state.update {
                    it.copy(
                        edit = AdminProfileEditUi(
                            AdminProfileEditPhase.FAILURE,
                            editFailureMessage(result.reason),
                        ),
                    )
                }
            }
        }
    }

    /** Clears the status banner once the user has acknowledged it. */
    fun editAcknowledged() {
        _state.update { it.copy(edit = AdminProfileEditUi()) }
    }

    /**
     * Persists the notifications preference under `preferences` only. The rules
     * allow that key on a self-edit; identity fields are never part of the payload.
     * A failure keeps the previously loaded value in the switch.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        val uid = _state.value.uid
        if (uid.isBlank()) {
            _state.update {
                it.copy(
                    settings = AdminProfileSettingsUi(
                        AdminSettingsPhase.FAILURE,
                        "No hay una sesión activa.",
                    ),
                )
            }
            return
        }

        _state.update {
            it.copy(settings = AdminProfileSettingsUi(AdminSettingsPhase.SAVING))
        }
        viewModelScope.launch {
            when (val result = users.updateProfile(uid, buildNotificationsUpdateMap(enabled))) {
                AdminUserUpdateResult.Success -> _state.update {
                    it.copy(
                        notificationsEnabled = enabled,
                        settings = AdminProfileSettingsUi(
                            AdminSettingsPhase.SUCCESS,
                            "Preferencia guardada.",
                        ),
                    )
                }

                is AdminUserUpdateResult.Failure -> _state.update {
                    it.copy(
                        settings = AdminProfileSettingsUi(
                            AdminSettingsPhase.FAILURE,
                            notificationsFailureMessage(result.reason),
                        ),
                    )
                }
            }
        }
    }

    /** Clears the settings banner once the user has acknowledged it. */
    fun settingsAcknowledged() {
        _state.update { it.copy(settings = AdminProfileSettingsUi()) }
    }

    /** Reauthenticates with the current password, then writes the new one. */
    fun changePassword(currentPassword: String, newPassword: String) {
        val invalid = changePasswordValidationError(currentPassword, newPassword)
        if (invalid != null) {
            _state.update {
                it.copy(
                    passwordChange = AdminChangePasswordUi(
                        AdminPasswordChangePhase.FAILURE,
                        invalid,
                    ),
                )
            }
            return
        }

        _state.update {
            it.copy(passwordChange = AdminChangePasswordUi(AdminPasswordChangePhase.SAVING))
        }
        viewModelScope.launch {
            when (val result = auth.changePassword(currentPassword, newPassword)) {
                PasswordChangeResult.Success -> _state.update {
                    it.copy(
                        passwordChange = AdminChangePasswordUi(
                            AdminPasswordChangePhase.SUCCESS,
                            "Contraseña actualizada.",
                        ),
                    )
                }

                is PasswordChangeResult.Failure -> _state.update {
                    it.copy(
                        passwordChange = AdminChangePasswordUi(
                            AdminPasswordChangePhase.FAILURE,
                            passwordChangeMessage(result.reason),
                        ),
                    )
                }
            }
        }
    }

    /** Clears the change-password banner once the user has acknowledged it. */
    fun changePasswordAcknowledged() {
        _state.update { it.copy(passwordChange = AdminChangePasswordUi()) }
    }

    /** Called by the screen with the FileProvider URI the camera just filled. */
    fun changePhoto(uri: Uri) {
        val uid = _state.value.uid
        if (uid.isBlank()) {
            failUpload("No hay una sesión activa.")
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(upload = AdminPhotoUploadUi(AdminPhotoUploadPhase.COMPRESSING))
            }

            val bytes = compressor.compress(uri)
            if (bytes == null) {
                failUpload("No se pudo leer la foto. Inténtalo de nuevo.")
                return@launch
            }

            _state.update {
                it.copy(upload = AdminPhotoUploadUi(AdminPhotoUploadPhase.UPLOADING))
            }

            when (val result = uploader.uploadImage(
                bytes = bytes,
                fileName = "perfil_$uid.jpg",
                folder = CloudinaryFolder.PROFILES,
                scopeId = uid,
            )) {
                is CloudinaryResult.Success -> {
                    _state.update {
                        it.copy(upload = AdminPhotoUploadUi(AdminPhotoUploadPhase.SAVING))
                    }
                    val write = users.updatePhoto(
                        uid = uid,
                        photoUrl = result.asset.url,
                        photoPublicId = result.asset.publicId,
                    )
                    if (write is AdminUserUpdateResult.Success) {
                        _state.update {
                            it.copy(
                                photoUrl = result.asset.url,
                                upload = AdminPhotoUploadUi(
                                    phase = AdminPhotoUploadPhase.SUCCESS,
                                    message = uploadPhaseLabel(AdminPhotoUploadPhase.SUCCESS),
                                ),
                            )
                        }
                        refresh(keepPhoto = true)
                    } else {
                        failUpload(writeFailureMessage(write))
                    }
                }

                CloudinaryResult.NotConfigured ->
                    failUpload(
                        "La subida de fotos no está configurada en esta instalación. " +
                            "Configura Cloudinary para poder cambiar la foto.",
                    )

                CloudinaryResult.NetworkError ->
                    failUpload("Sin conexión. La foto no se guardó; inténtalo de nuevo.")

                is CloudinaryResult.Rejected ->
                    failUpload("Cloudinary rechazó la foto (${result.httpCode}).")

                is CloudinaryResult.ServerError ->
                    failUpload("El servicio de imágenes falló. Inténtalo de nuevo.")
            }
        }
    }

    /** A failed step leaves the previous photo untouched and says why. */
    private fun failUpload(message: String) {
        _state.update {
            it.copy(upload = AdminPhotoUploadUi(AdminPhotoUploadPhase.FAILURE, message))
        }
    }

    private fun editFailureMessage(error: AdminUserUpdateError): String = when (error) {
        AdminUserUpdateError.NO_SESSION -> "La sesión terminó. Vuelve a iniciar sesión."
        AdminUserUpdateError.NETWORK,
        AdminUserUpdateError.TIMEOUT,
        -> "Sin conexión. El perfil no se guardó; inténtalo de nuevo."

        AdminUserUpdateError.DENIED -> "No tienes permiso para editar tu perfil."
        AdminUserUpdateError.SUBJECT_TAKEN ->
            "La materia ya tiene docente en ese grupo. No se pudo guardar el perfil."
        AdminUserUpdateError.UNKNOWN -> "No se pudo guardar el perfil."
    }

    private fun writeFailureMessage(result: AdminUserUpdateResult): String = when (result) {
        AdminUserUpdateResult.Success -> ""
        is AdminUserUpdateResult.Failure -> when (result.reason) {
            AdminUserUpdateError.NO_SESSION -> "La sesión terminó. Vuelve a iniciar sesión."
            AdminUserUpdateError.NETWORK,
            AdminUserUpdateError.TIMEOUT,
            -> "Sin conexión. La foto no se guardó; inténtalo de nuevo."

            AdminUserUpdateError.DENIED -> "No tienes permiso para guardar la foto."
            AdminUserUpdateError.SUBJECT_TAKEN ->
                "La materia ya tiene docente en ese grupo. No se pudo guardar la foto."
            AdminUserUpdateError.UNKNOWN -> "No se pudo guardar la foto."
        }
    }
}
