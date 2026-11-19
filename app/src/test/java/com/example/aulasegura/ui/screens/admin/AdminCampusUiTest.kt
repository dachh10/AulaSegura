package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminCampusConfig
import com.example.aulasegura.domain.model.AdminSchool
import com.example.aulasegura.domain.model.SERVER_TIMESTAMP_SENTINEL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Pure helpers only: no Firebase, no ViewModel, no Compose. */
class AdminCampusUiTest {

    private val validConfig = AdminCampusConfig(
        latitude = 19.2926,
        longitude = -99.6557,
        radiusMeters = 200,
        entryTime = "08:00",
        exitTime = "14:00",
        requireLocation = true,
    )

    @Test
    fun `radius outside 20 to 5000 is rejected and valid values are accepted`() {
        assertNotNull(validateRadius(MIN_RADIUS_METERS - 1))
        assertNotNull(validateRadius(0))
        assertNotNull(validateRadius(MAX_RADIUS_METERS + 1))

        assertNull(validateRadius(MIN_RADIUS_METERS))
        assertNull(validateRadius(200))
        assertNull(validateRadius(MAX_RADIUS_METERS))
    }

    @Test
    fun `a zero radius error calls out the geofence lockout`() {
        assertTrue(validateRadius(0)!!.contains("$MIN_RADIUS_METERS m"))
        assertTrue(validateRadius(6000)!!.contains("$MAX_RADIUS_METERS m"))
    }

    @Test
    fun `invalid time format is rejected including out of range hours and minutes`() {
        assertNull(validateSchedule("08:00", "14:00"))
        assertNull(validateSchedule("00:00", "23:59"))

        assertNotNull(validateSchedule("8:00", "14:00"))
        assertNotNull(validateSchedule("08:0", "14:00"))
        assertNotNull(validateSchedule("08:60", "14:00"))
        assertNotNull(validateSchedule("24:00", "14:00"))
        assertNotNull(validateSchedule("", "14:00"))
        assertNotNull(validateSchedule("08:00", "tomorrow"))
    }

    @Test
    fun `entry at or after exit is rejected`() {
        assertNotNull(validateSchedule("14:00", "08:00"))
        assertNotNull(validateSchedule("08:00", "08:00"))
        assertNull(validateSchedule("07:59", "08:00"))
    }

    @Test
    fun `update map has only the four writable fields plus updatedAt`() {
        val map = buildCampusUpdate(validConfig)

        assertEquals(
            setOf("location", "radiusMeters", "schedule", "requireLocation", "updatedAt"),
            map.keys,
        )

        // Immutable and identity fields must never reach the write path.
        listOf("key", "createdAt", "name", "cycle", "address", "schoolId").forEach {
            assertFalse("map must not contain $it", map.containsKey(it))
        }

        assertEquals(
            mapOf("lat" to 19.2926, "lng" to -99.6557),
            map["location"],
        )
        assertEquals(200, map["radiusMeters"])
        assertEquals(
            mapOf("entryTime" to "08:00", "exitTime" to "14:00"),
            map["schedule"],
        )
        assertEquals(true, map["requireLocation"])
        assertEquals(SERVER_TIMESTAMP_SENTINEL, map["updatedAt"])
    }

    @Test
    fun `slider endpoints map to the authoritative range`() {
        assertEquals(MIN_RADIUS_METERS, radiusFromFraction(0f))
        assertEquals(MAX_RADIUS_METERS, radiusFromFraction(1f))
        assertEquals(0f, fractionFromRadius(MIN_RADIUS_METERS))
        assertEquals(1f, fractionFromRadius(MAX_RADIUS_METERS))
        assertNull(validateRadius(radiusFromFraction(0.5f)))
    }

    @Test
    fun `last saved hint is explicit when no timestamp is stored`() {
        assertEquals("Sin guardados registrados", formatLastSaved(null))
        assertTrue(formatLastSaved(1_700_000_000_000L).startsWith("Último guardado:"))
    }

    @Test
    fun `valid coordinates are accepted and parsed`() {
        val result = validateCoordinates("19.282600", "-99.655700")

        assertTrue(result is CoordinateValidation.Valid)
        val valid = result as CoordinateValidation.Valid
        assertEquals(19.2826, valid.latitude, 0.0000001)
        assertEquals(-99.6557, valid.longitude, 0.0000001)
    }

    @Test
    fun `blank coordinate fields are rejected`() {
        assertNotNull((validateCoordinates("", "") as? CoordinateValidation.Invalid)?.message)
        assertNotNull((validateCoordinates("19.2826", "") as? CoordinateValidation.Invalid)?.message)
        assertNotNull((validateCoordinates("  ", "-99.6557") as? CoordinateValidation.Invalid)?.message)
    }

