package com.example.aulasegura.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class SchoolScheduleTest {

    private fun site(entry: String?, exit: String?) = SchoolSite(
        schoolId = "sch-1",
        name = "Escuela",
        latitude = 19.2,
        longitude = -99.6,
        radiusMeters = 150.0,
        entryTime = entry,
        exitTime = exit,
    )

    @Test
    fun `a configured window blocks roll calls outside it`() {
        val school = site("07:00", "13:00")

        assertFalse(school.isWithinSchedule(LocalTime.of(6, 59)))
        assertTrue(school.isWithinSchedule(LocalTime.of(7, 0)))
        assertTrue(school.isWithinSchedule(LocalTime.of(10, 30)))
        assertTrue(school.isWithinSchedule(LocalTime.of(13, 0)))
        assertFalse(school.isWithinSchedule(LocalTime.of(13, 1)))
    }

    @Test
    fun `a missing or malformed window means no restriction`() {
        assertTrue(site(null, null).isWithinSchedule(LocalTime.of(3, 0)))
        assertTrue(site("", "").isWithinSchedule(LocalTime.of(3, 0)))
        assertTrue(site("malformed", "13:00").isWithinSchedule(LocalTime.of(3, 0)))
    }
}
