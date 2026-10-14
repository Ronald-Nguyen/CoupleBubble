package com.aistudio.couplebubble.qxztrw.core.ui

import com.aistudio.couplebubble.qxztrw.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
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

    @Test
    fun relativeTimeUnderOneMinuteIsJustNow() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 10, 14, 15, 30, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(UiText.Resource(R.string.relative_time_just_now), formatRelativeTime(now - 10_000, now, zone))
        assertEquals(UiText.Resource(R.string.relative_time_just_now), formatRelativeTime(now, now, zone))
        assertEquals(UiText.Resource(R.string.relative_time_just_now), formatRelativeTime(now + 5_000, now, zone))
    }

    @Test
    fun relativeTimeMinutesUsesPlurals() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 10, 14, 15, 30, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(UiText.Plural(R.plurals.relative_time_minutes, 1), formatRelativeTime(now - 60_000, now, zone))
        assertEquals(UiText.Plural(R.plurals.relative_time_minutes, 5), formatRelativeTime(now - 5 * 60_000, now, zone))
        assertEquals(UiText.Plural(R.plurals.relative_time_minutes, 59), formatRelativeTime(now - 59 * 60_000, now, zone))
    }

    @Test
    fun relativeTimeEarlierTodayUsesHours() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 10, 14, 15, 30, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(UiText.Plural(R.plurals.relative_time_hours, 1), formatRelativeTime(now - 3_600_000, now, zone))
        assertEquals(UiText.Plural(R.plurals.relative_time_hours, 3), formatRelativeTime(now - 3 * 3_600_000, now, zone))
    }

    @Test
    fun relativeTimeYesterdayShowsGestern() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 10, 14, 15, 30, 0, 0, zone).toInstant().toEpochMilli()
        val yesterday = ZonedDateTime.of(2026, 10, 13, 20, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(UiText.Resource(R.string.relative_time_yesterday), formatRelativeTime(yesterday, now, zone))
    }

    @Test
    fun relativeTimeAFewDaysAgoUsesDaysPlural() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 10, 14, 15, 30, 0, 0, zone).toInstant().toEpochMilli()
        val threeDaysAgo = ZonedDateTime.of(2026, 10, 11, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(UiText.Plural(R.plurals.relative_time_days, 3), formatRelativeTime(threeDaysAgo, now, zone))
    }

    @Test
    fun relativeTimeOlderFallsBackToFormattedDate() {
        val zone = ZoneId.of("Europe/Berlin")
        val now = ZonedDateTime.of(2026, 10, 14, 15, 30, 0, 0, zone).toInstant().toEpochMilli()
        val olderThisYear = ZonedDateTime.of(2026, 6, 5, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
        val lastYear = ZonedDateTime.of(2025, 6, 5, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(UiText.Resource(R.string.relative_time_date, listOf("5. Juni")), formatRelativeTime(olderThisYear, now, zone))
        assertEquals(UiText.Resource(R.string.relative_time_date, listOf("5. Juni 2025")), formatRelativeTime(lastYear, now, zone))
    }
}
