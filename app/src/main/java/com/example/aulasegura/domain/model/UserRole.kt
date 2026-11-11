package com.example.aulasegura.domain.model

/**
 * Application role.
 *
 * [firestoreValue] is the exact string stored in `users/{uid}.role`. Never
 * compare against the enum name: the contract with Firestore is the lowercase
 * Spanish value, and Firestore security rules depend on it.
 */
enum class UserRole(val firestoreValue: String) {
    TEACHER("docente"),
    ADMIN("directivo"),
    TUTOR("tutor"),
    UNKNOWN("unknown");

    companion object {
        fun fromFirestore(value: String?): UserRole =
            entries.firstOrNull { it.firestoreValue == value } ?: UNKNOWN
    }
}
