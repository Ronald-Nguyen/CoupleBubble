package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.NoteType
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class FirebaseNotesRepository : NotesRepository {

    private companion object {
        const val FIRESTORE_UNAVAILABLE = "Firestore uninitialized"
        val WRITE_ACK_TIMEOUT = 2.seconds
    }

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private fun notesCollection(firestore: FirebaseFirestore, coupleId: String) =
        firestore.collection(FirestoreSchema.SPACES).document(coupleId).collection(FirestoreSchema.NOTES)

    override fun getNotes(coupleId: String): Flow<List<SharedNote>> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = notesCollection(firestore, coupleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { parseNote(it) } ?: emptyList())
            }

        awaitClose {
            registration.remove()
        }
    }

    private fun parseNote(doc: DocumentSnapshot): SharedNote? = try {
        val items = (doc.get("items") as? Map<*, *>)?.mapNotNull { (key, value) ->
            val itemId = key as? String ?: return@mapNotNull null
            val fields = value as? Map<*, *> ?: return@mapNotNull null
            NoteItem(
                id = itemId,
                text = fields["text"] as? String ?: "",
                checked = fields["checked"] as? Boolean ?: false,
                createdBy = fields["createdBy"] as? String,
                position = (fields["position"] as? Number)?.toLong() ?: 0L
            )
        } ?: emptyList()

        SharedNote(
            id = doc.id,
            title = doc.getString("title") ?: "",
            body = doc.getString("body") ?: "",
            type = NoteType.entries.firstOrNull { it.name == doc.getString("type") } ?: NoteType.TEXT,
            // Stored as "category" since labels used to be fixed categories; their IDs stayed the same
            labelId = doc.getString("category"),
            pinned = doc.getBoolean("pinned") ?: false,
            items = items,
            createdBy = doc.getString("createdBy"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            updatedAt = doc.getLong("updatedAt") ?: 0L
        )
    } catch (_: Exception) {
        null
    }

    private fun itemFields(item: NoteItem): Map<String, Any?> = mapOf(
        "text" to item.text,
        "checked" to item.checked,
        "createdBy" to item.createdBy,
        "position" to item.position
    )

    private fun itemsMap(items: List<NoteItem>): Map<String, Map<String, Any?>> =
        items.associate { it.id to itemFields(it) }

    override suspend fun addNote(coupleId: String, note: SharedNote): Result<SharedNote> {
        val firestore = db ?: return Result.failure(IllegalStateException(FIRESTORE_UNAVAILABLE))
        val noteId = note.id.ifBlank { UUID.randomUUID().toString() }
        return try {
            val write = notesCollection(firestore, coupleId).document(noteId).set(
                mapOf(
                    "title" to note.title,
                    "body" to note.body,
                    "type" to note.type.name,
                    "category" to note.labelId,
                    "pinned" to note.pinned,
                    "items" to itemsMap(note.items),
                    "createdBy" to note.createdBy,
                    "createdAt" to note.createdAt,
                    "updatedAt" to note.updatedAt
                )
            )
            // The offline cache applies the write immediately; don't block on the server ack
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(note.copy(id = noteId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateNoteText(
        coupleId: String,
        noteId: String,
        title: String?,
        body: String?,
        updatedAt: Long
    ): Result<Unit> {
        val fields = listOfNotNull(
            title?.let { FieldPath.of("title") to it },
            body?.let { FieldPath.of("body") to it }
        )
        return updateNote(coupleId, noteId, updatedAt, *fields.toTypedArray())
    }

    override suspend fun setNotePinned(coupleId: String, noteId: String, pinned: Boolean, updatedAt: Long): Result<Unit> =
        updateNote(coupleId, noteId, updatedAt, FieldPath.of("pinned") to pinned)

    override suspend fun setNoteLabel(
        coupleId: String,
        noteId: String,
        labelId: String?,
        updatedAt: Long
    ): Result<Unit> = updateNote(
        coupleId,
        noteId,
        updatedAt,
        FieldPath.of("category") to (labelId ?: FieldValue.delete())
    )

    override suspend fun replaceNoteContent(coupleId: String, note: SharedNote): Result<Unit> = updateNote(
        coupleId,
        note.id,
        note.updatedAt,
        FieldPath.of("type") to note.type.name,
        FieldPath.of("body") to note.body,
        FieldPath.of("items") to itemsMap(note.items)
    )

    override suspend fun deleteNote(coupleId: String, noteId: String): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException(FIRESTORE_UNAVAILABLE))
        return try {
            val delete = notesCollection(firestore, coupleId).document(noteId).delete()
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { delete.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addNoteItem(coupleId: String, noteId: String, item: NoteItem, updatedAt: Long): Result<Unit> =
        updateNote(coupleId, noteId, updatedAt, FieldPath.of("items", item.id) to itemFields(item))

    override suspend fun setNoteItemText(
        coupleId: String,
        noteId: String,
        itemId: String,
        text: String,
        updatedAt: Long
    ): Result<Unit> = updateNote(coupleId, noteId, updatedAt, FieldPath.of("items", itemId, "text") to text)

    override suspend fun setNoteItemChecked(
        coupleId: String,
        noteId: String,
        itemId: String,
        checked: Boolean,
        updatedAt: Long
    ): Result<Unit> = updateNote(coupleId, noteId, updatedAt, FieldPath.of("items", itemId, "checked") to checked)

    override suspend fun deleteNoteItems(
        coupleId: String,
        noteId: String,
        itemIds: Collection<String>,
        updatedAt: Long
    ): Result<Unit> {
        if (itemIds.isEmpty()) return Result.success(Unit)
        val deletions = itemIds.map { FieldPath.of("items", it) to FieldValue.delete() }
        return updateNote(coupleId, noteId, updatedAt, *deletions.toTypedArray())
    }

    private fun labelsDocument(firestore: FirebaseFirestore, coupleId: String) =
        firestore.collection(FirestoreSchema.SPACES).document(coupleId)
            .collection(FirestoreSchema.SETTINGS).document(FirestoreSchema.NOTE_LABELS_DOC)

    override fun getNoteLabels(coupleId: String): Flow<List<NoteLabel>?> = callbackFlow {
        val firestore = db
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = labelsDocument(firestore, coupleId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val labels = snapshot?.takeIf { it.exists() }?.get("labels") as? Map<*, *>
                trySend(labels?.mapNotNull { (key, value) ->
                    val labelId = key as? String ?: return@mapNotNull null
                    val fields = value as? Map<*, *> ?: return@mapNotNull null
                    NoteLabel(
                        id = labelId,
                        name = fields["name"] as? String ?: return@mapNotNull null,
                        position = (fields["position"] as? Number)?.toLong() ?: 0L
                    )
                })
            }

        awaitClose {
            registration.remove()
        }
    }

    override suspend fun saveNoteLabels(coupleId: String, labels: List<NoteLabel>): Result<Unit> =
        writeLabels(coupleId, labels.associate { it.id to labelFields(it) })

    override suspend fun deleteNoteLabel(coupleId: String, labelId: String, labelsToKeep: List<NoteLabel>): Result<Unit> =
        writeLabels(coupleId, labelsToKeep.associate { it.id to labelFields(it) } + (labelId to FieldValue.delete()))

    private fun labelFields(label: NoteLabel): Map<String, Any> = mapOf(
        "name" to label.name,
        "position" to label.position
    )

    /** A merged set only touches the given labels, so both partners can edit different labels at once. */
    private suspend fun writeLabels(coupleId: String, labels: Map<String, Any>): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException(FIRESTORE_UNAVAILABLE))
        return try {
            val write = labelsDocument(firestore, coupleId).set(mapOf("labels" to labels), SetOptions.merge())
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Field-path updates leave every other item untouched, so concurrent edits of different items merge. */
    private suspend fun updateNote(
        coupleId: String,
        noteId: String,
        updatedAt: Long,
        vararg fields: Pair<FieldPath, Any>
    ): Result<Unit> {
        val firestore = db ?: return Result.failure(IllegalStateException(FIRESTORE_UNAVAILABLE))
        return try {
            val ref: DocumentReference = notesCollection(firestore, coupleId).document(noteId)
            val moreFieldsAndValues = fields.flatMap { (path, value) -> listOf(path, value) }.toTypedArray()
            val write = ref.update(FieldPath.of("updatedAt"), updatedAt, *moreFieldsAndValues)
            withTimeoutOrNull(WRITE_ACK_TIMEOUT) { write.await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
