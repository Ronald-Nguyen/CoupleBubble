package com.aistudio.couplebubble.qxztrw.data.repository

import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.data.remote.firestoreUnavailable
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.ReunionMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.fields
import com.aistudio.couplebubble.qxztrw.data.remote.reunion
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseReunionRepository(private val firebase: FirebaseProvider) : ReunionRepository {

    private companion object {
        // The offline cache applies writes immediately; don't block on the server ack longer than this
        val WRITE_ACK_TIMEOUT = 2.seconds
    }

    override fun getReunion(coupleId: String): Flow<Reunion?> = callbackFlow {
        val firestore = firebase.firestore
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = firestore.reunion(coupleId).addSnapshotListener { snapshot, error ->
            // Keep the last state on errors: null would look like the reunion was cancelled
            if (error != null || snapshot == null) return@addSnapshotListener
            trySend(if (snapshot.exists()) ReunionMapper.fromFields(snapshot.fields()) else null)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveReunionDates(
        coupleId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        plannedAtDate: LocalDate
    ): Result<Unit> = write { firestore ->
        firestore.reunion(coupleId).set(
            ReunionMapper.datesToFields(startDate, endDate, plannedAtDate, System.currentTimeMillis()),
            SetOptions.merge()
        )
    }

    override suspend fun saveOwnPlace(coupleId: String, uid: String, place: ReunionPlace): Result<Unit> =
        setOwnEntry(coupleId, uid, ReunionMapper.placeToFields(place))

    override suspend fun removeOwnPlace(coupleId: String, uid: String): Result<Unit> =
        setOwnEntry(coupleId, uid, FieldValue.delete())

    override suspend fun deleteReunion(coupleId: String): Result<Unit> = write { firestore ->
        firestore.reunion(coupleId).delete()
    }

    /** Replaces (or deletes) only this partner's entry, leaving the dates and the other entry untouched. */
    private suspend fun setOwnEntry(coupleId: String, uid: String, value: Any): Result<Unit> = write { firestore ->
        firestore.reunion(coupleId).set(
            mapOf(FirestoreSchema.Reunion.PLACES to mapOf(uid to value)),
            SetOptions.mergeFieldPaths(listOf(FieldPath.of(FirestoreSchema.Reunion.PLACES, uid)))
        )
    }

    private suspend fun write(block: (FirebaseFirestore) -> Task<Void>): Result<Unit> {
        val firestore = firebase.firestore ?: return firestoreUnavailable()
        return try {
            val task = block(firestore)
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { task.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }
}
