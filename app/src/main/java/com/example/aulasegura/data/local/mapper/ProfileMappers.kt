package com.example.aulasegura.data.local.mapper

import com.example.aulasegura.data.local.entity.ProfileEntity
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole

/**
 * Profile cache mapping.
 *
 * [toCachedEntity] takes the timestamp instead of reading the clock so the
 * mapping stays a pure function and a test can assert on it.
 */
fun AppUser.toCachedEntity(now: Long): ProfileEntity = ProfileEntity(
    uid = uid,
    email = email,
    role = role.firestoreValue,
    fullName = fullName,
    schoolId = schoolId,
    active = active,
    photoUrl = photoUrl,
    subjects = subjects,
    updatedAt = now,
    estado = estado,
    vinculacion = vinculacion,
    hijoSolicitado = hijoSolicitado,
    fechaSolicitud = fechaSolicitud,
)

/**
 * Returns `null` when the cached role is not one the app understands.
 *
 * A profile the app cannot route is worse than no profile: it would land the
 * user on a screen that does not exist. `null` sends the caller back to the
 * network path, which is where a role change is supposed to be picked up.
 */
fun ProfileEntity.toCachedUser(): AppUser? {
    val parsed = UserRole.fromFirestore(role)
    if (parsed == UserRole.UNKNOWN) return null

    return AppUser(
        uid = uid,
        email = email,
        role = parsed,
        fullName = fullName,
        schoolId = schoolId,
        active = active,
        photoUrl = photoUrl,
        subjects = subjects,
        estado = estado,
        vinculacion = vinculacion,
        hijoSolicitado = hijoSolicitado,
        fechaSolicitud = fechaSolicitud,
    )
}
