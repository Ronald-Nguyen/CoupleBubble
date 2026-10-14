package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.aistudio.couplebubble.qxztrw.domain.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NotesSemanticsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<NotesEvent>()
    private val labels = listOf(NoteLabel(DefaultNoteLabelIds.SHOPPING, "Supermarkt", 1))

    private val checklist = SharedNote(
        id = "list",
        title = "Wocheneinkauf",
        type = NoteType.CHECKLIST,
        labelId = DefaultNoteLabelIds.SHOPPING,
        pinned = true,
        items = listOf(
            NoteItem("oat", "Hafermilch", position = 1),
            NoteItem("feta", "Feta", position = 2),
            NoteItem("done", "Brot", checked = true, position = 3)
        )
    )

    private fun setEditor() {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                NoteEditorScreen(note = checklist, labels = labels, authorColors = emptyMap(), onEvent = { events += it })
            }
        }
    }

    private fun customAction(tag: String, label: String) {
        val actions = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        actions.first { it.label == label }.action()
    }

    @Test
    fun itemRowIsOneToggleableNodeWithItsText() {
        setEditor()

        composeRule.onNodeWithTag("note_item_feta")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Feta")))
            .performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(NotesEvent.ItemCheckedChanged("feta", true), events.last())
    }

    @Test
    fun customActionsMoveAndDeleteItems() {
        setEditor()

        customAction("note_item_oat", "Nach unten verschieben")
        composeRule.waitForIdle()
        assertEquals(NotesEvent.ReorderItems(listOf("feta", "oat")), events.last())

        customAction("note_item_oat", "Löschen")
        assertEquals(NotesEvent.DeleteItem("oat"), events.last())
    }

    @Test
    fun movingTheFirstItemUpDoesNothing() {
        setEditor()

        customAction("note_item_oat", "Nach oben verschieben")

        assertEquals(emptyList<NotesEvent>(), events.filterIsInstance<NotesEvent.ReorderItems>())
    }

    @Test
    fun checkedItemsOnlyOfferDeletion() {
        setEditor()

        val labels = composeRule.onNodeWithTag("note_item_done").fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.CustomActions).orEmpty().map { it.label }
        assertEquals(listOf("Löschen"), labels)
    }

    @Test
    fun noteCardReadsASummary() {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                NoteCard(
                    note = checklist,
                    labelName = "Supermarkt",
                    authorColorHex = null,
                    isSelected = false,
                    onClick = {},
                    onLongClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("note_card_list")
            .assertContentDescriptionContains("Wocheneinkauf, Etikett Supermarkt, 1 von 3 erledigt, Angeheftet")
    }

    @Test
    fun summaryLeavesOutMissingParts() {
        assertEquals("Ideen", noteCardSummary("Ideen", null, null, null, null))
    }

    @Test
    fun filterChipsExposeTheirSelection() {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                NotesScreen(state = NotesUiState(notes = listOf(checklist), hasAnyNotes = true, labels = labels), onEvent = {})
            }
        }

        composeRule.onNodeWithTag("notes_filter_ALL").assertIsSelected()
        composeRule.onNodeWithTag("notes_filter_${DefaultNoteLabelIds.SHOPPING}").assertIsNotSelected()
    }
}
