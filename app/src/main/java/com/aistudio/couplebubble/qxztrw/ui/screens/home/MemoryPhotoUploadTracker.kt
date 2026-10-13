package com.aistudio.couplebubble.qxztrw.ui.screens.home

import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Local-first copies of memories whose photos are still uploading, and the badge state of each uploading slot.
 * The timeline shows [pendingMemories] in place of the remote version until the upload returns.
 */
class MemoryPhotoUploadTracker(private val syncedBadgeMillis: Long = 1_500L) {

    private val _pendingMemories = MutableStateFlow<Map<String, Memory>>(emptyMap())
    val pendingMemories: StateFlow<Map<String, Memory>> = _pendingMemories.asStateFlow()

    private val _syncStates = MutableStateFlow<Map<PhotoSlotKey, PhotoSyncState>>(emptyMap())
    val syncStates: StateFlow<Map<PhotoSlotKey, PhotoSyncState>> = _syncStates.asStateFlow()

    val hasPendingUploads: Boolean get() = _pendingMemories.value.isNotEmpty()

    /** [remote] with every pending local copy in place of its stored version, newest first. */
    fun visibleMemories(remote: List<Memory>, pending: Map<String, Memory>): List<Memory> =
        if (pending.isEmpty()) remote else (remote.filterNot { it.id in pending } + pending.values).sortedByDescending { it.date }

    /**
     * Shows [localMemory] with an upload badge on [uploadingSlot] until [upload] returns. A slot that ends up with
     * a Firebase Storage HTTPS URL briefly switches to SYNCED; a Base64 fallback or a failure just drops the badge.
     */
    suspend fun track(
        localMemory: Memory,
        uploadingSlot: PartnerRole?,
        upload: suspend () -> Result<Memory>
    ): Result<Memory> {
        val slot = uploadingSlot?.let { PhotoSlotKey(localMemory.id, isPartner1 = it == PartnerRole.PARTNER_1) }
            ?: return upload()

        _pendingMemories.update { it + (localMemory.id to localMemory) }
        _syncStates.update { it + (slot to PhotoSyncState.UPLOADING) }

        val result = upload()
        val saved = result.getOrNull()
        val url = if (slot.isPartner1) saved?.partner1ImageUrl else saved?.partner2ImageUrl
        val confirmed = url?.startsWith("https://") == true

        _pendingMemories.update { pending ->
            if (pending[localMemory.id] === localMemory) pending - localMemory.id else pending
        }
        _syncStates.update { states -> if (confirmed) states + (slot to PhotoSyncState.SYNCED) else states - slot }

        if (confirmed) {
            delay(syncedBadgeMillis)
            // A newer upload of the same slot may have started meanwhile; leave its badge alone
            _syncStates.update { states -> if (states[slot] == PhotoSyncState.SYNCED) states - slot else states }
        }
        return result
    }
}
