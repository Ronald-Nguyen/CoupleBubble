package com.aistudio.couplebubble.qxztrw.data.repository

import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.data.remote.firestoreUnavailable
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.MilestoneMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.fields
import com.aistudio.couplebubble.qxztrw.data.remote.milestones
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.repository.MilestoneRepository
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseMilestoneRepository(private val firebase: FirebaseProvider) : MilestoneRepository {

    private companion object {
        // The offline cache applies writes immediately; don't block on the server ack longer than this
        val WRITE_ACK_TIMEOUT = 2.seconds
    }

    override fun getCustomMilestones(coupleId: String): Flow<List<CustomMilestone>> = callbackFlow {
        val firestore = firebase.firestore
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.milestones(coupleId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Keep the last state: an empty list would look like everything was deleted
                return@addSnapshotListener
            }
            val milestones = snapshot?.documents
                ?.mapNotNull { MilestoneMapper.fromFields(it.id, it.fields()) }
                ?.sortedBy { it.date }
                .orEmpty()
            trySend(milestones)
        }

        awaitClose { registration.remove() }
    }

    override suspend fun addCustomMilestone(coupleId: String, milestone: CustomMilestone): Result<CustomMilestone> {
        val firestore = firebase.firestore ?: return firestoreUnavailable()
        val milestoneId = milestone.id.ifBlank { UUID.randomUUID().toString() }
        return try {
            val write = firestore.milestones(coupleId).document(milestoneId)
                .set(MilestoneMapper.toFields(milestone, createdAt = System.currentTimeMillis()))
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(milestone.copy(id = milestoneId))
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun setCustomMilestoneEnabled(coupleId: String, milestoneId: String, enabled: Boolean): Result<Unit> {
        val firestore = firebase.firestore ?: return firestoreUnavailable()
        return try {
            val write = firestore.milestones(coupleId).document(milestoneId)
                .update(FirestoreSchema.Milestone.ENABLED, enabled)
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }
}
