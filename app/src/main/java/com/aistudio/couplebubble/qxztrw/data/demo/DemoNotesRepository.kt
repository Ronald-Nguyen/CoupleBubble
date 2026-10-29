package com.aistudio.couplebubble.qxztrw.data.demo

import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.domain.repository.NotesRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The notes of the demo mode, in memory per space; every write is visible immediately, like Firestore's latency
 * compensation. The seeded sample space starts with [DemoSeed.notes].
 */
class DemoNotesRepository(nowMillis: Long = System.currentTimeMillis()) : NotesRepository {

    private val notesBySpace = MutableStateFlow(mapOf(DemoSeed.SPACE_ID to DemoSeed.notes(nowMillis)))

    // A space without an entry has never changed its labels
    private val labelsBySpace = MutableStateFlow<Map<String, List<NoteLabel>>>(emptyMap())

    private fun notes(coupleId: String): List<SharedNote> = notesBySpace.value[coupleId].orEmpty()

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
        editSpace(coupleId) { notes -> notes.filterNot { it.id == noteId } }
        return Result.success(Unit)
    }

    override suspend fun addNoteItem(
        coupleId: String,
        noteId: String,
        item: NoteItem,
        updatedAt: Long,
        updatedBy: String?,
        shiftedPositions: Map<String, Long>
    ): Result<Unit> = editNote(coupleId, noteId) { note ->
        val shifted = note.items.filterNot { it.id == item.id }.map { other ->
            shiftedPositions[other.id]?.let { other.copy(position = it) } ?: other
        }
        note.copy(items = shifted + item, updatedAt = updatedAt, updatedBy = updatedBy ?: note.updatedBy)
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

    override suspend fun applyLabelChanges(
        coupleId: String,
        upserts: List<NoteLabel>,
        deletedIds: Set<String>
    ): Result<Unit> {
        labelsBySpace.update { spaces ->
            val replaced = upserts.map { it.id }.toSet() + deletedIds
            spaces + (coupleId to spaces[coupleId].orEmpty().filterNot { it.id in replaced } + upserts)
        }
        return Result.success(Unit)
    }

    /** Adds a note as the simulated partner. */
    internal fun addPartnerNote(coupleId: String, note: SharedNote) {
        editSpace(coupleId) { notes -> notes + note }
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
