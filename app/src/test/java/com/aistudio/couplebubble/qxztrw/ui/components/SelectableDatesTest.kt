package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class SelectableDatesTest {

    private val today = LocalDate.of(2026, 10, 14)

    private fun millis(date: LocalDate) = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun maxBlocksTheFutureButKeepsToday() {
        val dates = selectableDatesBetween(max = today)
        assertTrue(dates.isSelectableDate(millis(today)))
        assertTrue(dates.isSelectableDate(millis(today.minusYears(3))))
        assertFalse(dates.isSelectableDate(millis(today.plusDays(1))))
        assertFalse(dates.isSelectableYear(2027))
    }

    @Test
    fun minBlocksThePastButKeepsToday() {
        val dates = selectableDatesBetween(min = today)
        assertTrue(dates.isSelectableDate(millis(today)))
        assertFalse(dates.isSelectableDate(millis(today.minusDays(1))))
        assertFalse(dates.isSelectableYear(2025))
    }

    @Test
    fun withoutBoundsEveryDayIsSelectable() {
        val dates = selectableDatesBetween()
        assertTrue(dates.isSelectableDate(millis(LocalDate.of(1990, 1, 1))))
        assertTrue(dates.isSelectableDate(millis(LocalDate.of(2090, 1, 1))))
    }
}
