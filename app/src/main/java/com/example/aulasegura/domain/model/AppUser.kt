package com.example.aulasegura.domain.model

/**
 * Authenticated user as the rest of the app understands it.
 *
 * This is a domain model: the UI and the use cases never see Firebase types.
 */
data class AppUser(
    val uid: String,
    val email: String,
    val role: UserRole,
    val fullName: String,
    val schoolId: String,
    val active: Boolean,
    val photoUrl: String? = null,
    /**
     * Full Cloudinary public id of the avatar, including the folder.
     *
     * Not cached in Room: the offline profile only needs [photoUrl] to show the
     * picture, and a replacement generates a new id. The field exists here because
     * `users/{uid}` stores it (`CONTEXT.md` §`users`).
     */
    val photoPublicId: String? = null,
    /** Subjects a teacher teaches. Empty for every other role. */
    val subjects: List<String> = emptyList(),
)
