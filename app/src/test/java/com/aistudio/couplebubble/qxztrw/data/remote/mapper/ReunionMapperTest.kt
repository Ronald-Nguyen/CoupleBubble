package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ReunionMapperTest {

    @Test
    fun readsDatesAndPlacesByUid() {
        val reunion = requireNotNull(
            ReunionMapper.fromFields(
                mapOf(
                    "startDate" to "2025-10-24",
                    "endDate" to "2025-10-26",
                    "plannedAt" to "2025-10-01",
                    "places" to mapOf(
                        "uid_mia" to mapOf("sharesLocation" to true, "lat" to 50.9, "lng" to 7L, "locatedAt" to 123L),
                        "uid_leo" to mapOf("city" to "Berlin")
                    )
                )
            )
        )
        assertEquals(LocalDate.of(2025, 10, 1), reunion.plannedAtDate)
        assertEquals(GeoPoint(50.9, 7.0), reunion.places.getValue("uid_mia").location)
        assertEquals("Berlin", reunion.places.getValue("uid_leo").city)
        assertFalse(reunion.places.getValue("uid_leo").sharesLocation)
    }

    @Test
    fun brokenDatesReadAsNothingPlanned() {
        assertNull(ReunionMapper.fromFields(mapOf("startDate" to "2025-10-24")))
        assertNull(ReunionMapper.fromFields(mapOf("startDate" to "2025-10-24", "endDate" to "2025-10-20")))
    }

    @Test
    fun placeIsStoredRoundedAndWithoutCoordinatesWhenSharingIsOff() {
        val shared = ReunionMapper.placeToFields(
            ReunionPlace(sharesLocation = true, location = GeoPoint(50.9375, 6.9603), locatedAtMillis = 5L, city = " ")
        )
        assertEquals(mapOf("sharesLocation" to true, "lat" to 50.9, "lng" to 7.0, "locatedAt" to 5L), shared)

        val off = ReunionMapper.placeToFields(
            ReunionPlace(sharesLocation = false, location = GeoPoint(50.9, 7.0), locatedAtMillis = 5L, city = "Köln")
        )
        assertEquals(mapOf("sharesLocation" to false, "city" to "Köln"), off)
    }
}
