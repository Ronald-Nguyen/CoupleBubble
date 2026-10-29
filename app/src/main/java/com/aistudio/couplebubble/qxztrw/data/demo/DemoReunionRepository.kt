package com.aistudio.couplebubble.qxztrw.data.demo

import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The reunion of the demo mode, in memory per space, with the same "own entry only" writes as Firestore. The
 * seeded sample space starts with a planned visit and the simulated partner sharing a location.
 */
class DemoReunionRepository(
    today: LocalDate = LocalDate.now(),
    nowMillis: Long = System.currentTimeMillis()
) : ReunionRepository {
    private val reunions = MutableStateFlow(mapOf(DemoSeed.SPACE_ID to DemoSeed.reunion(today, nowMillis)))

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
