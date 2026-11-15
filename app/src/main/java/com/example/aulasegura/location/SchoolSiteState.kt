package com.example.aulasegura.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.aulasegura.di.rememberAppContainer
import com.example.aulasegura.domain.model.SchoolSite
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cache state of the school site.
 *
 * [loaded] separates "still deciding" from "there is no school cached". The gate
 * needs the difference: navigating on the first frame would send a teacher who
 * has the school cached down the unverified path for no reason, and a teacher
 * standing 6 km away would skip the outside screen entirely.
 */
data class SchoolSiteState(
    val site: SchoolSite? = null,
    val loaded: Boolean = false,
)

/**
 * Observes the cached school site and refreshes it once in the background.
 *
 * [loaded] becomes true as soon as there is a site, or when the answer is
 * genuinely "no site": the refresh finished, or [SETTLE_MILLIS] went by. The cap
 * exists so a teacher with no signal is not stuck behind a 12 s Firestore
 * timeout before taking attendance — the offline answer is "cannot verify", and
 * that answer has to arrive quickly to be useful.
 */
@Composable
fun rememberSchoolSite(): SchoolSiteState {
    val container = rememberAppContainer()
    var state by remember { mutableStateOf(SchoolSiteState()) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                container.schoolRepository.observeSchool().collect { site ->
                    state = state.copy(
                        site = site,
                        // A cached site settles the question immediately; a null
                        // emission does not, because the refresh may still be on
                        // its way.
                        loaded = state.loaded || site != null,
                    )
                }
            }
            launch {
                withTimeoutOrNull(SETTLE_MILLIS) { container.schoolRepository.refreshSchool() }
                state = state.copy(loaded = true)
            }
        }
    }

    return state
}

/**
 * How long the gate waits for the school refresh before treating a cache miss as
 * "no school". Long enough for a school Wi-Fi round-trip, short enough that an
 * offline teacher is not blocked by it.
 */
private const val SETTLE_MILLIS = 3_000L
