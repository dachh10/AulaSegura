package com.example.aulasegura.ui.screens.teacher

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.PhotoCompressor
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.GroupRepository
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.ui.profile.AvatarUploadResult
import com.example.aulasegura.ui.profile.passwordChangeError
import com.example.aulasegura.ui.profile.passwordChangeMessage
import com.example.aulasegura.ui.profile.profileEditError
import com.example.aulasegura.ui.profile.profileUpdateMessage
import com.example.aulasegura.ui.profile.uploadProfileAvatar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Profile, avatar and account settings of the signed-in teacher.
 *
 * Everything starts from `auth.currentUser`: the profile was resolved at sign-in
 * (or restored from Room on a cold start), so the screen can render real data
 * before a single network call. Every write refreshes both the in-memory profile
 * and its cache in the repository, so the rest of the app sees the change without
 * a reload.
 */
class TeacherProfileViewModel(
    private val auth: AuthRepository,
    private val groups: GroupRepository,
    private val compressor: PhotoCompressor,
    private val uploader: CloudinaryUploader,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        auth.currentUser?.toProfileUi() ?: TeacherProfileUi(),
    )
    val ui: StateFlow<TeacherProfileUi> = _ui.asStateFlow()

    init {
        // The Stitch card shows assigned groups. They live in the roster, not in
        // the profile: observing them keeps the chips real when the director changes them.
        viewModelScope.launch {
            groups.observeMyGroups().collect { list ->
                _ui.value = _ui.value.copy(groups = list.map { it.label })
            }
        }
    }

    // ---- Avatar -------------------------------------------------------------

    /** Compresses, uploads and stores the new avatar, in that order. */
    fun changePhoto(uri: Uri) {
        if (_ui.value.uploading || _ui.value.saving) return

        val uid = auth.currentUser?.uid
        if (uid == null) {
            _ui.value = _ui.value.copy(message = "Tu sesión expiró. Vuelve a iniciar sesión.")
            return
        }

        _ui.value = _ui.value.copy(uploading = true, message = null, saved = false)

        viewModelScope.launch {
            val bytes = compressor.compress(uri)
            when (val result = uploadProfileAvatar(uid, bytes, uploader, auth)) {
                is AvatarUploadResult.Uploaded -> {
                    _ui.value = auth.currentUser?.toProfileUi()?.copy(
                        saved = true,
                        message = "Foto actualizada.",
                    ) ?: _ui.value.copy(message = "Tu sesión expiró.")
                }
                is AvatarUploadResult.Failed -> finishPhoto(message = result.message)
            }
        }
    }

    // ---- Edit profile -------------------------------------------------------

    fun openEditProfile() {
        val current = auth.currentUser
        _ui.value = _ui.value.copy(
            screen = ProfileSection.EditProfile,
            editName = current?.fullName.orEmpty(),
            editSubjects = subjectsInput(current?.subjects.orEmpty()),
            message = null,
        )
    }

    fun setEditName(value: String) {
        _ui.value = _ui.value.copy(editName = value.take(NAME_MAX_CHARS))
    }

    fun setEditSubjects(value: String) {
        _ui.value = _ui.value.copy(editSubjects = value.take(SUBJECTS_INPUT_MAX_CHARS))
    }

    fun saveProfile() {
        if (_ui.value.saving) return
        val name = _ui.value.editName.trim()
        val invalid = profileEditError(name)
        if (invalid != null) {
            _ui.value = _ui.value.copy(message = invalid)
            return
        }

        _ui.value = _ui.value.copy(saving = true, message = null)
        viewModelScope.launch {
            val result = auth.updateProfile(
                fullName = name,
                subjects = parseSubjects(_ui.value.editSubjects),
            )
            when (result) {
                ProfileUpdateResult.Saved -> {
                    _ui.value = auth.currentUser?.toProfileUi()?.copy(
                        screen = ProfileSection.None,
                        saving = false,
                        saved = true,
                        message = profileUpdateMessage(result),
                    ) ?: _ui.value.copy(saving = false, message = "Tu sesión expiró.")
                }
                else -> _ui.value = _ui.value.copy(
                    saving = false,
                    message = profileUpdateMessage(result),
                )
            }
        }
    }

    // ---- Change password ----------------------------------------------------

    fun openChangePassword() {
        _ui.value = _ui.value.copy(
            screen = ProfileSection.ChangePassword,
            currentPassword = "",
            newPassword = "",
            confirmPassword = "",
            message = null,
        )
    }

    fun setCurrentPassword(value: String) {
        _ui.value = _ui.value.copy(currentPassword = value)
    }

    fun setNewPassword(value: String) {
        _ui.value = _ui.value.copy(newPassword = value)
    }

    fun setConfirmPassword(value: String) {
        _ui.value = _ui.value.copy(confirmPassword = value)
    }

    fun savePassword() {
        if (_ui.value.saving) return
        val state = _ui.value
        val invalid = passwordChangeError(
            current = state.currentPassword,
            new = state.newPassword,
            confirmation = state.confirmPassword,
        )
        if (invalid != null) {
            _ui.value = state.copy(message = invalid)
            return
        }

        _ui.value = state.copy(saving = true, message = null)
        viewModelScope.launch {
            val result = auth.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword,
            )
            _ui.value = _ui.value.copy(
                saving = false,
                screen = if (result == PasswordChangeResult.Changed) {
                    ProfileSection.None
                } else {
                    _ui.value.screen
                },
                message = passwordChangeMessage(result),
            )
        }
    }

    // ---- Shared -------------------------------------------------------------

    fun closeSection() {
        _ui.value = _ui.value.copy(screen = ProfileSection.None, message = null)
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    /** Re-reads the session profile after a form wrote it, so the header is fresh. */
    fun refresh() {
        auth.currentUser?.let { _ui.value = it.toProfileUi() }
    }

    private fun finishPhoto(message: String) {
        _ui.value = _ui.value.copy(uploading = false, message = message)
    }

    private companion object {
        const val NAME_MAX_CHARS = 80
        const val SUBJECTS_INPUT_MAX_CHARS = 200
    }
}
