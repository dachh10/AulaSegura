package com.example.aulasegura.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last known `users/{uid}` profile, so a cold start with no network still knows
 * who is signed in.
 *
 * Without this row the teacher's own app is unusable in the one scenario the
 * phase exists for: open the app in a classroom with no signal and take
 * attendance. Firebase Auth keeps the session locally, but the profile lives in
 * Firestore, and Firestore's persistent cache is deliberately off — so the
 * profile has to be cached here, in the same database as everything else.
 *
 * Exactly one row is kept: the sign-in path replaces it. [role] stores the
 * Firestore value (`docente`, `directivo`, `tutor`), never the enum name.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val role: String,
    val fullName: String,
    val schoolId: String,
    val active: Boolean,
    val photoUrl: String? = null,
    val subjects: List<String> = emptyList(),
    val updatedAt: Long,
    /** Account state and tutor linking, cached so the QR survives an offline start. */
    val estado: String = "",
    val vinculacion: String = "",
    val hijoSolicitado: String = "",
    val fechaSolicitud: Long = 0L,
)
