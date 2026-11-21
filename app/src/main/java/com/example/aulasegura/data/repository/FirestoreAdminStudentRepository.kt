package com.example.aulasegura.data.repository

import com.example.aulasegura.domain.model.AdminStudent
import com.example.aulasegura.domain.model.groupIdFor
import com.example.aulasegura.domain.model.groupNameFor
import com.example.aulasegura.domain.model.isValidSection
import com.example.aulasegura.domain.model.MAX_ROSTER_SIZE
import com.example.aulasegura.domain.model.nextListNumber
import com.example.aulasegura.domain.model.renumberRoster
import com.example.aulasegura.domain.model.StudentListEntry
import com.example.aulasegura.domain.model.orderStudentList
import com.example.aulasegura.domain.model.toStudentListEntry
import com.example.aulasegura.domain.repository.AdminGroupFields
import com.example.aulasegura.domain.repository.AdminStudentError
import com.example.aulasegura.domain.repository.AdminStudentFields
import com.example.aulasegura.domain.repository.AdminStudentInput
import com.example.aulasegura.domain.repository.AdminStudentRepository
import com.example.aulasegura.domain.repository.AdminStudentResult
import com.example.aulasegura.domain.repository.ApproveStudentResult
import com.example.aulasegura.domain.repository.ApprovedStudentInput
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.approvedStudentDocument
import com.example.aulasegura.domain.repository.tutorProfileOnApproval
import com.example.aulasegura.domain.repository.CreateStudentResult
import com.example.aulasegura.domain.repository.LinkingCodeResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

/**
 * The principal's students, straight from Firestore.
 *
 * Reads are one-shot `get(Source.SERVER)` with a timeout. Rules are not a query
 * filter: the `schoolId` equality clause must be in the query itself, and a live
 * listener would burn Spark's read budget while the screen is open.
 *
 * Both writes that change the roster are batches. The teacher's roll call reads
 * `students where groupId == X AND active == true`, and the principal's group card
 * prints `studentCount`; changing one without the other would leave the app lying
 * to both roles. A student is never deleted — `active = false` is the baja.
 */
