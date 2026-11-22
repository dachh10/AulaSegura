package com.example.aulasegura.ui.screens.admin

import com.example.aulasegura.domain.model.AdminCampusConfig
import com.example.aulasegura.domain.model.AdminSchool
import com.example.aulasegura.domain.model.toUpdateMap
import com.example.aulasegura.domain.repository.AdminSchoolError
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * What the campus screen renders, plus pure helpers for validation and formatting.
 *
 * No Firebase, no Compose, no ViewModel here. Every value the screen prints comes
 * from these functions applied to the domain model, and the update map they build
 * is the same one the data layer writes.
 */

/** Authoritative geofence range (CONTEXT.md §8.1): `0` locks the whole school out. */
const val MIN_RADIUS_METERS = 20
const val MAX_RADIUS_METERS = 5000

/** Slider granularity, kept from the mockup; only the endpoints changed. */
const val RADIUS_STEP_METERS = 5

/**
 * Why the change is not instant on the teacher's phone. It is shown next to the
 * radius so a principal does not assume a save reached a classroom without signal.
 */
const val TEACHER_REFRESH_HINT =
    "El docente verá el cambio tras su próxima actualización con conexión. " +
        "Sin señal, el pase de lista usa el último radio guardado en el celular."

/** Result of parsing the editable coordinate fields; no Firebase, no Compose. */
sealed interface CoordinateValidation {
    data class Valid(val latitude: Double, val longitude: Double) : CoordinateValidation
    data class Invalid(val message: String) : CoordinateValidation
}

/**
 * Formats a coordinate for both the text fields and the map hint.
 *
 * `Locale.US` is intentional: the device locale is Spanish, where `String.format`
 * would emit a comma that [validateCoordinates] would otherwise have to reparse.
 */
fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

/**
 * Parses the draft latitude/longitude text. Both `.` and `,` decimal separators are
 * accepted because a Spanish keyboard produces commas on `KeyboardType.Decimal`.
 * `0,0` is rejected: `AdminSchool.hasLocation` treats it as "no location".
 */
fun validateCoordinates(latitudeText: String, longitudeText: String): CoordinateValidation {
    val latRaw = latitudeText.trim().replace(',', '.')
    val lngRaw = longitudeText.trim().replace(',', '.')

    if (latRaw.isBlank() || lngRaw.isBlank()) {
        return CoordinateValidation.Invalid("Escribe la latitud y la longitud del plantel.")
    }

    val latitude = latRaw.toDoubleOrNull()
    val longitude = lngRaw.toDoubleOrNull()
    if (latitude == null || longitude == null ||
        latitude.isNaN() || latitude.isInfinite() ||
        longitude.isNaN() || longitude.isInfinite()
    ) {
        return CoordinateValidation.Invalid("Las coordenadas deben ser números válidos.")
    }

    if (latitude !in -90.0..90.0) {
        return CoordinateValidation.Invalid("La latitud debe estar entre -90 y 90.")
    }
    if (longitude !in -180.0..180.0) {
        return CoordinateValidation.Invalid("La longitud debe estar entre -180 y 180.")
    }
    if (latitude == 0.0 && longitude == 0.0) {
        return CoordinateValidation.Invalid(
            "La ubicación no está definida. Indica una latitud y longitud válidas.",
        )
    }

    return CoordinateValidation.Valid(latitude, longitude)
}

/** The school name is required so every screen can print a real name, not an id. */
fun validateSchoolName(value: String): String? =
    if (value.trim().length < 3) "Escribe el nombre del plantel." else null

data class AdminCampusUiState(
    val loading: Boolean = true,
    val nameText: String = "",
    val school: AdminSchool? = null,
    val radiusMeters: Int = AdminSchool.DEFAULT_RADIUS_METERS,
    val entryTime: String = "",
    val exitTime: String = "",
    val requireLocation: Boolean = true,
    val latitudeText: String = "",
    val longitudeText: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val saveError: String? = null,
) {
    val radiusError: String? get() = validateRadius(radiusMeters)
    val scheduleError: String? get() = validateSchedule(entryTime, exitTime)
    val coordinateValidation: CoordinateValidation
        get() = validateCoordinates(latitudeText, longitudeText)
    val coordinateError: String?
        get() = (coordinateValidation as? CoordinateValidation.Invalid)?.message
    val nameError: String? get() = validateSchoolName(nameText)

    val canSave: Boolean
        get() = !loading && !saving && school != null &&
            nameError == null && coordinateError == null &&
            radiusError == null && scheduleError == null

    /** True only after a completed load that found no school document. */
    val empty: Boolean get() = !loading && error == null && school == null

    val lastSaved: String get() = formatLastSaved(school?.updatedAtMillis)
}

