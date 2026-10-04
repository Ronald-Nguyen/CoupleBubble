package com.aistudio.couplebubble.qxztrw.repository

import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.SharedNote
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
    suspend fun updateNoteText(coupleId: String, noteId: String, title: String?, body: String?, updatedAt: Long): Result<Unit>
    suspend fun setNotePinned(coupleId: String, noteId: String, pinned: Boolean, updatedAt: Long): Result<Unit>
    suspend fun setNoteLabel(coupleId: String, noteId: String, labelId: String?, updatedAt: Long): Result<Unit>

    /** Overwrites type, body and all items at once; used when a note switches between text and checklist. */
    suspend fun replaceNoteContent(coupleId: String, note: SharedNote): Result<Unit>
    suspend fun deleteNote(coupleId: String, noteId: String): Result<Unit>
    suspend fun addNoteItem(coupleId: String, noteId: String, item: NoteItem, updatedAt: Long): Result<Unit>
    suspend fun setNoteItemText(coupleId: String, noteId: String, itemId: String, text: String, updatedAt: Long): Result<Unit>
    suspend fun setNoteItemChecked(coupleId: String, noteId: String, itemId: String, checked: Boolean, updatedAt: Long): Result<Unit>
    suspend fun deleteNoteItems(coupleId: String, noteId: String, itemIds: Collection<String>, updatedAt: Long): Result<Unit>

    /**
     * The space's labels, or `null` while nobody has changed them yet; the default labels apply then.
     * Labels live in one document (`spaces/{id}/settings/noteLabels`), one map entry per label.
     */
    fun getNoteLabels(coupleId: String): Flow<List<NoteLabel>?>

    /** Adds or replaces [labels] without touching the other labels. */
    suspend fun saveNoteLabels(coupleId: String, labels: List<NoteLabel>): Result<Unit>

    /** Removes [labelId]; [labelsToKeep] are written in the same call (used to materialize the defaults). */
    suspend fun deleteNoteLabel(coupleId: String, labelId: String, labelsToKeep: List<NoteLabel>): Result<Unit>
}
