package com.example.aulasegura.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aulasegura.domain.repository.AdminSchoolRepository
import com.example.aulasegura.domain.repository.AdminSchoolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Loads the school once, then saves only the campus fields.
 *
 * No Firebase types here: the repository returns a domain model and a typed
 * result, so the state machine can be driven by a fake in a unit test. The centre
 * coordinates live in the draft text fields (typed or moved on the map) and are
 * validated before they are copied into the update.
 */
class AdminCampusViewModel(
    private val schools: AdminSchoolRepository,
    private val nameResolver: com.example.aulasegura.domain.repository.SchoolNameResolver? = null,
) : ViewModel() {

    /** A name the director typed wins over any automatic geocoder result. */
    private var nameEditedByUser = false
    private var lastAutoFilledCoordinates: String? = null

    private val _state = MutableStateFlow(AdminCampusUiState())
    val state: StateFlow<AdminCampusUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Reloads from the server, including fresh coordinate drafts and the saved hint. */
    fun refresh() = reload(keepCoordinates = false)

    /**
     * [keepCoordinates] is used after a successful save: the server now holds the
     * edited location, so the exact texts the principal saw must not snap back to
     * a stale cached copy while the reload is in flight.
     */
    private fun reload(keepCoordinates: Boolean) {
        _state.update { it.copy(loading = true, error = null, saveError = null) }
        viewModelScope.launch {
            when (val result = schools.getSchool()) {
                is AdminSchoolResult.Success -> {
                    val school = result.data
                    if (!keepCoordinates) {
                        nameEditedByUser = false
                        lastAutoFilledCoordinates = null
                    }
                    _state.update { current ->
                        current.copy(
                            loading = false,
                            school = school,
                            nameText = if (keepCoordinates) current.nameText else school.name,
                            radiusMeters = school.radiusMeters,
                            entryTime = school.entryTime,
                            exitTime = school.exitTime,
                            requireLocation = school.requireLocation,
                            latitudeText = if (keepCoordinates) {
                                current.latitudeText
                            } else if (school.hasLocation) {
                                formatCoordinate(school.latitude)
                            } else {
                                ""
                            },
                            longitudeText = if (keepCoordinates) {
                                current.longitudeText
                            } else if (school.hasLocation) {
                                formatCoordinate(school.longitude)
                            } else {
                                ""
                            },
                            error = null,
                        )
                    }
                }
                is AdminSchoolResult.Failure -> _state.update {
                    it.copy(loading = false, error = result.reason.toMessage())
                }
            }
        }
    }

    fun setName(value: String) {
        nameEditedByUser = true
        _state.update { it.copy(nameText = value, saved = false, saveError = null) }
    }

    /** Fills the name from the centre once, unless the director typed one already. */
    private fun autoFillNameFromCoordinates() {
        val resolver = nameResolver ?: return
        if (nameEditedByUser) return
        val valid = validateCoordinates(
            _state.value.latitudeText,
            _state.value.longitudeText,
        ) as? com.example.aulasegura.ui.screens.admin.CoordinateValidation.Valid
            ?: return
        val key = "${valid.latitude},${valid.longitude}"
        if (key == lastAutoFilledCoordinates) return
        lastAutoFilledCoordinates = key
        viewModelScope.launch {
            val resolved = resolver.resolve(valid.latitude, valid.longitude)
            if (resolved.isNullOrBlank() || nameEditedByUser) return@launch
            _state.update { current -> current.copy(nameText = resolved, saved = false) }
        }
    }

    fun setRadius(radiusMeters: Int) {
        _state.update {
            it.copy(
                radiusMeters = radiusMeters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS),
                saved = false,
                saveError = null,
            )
        }
    }

    fun setEntryTime(value: String) {
        _state.update { it.copy(entryTime = value, saved = false, saveError = null) }
    }

    fun setExitTime(value: String) {
        _state.update { it.copy(exitTime = value, saved = false, saveError = null) }
    }

    fun setRequireLocation(value: Boolean) {
        _state.update { it.copy(requireLocation = value, saved = false, saveError = null) }
    }

    fun setLatitude(value: String) {
        _state.update { it.copy(latitudeText = value, saved = false, saveError = null) }
        autoFillNameFromCoordinates()
    }

    fun setLongitude(value: String) {
        _state.update { it.copy(longitudeText = value, saved = false, saveError = null) }
        autoFillNameFromCoordinates()
    }

    /** Applies a map drag or tap to both text fields using the non-locale formatter. */
    fun setLocation(latitude: Double, longitude: Double) {
        _state.update {
            it.copy(
                latitudeText = formatCoordinate(latitude),
                longitudeText = formatCoordinate(longitude),
                saved = false,
                saveError = null,
            )
        }
        autoFillNameFromCoordinates()
    }

    fun save() {
        val current = _state.value
        if (current.saving) return

        val coordinates = validateCoordinates(current.latitudeText, current.longitudeText)
        if (coordinates is CoordinateValidation.Invalid) {
            _state.update { it.copy(saveError = coordinates.message) }
            return
        }

        val validation = current.nameError ?: current.radiusError ?: current.scheduleError
        if (validation != null) {
            _state.update { it.copy(saveError = validation) }
            return
        }
        if (current.school == null) return

        val draft = coordinates as CoordinateValidation.Valid
        val config = buildCampusConfig(current, draft.latitude, draft.longitude)

        _state.update { it.copy(saving = true, saved = false, saveError = null) }
        viewModelScope.launch {
            when (val result = schools.updateCampus(config)) {
                is AdminSchoolResult.Success -> {
                    _state.update { it.copy(saving = false, saved = true) }
                    reload(keepCoordinates = true)
                }
                is AdminSchoolResult.Failure -> _state.update {
                    it.copy(saving = false, saveError = result.reason.toMessage())
                }
            }
        }
    }
}
