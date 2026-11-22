package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminUser
import com.example.aulasegura.domain.model.UserRole
import com.example.aulasegura.domain.repository.AdminUserFields
import com.example.aulasegura.domain.repository.AdminUserUpdateError
import com.example.aulasegura.domain.repository.PasswordChangeError
import com.example.aulasegura.domain.repository.passwordChangeErrorFromCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The profile's transformation, initials fallback, field rows and upload labels. */
class AdminProfileUiTest {

    @Test
    fun `avatar transform is inserted after upload and is idempotent`() {
        val original = "https://res.cloudinary.com/demo/image/upload/v1712/abc.jpg"
        val expected =
            "https://res.cloudinary.com/demo/image/upload/" +
                "w_512,h_512,c_fill,g_face,q_auto,f_auto/v1712/abc.jpg"

        assertEquals(expected, adminAvatarUrl(original))

        // Applying the helper to a URL that already carries the transform must
        // not nest a second one: Cloudinary would silently honour the inner one.
        assertEquals(expected, adminAvatarUrl(expected))
    }

    @Test
    fun `no url means no avatar and the name falls back to initials`() {
        assertNull(adminAvatarUrl(null))
        assertNull(adminAvatarUrl(""))
        assertNull(adminAvatarUrl("   "))

        assertEquals("PR", adminInitials("Patricia Rangel Vega"))
        assertEquals("P", adminInitials("Patricia"))
        assertEquals("?", adminInitials(null))
    }

    @Test
    fun `field rows print the real profile values`() {
        val user = AdminUser(
            uid = "u-directora",
            email = "directora@escuela.mx",
            role = UserRole.ADMIN,
            firstName = "Patricia",
            lastName = "Rangel",
            fullName = "Patricia Rangel Vega",
            phone = "5551000099",
            schoolId = "esc-01",
            subjects = emptyList(),
            groupIds = emptyList(),
            studentIds = emptyList(),
            active = true,
            photoUrl = "https://res.cloudinary.com/demo/image/upload/abc.jpg",
            photoPublicId = "aulasegura/profiles/u-directora/abc",
        )

        assertEquals(
            listOf(
                AdminProfileFieldRow("Nombre", "Patricia Rangel Vega"),
                AdminProfileFieldRow("Correo", "directora@escuela.mx"),
                AdminProfileFieldRow("Escuela", "esc-01"),
                AdminProfileFieldRow("Teléfono", "5551000099"),
            ),
            adminProfileFieldRows(user),
        )
    }

    @Test
    fun `the school row shows the real name and falls back to the id`() {
        val rows = adminProfileFieldRows(
            fullName = "Patricia Rangel Vega",
            email = "directora@escuela.mx",
            schoolId = "esc-01",
            phone = "",
            schoolName = "Escuela Benito Juárez",
        )

        assertEquals("Escuela Benito Juárez", rows.first { it.label == "Escuela" }.value)
        assertEquals(
            "esc-01",
            adminProfileFieldRows(
                fullName = "Patricia",
                email = "a@b.c",
                schoolId = "esc-01",
                phone = "",
            ).first { it.label == "Escuela" }.value,
        )
    }

    @Test
    fun `profile validation rejects blank names`() {
        assertNull(profileValidationError("Patricia", "Rangel"))
        assertTrue(profileValidationError("  ", "Rangel")!!.contains("nombre"))
        assertTrue(profileValidationError("Patricia", "")!!.contains("apellido"))
    }

    @Test
    fun `profile update map trims values, derives fullName and carries no identity fields`() {
        val map = buildProfileUpdateMap("  Patricia  ", " Rangel Vega ", " 5551000099 ")

        assertEquals(4, map.size)
        assertEquals("Patricia", map[AdminUserFields.FIRST_NAME])
        assertEquals("Rangel Vega", map[AdminUserFields.LAST_NAME])
        assertEquals("Patricia Rangel Vega", map[AdminUserFields.FULL_NAME])
        assertEquals("5551000099", map[AdminUserFields.PHONE])

        listOf(
            AdminUserFields.EMAIL,
            AdminUserFields.ROLE,
            AdminUserFields.ACTIVE,
            AdminUserFields.SCHOOL_ID,
            AdminUserFields.UID,
            AdminUserFields.CREATED_AT,
        ).forEach { assertFalse(map.containsKey(it)) }
    }