class FirestoreAdminStudentRepository(
    private val firestore: FirebaseFirestore,
    private val auth: AuthRepository,
) : AdminStudentRepository {

    override suspend fun getStudents(): List<AdminStudent> {
        val schoolId = auth.currentUser?.schoolId ?: return emptyList()

        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(AdminStudentFields.COLLECTION)
                .whereEqualTo(AdminStudentFields.SCHOOL_ID, schoolId)
                .get(Source.SERVER)
                .await()
        }
        return snapshot.documents.map { AdminStudent.fromDocument(it.id, it.safeData()) }
    }

    override suspend fun createStudent(input: AdminStudentInput): CreateStudentResult {
        val schoolId = auth.currentUser?.schoolId
            ?: return CreateStudentResult.Failure(AdminStudentError.NO_SESSION)

        val groupId = input.groupId.trim()
        // The composite index for schoolId + groupId + active covers this read.
        // Rules are not a query filter: without the schoolId clause a cross-school
        // document with the same groupId can fail the whole query with DENIED.
        val rosterQuery = firestore.collection(AdminStudentFields.COLLECTION)
            .whereEqualTo(AdminStudentFields.SCHOOL_ID, schoolId)
            .whereEqualTo(AdminStudentFields.GROUP_ID, groupId)
            .whereEqualTo(AdminStudentFields.ACTIVE, true)
        val groupRef = firestore.collection(AdminStudentFields.GROUP_COLLECTION).document(groupId)

        return try {
            // Firestore's Android Transaction.get() only accepts a DocumentReference,
            // not a Query. Enumerate the matching ids first, then read each document
            // through tx.get(ref) so those reads are part of the transaction.
            val rosterIds = withTimeout(READ_TIMEOUT_MILLIS) {
                rosterQuery.get(Source.SERVER).await().documents.map { it.id }
            }

            // Read and writes share ONE transaction: two concurrent altas cannot
            // both compute the same numbers and leave the roster out of order.
            val createdStudentId = withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.runTransaction { tx ->
                    // READ phase — every read happens before any write.
                    val existing = rosterIds.map { id ->
                        val ref = firestore.collection(AdminStudentFields.COLLECTION).document(id)
                        AdminStudent.fromDocument(id, tx.get(ref).safeData()).toStudentListEntry()
                    }

                    // A provisional unique id keeps the new row distinct for the id
                    // tiebreak; the final id uses the computed number below.
                    val uniqueSuffix = UUID.randomUUID().toString()
                    val provisionalId = "new-$uniqueSuffix"
                    val newEntry = StudentListEntry(
                        id = provisionalId,
                        lastName = input.lastName.trim(),
                        firstName = input.firstName.trim(),
                        listNumber = 0,
                    )
                    val ordered = orderStudentList(existing, newEntry)
                    val listNumber = ordered.first { it.id == provisionalId }.listNumber

                    // A stable id would need an existence probe, and both a direct
                    // get() and a documentId() query on a nonexistent student are
                    // denied by the rules. The UUID suffix avoids any probe and lets
                    // a deactivated student with the same number keep its history.
                    val newRef = firestore.collection(AdminStudentFields.COLLECTION)
                        .document(
                            "${AdminStudentFields.studentId(groupId, listNumber)}-$uniqueSuffix",
                        )
                    val document = AdminStudentFields
                        .newStudentDocument(input, schoolId, listNumber)
                        .toMutableMap()
                    document[AdminStudentFields.CREATED_AT] = FieldValue.serverTimestamp()
                    document[AdminStudentFields.UPDATED_AT] = FieldValue.serverTimestamp()

                    // WRITE phase. Partial updates preserve schoolId on the resulting
                    // resource, which the rules require — never put it in the map.
                    val originalNumbers = existing.associate { it.id to it.listNumber }
                    ordered.forEach { entry ->
                        val previous = originalNumbers[entry.id]
                        if (previous != null && previous != entry.listNumber) {
                            tx.update(
                                firestore.collection(AdminStudentFields.COLLECTION)
                                    .document(entry.id),
                                mapOf(
                                    AdminStudentFields.LIST_NUMBER to entry.listNumber,
                                    AdminStudentFields.UPDATED_AT to FieldValue.serverTimestamp(),
                                ),
                            )
                        }
                    }
                    tx.set(newRef, document)
                    tx.update(
                        groupRef,
                        mapOf(
                            AdminGroupFields.STUDENT_COUNT to FieldValue.increment(1),
                            AdminGroupFields.UPDATED_AT to FieldValue.serverTimestamp(),
                        ),
                    )
                    newRef.id
                }.await()
            }
            CreateStudentResult.Success(createdStudentId)
        } catch (error: Exception) {
            val mapped = mapError(error)
            CreateStudentResult.Failure(mapped.reason, mapped.detail)
        }
    }

    override suspend fun approveStudent(input: ApprovedStudentInput): ApproveStudentResult {
        val schoolId = input.schoolId.ifBlank { auth.currentUser?.schoolId.orEmpty() }
        if (schoolId.isBlank() || input.tutorUid.isBlank() || input.alumno.isBlank()) {
            return ApproveStudentResult.Failure(AdminStudentError.UNKNOWN)
        }
        val studentId = "stu-qr-${UUID.randomUUID()}"
        val document = approvedStudentDocument(input, schoolId).toMutableMap().apply {
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            val batch = firestore.batch()
            // When the director assigned a grade+section at approval time, create
            // the canonical group on demand with a deterministic id. Same classroom
            // always maps to the same document, so it shows up in GRUPOS and a
            // second approval to that classroom only increments its studentCount.
            val gradeNumber = input.grado.toIntOrNull()
            val section = input.grupo.trim().uppercase()
            val hasGroup = gradeNumber != null && gradeNumber in 1..6 &&
                section in listOf("A", "B", "C", "D")
            val resolvedGroupId = if (hasGroup) groupIdFor(gradeNumber, section) else input.groupId
            if (hasGroup) {
                val groupRef = firestore.collection(AdminStudentFields.GROUP_COLLECTION)
                    .document(resolvedGroupId)
                val groupDoc = AdminGroupFields.newGroupDocument(
                    name = groupNameFor(gradeNumber, section),
                    grade = gradeNumber,
                    section = section,
                    schoolId = schoolId,
                ).toMutableMap().apply {
                    // Single write per document: increment lives inside set(merge)
                    // so the counter accumulates instead of resetting to 1.
                    put(AdminGroupFields.STUDENT_COUNT, FieldValue.increment(1L))
                    put(AdminGroupFields.UPDATED_AT, FieldValue.serverTimestamp())
                }
                batch.set(groupRef, groupDoc, com.google.firebase.firestore.SetOptions.merge())
            }
            // Keep the student document's group fields canonical too.
            if (hasGroup) {
                document[AdminStudentFields.GROUP_ID] = resolvedGroupId
                document[AdminStudentFields.GRUPO] = groupNameFor(gradeNumber, section)
                document[AdminStudentFields.GRADO] = gradeNumber.toString()
            }
            if (input.duplicateStudentId.isNullOrBlank()) {
                batch.set(
                    firestore.collection(AdminStudentFields.COLLECTION).document(studentId),
                    document,
                )
            } else {
                // A second parent keeps the existing student and its history:
                // only the tutor lists change.
                val existing = firestore.collection(AdminStudentFields.COLLECTION)
                    .document(input.duplicateStudentId)
                batch.update(
                    existing,
                    mapOf(
                        AdminStudentFields.TUTOR_IDS to FieldValue.arrayUnion(input.tutorUid),
                        AdminStudentFields.TUTORES to FieldValue.arrayUnion(input.tutorUid),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            val tutorUpdate = tutorProfileOnApproval(
                studentId = input.duplicateStudentId ?: studentId,
                groupId = resolvedGroupId,
            ).toMutableMap()
            tutorUpdate["updatedAt"] = FieldValue.serverTimestamp()
            batch.update(
                firestore.collection("users").document(input.tutorUid),
                tutorUpdate,
            )
            batch.commit().await()
            ApproveStudentResult.Success(studentId)
        } catch (e: Exception) {
            ApproveStudentResult.Failure(mapError(e).reason)
        }
    }

    override suspend fun setActive(studentId: String, active: Boolean): AdminStudentResult {
        if (auth.currentUser == null) {
            return AdminStudentResult.Failure(AdminStudentError.NO_SESSION)
        }

        return try {
            val studentRef = firestore.collection(AdminStudentFields.COLLECTION).document(studentId)
            val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
                studentRef.get(Source.SERVER).await()
            }
            if (!snapshot.exists()) {
                return AdminStudentResult.Failure(AdminStudentError.UNKNOWN)
            }
            val student = AdminStudent.fromDocument(snapshot.id, snapshot.safeData())
            if (student.active == active) return AdminStudentResult.Success
            if (student.groupId.isBlank()) {
                return AdminStudentResult.Failure(AdminStudentError.UNKNOWN)
            }

            val delta = if (active) 1L else -1L
            val batch = firestore.batch()
            batch.update(
                studentRef,
                mapOf(
                    AdminStudentFields.ACTIVE to active,
                    AdminStudentFields.UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            batch.update(
                firestore.collection(AdminStudentFields.GROUP_COLLECTION).document(student.groupId),
                mapOf(
                    AdminGroupFields.STUDENT_COUNT to FieldValue.increment(delta),
                    AdminGroupFields.UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }
            AdminStudentResult.Success
        } catch (error: Exception) {
            val mapped = mapError(error)
            AdminStudentResult.Failure(mapped.reason, mapped.detail)
        }
    }

    override suspend fun assignGroup(
        studentIds: List<String>,
        groupId: String,
        grado: Int,
        grupo: String,
    ): AdminStudentResult {
        val director = auth.currentUser
            ?: return AdminStudentResult.Failure(AdminStudentError.NO_SESSION)
        if (studentIds.isEmpty() || !isValidSection(grupo)) {
            return AdminStudentResult.Failure(AdminStudentError.UNKNOWN)
        }

        // The grade+section is the source of truth; the group id is DERIVED, so
        // the same classroom is always the same document. set(merge) creates it
        // on first use and is a no-op afterwards — no duplicates, no transaction.
        val resolvedId = groupIdFor(grado, grupo)
        val canonicalName = groupNameFor(grado, grupo)
        val schoolId = director.schoolId.ifBlank { auth.currentUser?.schoolId.orEmpty() }
        val groupRef = firestore.collection(AdminStudentFields.GROUP_COLLECTION).document(resolvedId)
        val groupDocument = AdminGroupFields.newGroupDocument(
            name = canonicalName,
            grade = grado,
            section = grupo.trim().uppercase(),
            schoolId = schoolId,
        ).toMutableMap().apply {
            // ONE write per document per batch. Put the increment INSIDE the
            // set(merge) so the counter accumulates across assignments; a second
            // batch.update on the same groupRef would be ignored/overwritten and
            // leave the count stuck at 1.
            put(AdminGroupFields.STUDENT_COUNT, FieldValue.increment(studentIds.size.toLong()))
            put(AdminGroupFields.UPDATED_AT, FieldValue.serverTimestamp())
        }

        return try {
            // Read the group's current roster to hand each new student the next
            // free list number (1..30). A full group cannot take more students.
            val roster = withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(AdminStudentFields.COLLECTION)
                    .whereEqualTo(AdminStudentFields.GROUP_ID, resolvedId)
                    .get(Source.SERVER).await()
            }.documents.mapNotNull { snap ->
                runCatching { AdminStudent.fromDocument(snap.id, snap.safeData()) }.getOrNull()
            }
            if (roster.size + studentIds.size > MAX_ROSTER_SIZE) {
                return AdminStudentResult.Failure(AdminStudentError.UNKNOWN)
            }
            var next = nextListNumber(roster) ?: 1

            val batch = firestore.batch()
            // get-or-create of the classroom, idempotent on repeat.
            batch.set(groupRef, groupDocument, com.google.firebase.firestore.SetOptions.merge())
            studentIds.forEach { id ->
                batch.update(
                    firestore.collection(AdminStudentFields.COLLECTION).document(id),
                    mapOf(
                        AdminStudentFields.GROUP_ID to resolvedId,
                        AdminStudentFields.GRUPO to canonicalName,
                        AdminStudentFields.GRADO to grado.toString(),
                        AdminStudentFields.LIST_NUMBER to next,
                        AdminStudentFields.UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                next++
            }
            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }
            AdminStudentResult.Success
        } catch (error: Exception) {
            val mapped = mapError(error)
            AdminStudentResult.Failure(mapped.reason, mapped.detail)
        }
    }

    override suspend fun renumberRoster(groupId: String): AdminStudentResult {
        if (auth.currentUser == null) {
            return AdminStudentResult.Failure(AdminStudentError.NO_SESSION)
        }
        return try {
            val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(AdminStudentFields.COLLECTION)
                    .whereEqualTo(AdminStudentFields.GROUP_ID, groupId)
                    .get(Source.SERVER).await()
            }
            val roster = snapshot.documents.mapNotNull { snap ->
                runCatching { AdminStudent.fromDocument(snap.id, snap.safeData()) }.getOrNull()
            }
            val numbered = renumberRoster(roster)
            val batch = firestore.batch()
            numbered.forEach { entry ->
                batch.update(
                    firestore.collection(AdminStudentFields.COLLECTION).document(entry.studentId),
                    mapOf(
                        AdminStudentFields.LIST_NUMBER to entry.listNumber,
                        AdminStudentFields.UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
            }
            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }
            AdminStudentResult.Success
        } catch (error: Exception) {
            val mapped = mapError(error)
            AdminStudentResult.Failure(mapped.reason, mapped.detail)
        }
    }

    override suspend fun createLinkingCode(studentId: String): LinkingCodeResult {
        val user = auth.currentUser
            ?: return LinkingCodeResult.Failure(AdminStudentError.NO_SESSION)

        return try {
            // The code carries the group as well as the student on purpose: a
            // tutor registering cannot read students/{id} yet, because that rule
            // requires an active profile and users/{uid} does not exist until
            // the registration batch commits.
            val student = withTimeout(READ_TIMEOUT_MILLIS) {
                firestore.collection(AdminStudentFields.COLLECTION)
                    .document(studentId)
                    .get(Source.SERVER)
                    .await()
            }
            val groupId = student.getString(AdminStudentFields.GROUP_ID)
                ?: return LinkingCodeResult.Failure(AdminStudentError.UNKNOWN)

            val codes = firestore.collection(AdminStudentFields.LINKING_CODES_COLLECTION)
            repeat(CODE_ATTEMPTS) {
                val code = newCode()
                val ref = codes.document(code)
                val exists = withTimeout(READ_TIMEOUT_MILLIS) {
                    ref.get(Source.SERVER).await().exists()
                }
                if (exists) return@repeat

                val expiresAt = Timestamp(
                    Date(System.currentTimeMillis() + LINKING_CODE_VALIDITY_MILLIS),
                )
                // The rules require schoolId and createdBy on create, and leave
                // usedBy/usedAt to the tutor's one-time redemption update.
                val document = mapOf(
                    AdminStudentFields.STUDENT_ID to studentId,
                    AdminStudentFields.GROUP_ID to groupId,
                    AdminStudentFields.SCHOOL_ID to user.schoolId,
                    AdminStudentFields.CREATED_BY to user.uid,
                    AdminStudentFields.CREATED_AT to FieldValue.serverTimestamp(),
                    AdminStudentFields.EXPIRES_AT to expiresAt,
                    AdminStudentFields.USED_BY to null,
                )
                withTimeout(WRITE_TIMEOUT_MILLIS) { ref.set(document).await() }
                return LinkingCodeResult.Success(code)
            }
            LinkingCodeResult.Failure(AdminStudentError.UNKNOWN)
        } catch (error: Exception) {
            val mapped = mapError(error)
            LinkingCodeResult.Failure(mapped.reason, mapped.detail)
        }
    }

    override suspend fun getLinkingCode(studentId: String): String? {
        if (auth.currentUser == null) return null

        // Single-field equality query: Firestore's automatic index covers it, so no
        // schoolId clause (and therefore no new composite index) is needed here.
        // The rules allow a directivo to list linkingCodes; the school boundary is
        // enforced by matching the exact studentId the principal selected.
        val snapshot = withTimeout(READ_TIMEOUT_MILLIS) {
            firestore.collection(AdminStudentFields.LINKING_CODES_COLLECTION)
                .whereEqualTo(AdminStudentFields.STUDENT_ID, studentId)
                .get(Source.SERVER)
                .await()
        }
        val now = Timestamp(Date())
        return snapshot.documents.firstOrNull { document ->
            document.get(AdminStudentFields.USED_BY) == null &&
                (document.getTimestamp(AdminStudentFields.EXPIRES_AT)?.let { it >= now } ?: false)
        }?.id
    }

    /** Uppercase alphanumeric, without the ambiguous 0/O and 1/I pairs. */
    private fun newCode(): String = (1..AdminStudentFields.LINKING_CODE_LENGTH)
        .map { CODE_ALPHABET[secureRandom.nextInt(CODE_ALPHABET.length)] }
        .joinToString("")

    private fun mapError(error: Exception): MappedStudentError {
        if (error is TimeoutCancellationException) {
            return MappedStudentError(AdminStudentError.TIMEOUT)
        }
        if (error is FirebaseFirestoreException) {
            val reason = when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                FirebaseFirestoreException.Code.UNAUTHENTICATED,
                -> AdminStudentError.DENIED

                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                -> AdminStudentError.NETWORK

                else -> AdminStudentError.UNKNOWN
            }
            // Diagnostics only: expose the raw Firestore code/message so we can see
            // which write the deployed rules reject on a real device. NETWORK keeps
            // its honest copy with no detail.
            val detail = when (reason) {
                AdminStudentError.DENIED, AdminStudentError.UNKNOWN ->
                    firestoreDetail(error.code.name, error.message)

                else -> null
            }
            return MappedStudentError(reason, detail)
        }
        return MappedStudentError(AdminStudentError.UNKNOWN)
    }

    private data class MappedStudentError(
        val reason: AdminStudentError,
        val detail: String? = null,
    )

    private companion object {
        const val READ_TIMEOUT_MILLIS = 12_000L
        const val WRITE_TIMEOUT_MILLIS = 12_000L
        const val CODE_ATTEMPTS = 12
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val LINKING_CODE_VALIDITY_MILLIS =
            AdminStudentFields.LINKING_CODE_VALIDITY_DAYS * 24L * 60 * 60 * 1000
        val secureRandom = SecureRandom()
    }
}

/**
 * Single conversion at the data boundary keeps the domain mapper free of Firebase
 * types. A missing document yields an empty map, which every mapper field tolerates.
 */
private fun DocumentSnapshot.safeData(): Map<String, Any?> = data ?: emptyMap()

/** Upper bound for the whole diagnostic string, code included. */
private const val FIRESTORE_DETAIL_MAX_LENGTH = 160

/**
 * Builds a short, single-line diagnostic from a Firestore code and message.
 *
 * Only the first line is kept (no stack traces), common secret patterns are
 * redacted, whitespace is collapsed and the result is bounded. No document
 * contents are read here — just the code and the platform message.
 */
private fun firestoreDetail(code: String, message: String?): String {
    val canonicalCode = code.lowercase().replace('_', '-')
    val safeMessage = message
        ?.lineSequence()
        ?.firstOrNull()
        .orEmpty()
        .replace(Regex("(?i)(bearer\\s+)\\S+"), "$1[redacted]")
        .replace(
            Regex("(?i)(token|secret|password|authorization|api[_-]?key)\\s*[=:]\\s*\\S+"),
            "[redacted]",
        )
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(FIRESTORE_DETAIL_MAX_LENGTH)
    val detail = if (safeMessage.isEmpty()) canonicalCode else "$canonicalCode: $safeMessage"
    return detail.take(FIRESTORE_DETAIL_MAX_LENGTH)
}
