package com.example.aulasegura.data.repository

import com.example.aulasegura.data.local.dao.ProfileDao
import com.example.aulasegura.data.local.mapper.toCachedEntity
import com.example.aulasegura.data.local.mapper.toCachedUser
import com.example.aulasegura.data.mapper.profilePhotoUpdate
import com.example.aulasegura.data.mapper.profileUpdate
import com.example.aulasegura.domain.model.AppUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AccountRegistrationInput
import com.example.aulasegura.domain.repository.AccountRegistrationResult
import com.example.aulasegura.domain.repository.AuthFailure
import com.example.aulasegura.domain.repository.AuthRepository
import com.example.aulasegura.domain.repository.AuthResult
import com.example.aulasegura.domain.repository.LinkingState
import com.example.aulasegura.domain.repository.RegistrationKind
import com.example.aulasegura.domain.repository.PasswordChangeError
import com.example.aulasegura.domain.repository.PasswordChangeResult
import com.example.aulasegura.domain.repository.PasswordResetResult
import com.example.aulasegura.domain.repository.ProfilePhotoResult
import com.example.aulasegura.domain.repository.ProfileUpdateResult
import com.example.aulasegura.domain.repository.TutorRegistrationInput
import com.example.aulasegura.domain.repository.TutorRegistrationResult
import com.example.aulasegura.domain.repository.isValidStudentCode
import com.example.aulasegura.domain.repository.normalizeStudentCode
import com.example.aulasegura.domain.repository.passwordChangeErrorFromCode
import com.example.aulasegura.domain.telemetry.AnalyticsEvent
import com.example.aulasegura.domain.telemetry.AnalyticsLogger
import com.example.aulasegura.domain.telemetry.NoOpAnalyticsLogger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Firebase implementation of [AuthRepository].
 *
 * The role is never trusted from the client side alone: it is read from
 * `users/{uid}` and, on the server, the security rules validate it again on
 * every operation.
 */
class AuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val profileDao: ProfileDao,
    private val analytics: AnalyticsLogger = NoOpAnalyticsLogger,
) : AuthRepository {

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Cached so `schoolId`, `subjects` and `fullName` are available without a
     * second `users/{uid}` read per screen. Written only by [resolveProfile] —
     * from the network or, offline, from the Room copy — and cleared on sign-out.
     */
    @Volatile
    override var currentUser: AppUser? = null
        private set

    override suspend fun signIn(email: String, password: String): AuthResult =
        try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            resolveProfile().also { result ->
                // Only a real sign-in is logged; restoreSession() on startup is
                // the same user coming back, not a login.
                if (result is AuthResult.Success) {
                    analytics.log(AnalyticsEvent.LoginSuccess(result.user.role))
                }
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult.Failure(AuthFailure.USER_DISABLED)
        } catch (e: FirebaseNetworkException) {
            AuthResult.Failure(AuthFailure.NETWORK)
        } catch (e: Exception) {
            signOut()
            AuthResult.Failure(AuthFailure.UNKNOWN)
        }

    override suspend fun registerTutor(input: TutorRegistrationInput): TutorRegistrationResult {
        val code = normalizeStudentCode(input.code)
        if (!isValidStudentCode(code)) return TutorRegistrationResult.InvalidCode

        suspend fun reject(result: TutorRegistrationResult): TutorRegistrationResult {
            // No half accounts: anything that fails after Auth was created
            // deletes that account instead of leaving the email taken.
            discardHalfRegisteredAccount()
            return result
        }

        return try {
            // Auth first: `linkingCodes.get` is gated by isSignedIn(), so the code
            // cannot be read before the account exists.
            val created = auth.createUserWithEmailAndPassword(
                input.email.trim(), input.password,
            ).await()
            val uid = created.user?.uid ?: return reject(TutorRegistrationResult.Failed)

            val codeDoc = firestore.collection(LINKING_CODES).document(code)
                .get(Source.SERVER).await()
            if (!codeDoc.exists()) return reject(TutorRegistrationResult.InvalidCode)
            if (codeDoc.getString(FIELD_USED_BY) != null) {
                return reject(TutorRegistrationResult.CodeAlreadyUsed)
            }
            codeDoc.getTimestamp(FIELD_EXPIRES_AT)?.toDate()?.let { expires ->
                if (expires.before(Date())) return reject(TutorRegistrationResult.ExpiredCode)
            }
            val studentId = codeDoc.getString(FIELD_STUDENT_ID)
                ?: return reject(TutorRegistrationResult.InvalidCode)
            val schoolId = codeDoc.getString(FIELD_SCHOOL_ID)
                ?: return reject(TutorRegistrationResult.InvalidCode)
            // The group travels inside the code. Reading students/{id} here is
            // not possible: that rule needs an active profile, and users/{uid}
            // is only a pending write until the batch below commits.
            val groupId = codeDoc.getString(FIELD_GROUP_ID)
                ?: return reject(TutorRegistrationResult.InvalidCode)

            val (firstName, lastName) = splitName(input.fullName)
            val userData = mapOf(
                "uid" to uid,
                FIELD_EMAIL to input.email.trim(),
                FIELD_ROLE to "tutor",
                "firstName" to firstName,
                "lastName" to lastName,
                FIELD_FULL_NAME to input.fullName.trim(),
                "phone" to input.phone.trim(),
                FIELD_SCHOOL_ID to schoolId,
                "groupIds" to listOf(groupId),
                "studentIds" to listOf(studentId),
                FIELD_ACTIVE to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                // Lets the rules verify the exact code without listing codes.
                "registrationCode" to code,
            )

            // One batch: if any rule refuses, no document changes at all.
            val batch = firestore.batch()
            batch.set(firestore.collection(USERS).document(uid), userData)
            batch.update(firestore.collection(STUDENTS).document(studentId), mapOf(
                "tutorIds" to FieldValue.arrayUnion(uid),
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            batch.update(firestore.collection(LINKING_CODES).document(code), mapOf(
                FIELD_USED_BY to uid,
                "usedAt" to FieldValue.serverTimestamp(),
            ))
            withTimeout(WRITE_TIMEOUT_MILLIS) { batch.commit().await() }

            val user = AppUser(
                uid = uid,
                email = input.email.trim(),
                role = UserRole.TUTOR,
                fullName = input.fullName.trim(),
                schoolId = schoolId,
                active = true,
            )
            currentUser = user
            runCatching { profileDao.upsert(user.toCachedEntity(System.currentTimeMillis())) }
            analytics.log(AnalyticsEvent.LoginSuccess(UserRole.TUTOR))
            TutorRegistrationResult.Success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            TutorRegistrationResult.EmailInUse
        } catch (e: FirebaseAuthWeakPasswordException) {
            TutorRegistrationResult.WeakPassword
        } catch (e: FirebaseNetworkException) {
            discardHalfRegisteredAccount()
            TutorRegistrationResult.NetworkError
        } catch (e: FirebaseFirestoreException) {
            discardHalfRegisteredAccount()
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                TutorRegistrationResult.Rejected
            } else {
                TutorRegistrationResult.Failed
            }
        } catch (e: Exception) {
            discardHalfRegisteredAccount()
            TutorRegistrationResult.Failed
        }
    }

    override suspend fun registerAccount(
        input: AccountRegistrationInput,
    ): AccountRegistrationResult {
        val isTutor = input.kind == RegistrationKind.TUTOR
        suspend fun reject(result: AccountRegistrationResult): AccountRegistrationResult {
            discardHalfRegisteredAccount()
            return result
        }

        return try {
            val created = auth.createUserWithEmailAndPassword(
                input.email.trim(), input.password,
            ).await()
            val uid = created.user?.uid ?: return reject(AccountRegistrationResult.Failed)
            runCatching { created.user?.sendEmailVerification()?.await() }

            val (firstName, lastName) = splitName(input.fullName)
            val roleValue = if (isTutor) "tutor" else "docente"
            val stateValue = if (isTutor) "activo" else "pendiente"
            val now = FieldValue.serverTimestamp()
            val userData = mutableMapOf<String, Any>(
                "uid" to uid,
                FIELD_EMAIL to input.email.trim(),
                // `role` is kept as a compatibility alias while the rest of the
                // app migrates; `rol` is the contract described in the .md.
                FIELD_ROLE to roleValue,
                FIELD_ROL to roleValue,
                FIELD_ESTADO to stateValue,
                FIELD_NOMBRE to input.fullName.trim(),
                FIELD_CORREO to input.email.trim(),
                "firstName" to firstName,
                "lastName" to lastName,
                FIELD_FULL_NAME to input.fullName.trim(),
                "phone" to input.phone.trim(),
                FIELD_TELEFONO to input.phone.trim(),
                FIELD_SCHOOL_ID to DEFAULT_SCHOOL_ID,
                FIELD_ACTIVE to true,
                FIELD_FECHA_SOLICITUD to now,
                "createdAt" to now,
                "updatedAt" to now,
            )
            if (isTutor) {
                userData[FIELD_VINCULACION] = "pendiente"
                userData[FIELD_HIJO_SOLICITADO] = input.childName.trim()
            } else {
                userData[FIELD_SUBJECTS] = listOf(input.subject.trim())
            }

            withTimeout(WRITE_TIMEOUT_MILLIS) {
                firestore.collection(USERS).document(uid).set(userData).await()
            }

            val user = AppUser(
                uid = uid,
                email = input.email.trim(),
                role = if (isTutor) UserRole.TUTOR else UserRole.TEACHER,
                fullName = input.fullName.trim(),
                schoolId = DEFAULT_SCHOOL_ID,
                active = true,
                estado = stateValue,
                vinculacion = if (isTutor) "pendiente" else "",
                hijoSolicitado = if (isTutor) input.childName.trim() else "",
            )
            currentUser = user
            runCatching { profileDao.upsert(user.toCachedEntity(System.currentTimeMillis())) }
            analytics.log(AnalyticsEvent.LoginSuccess(user.role))
            AccountRegistrationResult.Success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            AccountRegistrationResult.EmailInUse
        } catch (e: FirebaseAuthWeakPasswordException) {
            AccountRegistrationResult.WeakPassword
        } catch (e: FirebaseNetworkException) {
            reject(AccountRegistrationResult.NetworkError)
        } catch (e: FirebaseFirestoreException) {
            reject(
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    AccountRegistrationResult.Rejected
                } else {
                    AccountRegistrationResult.Failed
                },
            )
        } catch (e: Exception) {
            reject(AccountRegistrationResult.Failed)
        }
    }

    override suspend fun isEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        return runCatching {
            user.reload()
            user.isEmailVerified
        }.getOrDefault(false)
    }

    override suspend fun resendVerificationEmail(): Result<Unit> =
        runCatching { auth.currentUser?.sendEmailVerification()?.await() ?: Unit }

    override suspend fun refreshProfile(): AuthResult = resolveProfile()

    /**
     * A half-registered account is worse than a failed one: the email stays
     * taken, every retry answers "account already exists", and signing in finds
     * no profile to route. Deleting the Auth user is the only way back to a
     * clean state, so a refused batch never leaves a ghost behind.
     */
    private suspend fun discardHalfRegisteredAccount() {
        runCatching { auth.currentUser?.delete()?.await() }
        signOut()
    }

    override suspend fun restoreSession(): AuthResult {
        if (auth.currentUser == null) return AuthResult.Failure(AuthFailure.NO_SESSION)
        return resolveProfile()
    }

    override suspend fun updateProfilePhoto(
        photoUrl: String,
        photoPublicId: String,
    ): ProfilePhotoResult {
        val profile = currentUser ?: return ProfilePhotoResult.SignedOut
        val uid = auth.currentUser?.uid
        if (uid == null || uid != profile.uid) return ProfilePhotoResult.SignedOut

        return try {
            firestore.collection(USERS).document(uid)
                .update(profilePhotoUpdate(photoUrl, photoPublicId))
                .await()

            // The avatar must already be visible when the screen comes back: the
            // in-memory profile feeds the greeting and the UI directly, and Room
            // is what restores it on the next cold start without a network read.
            val updated = profile.copy(
                photoUrl = photoUrl,
                photoPublicId = photoPublicId,
            )
            currentUser = updated
            runCatching {
                profileDao.upsert(updated.toCachedEntity(System.currentTimeMillis()))
            }

            ProfilePhotoResult.Saved
        } catch (denied: FirebaseFirestoreException) {
            if (denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                ProfilePhotoResult.Rejected
            } else {
                ProfilePhotoResult.Failed
            }
        } catch (e: Exception) {
            ProfilePhotoResult.Failed
        }
    }

    override suspend fun updateProfile(
        fullName: String,
        subjects: List<String>,
    ): ProfileUpdateResult {
        val profile = currentUser ?: return ProfileUpdateResult.SignedOut
        val uid = auth.currentUser?.uid
        if (uid == null || uid != profile.uid) return ProfileUpdateResult.SignedOut

        return try {
            firestore.collection(USERS).document(uid)
                .update(profileUpdate(fullName, subjects))
                .await()

            val updated = profile.copy(fullName = fullName, subjects = subjects)
            currentUser = updated
            runCatching {
                profileDao.upsert(updated.toCachedEntity(System.currentTimeMillis()))
            }

            ProfileUpdateResult.Saved
        } catch (denied: FirebaseFirestoreException) {
            if (denied.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                ProfileUpdateResult.Rejected
            } else {
                ProfileUpdateResult.Failed
            }
        } catch (e: Exception) {
            ProfileUpdateResult.Failed
        }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): PasswordChangeResult {
        val firebaseUser = auth.currentUser
            ?: return PasswordChangeResult.SignedOut
        val email = firebaseUser.email ?: currentUser?.email
            ?: return PasswordChangeResult.SignedOut

        return try {
            // Firebase rejects updatePassword after a long session without a
            // recent login. Reauthenticating with the current password is what
            // makes the change possible instead of failing hours into the shift.
            firebaseUser.reauthenticate(
                EmailAuthProvider.getCredential(email.trim(), currentPassword),
            ).await()
            firebaseUser.updatePassword(newPassword).await()
            PasswordChangeResult.Changed
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            PasswordChangeResult.WrongPassword
        } catch (e: FirebaseAuthWeakPasswordException) {
            PasswordChangeResult.WeakPassword
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            PasswordChangeResult.RequiresRecentLogin
        } catch (e: FirebaseNetworkException) {
            PasswordChangeResult.NetworkError
        } catch (e: FirebaseAuthException) {
            // Keep the pure classifier in the loop for codes the typed exceptions
            // above do not cover; the reason is still exposed through Failure.
            when (passwordChangeErrorFromCode(e.errorCode)) {
                PasswordChangeError.WRONG_PASSWORD -> PasswordChangeResult.WrongPassword
                PasswordChangeError.WEAK_PASSWORD -> PasswordChangeResult.WeakPassword
                PasswordChangeError.REQUIRES_RECENT_LOGIN -> PasswordChangeResult.RequiresRecentLogin
                PasswordChangeError.NETWORK -> PasswordChangeResult.NetworkError
                PasswordChangeError.NO_SESSION,
                PasswordChangeError.UNKNOWN,
                -> PasswordChangeResult.Failed
            }
        } catch (e: Exception) {
            PasswordChangeResult.Failed
        }
    }

    override fun signOut() {
        currentUser = null
        auth.signOut()
    }

    override suspend fun sendPasswordReset(email: String): PasswordResetResult =
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            PasswordResetResult.Sent
        } catch (e: FirebaseAuthInvalidUserException) {
            // Anti-enumeration: an address without an account must look exactly
            // like a successful send, or the screen becomes an account oracle.
            PasswordResetResult.Sent
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Malformed address. The SDK reports both a bad format and a rejected
            // credential with this type; a reset email can only hit the former.
            PasswordResetResult.InvalidEmail
        } catch (e: FirebaseNetworkException) {
            PasswordResetResult.NetworkError
        } catch (e: Exception) {
            PasswordResetResult.Unknown
        }

    override fun observeAccountActive(): Flow<Boolean> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                // A failed read means "unknown", not "deactivated". Staying silent
                // is the only safe behaviour: never sign out on a network hiccup.
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }
                trySend(snapshot.getBoolean(FIELD_ACTIVE) ?: true)
            }

        awaitClose { registration.remove() }
    }

    override fun observeLinkingState(): Flow<LinkingState> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                // A failed or empty read is not a rejection: stay silent and keep
                // the last known state, like the account-active listener does.
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                trySend(
                    LinkingState(
                        vinculacion = snapshot.getString(FIELD_VINCULACION).orEmpty(),
                        hijoSolicitado = snapshot.getString(FIELD_HIJO_SOLICITADO).orEmpty(),
                        estado = snapshot.getString(FIELD_ESTADO).orEmpty(),
                    ),
                )
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setTutorLinking(uid: String, vinculacion: String): Result<Unit> {
        val director = currentUser
            ?: return Result.failure(IllegalStateException("NO_SESSION"))
        if (director.role != UserRole.ADMIN) {
            return Result.failure(IllegalStateException("NOT_DIRECTOR"))
        }
        if (uid.isBlank() || vinculacion !in setOf("vinculado", "rechazada")) {
            return Result.failure(IllegalArgumentException("INVALID_LINKING"))
        }
        return runCatching {
            firestore.collection(USERS).document(uid)
                .update(
                    FIELD_VINCULACION, vinculacion,
                    "updatedAt", FieldValue.serverTimestamp(),
                ).await()
        }
    }

    override suspend fun updateChildName(name: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("NO_SESSION"))
        val trimmed = name.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("EMPTY_NAME"))
        return runCatching {
            firestore.collection(USERS).document(uid)
                .update(FIELD_HIJO_SOLICITADO, trimmed).await()
            currentUser = currentUser?.copy(hijoSolicitado = trimmed)
        }
    }

    private suspend fun resolveProfile(): AuthResult {
        val firebaseUser = auth.currentUser
            ?: return AuthResult.Failure(AuthFailure.NO_SESSION)

        val snapshot = try {
            firestore.collection(USERS).document(firebaseUser.uid).get().await()
        } catch (e: FirebaseNetworkException) {
            // No network: the last profile read on this device is still the truth
            // about who is signed in. Without this branch a teacher who opens the
            // app inside a school with no signal lands on the login screen.
            return cachedProfile(firebaseUser.uid)
        } catch (e: FirebaseFirestoreException) {
            return cachedProfile(firebaseUser.uid)
        }

        if (!snapshot.exists()) {
            signOut()
            return AuthResult.Failure(AuthFailure.PROFILE_MISSING)
        }

        val active = snapshot.getBoolean(FIELD_ACTIVE) ?: true
        if (!active) {
            signOut()
            return AuthResult.Failure(AuthFailure.PROFILE_INACTIVE)
        }

        val role = UserRole.fromFirestore(snapshot.getString(FIELD_ROLE))
        if (role == UserRole.UNKNOWN) {
            signOut()
            return AuthResult.Failure(AuthFailure.ROLE_UNKNOWN)
        }

        val user = AppUser(
            uid = firebaseUser.uid,
            email = snapshot.getString(FIELD_EMAIL) ?: firebaseUser.email.orEmpty(),
            role = role,
            fullName = snapshot.getString(FIELD_FULL_NAME).orEmpty(),
            schoolId = snapshot.getString(FIELD_SCHOOL_ID).orEmpty(),
            active = active,
            photoUrl = snapshot.getString(FIELD_PHOTO_URL),
            subjects = (snapshot.get(FIELD_SUBJECTS) as? List<*>)?.filterIsInstance<String>()
                ?: emptyList(),
            estado = snapshot.getString(FIELD_ESTADO).orEmpty(),
            vinculacion = snapshot.getString(FIELD_VINCULACION).orEmpty(),
            hijoSolicitado = snapshot.getString(FIELD_HIJO_SOLICITADO).orEmpty(),
            fechaSolicitud = snapshot.getTimestamp(FIELD_FECHA_SOLICITUD)?.toDate()?.time ?: 0L,
        )

        // Cached only after the profile is fully valid: a rejected account must
        // not leave a usable user behind. The cache is replaced, never appended,
        // so a shared device keeps exactly one profile.
        runCatching {
            profileDao.clear()
            profileDao.upsert(user.toCachedEntity(System.currentTimeMillis()))
        }

        currentUser = user
        return AuthResult.Success(user)
    }

    /**
     * The offline path of [resolveProfile].
     *
     * Firestore's persistent cache is deliberately off, so the network failure
     * has to fall back to the Room copy of the profile. A deactivated account can
     * keep working offline until the next successful read — that is the price of
     * a usable offline app, and the safety net is [observeAccountActive], which
     * signs the session out as soon as a real value arrives.
     */
    private suspend fun cachedProfile(uid: String): AuthResult {
        val cached = runCatching { profileDao.findById(uid) }.getOrNull()
            ?.toCachedUser()
            ?: return AuthResult.Failure(AuthFailure.NETWORK)

        currentUser = cached
        return AuthResult.Success(cached)
    }

    private companion object {
        const val USERS = "users"
        const val STUDENTS = "students"
        const val LINKING_CODES = "linkingCodes"
        const val FIELD_USED_BY = "usedBy"
        const val FIELD_STUDENT_ID = "studentId"
        const val FIELD_EXPIRES_AT = "expiresAt"
        const val FIELD_GROUP_ID = "groupId"
        const val WRITE_TIMEOUT_MILLIS = 10_000L
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_FULL_NAME = "fullName"
        const val FIELD_SCHOOL_ID = "schoolId"
        const val FIELD_ACTIVE = "active"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_SUBJECTS = "subjects"
        const val FIELD_ROL = "rol"
        const val FIELD_ESTADO = "estado"
        const val FIELD_NOMBRE = "nombre"
        const val FIELD_CORREO = "correo"
        const val FIELD_TELEFONO = "telefono"
        const val FIELD_VINCULACION = "vinculacion"
        const val FIELD_HIJO_SOLICITADO = "hijoSolicitado"
        const val FIELD_FECHA_SOLICITUD = "fechaSolicitud"
        const val DEFAULT_SCHOOL_ID = "sch-benito-juarez"

        fun splitName(full: String): Pair<String, String> {
            val parts = full.trim().split(Regex("\\s+"), limit = 2)
            return parts.firstOrNull().orEmpty() to parts.getOrNull(1).orEmpty()
        }
    }
}
