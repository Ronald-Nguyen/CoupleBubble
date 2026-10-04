package com.aistudio.couplebubble.qxztrw.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.model.NoteType
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.repository.NotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.UUID

data class NotesUiState(
    /** Notes matching [labelFilter]: pinned first, then most recently changed. */
    val notes: List<SharedNote> = emptyList(),
    val hasAnyNotes: Boolean = false,
    /** The space's labels in display order. */
    val labels: List<NoteLabel> = emptyList(),
    val labelFilter: String? = null,
    /** Label ID → number of notes carrying it. */
    val labelUsage: Map<String, Int> = emptyMap(),
    val showLabelManager: Boolean = false,
    val openNote: SharedNote? = null,
    val noteToDelete: SharedNote? = null,
    /** Partner UID → accent color hex, for the author dots. */
    val authorColors: Map<String, String> = emptyMap()
) {
    /** Label ID → name; notes whose label was deleted simply have none. */
    val labelNames: Map<String, String> = labels.associate { it.id to it.name }
}

sealed interface NotesEvent {
    data class FilterSelected(val labelId: String?) : NotesEvent
    data object CreateNote : NotesEvent
    data class OpenNote(val noteId: String) : NotesEvent
    data object CloseNote : NotesEvent
    data class TitleChanged(val title: String) : NotesEvent
    data class BodyChanged(val body: String) : NotesEvent
    data object TogglePinned : NotesEvent
    data class LabelChanged(val labelId: String?) : NotesEvent
    data object ConvertNoteType : NotesEvent
    data class AddItem(val text: String) : NotesEvent
    data class ItemTextChanged(val itemId: String, val text: String) : NotesEvent
    data class ItemCheckedChanged(val itemId: String, val checked: Boolean) : NotesEvent
    data class DeleteItem(val itemId: String) : NotesEvent
    data object ClearCheckedItems : NotesEvent
    data object RequestDeleteNote : NotesEvent
    data object ConfirmDeleteNote : NotesEvent
    data object DismissDeleteNote : NotesEvent
    data class ShowLabelManager(val show: Boolean) : NotesEvent
    data class AddLabel(val name: String) : NotesEvent
    data class RenameLabel(val labelId: String, val name: String) : NotesEvent
    data class DeleteLabel(val labelId: String) : NotesEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val spaceFlow: StateFlow<CoupleSpace?>,
    private val userProfileFlow: StateFlow<UserProfile?>,
    /** Labels a space shows until either partner changes them; names come from string resources. */
    private val defaultLabels: List<NoteLabel> = emptyList(),
    started: SharingStarted = SharingStarted.WhileSubscribed(5000),
    private val clock: () -> Long = System::currentTimeMillis,
    private val textDebounceMillis: Long = 500L
) : ViewModel() {

    private data class LocalState(
        val labelFilter: String? = null,
        val showLabelManager: Boolean = false,
        val openNoteId: String? = null,
        val noteToDelete: SharedNote? = null,
        // A freshly created note, shown in the editor until the snapshot listener delivers it
        val pendingCreated: SharedNote? = null
    )

    private class PendingWrite(val job: Job, val write: suspend () -> Unit)

    private val local = MutableStateFlow(LocalState())

    // Debounced text writes keyed by "<noteId>/<field>" or "label/<labelId>", so typing doesn't cost one Firestore write per key stroke
    private val pendingWrites = mutableMapOf<String, PendingWrite>()

    // Latest typed title/body per note; the snapshot may still lag behind when the editor closes
    private val typedTitles = mutableMapOf<String, String>()
    private val typedBodies = mutableMapOf<String, String>()

    // Last position handed out per note, so quickly added items stay in order before the snapshot catches up
    private val lastItemPositions = mutableMapOf<String, Long>()

    private val currentCoupleId: String? get() = spaceFlow.value?.id
    private val currentUid: String? get() = userProfileFlow.value?.uid

    private val remoteNotes: StateFlow<List<SharedNote>> = spaceFlow
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(emptyList()) else notesRepository.getNotes(coupleId)
        }
        .onEach { notes ->
            local.update { state ->
                val created = state.pendingCreated
                if (created != null && notes.any { it.id == created.id }) state.copy(pendingCreated = null) else state
            }
        }
        .stateIn(viewModelScope, started, emptyList())

    // null until either partner changes the labels; the defaults apply until then
    private val remoteLabels: StateFlow<List<NoteLabel>?> = spaceFlow
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(null) else notesRepository.getNoteLabels(coupleId)
        }
        .stateIn(viewModelScope, started, null)

    val uiState: StateFlow<NotesUiState> = combine(
        remoteNotes,
        remoteLabels,
        spaceFlow,
        local
    ) { notes, storedLabels, space, state ->
        val labels = NoteOrganizer.sortedLabels(storedLabels ?: defaultLabels)
        // A filter on a label the partner just deleted falls back to all notes
        val filter = state.labelFilter?.takeIf { id -> labels.any { it.id == id } }
        NotesUiState(
            notes = NoteOrganizer.visibleNotes(notes, filter),
            hasAnyNotes = notes.isNotEmpty(),
            labels = labels,
            labelFilter = filter,
            labelUsage = NoteOrganizer.labelUsage(notes),
            showLabelManager = state.showLabelManager,
            openNote = resolveOpenNote(notes, state),
            noteToDelete = state.noteToDelete,
            authorColors = NoteOrganizer.authorColors(space)
        )
    }.stateIn(viewModelScope, started, NotesUiState())

    init {
        // A different space (or disconnecting) closes the editor and resets the filter
        viewModelScope.launch {
            spaceFlow.map { it?.id }.distinctUntilChanged().drop(1).collect {
                local.value = LocalState()
            }
        }
    }

    private val openNote: SharedNote? get() = resolveOpenNote(remoteNotes.value, local.value)

    private val labelsCustomized: Boolean get() = remoteLabels.value != null
    private val currentLabels: List<NoteLabel> get() = NoteOrganizer.sortedLabels(remoteLabels.value ?: defaultLabels)

    // A note deleted by the partner while open resolves to null, which closes the editor
    private fun resolveOpenNote(notes: List<SharedNote>, state: LocalState): SharedNote? {
        val id = state.openNoteId ?: return null
        return notes.firstOrNull { it.id == id } ?: state.pendingCreated?.takeIf { it.id == id }
    }

    fun onEvent(event: NotesEvent) {
        when (event) {
            is NotesEvent.FilterSelected -> onLabelFilterSelected(event.labelId)
            NotesEvent.CreateNote -> onCreateNote()
            is NotesEvent.OpenNote -> onOpenNote(event.noteId)
            NotesEvent.CloseNote -> onCloseNote()
            is NotesEvent.TitleChanged -> onTitleChanged(event.title)
            is NotesEvent.BodyChanged -> onBodyChanged(event.body)
            NotesEvent.TogglePinned -> onTogglePinned()
            is NotesEvent.LabelChanged -> onLabelChanged(event.labelId)
            NotesEvent.ConvertNoteType -> onConvertNoteType()
            is NotesEvent.AddItem -> onAddItem(event.text)
            is NotesEvent.ItemTextChanged -> onItemTextChanged(event.itemId, event.text)
            is NotesEvent.ItemCheckedChanged -> onItemCheckedChanged(event.itemId, event.checked)
            is NotesEvent.DeleteItem -> onDeleteItem(event.itemId)
            NotesEvent.ClearCheckedItems -> onClearCheckedItems()
            NotesEvent.RequestDeleteNote -> onRequestDeleteNote()
            NotesEvent.ConfirmDeleteNote -> onConfirmDeleteNote()
            NotesEvent.DismissDeleteNote -> onDismissDeleteNote()
            is NotesEvent.ShowLabelManager -> onShowLabelManager(event.show)
            is NotesEvent.AddLabel -> onAddLabel(event.name)
            is NotesEvent.RenameLabel -> onRenameLabel(event.labelId, event.name)
            is NotesEvent.DeleteLabel -> onDeleteLabel(event.labelId)
        }
    }

    private fun onLabelFilterSelected(labelId: String?) {
        local.update { it.copy(labelFilter = labelId) }
    }

    private fun onCreateNote() {
        val coupleId = currentCoupleId ?: return
        val now = clock()
        val note = SharedNote(
            id = UUID.randomUUID().toString(),
            type = NoteType.TEXT,
            labelId = local.value.labelFilter?.takeIf { id -> currentLabels.any { it.id == id } },
            createdBy = currentUid,
            createdAt = now,
            updatedAt = now
        )
        local.update { it.copy(openNoteId = note.id, pendingCreated = note) }
        viewModelScope.launch { notesRepository.addNote(coupleId, note) }
    }

    private fun onOpenNote(noteId: String) {
        local.update { it.copy(openNoteId = noteId) }
    }

    /** Leaves the editor; a note that is still completely empty is deleted, like in Keep. */
    private fun onCloseNote() {
        val note = openNote
        local.update { it.copy(openNoteId = null, pendingCreated = null) }
        if (note == null) return
        val coupleId = currentCoupleId ?: return

        val isEmpty = note.copy(
            title = typedTitles[note.id] ?: note.title,
            body = typedBodies[note.id] ?: note.body
        ).isEmpty
        typedTitles.remove(note.id)
        typedBodies.remove(note.id)
        lastItemPositions.remove(note.id)

        viewModelScope.launch {
            if (isEmpty) {
                cancelPendingWrites(note.id)
                notesRepository.deleteNote(coupleId, note.id)
            } else {
                flushPendingWrites(note.id)
            }
        }
    }

    private fun onTitleChanged(title: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        typedTitles[note.id] = title
        scheduleWrite("${note.id}/title") {
            notesRepository.updateNoteText(coupleId, note.id, title = title, body = null, updatedAt = clock())
        }
    }

    private fun onBodyChanged(body: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        typedBodies[note.id] = body
        scheduleWrite("${note.id}/body") {
            notesRepository.updateNoteText(coupleId, note.id, title = null, body = body, updatedAt = clock())
        }
    }

    private fun onTogglePinned() {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        viewModelScope.launch { notesRepository.setNotePinned(coupleId, note.id, !note.pinned, clock()) }
    }

    private fun onLabelChanged(labelId: String?) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        if (note.labelId == labelId) return
        viewModelScope.launch { notesRepository.setNoteLabel(coupleId, note.id, labelId, clock()) }
    }

    private fun onShowLabelManager(show: Boolean) {
        local.update { it.copy(showLabelManager = show) }
        if (!show) viewModelScope.launch { flushPendingWrites(LABEL_KEY_PREFIX) }
    }

    /**
     * The first change to the labels also writes the defaults, because from then on only stored labels count.
     * Afterwards every write touches just its own label.
     */
    private fun labelsToWrite(changed: NoteLabel): List<NoteLabel> =
        if (labelsCustomized) listOf(changed) else currentLabels.filterNot { it.id == changed.id } + changed

    private fun onAddLabel(name: String) {
        val coupleId = currentCoupleId ?: return
        val normalized = NoteOrganizer.normalizeLabelName(name)
        val labels = currentLabels
        if (normalized.isEmpty() || NoteOrganizer.isLabelNameTaken(labels, normalized)) return

        val label = NoteLabel(
            id = UUID.randomUUID().toString(),
            name = normalized,
            position = NoteOrganizer.nextLabelPosition(labels)
        )
        val toWrite = labelsToWrite(label)
        viewModelScope.launch { notesRepository.saveNoteLabels(coupleId, toWrite) }
    }

    /** Debounced like note text; blank names and names another label already uses are not saved. */
    private fun onRenameLabel(labelId: String, name: String) {
        val coupleId = currentCoupleId ?: return
        scheduleWrite("$LABEL_KEY_PREFIX/$labelId") {
            val labels = currentLabels
            val label = labels.firstOrNull { it.id == labelId } ?: return@scheduleWrite
            val normalized = NoteOrganizer.normalizeLabelName(name)
            val isValid = normalized.isNotEmpty() && !NoteOrganizer.isLabelNameTaken(labels, normalized, exceptId = labelId)
            if (isValid && normalized != label.name) {
                notesRepository.saveNoteLabels(coupleId, labelsToWrite(label.copy(name = normalized)))
            }
        }
    }

    /** Removes the label; notes keep their content and simply lose it. */
    private fun onDeleteLabel(labelId: String) {
        val coupleId = currentCoupleId ?: return
        pendingWrites.remove("$LABEL_KEY_PREFIX/$labelId")?.job?.cancel()
        val labelsToKeep = if (labelsCustomized) emptyList() else currentLabels.filterNot { it.id == labelId }
        val labeledNotes = remoteNotes.value.filter { it.labelId == labelId }
        local.update { if (it.labelFilter == labelId) it.copy(labelFilter = null) else it }

        viewModelScope.launch {
            notesRepository.deleteNoteLabel(coupleId, labelId, labelsToKeep)
            // Keep each note's updatedAt, so removing a label doesn't reorder the overview
            labeledNotes.forEach { note ->
                notesRepository.setNoteLabel(coupleId, note.id, null, note.updatedAt)
            }
        }
    }

    /** Switches between text and checklist: lines become items and items become lines. */
    private fun onConvertNoteType() {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        viewModelScope.launch {
            flushPendingWrites(note.id)
            val converted = when (note.type) {
                NoteType.TEXT -> note.copy(
                    type = NoteType.CHECKLIST,
                    body = "",
                    items = NoteOrganizer.textToItems(
                        body = typedBodies[note.id] ?: note.body,
                        createdBy = currentUid,
                        newId = { UUID.randomUUID().toString() }
                    )
                )
                NoteType.CHECKLIST -> note.copy(
                    type = NoteType.TEXT,
                    body = NoteOrganizer.itemsToText(note.items),
                    items = emptyList()
                )
            }
            typedBodies.remove(note.id)
            lastItemPositions.remove(note.id)
            notesRepository.replaceNoteContent(coupleId, converted.copy(updatedAt = clock()))
        }
    }

    private fun onAddItem(text: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val position = maxOf(NoteOrganizer.nextPosition(note.items), (lastItemPositions[note.id] ?: 0L) + 1L)
        lastItemPositions[note.id] = position
        val item = NoteItem(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            createdBy = currentUid,
            position = position
        )
        viewModelScope.launch { notesRepository.addNoteItem(coupleId, note.id, item, clock()) }
    }

    private fun onItemTextChanged(itemId: String, text: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        scheduleWrite("${note.id}/item/$itemId") {
            notesRepository.setNoteItemText(coupleId, note.id, itemId, text, clock())
        }
    }

    private fun onItemCheckedChanged(itemId: String, checked: Boolean) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        viewModelScope.launch { notesRepository.setNoteItemChecked(coupleId, note.id, itemId, checked, clock()) }
    }

    private fun onDeleteItem(itemId: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        pendingWrites.remove("${note.id}/item/$itemId")?.job?.cancel()
        viewModelScope.launch { notesRepository.deleteNoteItems(coupleId, note.id, listOf(itemId), clock()) }
    }

    private fun onClearCheckedItems() {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val checkedIds = note.items.filter { it.checked }.map { it.id }
        if (checkedIds.isEmpty()) return
        checkedIds.forEach { pendingWrites.remove("${note.id}/item/$it")?.job?.cancel() }
        viewModelScope.launch { notesRepository.deleteNoteItems(coupleId, note.id, checkedIds, clock()) }
    }

    private fun onRequestDeleteNote() {
        val note = openNote ?: return
        local.update { it.copy(noteToDelete = note) }
    }

    private fun onDismissDeleteNote() {
        local.update { it.copy(noteToDelete = null) }
    }

    private fun onConfirmDeleteNote() {
        val note = local.value.noteToDelete ?: return
        val coupleId = currentCoupleId ?: return
        cancelPendingWrites(note.id)
        typedTitles.remove(note.id)
        typedBodies.remove(note.id)
        lastItemPositions.remove(note.id)
        local.update { state ->
            state.copy(
                noteToDelete = null,
                openNoteId = state.openNoteId.takeUnless { it == note.id },
                pendingCreated = state.pendingCreated?.takeUnless { it.id == note.id }
            )
        }
        viewModelScope.launch { notesRepository.deleteNote(coupleId, note.id) }
    }

    private fun scheduleWrite(key: String, write: suspend () -> Unit) {
        pendingWrites.remove(key)?.job?.cancel()
        val job = viewModelScope.launch {
            delay(textDebounceMillis)
            val ownJob = coroutineContext.job
            if (pendingWrites[key]?.job === ownJob) pendingWrites.remove(key)
            write()
        }
        pendingWrites[key] = PendingWrite(job, write)
    }

    /** Runs the debounced writes under [keyPrefix] (a note ID or the label prefix) right away. */
    private suspend fun flushPendingWrites(keyPrefix: String) {
        val keys = pendingWrites.keys.filter { it.startsWith("$keyPrefix/") }
        keys.forEach { key ->
            val pending = pendingWrites.remove(key) ?: return@forEach
            pending.job.cancel()
            pending.write()
        }
    }

    private fun cancelPendingWrites(noteId: String) {
        pendingWrites.keys.filter { it.startsWith("$noteId/") }.forEach { key ->
            pendingWrites.remove(key)?.job?.cancel()
        }
    }

    private companion object {
        const val LABEL_KEY_PREFIX = "label"
    }
}
