package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.fake.FakeSessionStore
import com.aistudio.couplebubble.qxztrw.data.fake.MockNotesRepository
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.domain.model.GestureHint
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private class CountingNotesRepository : MockNotesRepository() {
        var textWrites = 0
        var itemTextWrites = 0

        override suspend fun updateNoteText(
            coupleId: String,
            noteId: String,
            title: String?,
            body: String?,
            updatedAt: Long,
            updatedBy: String?
        ): Result<Unit> {
            textWrites++
            return super.updateNoteText(coupleId, noteId, title, body, updatedAt, updatedBy)
        }

        override suspend fun setNoteItemText(
            coupleId: String,
            noteId: String,
            itemId: String,
            text: String,
            updatedAt: Long,
            updatedBy: String?
        ): Result<Unit> {
            itemTextWrites++
            return super.setNoteItemText(coupleId, noteId, itemId, text, updatedAt, updatedBy)
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private val space = MutableStateFlow<CoupleSpace?>(
        CoupleSpace(
            id = SPACE_ID,
            partner1Id = "uid_alex",
            partner2Id = "uid_sam",
            partner1Name = "Alex",
            partner2Name = "Sam",
            partner1ColorHex = "#E65D2E",
            partner2ColorHex = "#4ECDC4"
        )
    )
    private val profile = MutableStateFlow<UserProfile?>(UserProfile(uid = "uid_alex"))
    private val repository = CountingNotesRepository()
    private val sessionStore = FakeSessionStore()
    private var now = 1_000L
    private lateinit var viewModel: NotesViewModel

    private val state: NotesUiState get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NotesViewModel(
            notesRepository = repository,
            spaceFlow = space,
            userProfileFlow = profile,
            defaultLabels = DEFAULT_LABELS,
            sessionStore = sessionStore,
            started = SharingStarted.Eagerly,
            clock = { now },
            textDebounceMillis = DEBOUNCE
        )
        effectsJob = CoroutineScope(UnconfinedTestDispatcher(testDispatcher.scheduler)).launch { viewModel.effects.toList(effects) }
    }

    @After
    fun tearDown() {
        effectsJob.cancel()
        Dispatchers.resetMain()
    }

    private fun TestScope.settle() = testScheduler.runCurrent()

    private val effects = mutableListOf<CoupleEffect>()
    private lateinit var effectsJob: Job

    /** The last snackbar sent, so a test can press its action or let it go away. */
    private fun snackbar(): CoupleEffect.Snackbar = effects.filterIsInstance<CoupleEffect.Snackbar>().last()

    private fun storedNote(id: String): SharedNote? = repository.notes(SPACE_ID).firstOrNull { it.id == id }

    private suspend fun TestScope.seed(vararg notes: SharedNote) {
        notes.forEach { repository.addNote(SPACE_ID, it) }
        settle()
    }

    private fun TestScope.createAndOpenNote(): String {
        viewModel.onEvent(NotesEvent.CreateNote)
        settle()
        return requireNotNull(state.openNote).id
    }

    @Test
    fun withoutSpaceThereAreNoNotes() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Einkauf"))
        space.value = null
        settle()

        assertTrue(state.notes.isEmpty())
        assertFalse(state.hasAnyNotes)
    }

    @Test
    fun notesOfTheSpaceAppearPinnedFirst() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "older", title = "Alt", updatedAt = 1),
            SharedNote(id = "pinned", title = "Wichtig", pinned = true, updatedAt = 0),
            SharedNote(id = "newer", title = "Neu", updatedAt = 2)
        )

        assertEquals(listOf("pinned", "newer", "older"), state.notes.map { it.id })
        assertTrue(state.hasAnyNotes)
    }

    @Test
    fun authorColorsComeFromTheSpace() = runTest(testDispatcher) {
        settle()

        assertEquals(mapOf("uid_alex" to "#E65D2E", "uid_sam" to "#4ECDC4"), state.authorColors)
    }

    @Test
    fun filterLimitsTheVisibleNotes() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "shopping", labelId = DefaultNoteLabelIds.SHOPPING),
            SharedNote(id = "ideas", labelId = DefaultNoteLabelIds.IDEAS)
        )

        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.IDEAS))
        settle()

        assertEquals(DefaultNoteLabelIds.IDEAS, state.labelFilter)
        assertEquals(listOf("ideas"), state.notes.map { it.id })
        assertTrue(state.hasAnyNotes)
    }

    @Test
    fun createdNoteOpensWithActiveFilterAndAuthor() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.BUCKET_LIST))
        val noteId = createAndOpenNote()

        val open = requireNotNull(state.openNote)
        assertEquals(NoteType.TEXT, open.type)
        assertEquals(DefaultNoteLabelIds.BUCKET_LIST, open.labelId)
        assertEquals("uid_alex", open.createdBy)
        assertEquals("uid_alex", open.updatedBy)
        assertEquals(now, open.createdAt)
        assertNotNull(storedNote(noteId))
    }

    @Test
    fun createNoteWithoutSpaceDoesNothing() = runTest(testDispatcher) {
        space.value = null
        settle()

        viewModel.onEvent(NotesEvent.CreateNote)
        settle()

        assertNull(state.openNote)
        assertTrue(repository.notes(SPACE_ID).isEmpty())
    }

    @Test
    fun createNoteWithContent_multipleBullets_createsChecklistAndOpensIt() = runTest(testDispatcher) {
        val sharedText = "- Äpfel\n- Birnen\n- Orangen"
        viewModel.onEvent(NotesEvent.CreateNoteWithContent(text = sharedText, title = "Obst"))
        settle()

        val open = state.openNote
        assertNotNull(open)
        assertEquals(NoteType.CHECKLIST, open!!.type)
        assertEquals("Obst", open.title)
        assertEquals(listOf("Äpfel", "Birnen", "Orangen"), open.items.map { it.text })
        assertEquals("", open.body)

        val stored = storedNote(open.id)
        assertNotNull(stored)
        assertEquals(NoteType.CHECKLIST, stored!!.type)
        assertEquals(3, stored.items.size)
    }

    @Test
    fun createNoteWithContent_plainText_createsTextNoteAndOpensIt() = runTest(testDispatcher) {
        val sharedText = "Hier ist ein langes Rezept für Kuchen."
        viewModel.onEvent(NotesEvent.CreateNoteWithContent(text = sharedText))
        settle()

        val open = state.openNote
        assertNotNull(open)
        assertEquals(NoteType.TEXT, open!!.type)
        assertEquals("", open.title)
        assertEquals(sharedText, open.body)
        assertTrue(open.items.isEmpty())

        val stored = storedNote(open.id)
        assertNotNull(stored)
        assertEquals(NoteType.TEXT, stored!!.type)
        assertEquals(sharedText, stored.body)
    }

    @Test
    fun titleAndBodyEditsAreDebouncedIntoSingleWrites() = runTest(testDispatcher) {
        val noteId = createAndOpenNote()

        listOf("E", "Ei", "Eis").forEach { viewModel.onEvent(NotesEvent.TitleChanged(it)) }
        listOf("V", "Va", "Vanille").forEach { viewModel.onEvent(NotesEvent.BodyChanged(it)) }
        testScheduler.advanceTimeBy(DEBOUNCE - 1)
        settle()
        assertEquals(0, repository.textWrites)

        testScheduler.advanceTimeBy(2)
        settle()

        assertEquals(2, repository.textWrites)
        assertEquals("Eis", storedNote(noteId)?.title)
        assertEquals("Vanille", storedNote(noteId)?.body)
        assertEquals("uid_alex", storedNote(noteId)?.updatedBy)
    }

    @Test
    fun closingFlushesPendingEdits() = runTest(testDispatcher) {
        val noteId = createAndOpenNote()

        viewModel.onEvent(NotesEvent.TitleChanged("Packliste Lissabon"))
        viewModel.onEvent(NotesEvent.CloseNote)
        settle()

        assertNull(state.openNote)
        assertEquals("Packliste Lissabon", storedNote(noteId)?.title)
        assertEquals(1, repository.textWrites)
    }

    @Test
    fun closingAnEmptyNoteDeletesIt() = runTest(testDispatcher) {
        val noteId = createAndOpenNote()

        viewModel.onEvent(NotesEvent.CloseNote)
        settle()

        assertNull(storedNote(noteId))
        assertFalse(state.hasAnyNotes)
        assertEquals(CoupleEffect.Snackbar(UiText.Resource(R.string.note_empty_discarded)), snackbar())
    }

    @Test
    fun closingANoteWithContentShowsNoSnackbar() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        viewModel.onEvent(NotesEvent.CloseNote)
        settle()

        assertTrue(effects.isEmpty())
    }

    @Test
    fun closingAnEmptiedNoteDeletesItWithoutWritingTheText() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        viewModel.onEvent(NotesEvent.TitleChanged(""))
        viewModel.onEvent(NotesEvent.CloseNote)
        testScheduler.advanceUntilIdle()

        assertNull(storedNote("n1"))
        assertEquals(0, repository.textWrites)
    }

    @Test
    fun closingANoteWithContentKeepsIt() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Filme für Regentage"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()
        assertEquals("n1", state.openNote?.id)

        viewModel.onEvent(NotesEvent.CloseNote)
        settle()

        assertNull(state.openNote)
        assertNotNull(storedNote("n1"))
    }

    @Test
    fun pinAndCategoryChangesAreStored() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Urlaub"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        now = 2_000L
        viewModel.onEvent(NotesEvent.TogglePinned)
        settle()
        viewModel.onEvent(NotesEvent.LabelChanged(DefaultNoteLabelIds.BUCKET_LIST))
        settle()

        assertEquals(true, storedNote("n1")?.pinned)
        assertEquals(DefaultNoteLabelIds.BUCKET_LIST, storedNote("n1")?.labelId)
        assertEquals(2_000L, storedNote("n1")?.updatedAt)

        viewModel.onEvent(NotesEvent.TogglePinned)
        viewModel.onEvent(NotesEvent.LabelChanged(null))
        settle()

        assertEquals(false, storedNote("n1")?.pinned)
        assertNull(storedNote("n1")?.labelId)
    }

    @Test
    fun addedItemsAreTrimmedOrderedAndSignedByTheAuthor() = runTest(testDispatcher) {
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, title = "Einkauf"))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        // Both adds happen before the snapshot delivers the first one
        viewModel.onEvent(NotesEvent.AddItem("  Milch "))
        viewModel.onEvent(NotesEvent.AddItem("Brot"))
        viewModel.onEvent(NotesEvent.AddItem("   "))
        settle()

        val items = requireNotNull(state.openNote).items.sortedBy { it.position }
        assertEquals(listOf("Milch", "Brot"), items.map { it.text })
        assertEquals(listOf(1L, 2L), items.map { it.position })
        assertTrue(items.all { it.createdBy == "uid_alex" && !it.checked })
    }

    @Test
    fun itemsCanBeCheckedRenamedAndDeleted() = runTest(testDispatcher) {
        seed(
            SharedNote(
                id = "list",
                type = NoteType.CHECKLIST,
                items = listOf(
                    NoteItem(id = "a", text = "Milch", position = 1),
                    NoteItem(id = "b", text = "Brot", position = 2)
                )
            )
        )
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.ItemCheckedChanged("a", true))
        viewModel.onEvent(NotesEvent.ItemTextChanged("b", "Vollkorn"))
        viewModel.onEvent(NotesEvent.ItemTextChanged("b", "Vollkornbrot"))
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.itemTextWrites)
        assertEquals(true, storedNote("list")?.items?.first { it.id == "a" }?.checked)
        assertEquals("Vollkornbrot", storedNote("list")?.items?.first { it.id == "b" }?.text)

        viewModel.onEvent(NotesEvent.DeleteItem("a"))
        settle()

        assertEquals(listOf("b"), storedNote("list")?.items?.map { it.id })
    }

    @Test
    fun checkingAnItemDoesNotBumpNoteOrderOrUpdatedAt() = runTest(testDispatcher) {
        val olderList = SharedNote(
            id = "olderList",
            type = NoteType.CHECKLIST,
            updatedAt = 100L,
            items = listOf(NoteItem(id = "i1", text = "Milch", checked = false))
        )
        val newerNote = SharedNote(
            id = "newerNote",
            title = "Neu",
            updatedAt = 200L
        )
        seed(olderList, newerNote)
        assertEquals(listOf("newerNote", "olderList"), state.notes.map { it.id })

        now = 500L
        viewModel.onEvent(NotesEvent.OpenNote("olderList"))
        settle()

        // Checking an item does not count as a content change for sorting
        viewModel.onEvent(NotesEvent.ItemCheckedChanged("i1", true))
        settle()

        assertEquals(100L, storedNote("olderList")?.updatedAt)
        assertEquals(listOf("newerNote", "olderList"), state.notes.map { it.id })

        // Real content change (e.g. adding an item) does bump updatedAt and reorders
        viewModel.onEvent(NotesEvent.AddItem("Brot"))
        settle()

        assertEquals(500L, storedNote("olderList")?.updatedAt)
        assertEquals(listOf("olderList", "newerNote"), state.notes.map { it.id })
    }

    @Test
    fun deletingAnItemDropsItsPendingTextEdit() = runTest(testDispatcher) {
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, items = listOf(NoteItem(id = "a", text = "Milch"))))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.ItemTextChanged("a", "Hafermilch"))
        viewModel.onEvent(NotesEvent.DeleteItem("a"))
        testScheduler.advanceUntilIdle()

        assertEquals(0, repository.itemTextWrites)
        assertTrue(storedNote("list")?.items.orEmpty().isEmpty())
    }

    @Test
    fun clearCheckedItemsRemovesOnlyCheckedOnes() = runTest(testDispatcher) {
        seed(
            SharedNote(
                id = "list",
                type = NoteType.CHECKLIST,
                items = listOf(
                    NoteItem(id = "a", text = "Milch", checked = true),
                    NoteItem(id = "b", text = "Brot"),
                    NoteItem(id = "c", text = "Käse", checked = true)
                )
            )
        )
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.ClearCheckedItems)
        settle()

        assertEquals(listOf("b"), storedNote("list")?.items?.map { it.id })
    }

    @Test
    fun textNoteTurnsIntoChecklistAndBack() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Einkauf", body = "Milch"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        // The latest typed body counts, even before its debounced write
        viewModel.onEvent(NotesEvent.BodyChanged("- Milch\n- Brot\n\n[x] Kaffee"))
        viewModel.onEvent(NotesEvent.ConvertNoteType)
        settle()

        val checklist = requireNotNull(storedNote("n1"))
        assertEquals(NoteType.CHECKLIST, checklist.type)
        assertEquals("", checklist.body)
        assertEquals(listOf("Milch", "Brot", "Kaffee"), checklist.items.sortedBy { it.position }.map { it.text })
        assertEquals(listOf(false, false, true), checklist.items.sortedBy { it.position }.map { it.checked })
        assertTrue(checklist.items.all { it.createdBy == "uid_alex" })

        viewModel.onEvent(NotesEvent.ConvertNoteType)
        settle()

        val text = requireNotNull(storedNote("n1"))
        assertEquals(NoteType.TEXT, text.type)
        assertEquals("Milch\nBrot\n[x] Kaffee", text.body)
        assertTrue(text.items.isEmpty())
        assertEquals("n1", state.openNote?.id)
    }

    @Test
    fun deletingHidesTheNoteAtOnceAndOnlyDeletesAfterTheSnackbar() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Geschenkideen"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        viewModel.onEvent(NotesEvent.DeleteNote)
        settle()

        assertNull(state.openNote)
        assertFalse(state.hasAnyNotes)
        assertNotNull(storedNote("n1"))
        val action = requireNotNull(snackbar().action)
        assertEquals(UiText.Plural(R.plurals.notes_deleted, 1), snackbar().text)
        assertEquals(NotesEvent.UndoDeleteNotes(setOf("n1")), action.onActionPerformed)

        viewModel.onEvent(action.onDismissed as NotesEvent)
        settle()

        assertNull(storedNote("n1"))
        assertFalse(state.hasAnyNotes)
    }

    @Test
    fun undoBringsADeletedNoteBack() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Geschenkideen"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()
        viewModel.onEvent(NotesEvent.DeleteNote)
        settle()

        viewModel.onEvent(snackbar().action?.onActionPerformed as NotesEvent)
        // A late dismissal of the undone snackbar must not delete it after all
        viewModel.onEvent(NotesEvent.CommitDeleteNotes(setOf("n1")))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("n1"), state.notes.map { it.id })
        assertNotNull(storedNote("n1"))
    }

    @Test
    fun deletingSelectedNotesShowsTheCountAndCanBeUndone() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "a"), SharedNote(id = "n2", title = "b"), SharedNote(id = "n3", title = "c"))
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n3"))

        viewModel.onEvent(NotesEvent.DeleteSelected)
        settle()

        assertEquals(listOf("n2"), state.notes.map { it.id })
        assertFalse(state.isSelecting)
        val effect = snackbar()
        assertEquals(UiText.Plural(R.plurals.notes_deleted, 2), effect.text)

        viewModel.onEvent(effect.action?.onActionPerformed as NotesEvent)
        settle()

        assertEquals(setOf("n1", "n2", "n3"), state.notes.map { it.id }.toSet())
        assertEquals(3, repository.notes(SPACE_ID).size)
    }

    @Test
    fun selectAllSelectsAllVisibleNotes() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "a"),
            SharedNote(id = "n2", title = "b", labelId = DefaultNoteLabelIds.IDEAS),
            SharedNote(id = "n3", title = "c")
        )
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.SelectAll)
        settle()

        assertEquals(setOf("n1", "n2", "n3"), state.selectedNoteIds)

        // With an active filter, SelectAll only selects notes matching the filter
        viewModel.onEvent(NotesEvent.ClearSelection)
        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.IDEAS))
        settle()

        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n2"))
        viewModel.onEvent(NotesEvent.SelectAll)
        settle()

        assertEquals(setOf("n2"), state.selectedNoteIds)
    }

    @Test
    fun togglePinnedSelectedPinsWhenSomeOrNoneArePinnedAndUnpinsWhenAllArePinned() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "a", pinned = false),
            SharedNote(id = "n2", title = "b", pinned = true),
            SharedNote(id = "n3", title = "c", pinned = false)
        )
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n2"))

        // Not all selected notes are pinned -> pin all of them
        viewModel.onEvent(NotesEvent.TogglePinnedSelected)
        settle()

        assertTrue(storedNote("n1")?.pinned == true)
        assertTrue(storedNote("n2")?.pinned == true)
        assertFalse(storedNote("n3")?.pinned == true)
        assertFalse(state.isSelecting)

        // Now select n1 and n2 (both are pinned) -> unpin all of them
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n2"))
        viewModel.onEvent(NotesEvent.TogglePinnedSelected)
        settle()

        assertFalse(storedNote("n1")?.pinned == true)
        assertFalse(storedNote("n2")?.pinned == true)
        assertFalse(state.isSelecting)
    }

    @Test
    fun changeSelectedLabelUpdatesAllSelectedNotesAndClearsSelection() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "a"),
            SharedNote(id = "n2", title = "b", labelId = DefaultNoteLabelIds.SHOPPING),
            SharedNote(id = "n3", title = "c")
        )
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n2"))

        viewModel.onEvent(NotesEvent.ChangeSelectedLabel(DefaultNoteLabelIds.IDEAS))
        settle()

        assertEquals(DefaultNoteLabelIds.IDEAS, storedNote("n1")?.labelId)
        assertEquals(DefaultNoteLabelIds.IDEAS, storedNote("n2")?.labelId)
        assertNull(storedNote("n3")?.labelId)
        assertFalse(state.isSelecting)

        // Remove label with null
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n2"))
        viewModel.onEvent(NotesEvent.ChangeSelectedLabel(null))
        settle()

        assertNull(storedNote("n1")?.labelId)
        assertNull(storedNote("n2")?.labelId)
        assertFalse(state.isSelecting)
    }

    @Test
    fun failingPinOrLabelShowsSnackbar() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "a"))
        repository.failWrites = true

        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.TogglePinnedSelected)
        settle()

        assertEquals(UiText.Resource(R.string.note_save_failed), snackbar().text)

        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        viewModel.onEvent(NotesEvent.ChangeSelectedLabel("travel"))
        settle()

        assertEquals(UiText.Resource(R.string.note_save_failed), snackbar().text)
    }

    @Test
    fun aSecondDeletionCommitsTheFirstOne() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "a"), SharedNote(id = "n2", title = "b"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        viewModel.onEvent(NotesEvent.DeleteNote)
        viewModel.onEvent(NotesEvent.OpenNote("n2"))
        viewModel.onEvent(NotesEvent.DeleteNote)
        settle()

        assertNull(storedNote("n1"))
        assertNotNull(storedNote("n2"))
        // The first snackbar's undo comes too late now
        viewModel.onEvent(NotesEvent.UndoDeleteNotes(setOf("n1")))
        settle()
        assertFalse(state.hasAnyNotes)
    }

    @Test
    fun deletingAnItemCanBeUndoneWithTheSameIdTextStateAndPosition() = runTest(testDispatcher) {
        val milk = NoteItem(id = "a", text = "Milch", checked = true, createdBy = "uid_sam", position = 7)
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, items = listOf(milk, NoteItem(id = "b", text = "Brot", position = 8))))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.DeleteItem("a"))
        settle()

        assertEquals(listOf("b"), storedNote("list")?.items?.map { it.id })
        val effect = snackbar()
        assertEquals(UiText.Plural(R.plurals.note_items_deleted, 1), effect.text)
        assertNull(effect.action?.onDismissed)

        viewModel.onEvent(effect.action?.onActionPerformed as NotesEvent)
        settle()

        assertEquals(milk, storedNote("list")?.items?.first { it.id == "a" })
    }

    @Test
    fun clearedCheckedItemsCanBeUndone() = runTest(testDispatcher) {
        val done = listOf(
            NoteItem(id = "a", text = "Milch", checked = true, position = 1),
            NoteItem(id = "c", text = "Eier", checked = true, position = 3)
        )
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, items = done + NoteItem(id = "b", text = "Brot", position = 2)))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.ClearCheckedItems)
        settle()
        val effect = snackbar()
        assertEquals(UiText.Plural(R.plurals.note_items_deleted, 2), effect.text)
        assertEquals(listOf("b"), storedNote("list")?.items?.map { it.id })

        viewModel.onEvent(effect.action?.onActionPerformed as NotesEvent)
        settle()

        assertEquals(done, storedNote("list")?.items?.filter { it.checked }?.sortedBy { it.position })
    }

    @Test
    fun undoRestoresTheTypedTextOfADeletedItem() = runTest(testDispatcher) {
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, items = listOf(NoteItem(id = "a", text = "Milch"))))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.ItemTextChanged("a", "Hafermilch"))
        viewModel.onEvent(NotesEvent.DeleteItem("a"))
        settle()
        viewModel.onEvent(snackbar().action?.onActionPerformed as NotesEvent)
        testScheduler.advanceUntilIdle()

        assertEquals("Hafermilch", storedNote("list")?.items?.single()?.text)
    }

    @Test
    fun partnerChangesShowUpInTheOpenNote() = runTest(testDispatcher) {
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, title = "Einkauf"))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        repository.addNoteItem(SPACE_ID, "list", NoteItem(id = "p1", text = "Erdbeeren", createdBy = "uid_sam"), updatedAt = 5)
        settle()

        assertEquals(listOf("Erdbeeren"), state.openNote?.items?.map { it.text })
    }

    @Test
    fun noteDeletedByPartnerClosesTheEditor() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        repository.deleteNote(SPACE_ID, "n1")
        settle()

        assertNull(state.openNote)
        assertEquals(
            UiText.Resource(R.string.note_deleted_by_partner, listOf("Sam")),
            snackbar().text
        )
    }

    @Test
    fun switchingSpacesClosesTheEditorAndResetsTheFilter() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino", labelId = DefaultNoteLabelIds.IDEAS))
        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.IDEAS))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        space.value = CoupleSpace(id = "space_2")
        settle()

        assertNull(state.openNote)
        assertNull(state.labelFilter)
        assertFalse(state.hasAnyNotes)
    }

    @Test
    fun defaultLabelsApplyUntilSomeoneChangesThem() = runTest(testDispatcher) {
        settle()

        assertEquals(DEFAULT_LABELS, state.labels)
        assertNull(repository.labels(SPACE_ID))
    }

    @Test
    fun firstSavedLabelAlsoStoresTheDefaults() = runTest(testDispatcher) {
        settle()

        viewModel.onEvent(NotesEvent.OpenLabelManager)
        // Typed but never added with "+": "Fertig" still takes it
        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged("  Lissabon   2027 "))
        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()

        val stored = requireNotNull(repository.labels(SPACE_ID))
        assertNull(state.labelEditor)
        assertEquals(listOf("Einkauf", "Bucket List", "Ideen", "Lissabon 2027"), state.labels.map { it.name })
        assertEquals(4, stored.size)
        assertEquals(4L, stored.first { it.name == "Lissabon 2027" }.position)
        assertEquals(1, repository.labelWrites)
    }

    @Test
    fun blankAndDuplicateLabelsAreNotAdded() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged("   "))
        viewModel.onEvent(NotesEvent.DraftAddLabel)
        settle()
        assertEquals(3, state.labelEditor?.drafts?.size)

        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged(" einkauf "))
        viewModel.onEvent(NotesEvent.DraftAddLabel)
        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()

        assertFalse(requireNotNull(state.labelEditor).canSave)
        assertEquals(3, state.labelEditor?.drafts?.size)
        assertNull(repository.labels(SPACE_ID))
    }

    @Test
    fun editsAreOnlySavedWithFertig() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftRenameLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt "))
        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged("Filme"))
        viewModel.onEvent(NotesEvent.DraftAddLabel)
        viewModel.onEvent(NotesEvent.DraftToggleDeleteLabel(DefaultNoteLabelIds.IDEAS))
        testScheduler.advanceUntilIdle()
        assertNull(repository.labels(SPACE_ID))

        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()

        assertEquals(listOf("Supermarkt", "Bucket List", "Filme"), state.labels.map { it.name })
        assertEquals(1, repository.labelWrites)
    }

    @Test
    fun cancelDiscardsEverything() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftRenameLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt"))
        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged("Filme"))
        viewModel.onEvent(NotesEvent.DraftToggleDeleteLabel(DefaultNoteLabelIds.IDEAS))
        viewModel.onEvent(NotesEvent.CancelLabels)
        testScheduler.advanceUntilIdle()

        assertNull(state.labelEditor)
        assertEquals(DEFAULT_LABELS, state.labels)
        assertEquals(0, repository.labelWrites)
    }

    @Test
    fun closingWithChangesAsksFirst() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.RequestCloseLabels)
        settle()
        assertNull("Nothing to lose closes right away", state.labelEditor)

        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged("Filme"))
        viewModel.onEvent(NotesEvent.RequestCloseLabels)
        settle()
        assertTrue(requireNotNull(state.labelEditor).showDiscardPrompt)

        viewModel.onEvent(NotesEvent.KeepEditingLabels)
        settle()
        assertEquals("Filme", state.labelEditor?.newName)
        assertFalse(requireNotNull(state.labelEditor).showDiscardPrompt)

        viewModel.onEvent(NotesEvent.RequestCloseLabels)
        viewModel.onEvent(NotesEvent.ConfirmDiscardLabels)
        testScheduler.advanceUntilIdle()
        assertNull(state.labelEditor)
        assertEquals(0, repository.labelWrites)
    }

    @Test
    fun fertigWithoutChangesWritesNothing() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftToggleDeleteLabel(DefaultNoteLabelIds.IDEAS))
        viewModel.onEvent(NotesEvent.DraftToggleDeleteLabel(DefaultNoteLabelIds.IDEAS))
        viewModel.onEvent(NotesEvent.SaveLabels)
        testScheduler.advanceUntilIdle()

        assertNull(state.labelEditor)
        assertEquals(0, repository.labelWrites)
    }

    @Test
    fun deletingALabelKeepsItsNotesUnlabeledAndInPlace() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Wocheneinkauf", labelId = DefaultNoteLabelIds.SHOPPING, updatedAt = 7),
            SharedNote(id = "n2", title = "Reiseideen", labelId = DefaultNoteLabelIds.IDEAS, updatedAt = 9)
        )
        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.SHOPPING))
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftRenameLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt"))
        viewModel.onEvent(NotesEvent.DraftToggleDeleteLabel(DefaultNoteLabelIds.SHOPPING))
        now = 50L

        viewModel.onEvent(NotesEvent.SaveLabels)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(DefaultNoteLabelIds.BUCKET_LIST, DefaultNoteLabelIds.IDEAS), state.labels.map { it.id })
        assertNull(state.labelFilter)
        assertEquals(listOf("n2", "n1"), state.notes.map { it.id })
        assertNull(storedNote("n1")?.labelId)
        assertEquals(7L, storedNote("n1")?.updatedAt)
        assertEquals(DefaultNoteLabelIds.IDEAS, storedNote("n2")?.labelId)
    }

    @Test
    fun deletedDefaultsDoNotComeBack() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        listOf(DefaultNoteLabelIds.SHOPPING, DefaultNoteLabelIds.BUCKET_LIST, DefaultNoteLabelIds.IDEAS).forEach {
            viewModel.onEvent(NotesEvent.DraftToggleDeleteLabel(it))
        }
        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()

        assertTrue(state.labels.isEmpty())
        assertEquals(emptyList<NoteLabel>(), repository.labels(SPACE_ID))
        assertEquals(1, repository.labelWrites)
    }

    @Test
    fun partnerLabelChangesShowUp() = runTest(testDispatcher) {
        settle()

        repository.setLabels(SPACE_ID, listOf(NoteLabel("travel", "Lissabon", 1)))
        settle()

        assertEquals(listOf("Lissabon"), state.labels.map { it.name })
    }

    @Test
    fun partnerChangesMergeIntoTheOpenEditor() = runTest(testDispatcher) {
        repository.setLabels(SPACE_ID, listOf(NoteLabel("shopping", "Einkauf", 1), NoteLabel("ideas", "Ideen", 2)))
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        settle()
        viewModel.onEvent(NotesEvent.DraftRenameLabel("shopping", "Supermarkt"))

        repository.setLabels(
            SPACE_ID,
            listOf(NoteLabel("shopping", "Laden", 1), NoteLabel("ideas", "Geschenke", 2), NoteLabel("travel", "Reisen", 3))
        )
        settle()
        assertEquals(listOf("Supermarkt", "Geschenke", "Reisen"), state.labelEditor?.drafts?.map { it.name })

        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()
        assertEquals(listOf("Supermarkt", "Geschenke", "Reisen"), state.labels.map { it.name })
    }

    @Test
    fun renameOfALabelThePartnerDeletedIsSkipped() = runTest(testDispatcher) {
        repository.setLabels(SPACE_ID, listOf(NoteLabel("shopping", "Einkauf", 1), NoteLabel("ideas", "Ideen", 2)))
        viewModel.onEvent(NotesEvent.OpenLabelManager)
        settle()
        viewModel.onEvent(NotesEvent.DraftRenameLabel("ideas", "Geschenke"))

        repository.setLabels(SPACE_ID, listOf(NoteLabel("shopping", "Einkauf", 1)))
        settle()
        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()

        assertEquals(listOf("Einkauf"), state.labels.map { it.name })
    }

    @Test
    fun filterOnAMissingLabelShowsAllNotes() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino", labelId = DefaultNoteLabelIds.IDEAS))

        viewModel.onEvent(NotesEvent.FilterSelected("gone"))
        settle()

        assertNull(state.labelFilter)
        assertEquals(listOf("n1"), state.notes.map { it.id })
    }

    @Test
    fun labelUsageCountsNotesPerLabel() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", labelId = DefaultNoteLabelIds.IDEAS, title = "a"),
            SharedNote(id = "n2", labelId = DefaultNoteLabelIds.IDEAS, title = "b"),
            SharedNote(id = "n3", title = "c")
        )

        assertEquals(mapOf(DefaultNoteLabelIds.IDEAS to 2), state.labelUsage)
    }

    private val noteSaveFailed = CoupleEffect.Snackbar(UiText.Resource(R.string.note_save_failed))

    @Test
    fun failedNoteCreationClosesTheEditorWithAHint() = runTest(testDispatcher) {
        repository.failWrites = true

        viewModel.onEvent(NotesEvent.CreateNote)
        settle()

        assertNull(state.openNote)
        assertEquals(CoupleEffect.Snackbar(UiText.Resource(R.string.note_create_failed)), snackbar())
    }

    @Test
    fun aFailingSeriesOfTextWritesShowsOneSnackbar() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()
        repository.failWrites = true

        listOf("Kino am", "Kino am Freitag").forEach { title ->
            viewModel.onEvent(NotesEvent.TitleChanged(title))
            testScheduler.advanceTimeBy(DEBOUNCE + 1)
            settle()
        }
        viewModel.onEvent(NotesEvent.BodyChanged("Popcorn"))
        testScheduler.advanceTimeBy(DEBOUNCE + 1)
        settle()
        assertEquals(1, effects.count { it == noteSaveFailed })

        // A write that goes through ends the series; the next failure is reported again
        repository.failWrites = false
        viewModel.onEvent(NotesEvent.TitleChanged("Kino am Samstag"))
        testScheduler.advanceTimeBy(DEBOUNCE + 1)
        settle()
        repository.failWrites = true
        viewModel.onEvent(NotesEvent.TitleChanged("Kino am Sonntag"))
        testScheduler.advanceTimeBy(DEBOUNCE + 1)
        settle()

        assertEquals(2, effects.count { it == noteSaveFailed })
        assertEquals("Kino am Samstag", storedNote("n1")?.title)
    }

    @Test
    fun successfulNoteWritesShowNoSnackbar() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Urlaub"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        viewModel.onEvent(NotesEvent.TogglePinned)
        viewModel.onEvent(NotesEvent.TitleChanged("Urlaub 2027"))
        testScheduler.advanceTimeBy(DEBOUNCE + 1)
        settle()

        assertTrue(effects.isEmpty())
    }

    @Test
    fun failedPinAndItemWritesShowASnackbar() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Einkauf", type = NoteType.CHECKLIST))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()
        repository.failWrites = true

        viewModel.onEvent(NotesEvent.TogglePinned)
        settle()
        assertEquals(noteSaveFailed, snackbar())

        viewModel.onEvent(NotesEvent.AddItem("Milch"))
        settle()
        assertEquals(2, effects.count { it == noteSaveFailed })
        assertFalse(storedNote("n1")?.pinned ?: true)
    }

    @Test
    fun failedNoteDeletionBringsTheNoteBack() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Kino"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        viewModel.onEvent(NotesEvent.DeleteNote)
        settle()
        assertTrue(state.notes.none { it.id == "n1" })
        repository.failWrites = true

        viewModel.onEvent(NotesEvent.CommitDeleteNotes(setOf("n1")))
        settle()

        assertTrue(state.notes.any { it.id == "n1" })
        assertEquals(CoupleEffect.Snackbar(UiText.Resource(R.string.note_delete_failed)), snackbar())
    }

    @Test
    fun failedLabelSaveShowsASnackbar() = runTest(testDispatcher) {
        settle()
        repository.failWrites = true

        viewModel.onEvent(NotesEvent.OpenLabelManager)
        viewModel.onEvent(NotesEvent.DraftNewLabelNameChanged("Filme"))
        viewModel.onEvent(NotesEvent.SaveLabels)
        settle()

        assertNull(repository.labels(SPACE_ID))
        assertEquals(CoupleEffect.Snackbar(UiText.Resource(R.string.note_labels_save_failed)), snackbar())
    }

    @Test
    fun createChecklistCreatesNoteWithChecklistType() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.CreateChecklist)
        settle()

        val open = requireNotNull(state.openNote)
        assertEquals(NoteType.CHECKLIST, open.type)
        assertEquals(open.id, storedNote(open.id)?.id)
        assertEquals(NoteType.CHECKLIST, storedNote(open.id)?.type)
    }

    @Test
    fun insertItemAfterInsertsNewItemDirectlyBelowAndShiftsSubsequentItems() = runTest(testDispatcher) {
        val itemA = NoteItem(id = "a", text = "Milch", position = 1)
        val itemB = NoteItem(id = "b", text = "Brot", position = 2)
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, items = listOf(itemA, itemB)))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.InsertItemAfter(afterItemId = "a", newItemId = "new_item"))
        settle()

        val items = requireNotNull(storedNote("list")?.items)
        val ordered = NoteOrganizer.orderedItems(items)
        assertEquals(listOf("a", "new_item", "b"), ordered.map { it.id })
        assertEquals(2L, items.first { it.id == "new_item" }.position)
        assertEquals(3L, items.first { it.id == "b" }.position)
    }

    @Test
    fun deletingAnEmptyItemDoesNotShowUndoSnackbar() = runTest(testDispatcher) {
        val emptyItem = NoteItem(id = "empty", text = "", position = 1)
        seed(SharedNote(id = "list", type = NoteType.CHECKLIST, items = listOf(emptyItem)))
        viewModel.onEvent(NotesEvent.OpenNote("list"))
        settle()

        viewModel.onEvent(NotesEvent.DeleteItem("empty"))
        settle()

        assertTrue(requireNotNull(storedNote("list")?.items).isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun authorNamesMapsSpaceMembersToTheirNames() = runTest(testDispatcher) {
        settle()
        assertEquals(mapOf("uid_alex" to "Alex", "uid_sam" to "Sam"), state.authorNames)
    }

    @Test
    fun hasUnreadNotesIsTrueWhenPartnerNoteUpdatedAfterLastSeen() = runTest(testDispatcher) {
        sessionStore.saveNotesLastSeen(SPACE_ID, 500L)
        seed(SharedNote(id = "partner_note", updatedBy = "uid_sam", updatedAt = 800L))
        settle()

        assertTrue(state.hasUnreadNotes)
    }

    @Test
    fun hasUnreadNotesIsFalseWhenUpdatedByOwnUid() = runTest(testDispatcher) {
        sessionStore.saveNotesLastSeen(SPACE_ID, 500L)
        seed(SharedNote(id = "my_note", updatedBy = "uid_alex", updatedAt = 800L))
        settle()

        assertFalse(state.hasUnreadNotes)
    }

    @Test
    fun tabOpenedRecordsUnreadNotesAndUpdatesLastSeenInSessionStore() = runTest(testDispatcher) {
        sessionStore.saveNotesLastSeen(SPACE_ID, 500L)
        seed(
            SharedNote(id = "n1", updatedBy = "uid_sam", updatedAt = 700L),
            SharedNote(id = "n2", updatedBy = "uid_alex", updatedAt = 700L),
            SharedNote(id = "n3", updatedBy = "uid_sam", updatedAt = 400L)
        )
        settle()
        assertTrue(state.hasUnreadNotes)

        now = 1_500L
        viewModel.onEvent(NotesEvent.TabOpened)
        settle()

        assertEquals(setOf("n1"), state.unreadNoteIds)
        assertEquals(1_500L, sessionStore.notesLastSeenFlows[SPACE_ID]?.value)
        assertFalse(state.hasUnreadNotes)
    }

    @Test
    fun openingUnreadNoteClearsItsUnreadDot() = runTest(testDispatcher) {
        sessionStore.saveNotesLastSeen(SPACE_ID, 500L)
        seed(
            SharedNote(id = "n1", updatedBy = "uid_sam", updatedAt = 700L),
            SharedNote(id = "n2", updatedBy = "uid_sam", updatedAt = 800L)
        )
        settle()

        viewModel.onEvent(NotesEvent.TabOpened)
        settle()
        assertEquals(setOf("n1", "n2"), state.unreadNoteIds)

        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()
        assertEquals(setOf("n2"), state.unreadNoteIds)

        viewModel.onEvent(NotesEvent.CloseNote)
        settle()
        assertEquals(setOf("n2"), state.unreadNoteIds)
    }

    @Test
    fun partnerNoteArrivingWhileTabIsOpenIsMarkedAsUnread() = runTest(testDispatcher) {
        sessionStore.saveNotesLastSeen(SPACE_ID, 500L)
        settle()

        now = 1_000L
        viewModel.onEvent(NotesEvent.TabOpened)
        settle()
        assertTrue(state.unreadNoteIds.isEmpty())

        seed(SharedNote(id = "incoming", updatedBy = "uid_sam", updatedAt = 1_200L))
        settle()

        assertEquals(setOf("incoming"), state.unreadNoteIds)
    }

    @Test
    fun searchFiltersNotesAndUpdatesUiState() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Wocheneinkauf", body = "Milch und Käse"),
            SharedNote(id = "n2", title = "Filmideen", body = "Komödie"),
            SharedNote(id = "n3", title = "Rezept", items = listOf(NoteItem(text = "Kase")))
        )

        assertFalse(state.isSearchOpen)
        assertEquals(3, state.notes.size)

        viewModel.onEvent(NotesEvent.OpenSearch)
        settle()
        assertTrue(state.isSearchOpen)
        assertEquals("", state.searchQuery)
        assertEquals(3, state.notes.size)

        viewModel.onEvent(NotesEvent.SearchQueryChanged("käse"))
        settle()
        assertEquals("käse", state.searchQuery)
        assertEquals(listOf("n1", "n3"), state.notes.map { it.id })

        viewModel.onEvent(NotesEvent.CloseSearch)
        settle()
        assertFalse(state.isSearchOpen)
        assertEquals("", state.searchQuery)
        assertEquals(3, state.notes.size)
    }

    @Test
    fun searchCombinedWithLabelFilter() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Einkauf", body = "Käse", labelId = DefaultNoteLabelIds.SHOPPING),
            SharedNote(id = "n2", title = "Idee", body = "Käse", labelId = DefaultNoteLabelIds.IDEAS),
            SharedNote(id = "n3", title = "Einkauf", body = "Brot", labelId = DefaultNoteLabelIds.SHOPPING)
        )

        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.SHOPPING))
        settle()
        assertEquals(listOf("n1", "n3"), state.notes.map { it.id })

        viewModel.onEvent(NotesEvent.OpenSearch)
        viewModel.onEvent(NotesEvent.SearchQueryChanged("kase"))
        settle()
        assertEquals(listOf("n1"), state.notes.map { it.id })
    }

    @Test
    fun selectAllSelectsOnlyVisibleSearchResults() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Einkauf", body = "Käse"),
            SharedNote(id = "n2", title = "Notiz", body = "Käse"),
            SharedNote(id = "n3", title = "Urlaub", body = "Strand")
        )

        viewModel.onEvent(NotesEvent.OpenSearch)
        viewModel.onEvent(NotesEvent.SearchQueryChanged("kase"))
        settle()

        viewModel.onEvent(NotesEvent.SelectAll)
        settle()

        assertEquals(setOf("n1", "n2"), state.selectedNoteIds)
    }

    @Test
    fun noteLongPressHintShownWhenTwoNotesAndDismissedOnSelection() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Notiz 1"),
            SharedNote(id = "n2", title = "Notiz 2")
        )
        settle()

        assertEquals(GestureHint.NOTE_LONG_PRESS, state.activeGestureHint)

        viewModel.onEvent(NotesEvent.ToggleNoteSelection("n1"))
        settle()

        assertTrue(sessionStore.dismissedGestureHintsFlow.value.contains(GestureHint.NOTE_LONG_PRESS))
        assertNull(state.activeGestureHint)
    }

    @Test
    fun noteLongPressHintDismissedViaEvent() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Notiz 1"),
            SharedNote(id = "n2", title = "Notiz 2")
        )
        settle()

        assertEquals(GestureHint.NOTE_LONG_PRESS, state.activeGestureHint)

        viewModel.onEvent(NotesEvent.DismissGestureHint(GestureHint.NOTE_LONG_PRESS))
        settle()

        assertTrue(sessionStore.dismissedGestureHintsFlow.value.contains(GestureHint.NOTE_LONG_PRESS))
        assertNull(state.activeGestureHint)
    }

    @Test
    fun checklistDragHintShownInChecklistWithTwoItemsAndDismissedOnReorder() = runTest(testDispatcher) {
        seed(
            SharedNote(
                id = "c1",
                type = NoteType.CHECKLIST,
                items = listOf(
                    NoteItem(id = "i1", text = "Milch"),
                    NoteItem(id = "i2", text = "Brot")
                )
            )
        )
        settle()

        viewModel.onEvent(NotesEvent.OpenNote("c1"))
        settle()

        assertEquals(GestureHint.CHECKLIST_DRAG, state.activeGestureHint)

        viewModel.onEvent(NotesEvent.ReorderItems(listOf("i2", "i1")))
        settle()

        assertTrue(sessionStore.dismissedGestureHintsFlow.value.contains(GestureHint.CHECKLIST_DRAG))
        assertNull(state.activeGestureHint)
    }

    @Test
    fun checklistDragHintDismissedViaEvent() = runTest(testDispatcher) {
        seed(
            SharedNote(
                id = "c1",
                type = NoteType.CHECKLIST,
                items = listOf(
                    NoteItem(id = "i1", text = "Milch"),
                    NoteItem(id = "i2", text = "Brot")
                )
            )
        )
        settle()

        viewModel.onEvent(NotesEvent.OpenNote("c1"))
        settle()

        assertEquals(GestureHint.CHECKLIST_DRAG, state.activeGestureHint)

        viewModel.onEvent(NotesEvent.DismissGestureHint(GestureHint.CHECKLIST_DRAG))
        settle()

        assertTrue(sessionStore.dismissedGestureHintsFlow.value.contains(GestureHint.CHECKLIST_DRAG))
        assertNull(state.activeGestureHint)
    }

    private companion object {
        const val SPACE_ID = "space_1"
        const val DEBOUNCE = 500L
        val DEFAULT_LABELS = listOf(
            NoteLabel(DefaultNoteLabelIds.SHOPPING, "Einkauf", 1),
            NoteLabel(DefaultNoteLabelIds.BUCKET_LIST, "Bucket List", 2),
            NoteLabel(DefaultNoteLabelIds.IDEAS, "Ideen", 3)
        )
    }
}
