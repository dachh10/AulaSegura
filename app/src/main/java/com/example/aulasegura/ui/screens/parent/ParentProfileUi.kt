package com.example.aulasegura.ui.screens.parent

import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.ParentChild

/**
 * What the tutor's profile renders.
 *
 * Same split as the teacher's profile: the ViewModel resolves the strings and
 * the screen only lays them out. A constant left in the screen is a constant
 * the tutor cannot correct from the app.
 */
data class ParentProfileUi(
    val fullName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    /** The children linked to this account, as the roster knows them. */
    val children: List<ParentChildCardUi> = emptyList(),
    val childrenLoading: Boolean = false,
    /** Why the children are not on screen; `null` when there is nothing to say. */
    val childrenMessage: String? = null,
    val uploading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null,
    val screen: ParentProfileSection = ParentProfileSection.None,
    val editName: String = "",
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

/** The two full-screen account forms. */
sealed interface ParentProfileSection {

    data object None : ParentProfileSection

    data object EditProfile : ParentProfileSection

    data object ChangePassword : ParentProfileSection
}

/** One child of the tutor, as the "MIS HIJOS" card shows it. */
data class ParentChildCardUi(
    val studentId: String,
    val name: String,
    /** The group the child belongs to, as the school writes it: "4° D". */
    val grade: String,
    val initial: String,
)

/** Maps the cached session profile into the screen model. */
internal fun AppUser.toParentProfileUi(): ParentProfileUi = ParentProfileUi(
    fullName = fullName,
    email = email,
    photoUrl = photoUrl,
)

/**
 * The child as the roster knows it.
 *
 * The name is the first name, the same one the home screen greets with, and
 * the second line is the group the school assigned. A list number would be
 * another number the tutor did not ask for.
 */
internal fun ParentChild.toChildCardUi(): ParentChildCardUi = ParentChildCardUi(
    studentId = studentId,
    name = firstName.ifBlank { fullName },
    grade = groupLabel,
    initial = firstName.ifBlank { fullName }.trim().take(1).uppercase().ifBlank { "?" },
)