    @Test
    fun `non numeric coordinates including NaN and infinity are rejected`() {
        assertTrue(validateCoordinates("abc", "-99.6557") is CoordinateValidation.Invalid)
        assertTrue(validateCoordinates("19.2826", "xyz") is CoordinateValidation.Invalid)
        assertTrue(validateCoordinates("NaN", "Infinity") is CoordinateValidation.Invalid)
    }

    @Test
    fun `latitude outside -90 to 90 is rejected`() {
        assertTrue(validateCoordinates("90.0001", "-99.6557") is CoordinateValidation.Invalid)
        assertTrue(validateCoordinates("-90.0001", "-99.6557") is CoordinateValidation.Invalid)
        assertTrue(validateCoordinates("90", "-99.6557") is CoordinateValidation.Valid)
        assertTrue(validateCoordinates("-90", "-99.6557") is CoordinateValidation.Valid)
    }

    @Test
    fun `longitude outside -180 to 180 is rejected`() {
        assertTrue(validateCoordinates("19.2826", "180.0001") is CoordinateValidation.Invalid)
        assertTrue(validateCoordinates("19.2826", "-180.0001") is CoordinateValidation.Invalid)
        assertTrue(validateCoordinates("19.2826", "180") is CoordinateValidation.Valid)
        assertTrue(validateCoordinates("19.2826", "-180") is CoordinateValidation.Valid)
    }

    @Test
    fun `zero zero is rejected because it means no location`() {
        val result = validateCoordinates("0", "0.000000")

        assertTrue(result is CoordinateValidation.Invalid)
        assertTrue((result as CoordinateValidation.Invalid).message.contains("ubicación"))
    }

    @Test
    fun `comma decimal separator is accepted`() {
        val result = validateCoordinates("19,2826", "-99,6557")

        assertTrue(result is CoordinateValidation.Valid)
        val valid = result as CoordinateValidation.Valid
        assertEquals(19.2826, valid.latitude, 0.0000001)
        assertEquals(-99.6557, valid.longitude, 0.0000001)
    }

    @Test
    fun `formatting is Locale US and round trips under a Spanish default locale`() {
        val formatted = formatCoordinate(-99.6557)
        assertEquals("-99.655700", formatted)
        assertFalse(formatted.contains(','))

        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-MX"))
            val latitude = formatCoordinate(19.2826)
            val longitude = formatCoordinate(-99.6557)
            assertEquals("19.282600", latitude)
            assertEquals("-99.655700", longitude)
            assertTrue(validateCoordinates(latitude, longitude) is CoordinateValidation.Valid)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `config builder uses parsed draft coordinates instead of stored school coordinates`() {
        val school = AdminSchool(
            latitude = 1.0,
            longitude = 2.0,
            radiusMeters = 300,
            entryTime = "07:00",
            exitTime = "15:00",
            requireLocation = false,
        )
        val state = AdminCampusUiState(
            school = school,
            radiusMeters = 200,
            entryTime = "08:00",
            exitTime = "14:00",
            requireLocation = true,
            latitudeText = "19.282600",
            longitudeText = "-99.655700",
        )
        val draft = validateCoordinates(
            state.latitudeText,
            state.longitudeText,
        ) as CoordinateValidation.Valid
        val config = buildCampusConfig(state, draft.latitude, draft.longitude)

        assertEquals(19.2826, config.latitude, 0.0000001)
        assertEquals(-99.6557, config.longitude, 0.0000001)
        assertEquals(200, config.radiusMeters)
        assertEquals("08:00", config.entryTime)
        assertEquals("14:00", config.exitTime)
        assertTrue(config.requireLocation)
        assertEquals(
            mapOf("lat" to 19.2826, "lng" to -99.6557),
            buildCampusUpdate(config)["location"],
        )
        assertFalse(config.latitude == school.latitude && config.longitude == school.longitude)
    }

    @Test
    fun `save is disabled until the draft coordinates are valid`() {
        val school = AdminSchool(
            latitude = 19.2826,
            longitude = -99.6557,
            radiusMeters = 200,
            entryTime = "08:00",
            exitTime = "14:00",
        )
        val base = AdminCampusUiState(
            loading = false,
            school = school,
            radiusMeters = 200,
            entryTime = "08:00",
            exitTime = "14:00",
        )

        assertFalse(base.copy(latitudeText = "", longitudeText = "").canSave)
        assertFalse(base.copy(latitudeText = "0", longitudeText = "0").canSave)
        assertFalse(base.copy(latitudeText = "19.2826", longitudeText = "200").canSave)
        assertTrue(base.copy(latitudeText = "19.2826", longitudeText = "-99.6557").canSave)
    }
}
