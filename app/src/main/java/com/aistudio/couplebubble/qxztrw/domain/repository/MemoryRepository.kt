package com.aistudio.couplebubble.qxztrw.domain.repository

import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import kotlinx.coroutines.flow.Flow

/** The moments of a space (`spaces/{id}/memories`) with one photo slot per partner. */
interface MemoryRepository {
    fun getMemories(coupleId: String): Flow<List<Memory>>

    /**
     * Adds and edits moments. Only title, date and note of [memory] are written; its photo fields are ignored.
     * [ownPhoto] changes the signed-in user's slot, resolved from the auth UID when writing (like
     * [SpaceRepository.uploadProfilePhoto]); the partner's slot keeps whatever is stored. A photo change by a
     * user without a slot fails with [PartnerSlotUnknownException].
     */
    suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole? = null,
        ownPhoto: MemoryPhotoChange = MemoryPhotoChange.Keep,
    ): Result<Memory>
    suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole? = null,
        ownPhoto: MemoryPhotoChange = MemoryPhotoChange.Keep,
    ): Result<Memory>
    suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit>

    /** Shared switch whether the moment's yearly anniversaries count as milestones; writes only that field. */
    suspend fun setMemoryCountsAsMilestone(coupleId: String, memoryId: String, enabled: Boolean): Result<Unit>
}
