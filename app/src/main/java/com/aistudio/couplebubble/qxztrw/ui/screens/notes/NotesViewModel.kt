package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.core.guardedScope
import com.aistudio.couplebubble.qxztrw.core.backgroundGuard
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.DraftLabel
import com.aistudio.couplebubble.qxztrw.domain.model.GestureHint
import com.aistudio.couplebubble.qxztrw.domain.model.GestureHintRules
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.LabelNameError
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.model.partnerName
import com.aistudio.couplebubble.qxztrw.domain.repository.NotesRepository
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.SnackbarAction
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

@Immutable
data class NotesUiState(
    /** Notes matching [labelFilter]: pinned first, then most recently changed. */
    val notes: List<SharedNote> = emptyList(),
    val hasAnyNotes: Boolean = false,
    /** False until the first snapshot arrived; the empty state waits for it. */
    val isLoaded: Boolean = true,
    /** The space's labels in display order. */
    val labels: List<NoteLabel> = emptyList(),
    val labelFilter: String? = null,
    /** Label ID → number of notes carrying it. */
    val labelUsage: Map<String, Int> = emptyMap(),
    /** The open label editor with its unsaved changes; `null` while it is closed. */
    val labelEditor: LabelEditorState? = null,
    val openNote: SharedNote? = null,
    /** IDs of the notes picked by long-pressing in the overview; non-empty means selection mode. */
    val selectedNoteIds: Set<String> = emptySet(),
    /** Partner UID → accent color hex, for the author dots. */
    val authorColors: Map<String, String> = emptyMap(),
    /** Partner UID → display name, for author information. */
    val authorNames: Map<String, String> = emptyMap(),
    /** IDs of notes created or modified by partner since last seen that haven't been opened yet. */
    val unreadNoteIds: Set<String> = emptySet(),
    /** Whether the notes tab has unread partner changes since last seen. */
    val hasUnreadNotes: Boolean = false,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val dismissedGestureHints: Set<GestureHint> = emptySet()
) {
    /** Label ID → name; notes whose label was deleted simply have none. */
    val labelNames: Map<String, String> = labels.associate { it.id to it.name }
    val isSelecting: Boolean get() = selectedNoteIds.isNotEmpty()

    val showNoteLongPressHint: Boolean
        get() = GestureHintRules.shouldShowNoteLongPress(
            dismissedHints = dismissedGestureHints,
            noteCount = notes.size,
            isSelecting = isSelecting,
            isSearchOpen = isSearchOpen
        )

    val activeGestureHint: GestureHint?
        get() = GestureHintRules.resolveActiveNotesHint(
            dismissedHints = dismissedGestureHints,
            noteCount = notes.size,
            isOpenNote = openNote != null,
            isChecklist = openNote?.type == NoteType.CHECKLIST,
            openItemCount = openNote?.items?.count { !it.checked } ?: 0,
            isSelecting = isSelecting,
            isSearchOpen = isSearchOpen
        )
}

/** Unsaved changes in the label editor: nothing is written until "Fertig", "Abbrechen" throws everything away. */
@Immutable
data class LabelEditorState(
    val drafts: List<DraftLabel> = emptyList(),
    /** Text in the "new label" field; "Fertig" also adds it, without pressing "+". */
    val newName: String = "",
    /** Counts labels moved from the field into the list, so the field knows when to clear itself. */
    val addedCount: Int = 0,
    val showDiscardPrompt: Boolean = false
) {
    val errors: Map<String, LabelNameError> = NoteOrganizer.draftErrors(drafts)
    val newNameError: LabelNameError? = NoteOrganizer.newLabelError(drafts, newName)
    val hasChanges: Boolean = NoteOrganizer.hasLabelChanges(drafts, newName)
    val canSave: Boolean = errors.isEmpty() && newNameError == null
}

sealed interface NotesEvent {
    data object TabOpened : NotesEvent
    data class FilterSelected(val labelId: String?) : NotesEvent
    data class SearchQueryChanged(val query: String) : NotesEvent
    data object OpenSearch : NotesEvent
    data object CloseSearch : NotesEvent
    data object CreateNote : NotesEvent
    data object CreateChecklist : NotesEvent
    data class CreateNoteWithContent(val text: String, val title: String = "") : NotesEvent
    data class OpenNote(val noteId: String) : NotesEvent
    data object CloseNote : NotesEvent
    data class TitleChanged(val title: String) : NotesEvent
    data class BodyChanged(val body: String) : NotesEvent
    data object TogglePinned : NotesEvent
    data class LabelChanged(val labelId: String?) : NotesEvent
    data object ConvertNoteType : NotesEvent
    data class AddItem(val text: String) : NotesEvent
    data class InsertItemAfter(val afterItemId: String, val newItemId: String = UUID.randomUUID().toString()) : NotesEvent
    data class NewItemDraftChanged(val text: String) : NotesEvent
    data class ItemTextChanged(val itemId: String, val text: String) : NotesEvent
    data class ItemCheckedChanged(val itemId: String, val checked: Boolean) : NotesEvent
    data class DeleteItem(val itemId: String) : NotesEvent
    /** New order of the open checklist items after a drag and drop, top to bottom. */
    data class ReorderItems(val orderedIds: List<String>) : NotesEvent
    data object ClearCheckedItems : NotesEvent

