package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AdminDashboard
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AdminDashboardError
import com.example.aulasegura.domain.repository.AdminDashboardRepository
import com.example.aulasegura.domain.repository.AdminDashboardResult
import com.example.aulasegura.domain.repository.AuthRepository
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * The principal's campus numbers, scoped to the school of the open session.
 *
 * Three collections use Firestore's `count()` aggregate: a refresh downloads no
 * documents and costs one small read per collection. Teachers are the exception:
 * `schoolId + role` is a two-field equality filter that would need a composite
 * index, which this block must not add. Reading the school's `users` once and
 * counting the docente role in memory keeps it to one read with no index change.
 *
 * Every query is a single `schoolId` equality, exactly the clause the deployed
 * security rules check, so no new rule or index is involved.
 */
class FirestoreAdminDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : AdminDashboardRepository {

    override suspend fun getMetrics(): AdminDashboardResult {
        val schoolId = auth.currentUser?.schoolId
            ?: return AdminDashboardResult.Failure(AdminDashboardError.NO_SESSION)

        return try {
            withTimeout(READ_TIMEOUT_MILLIS) {
                val students = countDocuments(COLLECTION_STUDENTS, schoolId)
                val teachers = countTeachers(schoolId)
                val groups = countDocuments(COLLECTION_GROUPS, schoolId)
                val incidents = countDocuments(COLLECTION_INCIDENTS, schoolId)
                AdminDashboardResult.Success(
                    AdminDashboard.loaded(
                        studentCount = students,
                        teacherCount = teachers,
                        groupCount = groups,
                        incidentCount = incidents,
                    ),
                )
            }
        } catch (error: TimeoutCancellationException) {
            AdminDashboardResult.Failure(AdminDashboardError.TIMEOUT)
        } catch (error: FirebaseFirestoreException) {
            AdminDashboardResult.Failure(error.toDomainError())
        } catch (error: Exception) {
            AdminDashboardResult.Failure(AdminDashboardError.UNKNOWN)
        }
    }

    /** One aggregate read; no document bodies ever leave Firestore. */
    private suspend fun countDocuments(collection: String, schoolId: String): Int =
        firestore.collection(collection)
            .whereEqualTo(FIELD_SCHOOL_ID, schoolId)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
            .toInt()

    /**
     * Avoids a composite `schoolId + role` index. One school-filtered snapshot is
     * read and the typed Firestore role is compared to the domain contract value.
     */
    private suspend fun countTeachers(schoolId: String): Int {
        val snapshot = firestore.collection(COLLECTION_USERS)
            .whereEqualTo(FIELD_SCHOOL_ID, schoolId)
            .get()
            .await()
        val teacherRole = UserRole.TEACHER.firestoreValue
        return snapshot.documents.count { it.getString(FIELD_ROLE) == teacherRole }
    }

    private fun FirebaseFirestoreException.toDomainError(): AdminDashboardError = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED,
        -> AdminDashboardError.DENIED

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        -> AdminDashboardError.NETWORK

        else -> AdminDashboardError.UNKNOWN
    }

    private companion object {
        const val COLLECTION_STUDENTS = "students"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_GROUPS = "groups"
        const val COLLECTION_INCIDENTS = "incidents"

        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_ROLE = "role"

        const val READ_TIMEOUT_MILLIS = 12_000L
    }
}
