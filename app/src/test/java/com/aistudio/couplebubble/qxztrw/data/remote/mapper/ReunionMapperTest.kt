package com.aistudio.couplebubble.qxztrw.data.remote.mapper

import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
                        "uid_leo" to mapOf("sharesLocation" to false)
                    )
                )
            )
        )
        assertEquals(LocalDate.of(2025, 10, 1), reunion.plannedAtDate)
        assertEquals(GeoPoint(50.9, 7.0), reunion.places.getValue("uid_mia").location)
        assertFalse(reunion.places.getValue("uid_leo").sharesLocation)
        assertNull(reunion.places.getValue("uid_leo").location)
    }

    @Test
    fun brokenDatesReadAsNothingPlanned() {
        assertNull(ReunionMapper.fromFields(mapOf("startDate" to "2025-10-24")))
        assertNull(ReunionMapper.fromFields(mapOf("startDate" to "2025-10-24", "endDate" to "2025-10-20")))
    }

    @Test
    fun placeIsStoredRoundedAndWithoutCoordinatesWhenSharingIsOff() {
        val shared = ReunionMapper.placeToFields(
            ReunionPlace(sharesLocation = true, location = GeoPoint(50.9375, 6.9603), locatedAtMillis = 5L)
        )
        assertEquals(mapOf("sharesLocation" to true, "lat" to 50.9, "lng" to 7.0, "locatedAt" to 5L), shared)

        val off = ReunionMapper.placeToFields(
            ReunionPlace(sharesLocation = false, location = GeoPoint(50.9, 7.0), locatedAtMillis = 5L)
        )
        assertEquals(mapOf("sharesLocation" to false), off)
    }

    @Test
    fun oldCityFieldsAreIgnored() {
        val place = ReunionMapper.placeFromFields(mapOf("city" to "Berlin, DE", "cityLat" to 52.5, "cityLng" to 13.4))
        assertTrue(place.isEmpty)
    }
}
