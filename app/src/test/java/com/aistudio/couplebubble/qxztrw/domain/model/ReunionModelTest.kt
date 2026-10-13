package com.aistudio.couplebubble.qxztrw.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReunionModelTest {

    private val now = 1_760_000_000_000L
    private val cologne = GeoPoint(50.94, 6.96)
    private val berlin = GeoPoint(52.52, 13.40)

    @Test
    fun roundingKeepsOneDecimal() {
        assertEquals(GeoPoint(50.9, 7.0), GeoPoint(50.9375, 6.9603).rounded())
        assertEquals(GeoPoint(-33.9, 151.2), GeoPoint(-33.8688, 151.2093).rounded())
    }

    @Test
    fun distanceCologneBerlinIsAboutFourHundredSeventyKm() {
        val km = cologne.distanceKmTo(berlin)
        assertTrue("was $km", km in 470.0..485.0)
    }

    @Test
    fun upcomingCountsDaysAndProgressFromPlanning() {
        val reunion = Reunion(LocalDate.of(2025, 10, 24), LocalDate.of(2025, 10, 26), plannedAtDate = LocalDate.of(2025, 10, 4))
        val status = reunion.status(LocalDate.of(2025, 10, 14)) as ReunionStatus.Upcoming
        assertEquals(10, status.daysUntil)
        assertEquals(3, status.durationDays)
        assertEquals(0.5f, status.progress, 0.001f)
    }

    @Test
    fun togetherCountsTheDayAndIsOverAfterTheEnd() {
        val reunion = Reunion(LocalDate.of(2025, 10, 24), LocalDate.of(2025, 10, 26))
        assertEquals(ReunionStatus.Together(day = 1, durationDays = 3), reunion.status(LocalDate.of(2025, 10, 24)))
        assertEquals(ReunionStatus.Together(day = 3, durationDays = 3), reunion.status(LocalDate.of(2025, 10, 26)))
        assertEquals(ReunionStatus.Over, reunion.status(LocalDate.of(2025, 10, 27)))
    }

    @Test
    fun distanceUsesFreshLocationThenCity() {
        val reunion = Reunion(
            LocalDate.of(2025, 10, 24),
            LocalDate.of(2025, 10, 26),
            places = mapOf(
                "me" to ReunionPlace(sharesLocation = true, location = cologne, locatedAtMillis = now),
                "partner" to ReunionPlace(city = "berlin")
            )
        )
        assertEquals(cologne.distanceKmTo(berlin), reunion.distanceKm("me", now)!!.toDouble(), 1.0)
        assertEquals(reunion.distanceKm("me", now), reunion.distanceKm("partner", now))
    }

    @Test
    fun staleLocationFallsBackToCityOrNothing() {
        val stale = now - ReunionRules.LOCATION_MAX_AGE_MILLIS - 1
        val withCity = ReunionPlace(sharesLocation = true, location = berlin, locatedAtMillis = stale, city = "Hamburg")
        assertEquals(ReunionPlaceSource.City("Hamburg"), withCity.source(now))
        assertEquals(ReunionPlaceSource.None, withCity.copy(city = null).source(now))
        assertNull(Reunion(LocalDate.MIN, LocalDate.MIN, places = mapOf("me" to withCity.copy(city = null))).distanceKm("me", now))
    }

    @Test
    fun locationSwitchedOffDoesNotCount() {
        val place = ReunionPlace(sharesLocation = false, location = berlin, locatedAtMillis = now)
        assertNull(place.freshLocation(now))
    }

    @Test
    fun citiesMatchIgnoringCaseAndUmlauts() {
        assertEquals("Köln", ReunionCities.find(" koln ")?.name)
        assertEquals("Düsseldorf", ReunionCities.find("DÜSSELDORF")?.name)
        assertEquals(listOf("Koblenz", "Köln", "Konstanz"), ReunionCities.suggestions("ko", limit = 3).map { it.name })
        assertTrue(ReunionCities.suggestions("").isEmpty())
        assertNull(ReunionCities.find("Atlantis"))
    }
}