/** `null` means valid; any other value is ready to show under the slider. */
fun validateRadius(radiusMeters: Int): String? = when {
    radiusMeters < MIN_RADIUS_METERS -> "El radio mínimo es $MIN_RADIUS_METERS m."
    radiusMeters > MAX_RADIUS_METERS -> "El radio máximo es $MAX_RADIUS_METERS m."
    else -> null
}

private val TIME_PATTERN = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

private fun isValidTime(value: String): Boolean = TIME_PATTERN.matches(value)

/**
 * Both times must be `HH:mm` (24 h) and the entry must be before the exit.
 * Well-formed `HH:mm` strings sort lexicographically, so no date parsing is needed.
 */
fun validateSchedule(entryTime: String, exitTime: String): String? = when {
    !isValidTime(entryTime) -> "La entrada debe tener el formato HH:mm (00:00 a 23:59)."
    !isValidTime(exitTime) -> "La salida debe tener el formato HH:mm (00:00 a 23:59)."
    entryTime >= exitTime -> "La entrada debe ser anterior a la salida."
    else -> null
}

/** The exact Firestore payload, exposed here as a pure function for the tests. */
fun buildCampusUpdate(config: AdminCampusConfig): Map<String, Any?> = config.toUpdateMap()

/**
 * Builds the writable config from the draft state plus already-parsed coordinates.
 *
 * Coordinates are passed explicitly instead of read from [AdminCampusUiState.school]
 * so a typed or dragged draft can never be overwritten by stale stored values.
 */
fun buildCampusConfig(
    state: AdminCampusUiState,
    latitude: Double,
    longitude: Double,
): AdminCampusConfig = AdminCampusConfig(
    name = state.nameText.trim(),
    latitude = latitude,
    longitude = longitude,
    radiusMeters = state.radiusMeters,
    entryTime = state.entryTime,
    exitTime = state.exitTime,
    requireLocation = state.requireLocation,
)

/** Slider mapping, centralised so the visual track and the stored value agree. */
fun radiusFromFraction(fraction: Float): Int {
    val span = MAX_RADIUS_METERS - MIN_RADIUS_METERS
    val raw = MIN_RADIUS_METERS + fraction.coerceIn(0f, 1f) * span
    val stepped = (raw / RADIUS_STEP_METERS).toInt() * RADIUS_STEP_METERS
    return stepped.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
}

fun fractionFromRadius(radiusMeters: Int): Float =
    ((radiusMeters - MIN_RADIUS_METERS).toFloat() /
        (MAX_RADIUS_METERS - MIN_RADIUS_METERS).toFloat()).coerceIn(0f, 1f)

/** `"17 nov 2026, 10:24"`, or an explicit hint when the document has no timestamp. */
fun formatLastSaved(updatedAtMillis: Long?): String {
    if (updatedAtMillis == null) return "Sin guardados registrados"
    val locale = Locale.forLanguageTag("es-MX")
    val format = SimpleDateFormat("d MMM yyyy, HH:mm", locale)
    format.calendar = Calendar.getInstance(locale)
    return "Último guardado: ${format.format(java.util.Date(updatedAtMillis))}"
}

/** User-facing copy for a typed repository failure. */
fun AdminSchoolError.toMessage(): String = when (this) {
    AdminSchoolError.NO_SESSION -> "La sesión se cerró. Vuelve a iniciar sesión."
    AdminSchoolError.NOT_FOUND -> "No se encontró la ficha del plantel."
    AdminSchoolError.NETWORK -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
    AdminSchoolError.TIMEOUT -> "La operación tardó demasiado. Inténtalo de nuevo."
    AdminSchoolError.DENIED -> "No tienes permiso para editar la configuración del plantel."
    AdminSchoolError.UNKNOWN -> "No se pudo guardar la configuración del plantel."
}
