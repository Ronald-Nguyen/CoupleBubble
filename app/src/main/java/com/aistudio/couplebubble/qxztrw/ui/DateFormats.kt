package com.aistudio.couplebubble.qxztrw.ui

import java.time.format.DateTimeFormatter
import java.util.Locale

/** "25. Juni 2025", used for dates the user picks or reads in full. */
val GermanLongDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN)

/** "25.06.2025", for compact date fields. */
val GermanShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
