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
    private val hamburg = GeoPoint(53.55, 9.99)

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
    fun distanceUsesFreshLocationThenChosenCity() {
        val reunion = Reunion(
            LocalDate.of(2025, 10, 24),
            LocalDate.of(2025, 10, 26),
            places = mapOf(
                "me" to ReunionPlace(sharesLocation = true, location = cologne, locatedAtMillis = now),
                "partner" to ReunionPlace(city = "Berlin, DE", cityLocation = berlin)
            )
        )
        assertEquals(cologne.distanceKmTo(berlin), reunion.distanceKm("me", now)!!.toDouble(), 1.0)
        assertEquals(reunion.distanceKm("me", now), reunion.distanceKm("partner", now))
    }

    @Test
    fun staleLocationFallsBackToCityOrNothing() {
        val stale = now - ReunionRules.LOCATION_MAX_AGE_MILLIS - 1
        val withCity = ReunionPlace(
            sharesLocation = true,
            location = berlin,
            locatedAtMillis = stale,
            city = "Hamburg, DE",
            cityLocation = hamburg
        )
        assertEquals(ReunionPlaceSource.City("Hamburg, DE"), withCity.source(now))
        assertEquals(ReunionPlaceSource.None, withCity.copy(city = null, cityLocation = null).source(now))
        assertNull(
            Reunion(LocalDate.MIN, LocalDate.MIN, places = mapOf("me" to withCity.copy(cityLocation = null))).distanceKm("me", now)
        )
    }

    @Test
    fun aCityWithoutCoordinatesCountsAsNothing() {
        val place = ReunionPlace(city = "Hamburg, DE")
        assertEquals(ReunionPlaceSource.None, place.source(now))
        assertNull(place.effectivePoint(now))
    }

    @Test
    fun locationSwitchedOffDoesNotCount() {
        val place = ReunionPlace(sharesLocation = false, location = berlin, locatedAtMillis = now)
        assertNull(place.freshLocation(now))
    }
}

class ReunionCityIndexTest {

    private val cologne = ReunionCity("Köln", "DE", GeoPoint(50.94, 6.96), 1_000_000)
    private val cologneTx = ReunionCity("Cologne", "US", GeoPoint(30.0, -97.0), 5_000)
    private val frankfurt = ReunionCity("Frankfurt am Main", "DE", GeoPoint(50.11, 8.68), 700_000)
    private val frankfurtOder = ReunionCity("Frankfurt (Oder)", "DE", GeoPoint(52.35, 14.55), 60_000)
    private val koblenz = ReunionCity("Koblenz", "DE", GeoPoint(50.36, 7.59), 110_000)

    /** Entries are listed largest population first, the order the asset file guarantees. */
    private val index = ReunionCityIndex(
        listOf(
            ReunionCityIndex.Entry(cologne, listOf("koln", "cologne")),
            ReunionCityIndex.Entry(frankfurt, listOf("frankfurt am main", "frankfurt")),
            ReunionCityIndex.Entry(koblenz, listOf("koblenz")),
            ReunionCityIndex.Entry(frankfurtOder, listOf("frankfurt oder", "frankfurt (oder)", "frankfurt")),
            ReunionCityIndex.Entry(cologneTx, listOf("cologne"))
        )
    )

    @Test
    fun normalizeIgnoresCaseAccentsAndSpacing() {
        assertEquals("koln", ReunionCityIndex.normalize(" KÖLN "))
        assertEquals("dusseldorf", ReunionCityIndex.normalize("Düsseldorf"))
        assertEquals("strasse", ReunionCityIndex.normalize("Straße"))
    }

    @Test
    fun findMatchesNamesAndAlternateSpellings() {
        assertEquals(cologne, index.find("köln"))
        assertEquals(cologne, index.find("Koln"))
        assertEquals(cologne, index.find("Köln, DE"))
        assertNull(index.find("Köln, US"))
        assertNull(index.find("Atlantis"))
        assertNull(index.find(""))
    }

    @Test
    fun ambiguousNamesPreferTheLargerCityUnlessACountryIsGiven() {
        assertEquals(frankfurt, index.find("Frankfurt"))
        assertEquals(cologne, index.find("Cologne, DE"))
        assertEquals(cologneTx, index.find("Cologne, US"))
    }

    @Test
    fun suggestionsStartWithTheQueryAndRankByPopulation() {
        assertEquals(listOf(frankfurt, frankfurtOder), index.suggestions("fra"))
        assertEquals(listOf(cologne, koblenz), index.suggestions("ko", limit = 2))
        assertEquals(listOf(cologne), index.suggestions("köl"))
    }

    @Test
    fun suggestionsFallBackToContainedMatches() {
        assertEquals(listOf(frankfurt), index.suggestions("main"))
        assertTrue(index.suggestions("   ").isEmpty())
        assertTrue(index.suggestions("xyz").isEmpty())
    }

    @Test
    fun labelWithCountryCodeSearchesByTheNamePart() {
        assertEquals(listOf(koblenz), index.suggestions("Koblenz, DE"))
    }
}