    @Test
    fun `notifications preference writes only preferences and reads back safely`() {
        val map = buildNotificationsUpdateMap(false)

        assertEquals(1, map.size)
        assertEquals(mapOf("notificationsEnabled" to false), map[AdminUserFields.PREFERENCES])
        assertFalse(map.containsKey(AdminUserFields.SCHOOL_ID))
        assertFalse(map.containsKey(AdminUserFields.EMAIL))
        assertFalse(map.containsKey(AdminUserFields.ROLE))

        assertFalse(AdminUserFields.notificationsEnabledFrom(mapOf("notificationsEnabled" to false)))
        assertTrue(AdminUserFields.notificationsEnabledFrom(mapOf("notificationsEnabled" to true)))
        // Legacy or malformed documents default to notifications ON.
        assertTrue(AdminUserFields.notificationsEnabledFrom(null))
        assertTrue(AdminUserFields.notificationsEnabledFrom(emptyMap<String, Any>()))
        assertTrue(AdminUserFields.notificationsEnabledFrom(mapOf("notificationsEnabled" to "yes")))
    }

    @Test
    fun `password dialog guard requires the current password and six new characters`() {
        assertTrue(changePasswordValidationError("", "123456")!!.contains("actual"))
        assertTrue(changePasswordValidationError("old", "12345")!!.contains("6"))
        assertNull(changePasswordValidationError("old", "123456"))
    }

    @Test
    fun `password error codes and copy are honest about the failure`() {
        assertEquals(PasswordChangeError.WRONG_PASSWORD, passwordChangeErrorFromCode("ERROR_WRONG_PASSWORD"))
        assertEquals(PasswordChangeError.WEAK_PASSWORD, passwordChangeErrorFromCode("ERROR_WEAK_PASSWORD"))
        assertEquals(
            PasswordChangeError.REQUIRES_RECENT_LOGIN,
            passwordChangeErrorFromCode("ERROR_REQUIRES_RECENT_LOGIN"),
        )
        assertEquals(PasswordChangeError.NETWORK, passwordChangeErrorFromCode("ERROR_NETWORK_REQUEST_FAILED"))
        assertEquals(PasswordChangeError.UNKNOWN, passwordChangeErrorFromCode(null))

        assertTrue(passwordChangeMessage(PasswordChangeError.WRONG_PASSWORD).contains("actual"))
        assertTrue(passwordChangeMessage(PasswordChangeError.REQUIRES_RECENT_LOGIN).contains("iniciar sesión"))
        assertTrue(notificationsFailureMessage(AdminUserUpdateError.DENIED).contains("permiso"))
    }

    @Test
    fun `settings outcomes reuse the banner phase honestly`() {
        assertEquals(
            AdminProfileEditPhase.FAILURE,
            AdminProfileSettingsUi(AdminSettingsPhase.FAILURE, "error").toEditStatus().phase,
        )
        assertEquals(
            AdminProfileEditPhase.SUCCESS,
            AdminChangePasswordUi(AdminPasswordChangePhase.SUCCESS, "ok").toEditStatus().phase,
        )
    }

    @Test
    fun `upload phases have honest Spanish labels`() {
        assertEquals("", uploadPhaseLabel(AdminPhotoUploadPhase.IDLE))
        assertEquals("Comprimiendo foto…", uploadPhaseLabel(AdminPhotoUploadPhase.COMPRESSING))
        assertEquals("Subiendo foto…", uploadPhaseLabel(AdminPhotoUploadPhase.UPLOADING))
        assertEquals("Guardando cambios…", uploadPhaseLabel(AdminPhotoUploadPhase.SAVING))
        assertEquals("Foto actualizada", uploadPhaseLabel(AdminPhotoUploadPhase.SUCCESS))
        assertEquals(
            "No se pudo actualizar la foto",
            uploadPhaseLabel(AdminPhotoUploadPhase.FAILURE),
        )
    }
}
