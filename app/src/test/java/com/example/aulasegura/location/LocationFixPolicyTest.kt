package com.example.aulasegura.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Opening a form must not wait for a fresh GPS fix, but the button still must. */
class LocationFixPolicyTest {

    @Test
    fun `a map-only form opens with the last known position`() {
        assertFalse(shouldPreferFreshFix(useFreshFixOnEntry = false, attempt = 0))
    }

    @Test
    fun `an explicit location request asks for a fresh fix`() {
        assertTrue(shouldPreferFreshFix(useFreshFixOnEntry = false, attempt = 1))
    }

    @Test
    fun `geofence screens still ask for a fresh fix on entry`() {
        assertTrue(shouldPreferFreshFix(useFreshFixOnEntry = true, attempt = 0))
    }
}
