package com.aistudio.couplebubble.qxztrw.domain.model

import java.time.format.TextStyle
import java.util.Locale

private val WHITESPACE = Regex("""\s+""")

/**
 * Filter and search logic for [Memory] items across title, personal note, year, and month name.
 */
object MemoryFilter {

    /**
     * Filters [memories] by matching all whitespace-separated terms of [query] case- and umlaut-insensitively
     * against title, personal note, year, and month name.
     * When [query] is null, empty, or blank, all [memories] are returned in their original order.
     */
    fun filter(
        memories: List<Memory>,
        query: String?,
        locale: Locale = Locale.GERMAN
    ): List<Memory> {
        if (query.isNullOrBlank()) return memories
        return memories.filter { matches(it, query, locale) }
    }

    /**
     * Whether [memory] matches all search terms in [query].
     * Each term must match in at least one of the searchable fields:
     * title, note, year, or month name (full and short in [locale] and German).
     */
    fun matches(
        memory: Memory,
        query: String?,
        locale: Locale = Locale.GERMAN
    ): Boolean {
        if (query.isNullOrBlank()) return true
        val terms = query.trim().split(WHITESPACE).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return true

        val searchableTexts = buildList {
            if (memory.title.isNotBlank()) add(memory.title)
            if (memory.note.isNotBlank()) add(memory.note)
            add(memory.date.year.toString())

            val monthFull = memory.date.month.getDisplayName(TextStyle.FULL, locale)
            add(monthFull)
            val monthShort = memory.date.month.getDisplayName(TextStyle.SHORT, locale)
            add(monthShort)
            add(monthShort.trimEnd('.'))

            if (locale != Locale.GERMAN) {
                val deMonthFull = memory.date.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                add(deMonthFull)
                val deMonthShort = memory.date.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                add(deMonthShort)
                add(deMonthShort.trimEnd('.'))
            }
        }

        if (searchableTexts.isEmpty()) return false

        return terms.all { term ->
            val pattern = NoteOrganizer.searchPatternForTerm(term)
            if (pattern != null) {
                searchableTexts.any { pattern.containsMatchIn(it) }
            } else {
                searchableTexts.any { it.contains(term, ignoreCase = true) }
            }
        }
    }
}

/** Pure function delegate for [MemoryFilter.filter]. */
fun filterMemories(
    memories: List<Memory>,
    query: String?,
    locale: Locale = Locale.GERMAN
): List<Memory> = MemoryFilter.filter(memories, query, locale)
