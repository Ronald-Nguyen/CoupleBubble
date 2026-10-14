package com.aistudio.couplebubble.qxztrw.domain.model

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryFilterTest {

    private val m1 = Memory(
        id = "m1",
        title = "Erster Urlaub am Meer",
        date = LocalDate.of(2023, 7, 15),
        note = "Barfuß am Strand bis zum Sonnenuntergang"
    )

    private val m2 = Memory(
        id = "m2",
        title = "Gemeinsam eingezogen",
        date = LocalDate.of(2024, 3, 1),
        note = "Kisten geschleppt und Pizza auf dem Boden gegessen"
    )

    private val m3 = Memory(
        id = "m3",
        title = "Konzert in Berlin",
        date = LocalDate.of(2024, 10, 24),
        note = "Lieblingsband live erlebt"
    )

    private val m4 = Memory(
        id = "m4",
        title = "Herbstspaziergang",
        date = LocalDate.of(2025, 11, 5),
        note = "Bunte Blätter im Park gesammelt"
    )

    private val allMemories = listOf(m1, m2, m3, m4)

    @Test
    fun nullOrBlankQueryReturnsAllMemoriesUnmodified() {
        assertEquals(allMemories, filterMemories(allMemories, null))
        assertEquals(allMemories, filterMemories(allMemories, ""))
        assertEquals(allMemories, filterMemories(allMemories, "   "))
    }

    @Test
    fun filtersByTitle() {
        val results = filterMemories(allMemories, "Urlaub")
        assertEquals(listOf(m1), results)

        val multipleResults = filterMemories(allMemories, "in")
        // "in" matches "Gemeinsam eingezogen" (eingezogen), "Konzert in Berlin" (in, Berlin)
        assertTrue(multipleResults.contains(m2))
        assertTrue(multipleResults.contains(m3))
        assertFalse(multipleResults.contains(m1))
    }

    @Test
    fun filtersByNote() {
        val results = filterMemories(allMemories, "Strand")
        assertEquals(listOf(m1), results)

        val pizzaResults = filterMemories(allMemories, "pizza")
        assertEquals(listOf(m2), pizzaResults)
    }

    @Test
    fun filtersByYear() {
        val results2023 = filterMemories(allMemories, "2023")
        assertEquals(listOf(m1), results2023)

        val results2024 = filterMemories(allMemories, "2024")
        assertEquals(listOf(m2, m3), results2024)

        val results2025 = filterMemories(allMemories, "2025")
        assertEquals(listOf(m4), results2025)

        val results2026 = filterMemories(allMemories, "2026")
        assertTrue(results2026.isEmpty())
    }

    @Test
    fun filtersByMonthNameInGerman() {
        // Juli (m1)
        val juliHits = filterMemories(allMemories, "Juli")
        assertEquals(listOf(m1), juliHits)

        // März (m2) - full name and case-insensitivity
        val maerzHits = filterMemories(allMemories, "märz")
        assertEquals(listOf(m2), maerzHits)

        // Umlaut tolerance: "Maerz" matches "März"
        val maerzAeHits = filterMemories(allMemories, "maerz")
        assertEquals(listOf(m2), maerzAeHits)

        // Oktober (m3)
        val oktoberHits = filterMemories(allMemories, "oktober")
        assertEquals(listOf(m3), oktoberHits)

        // Short month / abbreviation e.g. "Okt"
        val oktHits = filterMemories(allMemories, "okt")
        assertEquals(listOf(m3), oktHits)

        // November (m4)
        val novHits = filterMemories(allMemories, "November")
        assertEquals(listOf(m4), novHits)
    }

    @Test
    fun multiTermQueryRequiresAllTermsToMatchAcrossFields() {
        // "Urlaub 2023" -> title="Urlaub", year=2023 -> matches m1
        val hit1 = filterMemories(allMemories, "Urlaub 2023")
        assertEquals(listOf(m1), hit1)

        // "Urlaub 2024" -> "Urlaub" in m1 (2023), so no memory has both Urlaub and 2024
        val noHit = filterMemories(allMemories, "Urlaub 2024")
        assertTrue(noHit.isEmpty())

        // "Berlin Oktober" -> title="Konzert in Berlin", month="Oktober" -> matches m3
        val hit3 = filterMemories(allMemories, "Berlin Oktober")
        assertEquals(listOf(m3), hit3)

        // "Pizza März 2024" -> note="Pizza", month="März", year=2024 -> matches m2
        val hit2 = filterMemories(allMemories, "Pizza März 2024")
        assertEquals(listOf(m2), hit2)
    }

    @Test
    fun noMatchReturnsEmptyList() {
        val results = filterMemories(allMemories, "Paris")
        assertTrue(results.isEmpty())
    }

    @Test
    fun handlesMemoriesWithEmptyTitleOrNote() {
        val emptyTextMemory = Memory(
            id = "empty",
            title = "",
            date = LocalDate.of(2025, 5, 20),
            note = ""
        )
        val list = listOf(emptyTextMemory)

        assertTrue(filterMemories(list, "2025").contains(emptyTextMemory))
        assertTrue(filterMemories(list, "Mai").contains(emptyTextMemory))
        assertFalse(filterMemories(list, "Urlaub").contains(emptyTextMemory))
    }
}
