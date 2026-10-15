package com.aistudio.couplebubble.qxztrw.domain.repository

import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import kotlinx.coroutines.flow.Flow

/**
 * Shared notes and checklists of a space. Items live in one map per note document, so every item change is a
 * field-level update and both partners can edit different items at the same time without overwriting each other.
 * Every write also bumps the note's `updatedAt` to [updatedAt].
 */
interface NotesRepository {
    fun getNotes(coupleId: String): Flow<List<SharedNote>>
    suspend fun addNote(coupleId: String, note: SharedNote): Result<SharedNote>
    /** Writes only the fields that are not `null`, so a title edit never overwrites the partner's body edit. */
    suspend fun updateNoteText(coupleId: String, noteId: String, title: String?, body: String?, updatedAt: Long, updatedBy: String? = null): Result<Unit>
    suspend fun setNotePinned(coupleId: String, noteId: String, pinned: Boolean, updatedAt: Long, updatedBy: String? = null): Result<Unit>
    suspend fun setNoteLabel(coupleId: String, noteId: String, labelId: String?, updatedAt: Long, updatedBy: String? = null): Result<Unit>

    /** Overwrites type, body and all items at once; used when a note switches between text and checklist. */
    suspend fun replaceNoteContent(coupleId: String, note: SharedNote): Result<Unit>
    suspend fun deleteNote(coupleId: String, noteId: String): Result<Unit>
    suspend fun addNoteItem(coupleId: String, noteId: String, item: NoteItem, updatedAt: Long, updatedBy: String? = null): Result<Unit>
    suspend fun setNoteItemText(coupleId: String, noteId: String, itemId: String, text: String, updatedAt: Long, updatedBy: String? = null): Result<Unit>
    suspend fun setNoteItemChecked(coupleId: String, noteId: String, itemId: String, checked: Boolean, updatedAt: Long, updatedBy: String? = null): Result<Unit>
    /** Writes only the given items' positions, so a reorder never rewrites the item texts. */
    suspend fun setNoteItemPositions(coupleId: String, noteId: String, positions: Map<String, Long>, updatedAt: Long, updatedBy: String? = null): Result<Unit>
    suspend fun deleteNoteItems(coupleId: String, noteId: String, itemIds: Collection<String>, updatedAt: Long, updatedBy: String? = null): Result<Unit>

    /**
     * The space's labels, or `null` while nobody has changed them yet; the default labels apply then.
     * Labels live in one document (`spaces/{id}/settings/noteLabels`), one map entry per label.
     */
    fun getNoteLabels(coupleId: String): Flow<List<NoteLabel>?>

    /**
     * Writes [upserts] and removes [deletedIds] in a single write, touching no other label, so the partner's
     * concurrent edits of other labels survive and nobody sees a half-applied change.
     */
    suspend fun applyLabelChanges(coupleId: String, upserts: List<NoteLabel>, deletedIds: Set<String>): Result<Unit>
}
