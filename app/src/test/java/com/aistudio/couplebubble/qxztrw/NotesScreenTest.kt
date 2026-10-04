package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.NoteType
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.NotesUiState
import com.aistudio.couplebubble.qxztrw.ui.screens.NotesScreen
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NotesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val events = mutableListOf<NotesEvent>()

    private val labels = listOf(
        NoteLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt", 1),
        NoteLabel(DefaultNoteLabelIds.IDEAS, "Ideen", 2),
        NoteLabel("travel", "Lissabon 2027", 3)
    )

    private val shoppingList = SharedNote(
        id = "shopping",
        title = "Wocheneinkauf",
        type = NoteType.CHECKLIST,
        labelId = DefaultNoteLabelIds.SHOPPING,
        pinned = true,
        createdBy = "uid_alex",
        items = listOf(
            NoteItem("i1", "Hafermilch", checked = true, position = 1),
            NoteItem("i2", "Feta", position = 2),
            NoteItem("i3", "Basilikum", position = 3),
            NoteItem("i4", "Blumen", position = 4)
        )
    )
    private val ideaNote = SharedNote(id = "idea", title = "Geschenkideen", body = "Fotobuch von Lissabon")

    private fun setContent(state: NotesUiState) {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                NotesScreen(state = state, onEvent = { events += it })
            }
        }
    }

    @Test
    fun emptyStateOffersToCreateTheFirstNote() {
        setContent(NotesUiState())

        composeRule.onNodeWithTag("empty_notes_card").assertIsDisplayed()
        composeRule.onNodeWithTag("add_first_note_button").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.CreateNote), events)
    }

    @Test
    fun fabCreatesANote() {
        setContent(NotesUiState(notes = listOf(ideaNote), hasAnyNotes = true))

        composeRule.onNodeWithTag("notes_fab").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.CreateNote), events)
    }

    @Test
    fun cardsShowContentProgressAndPinnedSection() {
        setContent(NotesUiState(notes = listOf(shoppingList, ideaNote), hasAnyNotes = true, labels = labels))

        composeRule.onNodeWithText("SUPERMARKT").assertIsDisplayed()
        composeRule.onNodeWithText("Wocheneinkauf").assertIsDisplayed()
        composeRule.onNodeWithText("Feta").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.note_progress, 1, 4)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.notes_section_pinned).uppercase()).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.notes_section_others).uppercase()).assertIsDisplayed()
        composeRule.onNodeWithText("Fotobuch von Lissabon").assertIsDisplayed()
    }

    @Test
    fun checkedItemsAreLeftOutOfTheCardPreview() {
        setContent(NotesUiState(notes = listOf(shoppingList), hasAnyNotes = true))

        composeRule.onNodeWithText("Hafermilch").assertDoesNotExist()
    }

    @Test
    fun tappingACardOpensTheNote() {
        setContent(NotesUiState(notes = listOf(ideaNote), hasAnyNotes = true))

        composeRule.onNodeWithTag("note_card_idea").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.OpenNote("idea")), events)
    }

    @Test
    fun filterChipsShowTheSpaceLabelsAndSelectOnlyOnChange() {
        setContent(NotesUiState(notes = listOf(ideaNote), hasAnyNotes = true, labels = labels))

        composeRule.onNodeWithText("Lissabon 2027").assertIsDisplayed()
        composeRule.onNodeWithTag("notes_filter_ALL").performClick()
        composeRule.onNodeWithTag("notes_filter_travel").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.FilterSelected("travel")), events)
    }

    @Test
    fun editChipOpensTheLabelManager() {
        setContent(NotesUiState(notes = listOf(ideaNote), hasAnyNotes = true, labels = labels))

        composeRule.onNodeWithTag("manage_labels_chip").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.ShowLabelManager(true)), events)
    }

    @Test
    fun cardOfANoteWithADeletedLabelShowsNoLabel() {
        setContent(NotesUiState(notes = listOf(shoppingList), hasAnyNotes = true, labels = labels.drop(1)))

        composeRule.onNodeWithText("SUPERMARKT").assertDoesNotExist()
        composeRule.onNodeWithText("Wocheneinkauf").assertIsDisplayed()
    }

    @Test
    fun filterWithoutMatchesShowsAHint() {
        setContent(NotesUiState(notes = emptyList(), hasAnyNotes = true, labelFilter = DefaultNoteLabelIds.BUCKET_LIST))

        composeRule.onNodeWithTag("notes_filter_empty").assertIsDisplayed()
        composeRule.onNodeWithTag("empty_notes_card").assertDoesNotExist()
    }

    @Test
    fun openNoteShowsTheEditorInsteadOfTheOverview() {
        setContent(NotesUiState(notes = listOf(ideaNote), hasAnyNotes = true, openNote = ideaNote))

        composeRule.onNodeWithTag("note_editor").assertIsDisplayed()
        composeRule.onNodeWithTag("notes_grid").assertDoesNotExist()
        assertTrue(events.isEmpty())
    }
}
