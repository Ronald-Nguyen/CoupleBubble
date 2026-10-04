package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/** In-memory notes per space; every write is visible immediately, like Firestore's latency compensation. */
open class MockNotesRepository : NotesRepository {

    private val notesBySpace = MutableStateFlow<Map<String, List<SharedNote>>>(emptyMap())

    // A space without an entry has never changed its labels
    private val labelsBySpace = MutableStateFlow<Map<String, List<NoteLabel>>>(emptyMap())

    fun notes(coupleId: String): List<SharedNote> = notesBySpace.value[coupleId].orEmpty()

    fun labels(coupleId: String): List<NoteLabel>? = labelsBySpace.value[coupleId]

    override fun getNotes(coupleId: String): Flow<List<SharedNote>> = notesBySpace.map { it[coupleId].orEmpty() }

    override suspend fun addNote(coupleId: String, note: SharedNote): Result<SharedNote> {
        val saved = note.copy(id = note.id.ifBlank { UUID.randomUUID().toString() })
        editSpace(coupleId) { notes -> notes.filterNot { it.id == saved.id } + saved }
        return Result.success(saved)
    }

    override suspend fun updateNoteText(
        coupleId: String,
        noteId: String,
        title: String?,
        body: String?,
        updatedAt: Long
    ): Result<Unit> = editNote(coupleId, noteId) {
        it.copy(title = title ?: it.title, body = body ?: it.body, updatedAt = updatedAt)
    }

    override suspend fun setNotePinned(coupleId: String, noteId: String, pinned: Boolean, updatedAt: Long): Result<Unit> =
        editNote(coupleId, noteId) { it.copy(pinned = pinned, updatedAt = updatedAt) }

    override suspend fun setNoteLabel(
        coupleId: String,
        noteId: String,
        labelId: String?,
        updatedAt: Long
    ): Result<Unit> = editNote(coupleId, noteId) { it.copy(labelId = labelId, updatedAt = updatedAt) }

    override suspend fun replaceNoteContent(coupleId: String, note: SharedNote): Result<Unit> =
        editNote(coupleId, note.id) {
            it.copy(type = note.type, body = note.body, items = note.items, updatedAt = note.updatedAt)
        }

    override suspend fun deleteNote(coupleId: String, noteId: String): Result<Unit> {
        editSpace(coupleId) { notes -> notes.filterNot { it.id == noteId } }
        return Result.success(Unit)
    }

    override suspend fun addNoteItem(coupleId: String, noteId: String, item: NoteItem, updatedAt: Long): Result<Unit> =
        editNote(coupleId, noteId) { note ->
            note.copy(items = note.items.filterNot { it.id == item.id } + item, updatedAt = updatedAt)
        }

    override suspend fun setNoteItemText(
        coupleId: String,
        noteId: String,
        itemId: String,
        text: String,
        updatedAt: Long
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(items = note.items.map { if (it.id == itemId) it.copy(text = text) else it }, updatedAt = updatedAt)
    }

    override suspend fun setNoteItemChecked(
        coupleId: String,
        noteId: String,
        itemId: String,
        checked: Boolean,
        updatedAt: Long
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(
            items = note.items.map { if (it.id == itemId) it.copy(checked = checked) else it },
            updatedAt = updatedAt
        )
    }

    override suspend fun deleteNoteItems(
        coupleId: String,
        noteId: String,
        itemIds: Collection<String>,
        updatedAt: Long
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(items = note.items.filterNot { it.id in itemIds }, updatedAt = updatedAt)
    }

    override fun getNoteLabels(coupleId: String): Flow<List<NoteLabel>?> = labelsBySpace.map { it[coupleId] }

    override suspend fun saveNoteLabels(coupleId: String, labels: List<NoteLabel>): Result<Unit> {
        labelsBySpace.update { spaces ->
            val ids = labels.map { it.id }.toSet()
            spaces + (coupleId to spaces[coupleId].orEmpty().filterNot { it.id in ids } + labels)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteNoteLabel(coupleId: String, labelId: String, labelsToKeep: List<NoteLabel>): Result<Unit> {
        saveNoteLabels(coupleId, labelsToKeep)
        labelsBySpace.update { spaces -> spaces + (coupleId to spaces[coupleId].orEmpty().filterNot { it.id == labelId }) }
        return Result.success(Unit)
    }

    private fun editSpace(coupleId: String, transform: (List<SharedNote>) -> List<SharedNote>) {
        notesBySpace.update { spaces -> spaces + (coupleId to transform(spaces[coupleId].orEmpty())) }
    }

    private fun editNote(coupleId: String, noteId: String, transform: (SharedNote) -> SharedNote): Result<Unit> {
        if (notes(coupleId).none { it.id == noteId }) {
            return Result.failure(NoSuchElementException("Note $noteId not found"))
        }
        editSpace(coupleId) { notes -> notes.map { if (it.id == noteId) transform(it) else it } }
        return Result.success(Unit)
    }
}
