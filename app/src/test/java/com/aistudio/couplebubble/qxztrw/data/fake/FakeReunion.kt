package com.aistudio.couplebubble.qxztrw.data.fake

import com.aistudio.couplebubble.qxztrw.data.location.LocationProvider
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory reunions per space, with the same "own entry only" writes as Firestore. */
class MockReunionRepository : ReunionRepository {
    val reunions = MutableStateFlow<Map<String, Reunion>>(emptyMap())

    /** Places written before any dates exist, like a Firestore merge into a missing document. */
    private val orphanPlaces = mutableMapOf<String, MutableMap<String, ReunionPlace>>()

    override fun getReunion(coupleId: String): Flow<Reunion?> = reunions.map { it[coupleId] }

    override suspend fun saveReunionDates(
        coupleId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        plannedAtDate: LocalDate
    ): Result<Unit> {
        reunions.update {
            val places = it[coupleId]?.places ?: orphanPlaces.remove(coupleId).orEmpty()
            it + (coupleId to Reunion(startDate, endDate, plannedAtDate, places))
        }
        return Result.success(Unit)
    }

    override suspend fun saveOwnPlace(coupleId: String, uid: String, place: ReunionPlace): Result<Unit> {
        val stored = place.copy(location = place.location?.rounded())
        val reunion = reunions.value[coupleId]
        if (reunion == null) {
            orphanPlaces.getOrPut(coupleId) { mutableMapOf() }[uid] = stored
        } else {
            reunions.update { it + (coupleId to reunion.copy(places = reunion.places + (uid to stored))) }
        }
        return Result.success(Unit)
    }

    override suspend fun removeOwnPlace(coupleId: String, uid: String): Result<Unit> {
        reunions.update { map -> map[coupleId]?.let { map + (coupleId to it.copy(places = it.places - uid)) } ?: map }
        return Result.success(Unit)
    }

    override suspend fun deleteReunion(coupleId: String): Result<Unit> {
        reunions.update { it - coupleId }
        return Result.success(Unit)
    }
}

/** Returns [location] while [granted]; counts how often a fix was asked for. */
class FakeLocationProvider(
    var granted: Boolean = true,
    var location: GeoPoint? = GeoPoint(50.9375, 6.9603)
) : LocationProvider {
    var requests = 0

    override fun hasPermission(): Boolean = granted

    override suspend fun currentApproximateLocation(): GeoPoint? {
        requests++
        return if (granted) location else null
    }
}
