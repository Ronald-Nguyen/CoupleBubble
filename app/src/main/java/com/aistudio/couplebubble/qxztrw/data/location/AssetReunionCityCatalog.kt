package com.aistudio.couplebubble.qxztrw.data.location

import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionCity
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionCityIndex
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionCityCatalog
import java.io.InputStream
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads the bundled `cities1000.tsv.gz` (built by `tools/build-city-index.py` from GeoNames, CC BY 4.0) once, on
 * [dispatcher]. [open] supplies the stream: the app opens the asset, tests open the file in the module folder.
 * The index is only built when the reunion dialog first asks for a city.
 */
class AssetReunionCityCatalog(
    private val open: () -> InputStream,
    private val dispatcher: CoroutineDispatcher
) : ReunionCityCatalog {

    private val mutex = Mutex()

    @Volatile
    private var index: ReunionCityIndex? = null

    override suspend fun suggestions(query: String, limit: Int): List<ReunionCity> = index().suggestions(query, limit)

    override suspend fun find(query: String): ReunionCity? = index().find(query)

    private suspend fun index(): ReunionCityIndex = index ?: mutex.withLock {
        index ?: withContext(dispatcher) { load() }.also { index = it }
    }

    private fun load(): ReunionCityIndex {
        val entries = open().use { stream ->
            GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).useLines { lines -> lines.mapNotNull(::parse).toList() }
        }
        return ReunionCityIndex(entries)
    }

    /** One line: name, ASCII name, country, latitude, longitude, population, comma-separated Latin alternate names. */
    private fun parse(line: String): ReunionCityIndex.Entry? {
        val columns = line.split('\t', limit = 7)
        if (columns.size < 6) return null
        val name = columns[0]
        val lat = columns[3].toDoubleOrNull() ?: return null
        val lng = columns[4].toDoubleOrNull() ?: return null
        val city = ReunionCity(
            name = name,
            country = columns[2],
            point = GeoPoint(lat, lng),
            population = columns[5].toLongOrNull() ?: 0L
        )
        val alternates = columns.getOrNull(6)?.split(',').orEmpty()
        val keys = (listOf(name, columns[1]) + alternates)
            .map(ReunionCityIndex::normalize)
            .filter { it.isNotEmpty() }
            .distinct()
        return ReunionCityIndex.Entry(city, keys)
    }

    companion object {
        const val ASSET = "cities1000.tsv.gz"
    }
}
