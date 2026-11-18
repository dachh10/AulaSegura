package com.example.aulasegura.location

import com.example.aulasegura.domain.model.SchoolSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The geofence is the one piece of this block that decides whether a session is
 * stored as verified, and it can be fully exercised without a device. These
 * tests pin the rule, not the implementation: inside with a good fix verifies,
 * outside never does, and a fix whose accuracy is worse than the radius can
 * never claim verification.
 */
class GeofenceTest {

    private val school = SchoolSite(
        schoolId = "sch-benito-juarez",
        name = "Escuela Primaria Benito Juárez",
        latitude = 19.2926,
        longitude = -99.6557,
        radiusMeters = 150.0,
    )

    @Test
    fun `the distance from a point to itself is zero`() {
        val distance = haversineMeters(19.2926, -99.6557, 19.2926, -99.6557)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `one thousandth of a degree of latitude is about 111 metres`() {
        val distance = haversineMeters(19.2926, -99.6557, 19.2936, -99.6557)

        assertEquals(111.2, distance, 0.5)
    }

    @Test
    fun `the distance is symmetric`() {
        val there = haversineMeters(19.2926, -99.6557, 19.30, -99.66)
        val back = haversineMeters(19.30, -99.66, 19.2926, -99.6557)

        assertEquals(there, back, 0.001)
    }

    @Test
    fun `a fix inside the radius with a good accuracy is verified`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 30f)

        val result = evaluateGeofence(fix, school)

        assertEquals(GeofenceReason.INSIDE, result.reason)
        assertTrue(result.verified)
        assertEquals(150.0, result.radiusMeters)
        assertEquals(55.6, result.distanceMeters!!, 1.0)
    }

    @Test
    fun `six kilometres away is outside no matter how good the accuracy is`() {
        // The device used for verification sits ~6.6 km from the school: this is
        // the real "outside" case, not a synthetic one.
        val fix = LocationFix(latitude = 19.352, longitude = -99.6557, accuracyMeters = 10f)

        val result = evaluateGeofence(fix, school)

        assertEquals(GeofenceReason.OUTSIDE, result.reason)
        assertFalse(result.verified)
        assertTrue("expected more than 6 km, got ${result.distanceMeters}", result.distanceMeters!! > 6_000)
    }

    @Test
    fun `an accuracy worse than the radius cannot verify even inside`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 200f)

        val result = evaluateGeofence(fix, school)

        assertEquals(GeofenceReason.ACCURACY_TOO_LOW, result.reason)
        assertFalse(result.verified)
        assertTrue("the fix is inside the fence, so the banner must say so", result.withinRadius)
    }

    @Test
    fun `a fix without accuracy cannot verify either`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = null)

        val result = evaluateGeofence(fix, school)

        assertEquals(GeofenceReason.ACCURACY_TOO_LOW, result.reason)
        assertFalse(result.verified)
    }

    @Test
    fun `no fix is no fix, and it keeps the radius for the message`() {
        val result = evaluateGeofence(null, school)

        assertEquals(GeofenceReason.NO_FIX, result.reason)
        assertFalse(result.verified)
        assertNull(result.distanceMeters)
        assertEquals(150.0, result.radiusMeters)
    }

    @Test
    fun `no cached school cannot verify and does not pretend to be outside`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 30f)

        val result = evaluateGeofence(fix, null)

        assertEquals(GeofenceReason.NO_SCHOOL, result.reason)
        assertFalse(result.verified)
        assertNull(result.distanceMeters)
        assertFalse(result.withinRadius)
    }

    @Test
    fun `the session keeps the reading and the verdict of that moment`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 30f)
        val result = evaluateGeofence(fix, school)

        val session = fix.toSessionLocation(result)

        assertEquals(19.2931, session.latitude, 0.0)
        assertEquals(-99.6557, session.longitude, 0.0)
        assertEquals(30.0, session.accuracyMeters!!, 0.0)
        assertEquals(result.distanceMeters!!, session.distanceMeters!!, 0.0)
        assertTrue(session.verified)
    }

    @Test
    fun `an unverified reading is stored as unverified, not dropped`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 500f)
        val result = evaluateGeofence(fix, school)

        val session = fix.toSessionLocation(result)

        assertFalse(session.verified)
        // The coordinates are still there: the session says "we asked the GPS and
        // it could not prove it", which is more useful than a null location.
        assertEquals(19.2931, session.latitude, 0.0)
    }

    @Test
    fun `the school policy defaults to requiring GPS`() {
        assertTrue(shouldRequireGps(school))
        assertTrue(shouldRequireGps(school.copy(requireLocation = true)))
    }

    @Test
    fun `a school may opt out but a missing school fails closed`() {
        assertFalse(shouldRequireGps(school.copy(requireLocation = false)))
        assertTrue(shouldRequireGps(null))
    }

    @Test
    fun `an opted-out school cannot produce a verified session even inside the radius`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 30f)
        val optedOut = school.copy(requireLocation = false)
        val result = evaluateGeofence(fix, optedOut)
        assertEquals(GeofenceReason.INSIDE, result.reason)

        val session = fix.toSessionLocation(result.honouring(optedOut))

        assertFalse(result.honouring(optedOut).verified)
        assertFalse(session.verified)
        // The real reading is still recorded: not faked, and not thrown away.
        assertEquals(19.2931, session.latitude, 0.0)
        assertEquals(-99.6557, session.longitude, 0.0)
        assertEquals(30.0, session.accuracyMeters!!, 0.0)
        assertEquals(result.distanceMeters!!, session.distanceMeters!!, 0.0)
    }

    @Test
    fun `honouring a school that requires GPS leaves the verdict untouched`() {
        val fix = LocationFix(latitude = 19.2931, longitude = -99.6557, accuracyMeters = 30f)
        val result = evaluateGeofence(fix, school)

        val honoured = result.honouring(school)

        assertEquals(result, honoured)
        assertTrue(fix.toSessionLocation(honoured).verified)
    }
}
