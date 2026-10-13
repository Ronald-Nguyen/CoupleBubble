package com.aistudio.couplebubble.qxztrw.domain.model

import java.text.Normalizer
import java.util.Locale

data class ReunionCity(val name: String, val point: GeoPoint)

/**
 * Built-in city list for the manual fallback: no network, no geocoder, no permission. Coordinates are city centers,
 * which is all an approximate distance needs.
 */
object ReunionCities {

    val all: List<ReunionCity> = listOf(
        "Aachen" to (50.78 to 6.08), "Augsburg" to (48.37 to 10.90), "Bamberg" to (49.89 to 10.89),
        "Bayreuth" to (49.95 to 11.58), "Berlin" to (52.52 to 13.40), "Bielefeld" to (52.02 to 8.53),
        "Bochum" to (51.48 to 7.22), "Bonn" to (50.74 to 7.10), "Braunschweig" to (52.27 to 10.52),
        "Bremen" to (53.08 to 8.80), "Bremerhaven" to (53.55 to 8.58), "Chemnitz" to (50.83 to 12.92),
        "Cottbus" to (51.76 to 14.33), "Darmstadt" to (49.87 to 8.65), "Dortmund" to (51.51 to 7.47),
        "Dresden" to (51.05 to 13.74), "Duisburg" to (51.43 to 6.76), "Düsseldorf" to (51.23 to 6.78),
        "Erfurt" to (50.98 to 11.03), "Erlangen" to (49.60 to 11.00), "Essen" to (51.46 to 7.01),
        "Flensburg" to (54.79 to 9.44), "Frankfurt am Main" to (50.11 to 8.68), "Frankfurt (Oder)" to (52.35 to 14.55),
        "Freiburg im Breisgau" to (47.99 to 7.84), "Fulda" to (50.55 to 9.68), "Gelsenkirchen" to (51.51 to 7.10),
        "Gera" to (50.88 to 12.08), "Gießen" to (50.58 to 8.68), "Göttingen" to (51.54 to 9.93),
        "Greifswald" to (54.09 to 13.38), "Hagen" to (51.36 to 7.47), "Halle (Saale)" to (51.48 to 11.97),
        "Hamburg" to (53.55 to 9.99), "Hamm" to (51.68 to 7.82), "Hannover" to (52.37 to 9.74),
        "Heidelberg" to (49.40 to 8.67), "Heilbronn" to (49.14 to 9.22), "Hildesheim" to (52.15 to 9.95),
        "Ingolstadt" to (48.77 to 11.43), "Jena" to (50.93 to 11.59), "Kaiserslautern" to (49.44 to 7.77),
        "Karlsruhe" to (49.01 to 8.40), "Kassel" to (51.31 to 9.48), "Kiel" to (54.32 to 10.13),
        "Koblenz" to (50.36 to 7.59), "Köln" to (50.94 to 6.96), "Konstanz" to (47.66 to 9.18),
        "Krefeld" to (51.33 to 6.56), "Leipzig" to (51.34 to 12.37), "Leverkusen" to (51.03 to 6.98),
        "Lübeck" to (53.87 to 10.69), "Ludwigshafen" to (49.48 to 8.44), "Lüneburg" to (53.25 to 10.41),
        "Magdeburg" to (52.12 to 11.63), "Mainz" to (50.00 to 8.27), "Mannheim" to (49.49 to 8.47),
        "Marburg" to (50.81 to 8.77), "Mönchengladbach" to (51.19 to 6.44), "Mülheim an der Ruhr" to (51.43 to 6.88),
        "München" to (48.14 to 11.58), "Münster" to (51.96 to 7.63), "Neubrandenburg" to (53.56 to 13.26),
        "Nürnberg" to (49.45 to 11.08), "Oberhausen" to (51.47 to 6.85), "Offenbach am Main" to (50.10 to 8.77),
        "Oldenburg" to (53.14 to 8.21), "Osnabrück" to (52.28 to 8.05), "Paderborn" to (51.72 to 8.75),
        "Passau" to (48.57 to 13.43), "Potsdam" to (52.39 to 13.06), "Regensburg" to (49.01 to 12.10),
        "Rostock" to (54.09 to 12.10), "Saarbrücken" to (49.24 to 6.99), "Schwerin" to (53.63 to 11.41),
        "Siegen" to (50.87 to 8.02), "Stralsund" to (54.31 to 13.09), "Stuttgart" to (48.78 to 9.18),
        "Trier" to (49.75 to 6.64), "Tübingen" to (48.52 to 9.06), "Ulm" to (48.40 to 9.99),
        "Wiesbaden" to (50.08 to 8.24), "Wolfsburg" to (52.42 to 10.79), "Wuppertal" to (51.26 to 7.15),
        "Würzburg" to (49.79 to 9.95), "Zwickau" to (50.72 to 12.50),
        // Austria
        "Wien" to (48.21 to 16.37), "Graz" to (47.07 to 15.44), "Linz" to (48.31 to 14.29),
        "Salzburg" to (47.81 to 13.06), "Innsbruck" to (47.27 to 11.40), "Klagenfurt" to (46.62 to 14.31),
        "Bregenz" to (47.50 to 9.75),
        // Switzerland
        "Zürich" to (47.38 to 8.54), "Basel" to (47.56 to 7.59), "Bern" to (46.95 to 7.45),
        "Luzern" to (47.05 to 8.31), "St. Gallen" to (47.42 to 9.38), "Genf" to (46.20 to 6.14),
        "Lausanne" to (46.52 to 6.63),
        // Neighbors couples often commute to
        "Amsterdam" to (52.37 to 4.90), "Brüssel" to (50.85 to 4.35), "Kopenhagen" to (55.68 to 12.57),
        "Luxemburg" to (49.61 to 6.13), "Paris" to (48.86 to 2.35), "Prag" to (50.08 to 14.44),
        "Warschau" to (52.23 to 21.01), "London" to (51.51 to -0.13)
    ).map { (name, coords) -> ReunionCity(name, GeoPoint(coords.first, coords.second)) }

    /** The city whose name matches [name] ignoring case, accents and spacing. */
    fun find(name: String): ReunionCity? {
        val key = normalize(name)
        if (key.isEmpty()) return null
        return all.firstOrNull { normalize(it.name) == key }
    }

    /** Up to [limit] cities starting with [query], then those containing it. */
    fun suggestions(query: String, limit: Int = 4): List<ReunionCity> {
        val key = normalize(query)
        if (key.isEmpty()) return emptyList()
        val starts = all.filter { normalize(it.name).startsWith(key) }
        val contains = all.filter { it !in starts && normalize(it.name).contains(key) }
        return (starts + contains).take(limit)
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.trim().lowercase(Locale.GERMAN), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace("ß", "ss")
        .replace(Regex("\\s+"), " ")
}
