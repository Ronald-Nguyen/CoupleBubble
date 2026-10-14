package com.aistudio.couplebubble.qxztrw.core.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
