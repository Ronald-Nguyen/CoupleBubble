package com.aistudio.couplebubble.qxztrw.data.repository

import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.FirestoreSchema
import com.aistudio.couplebubble.qxztrw.data.remote.firestoreUnavailable
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.NoteMapper
import com.aistudio.couplebubble.qxztrw.data.remote.mapper.fields
import com.aistudio.couplebubble.qxztrw.data.remote.noteLabels
import com.aistudio.couplebubble.qxztrw.data.remote.notes
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.domain.repository.NotesRepository
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseNotesRepository(private val firebase: FirebaseProvider) : NotesRepository {

    private companion object {
        val WRITE_ACK_TIMEOUT = 2.seconds
    }

    private val db: FirebaseFirestore? get() = firebase.firestore

    override fun getNotes(coupleId: String): Flow<List<SharedNote>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.notes(coupleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep the last state: an empty list would look like everything was deleted
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { NoteMapper.fromFields(it.id, it.fields()) }.orEmpty())
            }

        awaitClose {
            registration.remove()
        }
    }
        // The server ack of a local write repeats the same notes; don't recompute the overview for it
        .distinctUntilChanged()

    override suspend fun addNote(coupleId: String, note: SharedNote): Result<SharedNote> {
        val firestore = db ?: return firestoreUnavailable()
        val noteId = note.id.ifBlank { UUID.randomUUID().toString() }
        return try {
            val write = firestore.notes(coupleId).document(noteId).set(NoteMapper.toFields(note))
            // The offline cache applies the write immediately; don't block on the server ack
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(note.copy(id = noteId))
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun updateNoteText(
        coupleId: String,
        noteId: String,
        title: String?,
        body: String?,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> {
        val fields = listOfNotNull(
            title?.let { FieldPath.of(FirestoreSchema.Note.TITLE) to it },
            body?.let { FieldPath.of(FirestoreSchema.Note.BODY) to it }
        )
        return updateNote(coupleId, noteId, updatedAt, updatedBy, *fields.toTypedArray())
    }

    override suspend fun setNotePinned(coupleId: String, noteId: String, pinned: Boolean, updatedAt: Long): Result<Unit> =
        updateNote(coupleId, noteId, updatedAt, updatedBy = null, FieldPath.of(FirestoreSchema.Note.PINNED) to pinned)

    override suspend fun setNoteLabel(
        coupleId: String,
        noteId: String,
        labelId: String?,
        updatedAt: Long
    ): Result<Unit> = updateNote(
        coupleId,
        noteId,
        updatedAt,
        updatedBy = null,
        FieldPath.of(FirestoreSchema.Note.LABEL_ID) to (labelId ?: FieldValue.delete())
    )

    override suspend fun replaceNoteContent(coupleId: String, note: SharedNote): Result<Unit> = updateNote(
        coupleId,
        note.id,
        note.updatedAt,
        note.updatedBy,
        FieldPath.of(FirestoreSchema.Note.TYPE) to note.type.name,
        FieldPath.of(FirestoreSchema.Note.BODY) to note.body,
        FieldPath.of(FirestoreSchema.Note.ITEMS) to NoteMapper.itemsMap(note.items)
    )

    override suspend fun deleteNote(coupleId: String, noteId: String): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            val delete = firestore.notes(coupleId).document(noteId).delete()
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { delete.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    override suspend fun addNoteItem(coupleId: String, noteId: String, item: NoteItem, updatedAt: Long, updatedBy: String?): Result<Unit> =
        updateNote(coupleId, noteId, updatedAt, updatedBy, FieldPath.of(FirestoreSchema.Note.ITEMS, item.id) to NoteMapper.itemFields(item))

    override suspend fun setNoteItemText(
        coupleId: String,
        noteId: String,
        itemId: String,
        text: String,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = updateNote(coupleId, noteId, updatedAt, updatedBy, FieldPath.of(FirestoreSchema.Note.ITEMS, itemId, FirestoreSchema.NoteItem.TEXT) to text)

    override suspend fun setNoteItemChecked(
        coupleId: String,
        noteId: String,
        itemId: String,
        checked: Boolean,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = updateNote(coupleId, noteId, updatedAt, updatedBy, FieldPath.of(FirestoreSchema.Note.ITEMS, itemId, FirestoreSchema.NoteItem.CHECKED) to checked)

    override suspend fun setNoteItemPositions(
        coupleId: String,
        noteId: String,
        positions: Map<String, Long>,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> {
        if (positions.isEmpty()) return Result.success(Unit)
        val updates = positions.map { (itemId, position) -> FieldPath.of(FirestoreSchema.Note.ITEMS, itemId, FirestoreSchema.NoteItem.POSITION) to position }
        return updateNote(coupleId, noteId, updatedAt, updatedBy, *updates.toTypedArray())
    }

    override suspend fun deleteNoteItems(
        coupleId: String,
        noteId: String,
        itemIds: Collection<String>,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> {
        if (itemIds.isEmpty()) return Result.success(Unit)
        val deletions = itemIds.map { FieldPath.of(FirestoreSchema.Note.ITEMS, it) to FieldValue.delete() }
        return updateNote(coupleId, noteId, updatedAt, updatedBy, *deletions.toTypedArray())
    }

    override fun getNoteLabels(coupleId: String): Flow<List<NoteLabel>?> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = firestore.noteLabels(coupleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep the last labels: null would fall back to the default labels
                    return@addSnapshotListener
                }
                trySend(NoteMapper.labelsFromFields(snapshot?.takeIf { it.exists() }?.fields()))
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun applyLabelChanges(
        coupleId: String,
        upserts: List<NoteLabel>,
        deletedIds: Set<String>
    ): Result<Unit> = writeLabels(
        coupleId,
        upserts.associate { it.id to NoteMapper.labelFields(it) } + deletedIds.associateWith { FieldValue.delete() }
    )

    /** A merged set only touches the given labels, so both partners can edit different labels at once. */
    private suspend fun writeLabels(coupleId: String, labels: Map<String, Any>): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            val write = firestore.noteLabels(coupleId).set(mapOf(FirestoreSchema.NoteLabels.LABELS to labels), SetOptions.merge())
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }

    /** Field-path updates leave every other item untouched, so concurrent edits of different items merge. */
    private suspend fun updateNote(
        coupleId: String,
        noteId: String,
        updatedAt: Long,
        updatedBy: String? = null,
        vararg fields: Pair<FieldPath, Any>
    ): Result<Unit> {
        val firestore = db ?: return firestoreUnavailable()
        return try {
            val ref: DocumentReference = firestore.notes(coupleId).document(noteId)
            val baseUpdates = buildList {
                add(FieldPath.of(FirestoreSchema.Note.UPDATED_AT))
                add(updatedAt)
                if (updatedBy != null) {
                    add(FieldPath.of(FirestoreSchema.Note.UPDATED_BY))
                    add(updatedBy)
                }
            }
            val moreUpdates = fields.flatMap { (path, value) -> listOf(path, value) }
            val allUpdates = (baseUpdates + moreUpdates).toTypedArray()
            val write = ref.update(allUpdates[0] as FieldPath, allUpdates[1], *allUpdates.drop(2).toTypedArray())
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            Result.failure(e)
        }
    }
}
