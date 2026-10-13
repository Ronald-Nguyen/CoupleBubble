package com.aistudio.couplebubble.qxztrw.data.location

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs against the bundled `cities1000.tsv.gz` in `src/main/assets`, so a broken or missing city list fails here.
 * Unit tests don't see the app's assets, so the file is opened from the module folder instead.
 */
class AssetReunionCityCatalogTest {

    private val catalog = AssetReunionCityCatalog(
        { File("src/main/assets/${AssetReunionCityCatalog.ASSET}").inputStream() },
        Dispatchers.Unconfined
    )

    @Test
    fun findsGermanCitiesByNameAndSpelling() = runTest {
        val berlin = requireNotNull(catalog.find("Berlin, DE"))
        assertEquals("DE", berlin.country)
        assertTrue(berlin.point.latitude in 52.3..52.7)

        val cologne = requireNotNull(catalog.find("Köln"))
        assertTrue(cologne.point.latitude in 50.8..51.0)
        assertEquals(cologne, catalog.find("Koln, DE"))
    }

    @Test
    fun findsSmallPlacesWorldwide() = runTest {
        val city = catalog.find("Shanghai, CN")
        assertTrue(requireNotNull(city).point.latitude in 30.0..32.0)
        assertNotNull(catalog.find("Reykjavík"))
        assertNull(catalog.find("Zzyzxqwerthausen"))
    }

    @Test
    fun suggestionsOfferTheLargestMatchesFirst() = runTest {
        val suggestions = catalog.suggestions("Frankf", limit = 3)
        assertEquals(3, suggestions.size)
        assertTrue(suggestions.first().name.startsWith("Frankfurt"))
        assertTrue(suggestions.first().population >= suggestions.last().population)
    }
}
