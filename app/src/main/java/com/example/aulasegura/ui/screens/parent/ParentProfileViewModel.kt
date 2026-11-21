package com.example.aulasegura.ui.screens.parent

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.data.remote.cloudinary.CloudinaryFolder
import com.example.aulasegura.data.remote.cloudinary.CloudinaryResult
import com.example.aulasegura.data.remote.cloudinary.CloudinaryUploader
import com.example.aulasegura.data.remote.cloudinary.PhotoCompressor
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.ParentChildrenResult
import com.example.aulasegura.domain.repository.ParentRepository
import com.example.aulasegura.domain.repository.PasswordChangeResult
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
 * Profile, avatar and account settings of the signed-in tutor.
 *
 * The identity comes from `auth.currentUser`: the profile was resolved at
 * sign-in (or restored from Room on a cold start), so the screen renders real
 * data before a single network call. The children come from the same roster
 * the home screen reads, so a child added by the school appears here too.
 *
 * The writes are the teacher's writes: the rules let any active user edit
 * their own `fullName` and their own avatar, and the tutor owns no other
 * field of `users/{uid}`.
 */
class ParentProfileViewModel(
    private val auth: AuthRepository,
    private val parents: ParentRepository,
    private val compressor: PhotoCompressor,
    private val uploader: CloudinaryUploader,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        auth.currentUser?.toParentProfileUi() ?: ParentProfileUi(),
    )
    val ui: StateFlow<ParentProfileUi> = _ui.asStateFlow()

    init {
        loadChildren()
    }

    // ---- Children -----------------------------------------------------------

    /** Reads the children linked to this account, exactly like the home does. */
    fun loadChildren() {
        _ui.value = _ui.value.copy(childrenLoading = true, childrenMessage = null)
        viewModelScope.launch {
            when (val result = parents.loadChildren()) {
                is ParentChildrenResult.Loaded -> _ui.value = _ui.value.copy(
                    children = result.children.map { it.toChildCardUi() },
                    childrenLoading = false,
                    childrenMessage = null,
                )
                ParentChildrenResult.SignedOut -> _ui.value = _ui.value.copy(
                    childrenLoading = false,
                    childrenMessage = "Tu sesión expiró. Vuelve a iniciar sesión.",
                )
                is ParentChildrenResult.Failed -> _ui.value = _ui.value.copy(
                    childrenLoading = false,
                    childrenMessage = homeFailureMessage(result.reason),
                )
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
                    _ui.value = auth.currentUser?.toParentProfileUi()?.copy(
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
            screen = ParentProfileSection.EditProfile,
            editName = current?.fullName.orEmpty(),
            message = null,
        )
    }

    fun setEditName(value: String) {
        _ui.value = _ui.value.copy(editName = value.take(NAME_MAX_CHARS))
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
            // A tutor owns no subjects: the field travels empty so the write
            // cannot touch a list the school filled in.
            val result = auth.updateProfile(
                fullName = name,
                subjects = auth.currentUser?.subjects.orEmpty(),
            )
            when (result) {
                ProfileUpdateResult.Saved -> {
                    _ui.value = auth.currentUser?.toParentProfileUi()?.copy(
                        screen = ParentProfileSection.None,
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
            screen = ParentProfileSection.ChangePassword,
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
                    ParentProfileSection.None
                } else {
                    _ui.value.screen
                },
                message = passwordChangeMessage(result),
            )
        }
    }

    // ---- Shared -------------------------------------------------------------

    fun closeSection() {
        _ui.value = _ui.value.copy(screen = ParentProfileSection.None, message = null)
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    /** Re-reads the session profile after a form wrote it, so the header is fresh. */
    fun refresh() {
        auth.currentUser?.let { _ui.value = it.toParentProfileUi() }
    }

    private fun finishPhoto(message: String) {
        _ui.value = _ui.value.copy(uploading = false, message = message)
    }

    private companion object {
        const val NAME_MAX_CHARS = 80
    }
}
