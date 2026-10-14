package com.aistudio.couplebubble.qxztrw.core.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatsTest {

    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun memoryDateOfTheCurrentYearLeavesTheYearOut() {
        assertEquals("5. Juni", formatMemoryDate(LocalDate.of(2026, 6, 5), today))
    }

    @Test
    fun memoryDateOfAnotherYearShowsTheYear() {
        assertEquals("5. Juni 2024", formatMemoryDate(LocalDate.of(2024, 6, 5), today))
        assertEquals("31. Dezember 2025", formatMemoryDate(LocalDate.of(2025, 12, 31), today))
    }
}
