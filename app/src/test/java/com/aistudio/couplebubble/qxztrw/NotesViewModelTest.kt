package com.aistudio.couplebubble.qxztrw

import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.NoteType
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.repository.MockNotesRepository
import com.aistudio.couplebubble.qxztrw.ui.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.NotesUiState
import com.aistudio.couplebubble.qxztrw.ui.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
            updatedAt: Long
        ): Result<Unit> {
            textWrites++
            return super.updateNoteText(coupleId, noteId, title, body, updatedAt)
        }

        override suspend fun setNoteItemText(
            coupleId: String,
            noteId: String,
            itemId: String,
            text: String,
            updatedAt: Long
        ): Result<Unit> {
            itemTextWrites++
            return super.setNoteItemText(coupleId, noteId, itemId, text, updatedAt)
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private val space = MutableStateFlow<CoupleSpace?>(
        CoupleSpace(
            id = SPACE_ID,
            partner1Id = "uid_alex",
            partner2Id = "uid_sam",
            partner1ColorHex = "#E65D2E",
            partner2ColorHex = "#4ECDC4"
        )
    )
    private val profile = MutableStateFlow<UserProfile?>(UserProfile(uid = "uid_alex"))
    private val repository = CountingNotesRepository()
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
            started = SharingStarted.Eagerly,
            clock = { now },
            textDebounceMillis = DEBOUNCE
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.settle() = testScheduler.runCurrent()

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
        assertEquals("Milch\nBrot\nKaffee", text.body)
        assertTrue(text.items.isEmpty())
        assertEquals("n1", state.openNote?.id)
    }

    @Test
    fun deleteNeedsConfirmationAndClosesTheEditor() = runTest(testDispatcher) {
        seed(SharedNote(id = "n1", title = "Geschenkideen"))
        viewModel.onEvent(NotesEvent.OpenNote("n1"))
        settle()

        viewModel.onEvent(NotesEvent.RequestDeleteNote)
        settle()
        assertEquals("n1", state.noteToDelete?.id)

        viewModel.onEvent(NotesEvent.DismissDeleteNote)
        settle()
        assertNull(state.noteToDelete)
        assertNotNull(storedNote("n1"))

        viewModel.onEvent(NotesEvent.RequestDeleteNote)
        viewModel.onEvent(NotesEvent.ConfirmDeleteNote)
        settle()

        assertNull(state.noteToDelete)
        assertNull(state.openNote)
        assertNull(storedNote("n1"))
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
    fun firstNewLabelAlsoStoresTheDefaults() = runTest(testDispatcher) {
        settle()

        viewModel.onEvent(NotesEvent.AddLabel("  Lissabon   2027 "))
        settle()

        val stored = requireNotNull(repository.labels(SPACE_ID))
        assertEquals(listOf("Einkauf", "Bucket List", "Ideen", "Lissabon 2027"), state.labels.map { it.name })
        assertEquals(4, stored.size)
        assertEquals(4L, stored.first { it.name == "Lissabon 2027" }.position)
    }

    @Test
    fun blankAndDuplicateLabelsAreNotAdded() = runTest(testDispatcher) {
        settle()

        viewModel.onEvent(NotesEvent.AddLabel("   "))
        viewModel.onEvent(NotesEvent.AddLabel(" einkauf "))
        settle()

        assertNull(repository.labels(SPACE_ID))
        assertEquals(DEFAULT_LABELS, state.labels)
    }

    @Test
    fun renamesAreDebouncedAndValidated() = runTest(testDispatcher) {
        settle()

        viewModel.onEvent(NotesEvent.RenameLabel(DefaultNoteLabelIds.SHOPPING, "Super"))
        viewModel.onEvent(NotesEvent.RenameLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt "))
        testScheduler.advanceTimeBy(DEBOUNCE - 1)
        settle()
        assertNull(repository.labels(SPACE_ID))

        testScheduler.advanceTimeBy(2)
        settle()
        assertEquals(listOf("Supermarkt", "Bucket List", "Ideen"), state.labels.map { it.name })

        viewModel.onEvent(NotesEvent.RenameLabel(DefaultNoteLabelIds.SHOPPING, "IDEEN"))
        viewModel.onEvent(NotesEvent.RenameLabel(DefaultNoteLabelIds.BUCKET_LIST, "  "))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("Supermarkt", "Bucket List", "Ideen"), state.labels.map { it.name })
    }

    @Test
    fun closingTheLabelManagerSavesPendingRenames() = runTest(testDispatcher) {
        viewModel.onEvent(NotesEvent.ShowLabelManager(true))
        settle()
        assertTrue(state.showLabelManager)

        viewModel.onEvent(NotesEvent.RenameLabel(DefaultNoteLabelIds.IDEAS, "Geschenke"))
        viewModel.onEvent(NotesEvent.ShowLabelManager(false))
        settle()

        assertFalse(state.showLabelManager)
        assertEquals("Geschenke", repository.labels(SPACE_ID)?.first { it.id == DefaultNoteLabelIds.IDEAS }?.name)
    }

    @Test
    fun deletingALabelKeepsItsNotesUnlabeledAndInPlace() = runTest(testDispatcher) {
        seed(
            SharedNote(id = "n1", title = "Wocheneinkauf", labelId = DefaultNoteLabelIds.SHOPPING, updatedAt = 7),
            SharedNote(id = "n2", title = "Reiseideen", labelId = DefaultNoteLabelIds.IDEAS, updatedAt = 9)
        )
        viewModel.onEvent(NotesEvent.FilterSelected(DefaultNoteLabelIds.SHOPPING))
        viewModel.onEvent(NotesEvent.RenameLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt"))
        now = 50L

        viewModel.onEvent(NotesEvent.DeleteLabel(DefaultNoteLabelIds.SHOPPING))
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
        settle()

        listOf(DefaultNoteLabelIds.SHOPPING, DefaultNoteLabelIds.BUCKET_LIST, DefaultNoteLabelIds.IDEAS).forEach {
            viewModel.onEvent(NotesEvent.DeleteLabel(it))
            settle()
        }

        assertTrue(state.labels.isEmpty())
        assertEquals(emptyList<NoteLabel>(), repository.labels(SPACE_ID))
    }

    @Test
    fun partnerLabelChangesShowUp() = runTest(testDispatcher) {
        settle()

        repository.saveNoteLabels(SPACE_ID, listOf(NoteLabel("travel", "Lissabon", 1)))
        settle()

        assertEquals(listOf("Lissabon"), state.labels.map { it.name })
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
