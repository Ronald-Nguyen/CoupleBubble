package com.aistudio.couplebubble.qxztrw.data.repository

import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.data.remote.RemoteImageUploader
import com.aistudio.couplebubble.qxztrw.data.remote.firestoreUnavailable
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.MemoryMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.SpaceMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.fields
import com.aistudio.couplebubble.qxztrw.data.remote.memories
import com.aistudio.couplebubble.qxztrw.data.remote.keepListening
import com.aistudio.couplebubble.qxztrw.data.remote.snapshotExecutor
import com.aistudio.couplebubble.qxztrw.data.remote.space
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.error.findCause
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.withPhoto
import com.aistudio.couplebubble.qxztrw.domain.repository.MemoryRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Moments with one photo slot per partner; which slot is "yours" is always decided from the server copy. */
class FirebaseMemoryRepository(
    private val firebase: FirebaseProvider,
    private val uploader: RemoteImageUploader,
    private val spaces: SpaceRepository
) : MemoryRepository {

    private companion object {
        const val STORAGE_FOLDER = "memories"

        // Reads and offline-cached writes wait this long for the server before going on
        val CACHE_FIRST_TIMEOUT = 2.seconds
        val STORAGE_DELETE_TIMEOUT = 10.seconds
        val TRANSACTION_TIMEOUT = 10.seconds
    }

    private class SavedMemory(val memory: Memory, val replacedUrl: String?)

    override fun getMemories(coupleId: String): Flow<List<Memory>> = callbackFlow {
        val firestore = firebase.firestore
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val stop = keepListening { onError ->
            firestore.memories(coupleId).addSnapshotListener(snapshotExecutor) { snapshot, error ->
                if (error != null) {
                    // Keep the last state: an empty list would look like everything was deleted
                    onError()
                    return@addSnapshotListener
                }
                val memories = snapshot?.documents
                    ?.mapNotNull { MemoryMapper.fromFields(it.id, it.fields()) }
                    ?.sortedByDescending { it.date }
                    .orEmpty()
                trySend(memories)
            }
        }

        awaitClose(stop)
    }

    override suspend fun addMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange
    ): Result<Memory> = saveMemory(coupleId, memory, fallbackRole, ownPhoto, isNew = true)

    override suspend fun updateMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange
    ): Result<Memory> = saveMemory(coupleId, memory, fallbackRole, ownPhoto, isNew = false)

    private suspend fun saveMemory(
        coupleId: String,
        memory: Memory,
        fallbackRole: PartnerRole?,
        ownPhoto: MemoryPhotoChange,
        isNew: Boolean
    ): Result<Memory> {
        val firestore = firebase.firestore ?: return firestoreUnavailable()
        val memoryId = memory.id.ifBlank { UUID.randomUUID().toString() }
        val memoryRef = firestore.memories(coupleId).document(memoryId)
        val uid = firebase.currentUid
        val creator = uid?.takeIf { isNew }
        val textFields = listOfNotNull(
            FirestoreSchema.Memory.ID to memoryId,
            FirestoreSchema.Memory.TITLE to memory.title,
            FirestoreSchema.Memory.DATE to memory.date.toString(),
            FirestoreSchema.Memory.NOTE to memory.note,
            (if (isNew) FirestoreSchema.Memory.CREATED_AT else FirestoreSchema.Memory.UPDATED_AT) to System.currentTimeMillis(),
            creator?.let { FirestoreSchema.Memory.CREATED_BY to it },
            // After creation the switch is only written alone (setMemoryCountsAsMilestone), never by text edits
            (FirestoreSchema.Memory.COUNTS_AS_MILESTONE to memory.countsAsMilestone).takeIf { isNew }
        ).toMap()
        val textOnly = memory.copy(id = memoryId, imageUrl = null, createdBy = creator ?: memory.createdBy)

        // Text edits never touch photo fields, so they can't overwrite what the partner changed meanwhile
        if (ownPhoto == MemoryPhotoChange.Keep) {
            return try {
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { memoryRef.set(textFields, SetOptions.merge()).await() }
                Result.success(if (isNew) textOnly.copy(partner1ImageUrl = null, partner2ImageUrl = null) else textOnly)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                Result.failure(e)
            }
        }

        val expectedRole = spaces.currentSpace.value?.editableRole(uid, fallbackRole)
            ?: return Result.failure(PartnerSlotUnknownException())
        val newUrl = (ownPhoto as? MemoryPhotoChange.Replace)?.let { replace ->
            uploader.uploadOrDataUri(coupleId, STORAGE_FOLDER, "${memoryId}_${expectedRole.fileSlot}", replace.bytes)
                ?: return Result.failure(IllegalStateException("Memory photo could not be encoded"))
        }

        // The slot is resolved from the server copy inside the transaction; a role swap (which writes the space
        // and every memory in one transaction) is therefore either fully before or fully after this write.
        val saved = try {
            withTimeoutOrNull(TRANSACTION_TIMEOUT) {
                firestore.runTransaction { transaction ->
                    val space = transaction.get(firestore.space(coupleId))
                    if (!space.exists()) throw PartnerSlotUnknownException()
                    val role = SpaceMapper.members(space.fields()).editableRole(uid, fallbackRole)
                        ?: throw PartnerSlotUnknownException()

                    val stored = transaction.get(memoryRef)
                    val (slot1, slot2) = if (stored.exists()) MemoryMapper.storedSlots(stored.fields()) else null to null
                    val replaced = if (role == PartnerRole.PARTNER_1) slot1 else slot2
                    val new1 = if (role == PartnerRole.PARTNER_1) newUrl else slot1
                    val new2 = if (role == PartnerRole.PARTNER_2) newUrl else slot2
                    transaction.set(
                        memoryRef,
                        textFields + mapOf(
                            FirestoreSchema.Memory.PARTNER_1_IMAGE to (new1 ?: FieldValue.delete()),
                            FirestoreSchema.Memory.PARTNER_2_IMAGE to (new2 ?: FieldValue.delete()),
                            FirestoreSchema.Memory.LEGACY_IMAGE_URL to FieldValue.delete()
                        ),
                        SetOptions.merge()
                    )
                    SavedMemory(
                        textOnly.copy(partner1ImageUrl = new1, partner2ImageUrl = new2),
                        replaced.takeIf { it != newUrl && it != new1 && it != new2 }
                    )
                }.await()
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            if (e.findCause<PartnerSlotUnknownException>() != null) {
                return Result.failure(PartnerSlotUnknownException())
            }
            null
        }

        if (saved != null) {
            uploader.delete(saved.replacedUrl)
            return Result.success(saved.memory)
        }

        // Offline: transactions need the server; queue a merge of the text and only the own slot known locally
        return try {
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) {
                memoryRef.set(
                    textFields + (MemoryMapper.slotField(expectedRole) to (newUrl ?: FieldValue.delete())),
                    SetOptions.merge()
                ).await()
            }
            Result.success(textOnly.withPhoto(expectedRole, newUrl))
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun setMemoryCountsAsMilestone(coupleId: String, memoryId: String, enabled: Boolean): Result<Unit> {
        val firestore = firebase.firestore ?: return firestoreUnavailable()
        return try {
            val write = firestore.memories(coupleId).document(memoryId)
                .update(FirestoreSchema.Memory.COUNTS_AS_MILESTONE, enabled)
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { write.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun deleteMemory(coupleId: String, memoryId: String): Result<Unit> {
        val firestore = firebase.firestore ?: return firestoreUnavailable()
        val memoryRef = firestore.memories(coupleId).document(memoryId)
        return try {
            val snapshot = try {
                withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { memoryRef.get().await() }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                null
            }
            // The document goes first: if the process dies now, no moment points at photos that are already gone
            withTimeoutOrNull(CACHE_FIRST_TIMEOUT) { memoryRef.delete().await() }

            // Photo cleanup is best effort and must not hold up the deletion (offline Storage calls can hang)
            withTimeoutOrNull(STORAGE_DELETE_TIMEOUT) {
                snapshot?.let { MemoryMapper.allPhotoUrls(it.fields()) }.orEmpty().forEach { uploader.delete(it) }
                // Fixed slot paths used before uploads got unique names
                for (legacyName in listOf("${memoryId}_a.jpg", "${memoryId}_b.jpg")) {
                    uploader.deletePath(coupleId, STORAGE_FOLDER, legacyName)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }
}
