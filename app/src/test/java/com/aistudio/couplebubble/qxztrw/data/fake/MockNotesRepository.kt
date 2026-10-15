package com.aistudio.couplebubble.qxztrw.data.fake

import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.domain.repository.NotesRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory notes per space; every write is visible immediately, like Firestore's latency compensation. */
open class MockNotesRepository : NotesRepository {

    private val notesBySpace = MutableStateFlow<Map<String, List<SharedNote>>>(emptyMap())

    // A space without an entry has never changed its labels
    private val labelsBySpace = MutableStateFlow<Map<String, List<NoteLabel>>>(emptyMap())

    /** Makes every write fail, as refused Firestore writes would (e.g. offline for too long or a lost login). */
    var failWrites = false

    private fun refused(): Result<Nothing> = Result.failure(IllegalStateException("PERMISSION_DENIED"))

    fun notes(coupleId: String): List<SharedNote> = notesBySpace.value[coupleId].orEmpty()

    fun labels(coupleId: String): List<NoteLabel>? = labelsBySpace.value[coupleId]

    override fun getNotes(coupleId: String): Flow<List<SharedNote>> = notesBySpace.map { it[coupleId].orEmpty() }

    override suspend fun addNote(coupleId: String, note: SharedNote): Result<SharedNote> {
        if (failWrites) return refused()
        val saved = note.copy(id = note.id.ifBlank { UUID.randomUUID().toString() })
        editSpace(coupleId) { notes -> notes.filterNot { it.id == saved.id } + saved }
        return Result.success(saved)
    }

    override suspend fun updateNoteText(
        coupleId: String,
        noteId: String,
        title: String?,
        body: String?,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = editNote(coupleId, noteId) {
        it.copy(title = title ?: it.title, body = body ?: it.body, updatedAt = updatedAt, updatedBy = updatedBy ?: it.updatedBy)
    }

    override suspend fun setNotePinned(coupleId: String, noteId: String, pinned: Boolean, updatedAt: Long, updatedBy: String?): Result<Unit> =
        editNote(coupleId, noteId) { it.copy(pinned = pinned, updatedAt = updatedAt, updatedBy = updatedBy ?: it.updatedBy) }

    override suspend fun setNoteLabel(
        coupleId: String,
        noteId: String,
        labelId: String?,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = editNote(coupleId, noteId) { it.copy(labelId = labelId, updatedAt = updatedAt, updatedBy = updatedBy ?: it.updatedBy) }

    override suspend fun replaceNoteContent(coupleId: String, note: SharedNote): Result<Unit> =
        editNote(coupleId, note.id) {
            it.copy(type = note.type, body = note.body, items = note.items, updatedAt = note.updatedAt, updatedBy = note.updatedBy ?: it.updatedBy)
        }

    override suspend fun deleteNote(coupleId: String, noteId: String): Result<Unit> {
        if (failWrites) return refused()
        editSpace(coupleId) { notes -> notes.filterNot { it.id == noteId } }
        return Result.success(Unit)
    }

    override suspend fun addNoteItem(coupleId: String, noteId: String, item: NoteItem, updatedAt: Long, updatedBy: String?): Result<Unit> =
        editNote(coupleId, noteId) { note ->
            note.copy(items = note.items.filterNot { it.id == item.id } + item, updatedAt = updatedAt, updatedBy = updatedBy ?: note.updatedBy)
        }

    override suspend fun setNoteItemText(
        coupleId: String,
        noteId: String,
        itemId: String,
        text: String,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(items = note.items.map { if (it.id == itemId) it.copy(text = text) else it }, updatedAt = updatedAt, updatedBy = updatedBy ?: note.updatedBy)
    }

    override suspend fun setNoteItemChecked(
        coupleId: String,
        noteId: String,
        itemId: String,
        checked: Boolean,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(
            items = note.items.map { if (it.id == itemId) it.copy(checked = checked) else it },
            updatedAt = updatedAt,
            updatedBy = updatedBy ?: note.updatedBy
        )
    }

    override suspend fun setNoteItemPositions(
        coupleId: String,
        noteId: String,
        positions: Map<String, Long>,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(
            items = note.items.map { item -> positions[item.id]?.let { item.copy(position = it) } ?: item },
            updatedAt = updatedAt,
            updatedBy = updatedBy ?: note.updatedBy
        )
    }

    override suspend fun deleteNoteItems(
        coupleId: String,
        noteId: String,
        itemIds: Collection<String>,
        updatedAt: Long,
        updatedBy: String?
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        note.copy(items = note.items.filterNot { it.id in itemIds }, updatedAt = updatedAt, updatedBy = updatedBy ?: note.updatedBy)
    }

    override fun getNoteLabels(coupleId: String): Flow<List<NoteLabel>?> = labelsBySpace.map { it[coupleId] }

    /** Number of [applyLabelChanges] calls, so tests can check that saving the editor writes once. */
    var labelWrites = 0
        private set

    override suspend fun applyLabelChanges(
        coupleId: String,
        upserts: List<NoteLabel>,
        deletedIds: Set<String>
    ): Result<Unit> {
        labelWrites++
        if (failWrites) return refused()
        labelsBySpace.update { spaces ->
            val replaced = upserts.map { it.id }.toSet() + deletedIds
            spaces + (coupleId to spaces[coupleId].orEmpty().filterNot { it.id in replaced } + upserts)
        }
        return Result.success(Unit)
    }

    /** Simulates the partner changing labels while this device has the editor open. */
    fun setLabels(coupleId: String, labels: List<NoteLabel>) {
        labelsBySpace.update { it + (coupleId to labels) }
    }

    private fun editSpace(coupleId: String, transform: (List<SharedNote>) -> List<SharedNote>) {
        notesBySpace.update { spaces -> spaces + (coupleId to transform(spaces[coupleId].orEmpty())) }
    }

    private fun editNote(coupleId: String, noteId: String, transform: (SharedNote) -> SharedNote): Result<Unit> {
        if (failWrites) return refused()
        if (notes(coupleId).none { it.id == noteId }) {
            return Result.failure(NoSuchElementException("Note $noteId not found"))
        }
        editSpace(coupleId) { notes -> notes.map { if (it.id == noteId) transform(it) else it } }
        return Result.success(Unit)
    }
}
