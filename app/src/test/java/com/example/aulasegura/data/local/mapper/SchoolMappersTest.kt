package com.example.aulasegura.data.local.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The school document is the source of the geofence, and every way it can be
 * wrong is silent: a `location` that arrives as a nested map, a radius that
 * arrives as `Long`, a school with no location at all. The fallback radius is
 * the one that keeps a misconfigured school from locking the whole staff out.
 */
class SchoolMappersTest {

    @Test
    fun `the nested location map becomes the site centre`() {
        val entity = schoolEntityFrom(
            id = "sch-benito-juarez",
            data = mapOf(
                "name" to "Escuela Primaria Benito Juárez",
                "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
                "radiusMeters" to 150,
            ),
        )!!

        assertEquals(19.2926, entity.latitude!!, 0.0)
        assertEquals(-99.6557, entity.longitude!!, 0.0)
        assertEquals(150.0, entity.radiusMeters!!, 0.0)
    }

    @Test
    fun `a document without a name is skipped`() {
        val entity = schoolEntityFrom("sch-x", mapOf("radiusMeters" to 150))

        assertNull(entity)
    }

    @Test
    fun `a school without location has no usable site`() {
        val entity = schoolEntityFrom(
            id = "sch-benito-juarez",
            data = mapOf("name" to "Escuela", "radiusMeters" to 150),
        )!!

        assertNull(entity.toSchoolSite(fallbackRadiusMeters = 120.0))
    }

    @Test
    fun `the document radius is used when it is sane`() {
        val site = schoolEntityFrom(
            id = "sch-benito-juarez",
            data = mapOf(
                "name" to "Escuela",
                "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
                "radiusMeters" to 150,
            ),
        )!!.toSchoolSite(fallbackRadiusMeters = 120.0)

        assertEquals(150.0, site?.radiusMeters!!, 0.0)
    }

    @Test
    fun `a missing radius falls back to the validated default`() {
        val site = schoolEntityFrom(
            id = "sch-benito-juarez",
            data = mapOf(
                "name" to "Escuela",
                "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
            ),
        )!!.toSchoolSite(fallbackRadiusMeters = 120.0)

        assertEquals(120.0, site?.radiusMeters!!, 0.0)
    }

    @Test
    fun `a zero radius does not lock the school out`() {
        val site = schoolEntityFrom(
            id = "sch-benito-juarez",
            data = mapOf(
                "name" to "Escuela",
                "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
                "radiusMeters" to 0,
            ),
        )!!.toSchoolSite(fallbackRadiusMeters = 120.0)

        assertEquals(120.0, site?.radiusMeters!!, 0.0)
    }

    @Test
    fun `an absurd radius is replaced by the default`() {
        val site = schoolEntityFrom(
            id = "sch-benito-juarez",
            data = mapOf(
                "name" to "Escuela",
                "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
                "radiusMeters" to 99_999,
            ),
        )!!.toSchoolSite(fallbackRadiusMeters = 120.0)

        assertEquals(120.0, site?.radiusMeters!!, 0.0)
    }

    @Test
    fun `the requireLocation flag round-trips from Firestore to the cached site`() {
        for (flag in listOf(true, false)) {
            val entity = schoolEntityFrom(
                id = "sch-benito-juarez",
                data = mapOf(
                    "name" to "Escuela",
                    "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
                    "radiusMeters" to 150,
                    "requireLocation" to flag,
                ),
            )!!

            assertEquals(flag, entity.requireLocation)
            assertEquals(flag, entity.toSchoolSite(fallbackRadiusMeters = 120.0)!!.requireLocation)
        }
    }

    @Test
    fun `a document without a usable flag keeps requiring GPS`() {
        val base = mapOf(
            "name" to "Escuela",
            "location" to mapOf("lat" to 19.2926, "lng" to -99.6557),
        )
        val missing = schoolEntityFrom("sch-benito-juarez", base)!!
        val malformed = schoolEntityFrom("sch-benito-juarez", base + ("requireLocation" to "false"))!!

        assertTrue(missing.requireLocation)
        assertTrue(missing.toSchoolSite(fallbackRadiusMeters = 120.0)!!.requireLocation)
        assertTrue(malformed.requireLocation)
    }
}
