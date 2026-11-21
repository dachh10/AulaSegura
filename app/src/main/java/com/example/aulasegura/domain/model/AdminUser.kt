package com.example.aulasegura.domain.model

/**
 * A user as the principal's user list — and the principal's own profile — sees them.
 *
 * The role is the existing typed [UserRole], not a raw Firestore string. Mapping
 * goes through [roleFromFirestore], which falls back to [UserRole.UNKNOWN] so a
 * user with an unexpected role is still listed instead of crashing the screen.
 *
 * The photo fields default to null: a document written before profile photos
 * existed has neither, and the UI must fall back to initials instead of breaking.
 */
data class AdminUser(
    val uid: String,
    val email: String,
    val role: UserRole,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val phone: String,
    val schoolId: String = "",
    val subjects: List<String>,
    val groupIds: List<String>,
    val studentIds: List<String>,
    val active: Boolean,
    val photoUrl: String? = null,
    val photoPublicId: String? = null,
    /** The directora's own notifications preference, defaulting to on when absent. */
    val notificationsEnabled: Boolean = true,
    /** Written when the account owner confirms their email; gates the request queue. */
    val emailVerified: Boolean = false,
    /** When the teacher sent the access request; orders the director's queue. */
    val fechaSolicitud: Long = 0L,
    /** Account state: pendiente, aprobado or rechazado for teachers. */
    val estado: String = "",
) {
    companion object {
        /**
         * Maps the `role` string stored in Firestore to the typed enum.
         *
         * Unknown values (including a missing value) become [UserRole.UNKNOWN];
         * the list keeps the row and simply cannot place it in a role tab.
         */
        fun roleFromFirestore(value: String?): UserRole = UserRole.fromFirestore(value)

        /** Blank or missing photo fields become null so the UI can show initials. */
        fun photoField(value: String?): String? = value?.takeIf { it.isNotBlank() }
    }
}
