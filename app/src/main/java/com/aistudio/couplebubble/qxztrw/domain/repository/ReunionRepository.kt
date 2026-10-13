package com.aistudio.couplebubble.qxztrw.domain.repository

import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * The planned reunion of a space (`spaces/{id}/reunion/current`). Both partners edit the dates; each partner
 * writes only their own place entry, keyed by their auth UID.
 */
interface ReunionRepository {
    /** `null` while nothing is planned. Errors keep the last state. */
    fun getReunion(coupleId: String): Flow<Reunion?>
    suspend fun saveReunionDates(coupleId: String, startDate: LocalDate, endDate: LocalDate, plannedAtDate: LocalDate): Result<Unit>
    suspend fun saveOwnPlace(coupleId: String, uid: String, place: ReunionPlace): Result<Unit>
    suspend fun removeOwnPlace(coupleId: String, uid: String): Result<Unit>
    suspend fun deleteReunion(coupleId: String): Result<Unit>
}
