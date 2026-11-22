package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole

enum class TeacherRequestFilter(val label: String) {
    PENDIENTES("Pendientes"),
    ACEPTADAS("Aceptadas"),
    RECHAZADAS("Rechazadas"),
}

/** Pure queue rules: only verified pending teachers, newest request first. */
fun teacherRequests(
    users: List<AdminUser>,
    filter: TeacherRequestFilter,
): List<AdminUser> = users.asSequence()
    .filter { it.role == UserRole.TEACHER }
    .filter {
        when (filter) {
            TeacherRequestFilter.PENDIENTES ->
                it.emailVerified && it.estado == "pendiente"
            TeacherRequestFilter.ACEPTADAS -> it.estado == "aprobado"
            TeacherRequestFilter.RECHAZADAS -> it.estado == "rechazada" || it.estado == "rechazado"
        }
    }
    .sortedByDescending { it.fechaSolicitud }
    .toList()

fun pendingTeacherCount(users: List<AdminUser>): Int =
    teacherRequests(users, TeacherRequestFilter.PENDIENTES).size

/**
 * Decided requests are read-only: `null` only for pending cards, which are the
 * ones that still offer the accept/reject buttons.
 */
fun teacherRequestStateLabel(filter: TeacherRequestFilter): String? = when (filter) {
    TeacherRequestFilter.PENDIENTES -> null
    TeacherRequestFilter.ACEPTADAS -> "Aceptada"
    TeacherRequestFilter.RECHAZADAS -> "Rechazada"
}

/** Uids for the batch write; excludes unverified and already-decided teachers. */
fun acceptAllTeacherUids(users: List<AdminUser>): List<String> =
    teacherRequests(users, TeacherRequestFilter.PENDIENTES).map { it.uid }