    /** Deletes the open note: hidden at once, removed from Firestore once the undo snackbar is gone. */
    data object DeleteNote : NotesEvent
    data class ToggleNoteSelection(val noteId: String) : NotesEvent
    data object ClearSelection : NotesEvent
    data class DismissGestureHint(val hint: GestureHint) : NotesEvent
    data object SelectAll : NotesEvent
    data object TogglePinnedSelected : NotesEvent
    data class ChangeSelectedLabel(val labelId: String?) : NotesEvent

    /** Deletes all selected notes, with the same undo snackbar as [DeleteNote]. */
    data object DeleteSelected : NotesEvent

    /** "Rückgängig" on the snackbar: the hidden notes come back. */
    data class UndoDeleteNotes(val noteIds: Set<String>) : NotesEvent

    /** The snackbar went away without undo: the hidden notes are deleted for good. */
    data class CommitDeleteNotes(val noteIds: Set<String>) : NotesEvent

    /** "Rückgängig" after deleting checklist items: writes them back with id, text, state and position. */
    data class UndoDeleteItems(val noteId: String, val items: List<NoteItem>) : NotesEvent
    data object OpenLabelManager : NotesEvent
    data class DraftRenameLabel(val labelId: String, val name: String) : NotesEvent
    data class DraftNewLabelNameChanged(val name: String) : NotesEvent

    /** "+" or Enter in the new-label field: moves the typed name into the list, still unsaved. */
    data object DraftAddLabel : NotesEvent

    /** Marks a stored label for deletion or restores it; a label added in this session is just dropped. */
    data class DraftToggleDeleteLabel(val labelId: String) : NotesEvent
    data object SaveLabels : NotesEvent

    /** "Abbrechen": closes and discards without asking. */
    data object CancelLabels : NotesEvent

