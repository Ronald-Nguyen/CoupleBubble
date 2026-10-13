package com.aistudio.couplebubble.qxztrw.domain.model

import java.text.Normalizer
import java.util.Locale

/** A city from the bundled list. [label] ("Köln, DE") is what gets stored and shown, since many names repeat. */
data class ReunionCity(val name: String, val country: String, val point: GeoPoint, val population: Long) {
    val label: String get() = "$name, $country"
}

/**
 * Search over the bundled city list. [entries] must be sorted by population, largest first: that order decides
 * which match wins, so "Frankfurt" finds Frankfurt am Main before Frankfurt (Oder).
 */
class ReunionCityIndex(private val entries: List<Entry>) {

    /** A city with its normalized search keys: name, ASCII name and the Latin alternate names. */
    class Entry(val city: ReunionCity, val keys: List<String>)

    /** The city a typed value refers to: a name, optionally followed by a country code ("Berlin, DE"). */
    fun find(query: String): ReunionCity? {
        val (namePart, country) = splitLabel(query)
        val key = normalize(namePart)
        if (key.isEmpty()) return null
        return entries.firstOrNull { entry ->
            (country == null || entry.city.country == country) && key in entry.keys
        }?.city
    }

    /** Up to [limit] cities whose name starts with [query], then those that merely contain it. */
    fun suggestions(query: String, limit: Int = 5): List<ReunionCity> {
        val key = normalize(splitLabel(query).first)
        if (key.isEmpty()) return emptyList()
        val prefix = mutableListOf<ReunionCity>()
        for (entry in entries) {
            if (prefix.size >= limit) break
            if (entry.keys.any { it.startsWith(key) }) prefix += entry.city
        }
        if (prefix.size >= limit) return prefix
        val contains = entries.asSequence()
            .filter { entry -> entry.keys.none { it.startsWith(key) } && entry.keys.any { key in it } }
            .map { it.city }
            .take(limit - prefix.size)
        return prefix + contains
    }

    companion object {
        /** Lowercase, accents and spacing removed, so "koln", "Köln" and " KÖLN " are the same key. */
        fun normalize(value: String): String = Normalizer.normalize(value.trim().lowercase(Locale.GERMAN), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace("ß", "ss")
            .replace(Regex("\\s+"), " ")

        /** Splits "Berlin, DE" into the name and an uppercase country code; the code is optional. */
        private fun splitLabel(query: String): Pair<String, String?> {
            val comma = query.lastIndexOf(',')
            if (comma < 0) return query to null
            val country = query.substring(comma + 1).trim().uppercase(Locale.ROOT).takeIf { it.length == 2 }
            return if (country == null) query to null else query.substring(0, comma) to country
        }
    }
}
