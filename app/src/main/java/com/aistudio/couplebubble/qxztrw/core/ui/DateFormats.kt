package com.aistudio.couplebubble.qxztrw.core.ui

import com.aistudio.couplebubble.qxztrw.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** "25. Juni 2025", used for dates the user picks or reads in full. */
val GermanLongDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN)

/** "5. Juni 2025", for the day a couple got together. */
val GermanDayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)

/** "25.06.2025", for compact date fields. */
val GermanShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)

private val GermanDayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)

/** A moment's date: "5. Juni" in the current year, "5. Juni 2024" otherwise. */
fun formatMemoryDate(date: LocalDate, today: LocalDate): String =
    date.format(if (date.year == today.year) GermanDayMonth else GermanDayMonthYear)

/**
 * Formats [epochMillis] relative to [now]:
 * - < 1 min: "gerade eben"
 * - < 60 min: "vor %d Min."
 * - today: "vor %d Std."
 * - yesterday: "gestern"
 * - 2..6 days ago: "vor %d Tagen"
 * - otherwise: "5. Juni" (current year) or "5. Juni 2024"
 */
fun formatRelativeTime(
    epochMillis: Long,
    now: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): UiText {
    val deltaMillis = now - epochMillis
    if (deltaMillis < 60_000L) {
        return UiText.Resource(R.string.relative_time_just_now)
    }
    if (deltaMillis < 3_600_000L) {
        val minutes = maxOf(1, (deltaMillis / 60_000L).toInt())
        return UiText.Plural(R.plurals.relative_time_minutes, minutes)
    }

    val eventDate = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()

    if (eventDate == today) {
        val hours = maxOf(1, (deltaMillis / 3_600_000L).toInt())
        return UiText.Plural(R.plurals.relative_time_hours, hours)
    }

    if (eventDate == today.minusDays(1)) {
        return UiText.Resource(R.string.relative_time_yesterday)
    }

    val daysBetween = ChronoUnit.DAYS.between(eventDate, today)
    if (daysBetween in 2..6) {
        return UiText.Plural(R.plurals.relative_time_days, daysBetween.toInt())
    }

    return UiText.Resource(R.string.relative_time_date, listOf(formatMemoryDate(eventDate, today)))
}
