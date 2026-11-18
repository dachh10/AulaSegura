package com.example.aulasegura.domain.repository

import com.example.aulasegura.domain.model.AdminCampusConfig
import com.example.aulasegura.domain.model.AdminSchool

/**
 * One-shot read and whitelisted write of the signed-in principal's school.
 *
 * The write path is deliberately narrow: only the campus fields may change. The
 * repository never creates or deletes the school document, and never sends
 * `key`, `createdAt`, `name`, `cycle` or `address` — the first two are immutable
 * under the deployed rule, the rest are not part of this form.
 */
interface AdminSchoolRepository {
    suspend fun getSchool(): AdminSchoolResult<AdminSchool>

    suspend fun updateCampus(config: AdminCampusConfig): AdminSchoolResult<Unit>
}

/** Typed outcome, so the ViewModel never inspects an exception. */
sealed interface AdminSchoolResult<out T> {
    data class Success<T>(val data: T) : AdminSchoolResult<T>
    data class Failure(val reason: AdminSchoolError) : AdminSchoolResult<Nothing>
}

enum class AdminSchoolError {
    NO_SESSION,
    NOT_FOUND,
    NETWORK,
    TIMEOUT,
    DENIED,
    UNKNOWN,
}