    /** Back or a tap outside: closes right away without changes, otherwise asks first. */
    data object RequestCloseLabels : NotesEvent
    data object ConfirmDiscardLabels : NotesEvent
    data object KeepEditingLabels : NotesEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val spaceFlow: StateFlow<CoupleSpace?>,
    private val userProfileFlow: StateFlow<UserProfile?>,
    /** Labels a space shows until either partner changes them; names come from string resources. */
    private val defaultLabels: List<NoteLabel> = emptyList(),
    private val sessionStore: SessionStore? = null,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000),
    private val clock: () -> Long = System::currentTimeMillis,
    private val textDebounceMillis: Long = 500L,
    /** Outlives the ViewModel, so a pending deletion is still committed when it is cleared. */
    private val appScope: CoroutineScope? = null,
    /** Where the overview state is computed; `null` keeps it on the collector's dispatcher (tests). */
    private val computeDispatcher: CoroutineDispatcher? = null
) : ViewModel() {

    private data class LocalState(
        val labelFilter: String? = null,
        val labelEditor: LabelEditorState? = null,
        val openNoteId: String? = null,
        val selectedNoteIds: Set<String> = emptySet(),
        // A freshly created note, shown in the editor until the snapshot listener delivers it
        val pendingCreated: SharedNote? = null,
        val unreadNoteIds: Set<String> = emptySet(),
        val openedNoteIds: Set<String> = emptySet(),
        val baselineLastSeen: Long? = null,
        val searchQuery: String = "",
        val isSearchOpen: Boolean = false
    )

    private class NoteDerived(
        val notes: List<SharedNote>,
        val noteIds: Set<String>,
        val labelUsage: Map<String, Int>,
        val authorColors: Map<String, String>,
        val authorNames: Map<String, String>
    )

    private class PendingWrite(val job: Job, val write: suspend () -> Unit)

    private data class PendingDeletion(val coupleId: String, val noteIds: Set<String>)

    private val local = MutableStateFlow(LocalState())

    private val _effects = Channel<CoupleEffect>(Channel.BUFFERED)

    /** One-shot snackbars, shown by `MainActivity`; their results come back as [NotesEvent]s. */
    val effects: Flow<CoupleEffect> = _effects.receiveAsFlow()

    // The one deletion waiting for its undo snackbar; a second deletion commits it right away
    private var pendingDeletion: PendingDeletion? = null

    // Notes hidden from the overview: waiting for the undo snackbar or being deleted in Firestore
    private val hiddenNoteIds = MutableStateFlow<Set<String>>(emptySet())

    // Debounced text writes keyed by "<noteId>/<field>", so typing doesn't cost one Firestore write per key stroke
    private val pendingWrites = mutableMapOf<String, PendingWrite>()

    // True after a debounced text write failed until one succeeds again, so a failing series shows one snackbar
    private var textWritesFailing = false

    // Latest typed title/body per note; the snapshot may still lag behind when the editor closes
    private val typedTitles = mutableMapOf<String, String>()
    private val typedBodies = mutableMapOf<String, String>()

    // Latest typed text per checklist item, keyed by note ID, so a conversion doesn't fall back to a lagging snapshot
    private val typedItemTexts = mutableMapOf<String, MutableMap<String, String>>()

    // Last position handed out per note, so quickly added items stay in order before the snapshot catches up
    private val lastItemPositions = mutableMapOf<String, Long>()

    // Text typed into the "new item" field but not yet added; closing the editor adds it, so no Enter is needed
    private val itemDrafts = mutableMapOf<String, String>()

    private val currentCoupleId: String? get() = spaceFlow.value?.id
    private val currentUid: String? get() = userProfileFlow.value?.uid

    private val lastSeenFlow: StateFlow<Long> = spaceFlow
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId ->
            if (coupleId == null || sessionStore == null) flowOf(0L) else sessionStore.notesLastSeenFlow(coupleId)
        }
        .stateIn(guardedScope, started, 0L)

    // False until the first snapshot of the current space arrived, so the empty state doesn't flash on a cold start
    private val notesLoaded = MutableStateFlow(false)

    private val remoteNotes: StateFlow<List<SharedNote>> = spaceFlow
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId ->
            notesLoaded.value = false
            if (coupleId == null) flowOf(emptyList()) else notesRepository.getNotes(coupleId)
        }
        .onEach { notes ->
            notesLoaded.value = true
            local.update { state ->
                val created = state.pendingCreated
                val newPending = if (created != null && notes.any { it.id == created.id }) null else created
                val baseline = state.baselineLastSeen
                val newUnread = if (baseline != null) {
                    NoteOrganizer.unreadPartnerNoteIds(
                        notes = notes,
                        currentUserId = currentUid,
                        lastSeen = baseline,
                        openedNoteIds = state.openedNoteIds
                    )
                } else {
                    emptySet()
                }
                val noteIds = notes.mapTo(HashSet()) { it.id }
                val updatedUnread = (state.unreadNoteIds + newUnread).filterTo(mutableSetOf()) { it in noteIds }
                state.copy(pendingCreated = newPending, unreadNoteIds = updatedUnread)
            }
        }
        .stateIn(guardedScope, started, emptyList())

    // Derived only from notes and space, so typing a search or selecting cards doesn't recompute them
    private val noteDerived = combine(shownNotes(), spaceFlow) { notes, space ->
        NoteDerived(
            notes = notes,
            noteIds = notes.mapTo(HashSet()) { it.id },
            labelUsage = NoteOrganizer.labelUsage(notes),
            authorColors = NoteOrganizer.authorColors(space),
            authorNames = NoteOrganizer.authorNames(space)
        )
    }

    // null until either partner changes the labels; the defaults apply until then
    private val remoteLabels: StateFlow<List<NoteLabel>?> = spaceFlow
        .map { it?.id }
        .distinctUntilChanged()
        .flatMapLatest { coupleId ->
            if (coupleId == null) flowOf(null) else notesRepository.getNoteLabels(coupleId)
        }
        .stateIn(guardedScope, started, null)

    private fun shownNotes() = combine(remoteNotes, hiddenNoteIds) { notes, hidden ->
        if (hidden.isEmpty()) notes else notes.filterNot { it.id in hidden }
    }

    private val localAndHintsFlow = combine(
        local,
        sessionStore?.dismissedGestureHintsFlow ?: flowOf(emptySet()),
        ::Pair
    )

    val uiState: StateFlow<NotesUiState> = combine(
        noteDerived,
        remoteLabels,
        lastSeenFlow,
        localAndHintsFlow,
        notesLoaded
    ) { derived, storedLabels, lastSeen, (state, dismissedHints), loaded ->
        val notes = derived.notes
        val labels = NoteOrganizer.sortedLabels(storedLabels ?: defaultLabels)
        // A filter on a label the partner just deleted falls back to all notes
        val filter = state.labelFilter?.takeIf { id -> labels.any { it.id == id } }
        // Notes the partner deleted meanwhile drop out of the selection
        val selected = state.selectedNoteIds.filterTo(mutableSetOf()) { it in derived.noteIds }
        val uid = currentUid
        val hasUnread = NoteOrganizer.hasUnreadPartnerNotes(notes, uid, lastSeen)
        val searchQuery = state.searchQuery.takeIf { state.isSearchOpen }
        NotesUiState(
            notes = NoteOrganizer.visibleNotes(notes, filter, searchQuery),
            hasAnyNotes = notes.isNotEmpty(),
            isLoaded = loaded,
            labels = labels,
            labelFilter = filter,
            labelUsage = derived.labelUsage,
            labelEditor = state.labelEditor,
            openNote = resolveOpenNote(notes, state),
            selectedNoteIds = selected,
            authorColors = derived.authorColors,
            authorNames = derived.authorNames,
            unreadNoteIds = state.unreadNoteIds,
            hasUnreadNotes = hasUnread,
            searchQuery = state.searchQuery,
            isSearchOpen = state.isSearchOpen,
            dismissedGestureHints = dismissedHints
        )
    }
        .let { flow -> computeDispatcher?.let { flow.flowOn(it) } ?: flow }
        .conflate()
        .stateIn(guardedScope, started, NotesUiState())

    init {
        // A different space (or disconnecting) closes the editor and resets the filter
        viewModelScope.launch(backgroundGuard) {
            spaceFlow.map { it?.id }.distinctUntilChanged().drop(1).collect {
                local.value = LocalState()
            }
        }
        // The partner may change labels while the editor is open; untouched rows follow, edits here stay
        viewModelScope.launch(backgroundGuard) {
            remoteLabels.collect {
                updateLabelEditor { editor ->
                    editor.copy(drafts = NoteOrganizer.mergeRemoteLabels(editor.drafts, currentLabels))
                }
            }
        }
        // Closes the editor with a snackbar if the partner deletes the open note
        viewModelScope.launch(backgroundGuard) {
            var previousNotes: List<SharedNote>? = null
            remoteNotes.collect { notes ->
                val prev = previousNotes
                previousNotes = notes
                if (prev == null) return@collect
                val state = local.value
                val openId = state.openNoteId ?: return@collect
                if (openId !in hiddenNoteIds.value &&
                    state.pendingCreated?.id != openId &&
                    prev.any { it.id == openId } &&
                    notes.none { it.id == openId }
                ) {
                    cancelPendingWrites(openId)
                    typedTitles.remove(openId)
                    typedBodies.remove(openId)
                    typedItemTexts.remove(openId)
                    lastItemPositions.remove(openId)
                    itemDrafts.remove(openId)
                    local.update { it.copy(openNoteId = null) }
                    val partnerName = spaceFlow.value?.partnerName(currentUid).orEmpty()
                    _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.note_deleted_by_partner, listOf(partnerName))))
                }
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
            NotesEvent.TabOpened -> onTabOpened()
            is NotesEvent.FilterSelected -> onLabelFilterSelected(event.labelId)
            is NotesEvent.SearchQueryChanged -> local.update { it.copy(searchQuery = event.query) }
            NotesEvent.OpenSearch -> local.update { it.copy(isSearchOpen = true) }
            NotesEvent.CloseSearch -> local.update { it.copy(isSearchOpen = false, searchQuery = "") }
            NotesEvent.CreateNote -> onCreateNote(NoteType.TEXT)
            NotesEvent.CreateChecklist -> onCreateNote(NoteType.CHECKLIST)
            is NotesEvent.CreateNoteWithContent -> onCreateNoteWithContent(event.text, event.title)
            is NotesEvent.OpenNote -> onOpenNote(event.noteId)
            NotesEvent.CloseNote -> onCloseNote()
            is NotesEvent.TitleChanged -> onTitleChanged(event.title)
            is NotesEvent.BodyChanged -> onBodyChanged(event.body)
            NotesEvent.TogglePinned -> onTogglePinned()
            is NotesEvent.LabelChanged -> onLabelChanged(event.labelId)
            NotesEvent.ConvertNoteType -> onConvertNoteType()
            is NotesEvent.AddItem -> onAddItem(event.text)
            is NotesEvent.InsertItemAfter -> onInsertItemAfter(event.afterItemId, event.newItemId)
            is NotesEvent.NewItemDraftChanged -> openNote?.let { itemDrafts[it.id] = event.text }
            is NotesEvent.ItemTextChanged -> onItemTextChanged(event.itemId, event.text)
            is NotesEvent.ItemCheckedChanged -> onItemCheckedChanged(event.itemId, event.checked)
            is NotesEvent.DeleteItem -> onDeleteItem(event.itemId)
            is NotesEvent.ReorderItems -> {
                viewModelScope.launch(backgroundGuard) { sessionStore?.dismissGestureHint(GestureHint.CHECKLIST_DRAG) }
                onReorderItems(event.orderedIds)
            }
            NotesEvent.ClearCheckedItems -> onClearCheckedItems()
            NotesEvent.DeleteNote -> openNote?.let { deleteNotes(setOf(it.id)) }
            is NotesEvent.ToggleNoteSelection -> {
                viewModelScope.launch(backgroundGuard) { sessionStore?.dismissGestureHint(GestureHint.NOTE_LONG_PRESS) }
                onToggleNoteSelection(event.noteId)
            }
            is NotesEvent.DismissGestureHint -> {
                viewModelScope.launch(backgroundGuard) { sessionStore?.dismissGestureHint(event.hint) }
            }
            NotesEvent.ClearSelection -> local.update { it.copy(selectedNoteIds = emptySet()) }
            NotesEvent.SelectAll -> onSelectAll()
            NotesEvent.TogglePinnedSelected -> onTogglePinnedSelected()
            is NotesEvent.ChangeSelectedLabel -> onChangeSelectedLabel(event.labelId)
            NotesEvent.DeleteSelected -> onDeleteSelected()
            is NotesEvent.UndoDeleteNotes -> onUndoDeleteNotes(event.noteIds)
            is NotesEvent.CommitDeleteNotes -> pendingDeletion?.takeIf { it.noteIds == event.noteIds }?.let(::commitDeletion)
            is NotesEvent.UndoDeleteItems -> onUndoDeleteItems(event.noteId, event.items)
            NotesEvent.OpenLabelManager -> onOpenLabelManager()
            is NotesEvent.DraftRenameLabel -> updateLabelEditor { editor ->
                editor.copy(drafts = editor.drafts.map {
                    if (it.id == event.labelId) it.copy(name = event.name.take(NoteOrganizer.MAX_LABEL_NAME_LENGTH)) else it
                })
            }
            is NotesEvent.DraftNewLabelNameChanged -> updateLabelEditor {
                it.copy(newName = event.name.take(NoteOrganizer.MAX_LABEL_NAME_LENGTH))
            }
            NotesEvent.DraftAddLabel -> updateLabelEditor(::withTypedLabelAdded)
            is NotesEvent.DraftToggleDeleteLabel -> updateLabelEditor { editor ->
                editor.copy(drafts = editor.drafts.mapNotNull {
                    when {
                        it.id != event.labelId -> it
                        it.isNew -> null
                        else -> it.copy(isDeleted = !it.isDeleted)
                    }
                })
            }
            NotesEvent.SaveLabels -> onSaveLabels()
            NotesEvent.CancelLabels, NotesEvent.ConfirmDiscardLabels -> local.update { it.copy(labelEditor = null) }
            NotesEvent.RequestCloseLabels -> local.update { state ->
                val editor = state.labelEditor ?: return@update state
                state.copy(labelEditor = if (editor.hasChanges) editor.copy(showDiscardPrompt = true) else null)
            }
            NotesEvent.KeepEditingLabels -> updateLabelEditor { it.copy(showDiscardPrompt = false) }
        }
    }

    private fun onTabOpened() {
        val coupleId = currentCoupleId ?: return
        val uid = currentUid
        val now = clock()
        viewModelScope.launch(backgroundGuard) {
            // The stored value may not have loaded yet (cold start): read it, don't trust the initial 0
            val baseline = sessionStore?.notesLastSeenFlow(coupleId)?.first() ?: lastSeenFlow.value
            local.update { state ->
                val unread = NoteOrganizer.unreadPartnerNoteIds(
                    notes = remoteNotes.value,
                    currentUserId = uid,
                    lastSeen = baseline,
                    openedNoteIds = state.openedNoteIds
                )
                state.copy(
                    baselineLastSeen = baseline,
                    unreadNoteIds = state.unreadNoteIds + unread
                )
            }
            sessionStore?.saveNotesLastSeen(coupleId, now)
        }
    }

    private fun onLabelFilterSelected(labelId: String?) {
        local.update { it.copy(labelFilter = labelId) }
    }

    private fun onCreateNoteWithContent(text: String, title: String = "") {
        val uid = currentUid
        val isList = NoteOrganizer.hasMultipleBulletLines(text)
        if (isList) {
            val items = NoteOrganizer.textToItems(
                body = text,
                createdBy = uid,
                newId = { UUID.randomUUID().toString() }
            )
            onCreateNote(type = NoteType.CHECKLIST, title = title, body = "", items = items)
        } else {
            onCreateNote(type = NoteType.TEXT, title = title, body = text, items = emptyList())
        }
    }

    private fun onCreateNote(
        type: NoteType = NoteType.TEXT,
        title: String = "",
        body: String = "",
        items: List<NoteItem> = emptyList()
    ) {
        val coupleId = currentCoupleId ?: return
        val now = clock()
        val uid = currentUid
        val note = SharedNote(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title.take(InputField.NOTE_TITLE.maxLength).trim(),
            body = body,
            items = items,
            labelId = local.value.labelFilter?.takeIf { id -> currentLabels.any { it.id == id } },
            createdBy = uid,
            updatedBy = uid,
            createdAt = now,
            updatedAt = now
        )
        local.update { it.copy(
            openNoteId = note.id,
            pendingCreated = note,
            openedNoteIds = it.openedNoteIds + note.id
        ) }
        viewModelScope.launch(backgroundGuard) {
            if (notesRepository.addNote(coupleId, note).isSuccess) return@launch
            // Nothing was stored, so the editor of the new note closes instead of collecting edits that go nowhere
            cancelPendingWrites(note.id)
            local.update { state ->
                if (state.pendingCreated?.id != note.id) return@update state
                state.copy(openNoteId = state.openNoteId.takeUnless { it == note.id }, pendingCreated = null)
            }
            showSnackbar(R.string.note_create_failed)
        }
    }

    private fun onOpenNote(noteId: String) {
        local.update { it.copy(
            openNoteId = noteId,
            selectedNoteIds = emptySet(),
            unreadNoteIds = it.unreadNoteIds - noteId,
            openedNoteIds = it.openedNoteIds + noteId
        ) }
    }

    /** Leaves the editor; a note that is still completely empty is deleted, like in Keep. */
    private fun onCloseNote() {
        val note = openNote
        val draftAdded = note != null && !itemDrafts[note.id].isNullOrBlank()
        if (note != null) itemDrafts.remove(note.id)?.let { onAddItem(it) }
        local.update { it.copy(openNoteId = null, pendingCreated = null) }
        if (note == null) return
        val coupleId = currentCoupleId ?: return

        val isEmpty = !draftAdded && typedItemTexts[note.id].orEmpty().values.none { it.isNotBlank() } &&
            note.copy(
            title = typedTitles[note.id] ?: note.title,
            body = typedBodies[note.id] ?: note.body
        ).isEmpty
        typedTitles.remove(note.id)
        typedBodies.remove(note.id)
        typedItemTexts.remove(note.id)
        lastItemPositions.remove(note.id)

        if (isEmpty) _effects.trySend(CoupleEffect.Snackbar(UiText.Resource(R.string.note_empty_discarded)))
        viewModelScope.launch(backgroundGuard) {
            if (isEmpty) {
                cancelPendingWrites(note.id)
                if (notesRepository.deleteNote(coupleId, note.id).isFailure) showSnackbar(R.string.note_delete_failed)
            } else {
                flushPendingWrites(note.id)
            }
        }
    }

    private fun onTitleChanged(title: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        // The editor already cuts the title; this guards against anything longer reaching Firestore
        val title = title.take(InputField.NOTE_TITLE.maxLength)
        typedTitles[note.id] = title
        val uid = currentUid
        scheduleWrite(note.id, "title") {
            notesRepository.updateNoteText(coupleId, note.id, title = title, body = null, updatedAt = clock(), updatedBy = uid)
        }
    }

    private fun onBodyChanged(body: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        typedBodies[note.id] = body
        val uid = currentUid
        scheduleWrite(note.id, "body") {
            notesRepository.updateNoteText(coupleId, note.id, title = null, body = body, updatedAt = clock(), updatedBy = uid)
        }
    }

    private fun onTogglePinned() {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        writeNote(note.id) { notesRepository.setNotePinned(coupleId, note.id, !note.pinned, clock(), currentUid) }
    }

    private fun onLabelChanged(labelId: String?) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        if (note.labelId == labelId) return
        writeNote(note.id) { notesRepository.setNoteLabel(coupleId, note.id, labelId, clock(), currentUid) }
    }

    private fun onOpenLabelManager() {
        val drafts = currentLabels.map { DraftLabel(it.id, it.name, originalName = it.name) }
        local.update { it.copy(labelEditor = LabelEditorState(drafts = drafts)) }
    }

    private fun updateLabelEditor(transform: (LabelEditorState) -> LabelEditorState) {
        local.update { state -> state.labelEditor?.let { state.copy(labelEditor = transform(it)) } ?: state }
    }

    private fun withTypedLabelAdded(editor: LabelEditorState): LabelEditorState {
        val name = NoteOrganizer.normalizeLabelName(editor.newName)
        if (name.isEmpty() || editor.newNameError != null) return editor
        return editor.copy(
            drafts = editor.drafts + DraftLabel(UUID.randomUUID().toString(), name, originalName = null),
            newName = "",
            addedCount = editor.addedCount + 1
        )
    }

    /**
     * Writes everything in one go, including a typed label that was never added with "+". Notes of deleted
     * labels lose them; the notes themselves stay.
     */
    private fun onSaveLabels() {
        val editor = local.value.labelEditor ?: return
        if (!editor.canSave) return
        if (!editor.hasChanges) {
            local.update { it.copy(labelEditor = null) }
            return
        }
        val coupleId = currentCoupleId ?: return
        val drafts = withTypedLabelAdded(editor).drafts
        // The first change also writes the defaults, because from then on only stored labels count
        val changes = NoteOrganizer.labelChanges(currentLabels, drafts, writeAll = !labelsCustomized)
        local.update { state ->
            state.copy(
                labelEditor = null,
                labelFilter = state.labelFilter?.takeUnless { it in changes.deletedIds }
            )
        }
        if (changes.isEmpty) return

        val labeledNotes = remoteNotes.value.filter { it.labelId in changes.deletedIds }
        (appScope ?: viewModelScope).launch {
            if (notesRepository.applyLabelChanges(coupleId, changes.upserts, changes.deletedIds).isFailure) {
                showSnackbar(R.string.note_labels_save_failed)
                return@launch
            }
            // Keep each note's updatedAt, so removing a label doesn't reorder the overview
            val cleared = labeledNotes.map { note ->
                notesRepository.setNoteLabel(coupleId, note.id, null, note.updatedAt, currentUid).isSuccess || !noteExists(note.id)
            }
            if (!cleared.all { it }) showSnackbar(R.string.note_labels_save_failed)
        }
    }

    /** Switches between text and checklist: lines become items and items become lines. */
    private fun onConvertNoteType() {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        // Capture typed but not yet synced text before anything suspends, the snapshot may still lag behind
        val body = typedBodies.remove(note.id) ?: note.body
        val itemTexts = typedItemTexts.remove(note.id).orEmpty()
        val draft = itemDrafts.remove(note.id)?.trim().orEmpty()
        lastItemPositions.remove(note.id)
        val converted = when (note.type) {
            NoteType.TEXT -> note.copy(
                type = NoteType.CHECKLIST,
                body = "",
                items = NoteOrganizer.textToItems(
                    body = body,
                    // The lines belong to whoever wrote the note, not to whoever converts it
                    createdBy = note.createdBy ?: currentUid,
                    newId = { UUID.randomUUID().toString() }
                )
            )
            NoteType.CHECKLIST -> {
                val items = note.items.map { item -> itemTexts[item.id]?.let { item.copy(text = it) } ?: item }
                val withDraft = if (draft.isEmpty()) items else items + NoteItem(
                    id = UUID.randomUUID().toString(),
                    text = draft,
                    checked = false,
                    createdBy = currentUid,
                    position = NoteOrganizer.nextPosition(items)
                )
                note.copy(
                    type = NoteType.TEXT,
                    body = NoteOrganizer.itemsToText(withDraft),
                    items = emptyList()
                )
            }
        }
        viewModelScope.launch(backgroundGuard) {
            // Pending title writes still go out; body and item writes would be replaced by the conversion anyway
            cancelPendingWrites(note.id, except = "${note.id}/title")
            flushPendingWrites(note.id)
            reportNoteWrite(note.id, notesRepository.replaceNoteContent(coupleId, converted.copy(updatedAt = clock(), updatedBy = currentUid)))
        }
    }

    private fun onAddItem(text: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        itemDrafts.remove(note.id)

        val position = maxOf(NoteOrganizer.nextPosition(note.items), (lastItemPositions[note.id] ?: 0L) + 1L)
        lastItemPositions[note.id] = position
        val uid = currentUid
        val item = NoteItem(
            id = UUID.randomUUID().toString(),
            text = trimmed,
            createdBy = uid,
            position = position
        )
        writeNote(note.id) { notesRepository.addNoteItem(coupleId, note.id, item, clock(), updatedBy = uid) }
    }

    private fun onInsertItemAfter(afterItemId: String, newItemId: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val openItems = NoteOrganizer.orderedItems(note.items).filterNot { it.checked }
        val (newPosition, shiftedPositions) = NoteOrganizer.insertAfterPositions(openItems, afterItemId)
        val uid = currentUid
        val newItem = NoteItem(
            id = newItemId,
            text = "",
            createdBy = uid,
            position = newPosition
        )
        lastItemPositions[note.id] = maxOf(lastItemPositions[note.id] ?: 0L, newPosition, shiftedPositions.values.maxOrNull() ?: 0L)
        val now = clock()
        viewModelScope.launch(backgroundGuard) {
            if (shiftedPositions.isNotEmpty()) {
                notesRepository.setNoteItemPositions(coupleId, note.id, shiftedPositions, now, updatedBy = uid)
            }
            reportNoteWrite(note.id, notesRepository.addNoteItem(coupleId, note.id, newItem, now, updatedBy = uid))
        }
    }

    private fun onItemTextChanged(itemId: String, text: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        typedItemTexts.getOrPut(note.id) { mutableMapOf() }[itemId] = text
        val uid = currentUid
        scheduleWrite(note.id, "item/$itemId") {
            notesRepository.setNoteItemText(coupleId, note.id, itemId, text, clock(), updatedBy = uid)
        }
    }

    private fun onItemCheckedChanged(itemId: String, checked: Boolean) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        // Checking or unchecking an item keeps the note's updatedAt so it doesn't move to the top
        // Take the newest stored value, so a partner edit that arrived meanwhile is not set back in time
        val updatedAt = maxOf(note.updatedAt, remoteNotes.value.firstOrNull { it.id == note.id }?.updatedAt ?: 0L)
        writeNote(note.id) { notesRepository.setNoteItemChecked(coupleId, note.id, itemId, checked, updatedAt) }
    }

    private fun onDeleteItem(itemId: String) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val item = note.items.firstOrNull { it.id == itemId } ?: return
        val text = typedItemTexts[note.id]?.get(itemId) ?: item.text
        deleteItems(coupleId, note.id, listOf(item), showSnackbar = text.isNotBlank())
    }

    private fun onReorderItems(orderedIds: List<String>) {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val openItems = NoteOrganizer.orderedItems(note.items).filterNot { it.checked }
        val positions = NoteOrganizer.reorderedPositions(openItems, orderedIds)
        if (positions.isEmpty()) return
        val uid = currentUid
        writeNote(note.id) { notesRepository.setNoteItemPositions(coupleId, note.id, positions, clock(), updatedBy = uid) }
    }

    private fun onClearCheckedItems() {
        val note = openNote ?: return
        val coupleId = currentCoupleId ?: return
        val checked = note.items.filter { it.checked }
        if (checked.isEmpty()) return
        deleteItems(coupleId, note.id, checked)
    }

    /** Items are deleted right away; undo writes them back, so nothing has to be hidden locally meanwhile. */
    private fun deleteItems(coupleId: String, noteId: String, items: List<NoteItem>, showSnackbar: Boolean = true) {
        // Undo restores the text the user last saw, even if its debounced write never went out
        val typed = typedItemTexts[noteId]
        val deleted = items.map { item -> typed?.remove(item.id)?.let { item.copy(text = it) } ?: item }
        deleted.forEach { pendingWrites.remove("$noteId/item/${it.id}")?.job?.cancel() }
        val uid = currentUid
        writeNote(noteId) { notesRepository.deleteNoteItems(coupleId, noteId, deleted.map { it.id }, clock(), updatedBy = uid) }
        if (showSnackbar) {
            _effects.trySend(
                CoupleEffect.Snackbar(
                    text = UiText.Plural(R.plurals.note_items_deleted, deleted.size),
                    action = SnackbarAction(
                        label = UiText.Resource(R.string.note_delete_undo),
                        onActionPerformed = NotesEvent.UndoDeleteItems(noteId, deleted),
                        onDismissed = null
                    )
                )
            )
        }
    }

    private fun onUndoDeleteItems(noteId: String, items: List<NoteItem>) {
        val coupleId = currentCoupleId ?: return
        // The note itself may be gone meanwhile (deleted by the partner or here); then there is nothing to restore into
        if (noteId !in shownNoteIds()) return
        val now = clock()
        val uid = currentUid
        viewModelScope.launch(backgroundGuard) {
            val results = items.map { notesRepository.addNoteItem(coupleId, noteId, it, now, updatedBy = uid) }
            reportNoteWrite(noteId, results.firstOrNull { it.isFailure } ?: Result.success(Unit))
        }
    }

    private fun onToggleNoteSelection(noteId: String) {
        local.update { state ->
            val selected = if (noteId in state.selectedNoteIds) state.selectedNoteIds - noteId else state.selectedNoteIds + noteId
            state.copy(selectedNoteIds = selected)
        }
    }

    private fun onSelectAll() {
        val visible = NoteOrganizer.visibleNotes(
            notes = shownNotesList(),
            filter = local.value.labelFilter?.takeIf { id -> currentLabels.any { it.id == id } },
            query = local.value.searchQuery.takeIf { local.value.isSearchOpen }
        )
        val ids = visible.map { it.id }.toSet()
        if (ids.isNotEmpty()) {
            local.update { it.copy(selectedNoteIds = ids) }
        }
    }

    private fun onTogglePinnedSelected() {
        val coupleId = currentCoupleId ?: return
        val selectedIds = local.value.selectedNoteIds.intersect(shownNoteIds())
        if (selectedIds.isEmpty()) return
        val selectedNotes = remoteNotes.value.filter { it.id in selectedIds }
        val allPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned }
        val newPinned = !allPinned
        local.update { it.copy(selectedNoteIds = emptySet()) }
        val now = clock()
        viewModelScope.launch(backgroundGuard) {
            val results = selectedIds.map { id ->
                notesRepository.setNotePinned(coupleId, id, newPinned, now, currentUid)
            }
            if (results.any { it.isFailure }) {
                showSnackbar(R.string.note_save_failed)
            }
        }
    }

    private fun onChangeSelectedLabel(labelId: String?) {
        val coupleId = currentCoupleId ?: return
        val selectedIds = local.value.selectedNoteIds.intersect(shownNoteIds())
        if (selectedIds.isEmpty()) return
        local.update { it.copy(selectedNoteIds = emptySet()) }
        val now = clock()
        viewModelScope.launch(backgroundGuard) {
            val results = selectedIds.map { id ->
                notesRepository.setNoteLabel(coupleId, id, labelId, now, currentUid)
            }
            if (results.any { it.isFailure }) {
                showSnackbar(R.string.note_save_failed)
            }
        }
    }

    private fun onDeleteSelected() {
        val noteIds = local.value.selectedNoteIds.intersect(shownNoteIds())
        local.update { it.copy(selectedNoteIds = emptySet()) }
        if (noteIds.isNotEmpty()) deleteNotes(noteIds)
    }

    private fun shownNotesList(): List<SharedNote> {
        val hidden = hiddenNoteIds.value
        return if (hidden.isEmpty()) remoteNotes.value else remoteNotes.value.filterNot { it.id in hidden }
    }

    private fun shownNoteIds(): Set<String> {
        val hidden = hiddenNoteIds.value
        return remoteNotes.value.mapNotNullTo(mutableSetOf()) { note -> note.id.takeUnless { it in hidden } }
    }

    /** Hides the notes at once and offers undo; Firestore only deletes them once the snackbar is gone. */
    private fun deleteNotes(noteIds: Set<String>) {
        val coupleId = currentCoupleId ?: return
        pendingDeletion?.let(::commitDeletion)
        pendingDeletion = PendingDeletion(coupleId, noteIds)
        hiddenNoteIds.update { it + noteIds }
        local.update { state ->
            state.copy(
                openNoteId = state.openNoteId.takeUnless { it in noteIds },
                selectedNoteIds = state.selectedNoteIds - noteIds,
                unreadNoteIds = state.unreadNoteIds - noteIds
            )
        }
        // Debounced edits are flushed, so an undone note comes back with what was typed last
        viewModelScope.launch(backgroundGuard) { noteIds.forEach { flushPendingWrites(it) } }
        noteIds.forEach { noteId ->
            itemDrafts.remove(noteId)
            typedTitles.remove(noteId)
            typedBodies.remove(noteId)
            typedItemTexts.remove(noteId)
            lastItemPositions.remove(noteId)
        }
        _effects.trySend(
            CoupleEffect.Snackbar(
                text = UiText.Plural(R.plurals.notes_deleted, noteIds.size),
                action = SnackbarAction(
                    label = UiText.Resource(R.string.note_delete_undo),
                    onActionPerformed = NotesEvent.UndoDeleteNotes(noteIds),
                    onDismissed = NotesEvent.CommitDeleteNotes(noteIds)
                )
            )
        )
    }

    private fun onUndoDeleteNotes(noteIds: Set<String>) {
        if (pendingDeletion?.noteIds != noteIds) return
        pendingDeletion = null
        hiddenNoteIds.update { it - noteIds }
    }

    private fun commitDeletion(deletion: PendingDeletion) {
        if (pendingDeletion == deletion) pendingDeletion = null
        (appScope ?: viewModelScope).launch {
            val failed = deletion.noteIds.filter { notesRepository.deleteNote(deletion.coupleId, it).isFailure }
            // Notes that could not be deleted show up again
            hiddenNoteIds.update { it - deletion.noteIds }
            if (failed.isNotEmpty()) showSnackbar(R.string.note_delete_failed)
        }
    }

    override fun onCleared() {
        pendingDeletion?.let(::commitDeletion)
        // Text typed in the last 500 ms would be lost with the debounce job: write it in the app scope
        appScope?.let { scope ->
            val remaining = pendingWrites.values.toList()
            pendingWrites.clear()
            remaining.forEach { pending ->
                pending.job.cancel()
                scope.launch { pending.write() }
            }
        }
        super.onCleared()
    }

    /** Runs a single write of [noteId] and reports a failure. */
    private fun writeNote(noteId: String, write: suspend () -> Result<Unit>) {
        viewModelScope.launch(backgroundGuard) { reportNoteWrite(noteId, write()) }
    }

    /** A note the partner deleted meanwhile fails every write; that is no error worth showing. */
    private suspend fun reportNoteWrite(noteId: String, result: Result<Unit>) {
        if (result.isFailure && noteExists(noteId)) showSnackbar(R.string.note_save_failed)
    }

    private fun noteExists(noteId: String): Boolean =
        remoteNotes.value.any { it.id == noteId } || local.value.pendingCreated?.id == noteId

    private suspend fun showSnackbar(messageRes: Int) {
        _effects.send(CoupleEffect.Snackbar(UiText.Resource(messageRes)))
    }

    /** Debounces [write] per "<noteId>/<field>"; a series of failing text writes shows only one snackbar. */
    private fun scheduleWrite(noteId: String, field: String, write: suspend () -> Result<Unit>) {
        val key = "$noteId/$field"
        val reported: suspend () -> Unit = {
            val result = write()
            when {
                result.isSuccess -> textWritesFailing = false
                noteExists(noteId) && !textWritesFailing -> {
                    textWritesFailing = true
                    showSnackbar(R.string.note_save_failed)
                }
            }
        }
        pendingWrites.remove(key)?.job?.cancel()
        val job = viewModelScope.launch(backgroundGuard) {
            delay(textDebounceMillis)
            val ownJob = coroutineContext.job
            if (pendingWrites[key]?.job === ownJob) pendingWrites.remove(key)
            reported()
        }
        pendingWrites[key] = PendingWrite(job, reported)
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

    private fun cancelPendingWrites(noteId: String, except: String? = null) {
        pendingWrites.keys.filter { it.startsWith("$noteId/") && it != except }.forEach { key ->
            pendingWrites.remove(key)?.job?.cancel()
        }
    }
}
