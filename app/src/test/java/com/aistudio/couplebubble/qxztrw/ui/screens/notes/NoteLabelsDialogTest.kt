package com.aistudio.couplebubble.qxztrw.ui.screens.notes

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.DraftLabel
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
class NoteLabelsDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val events = mutableListOf<NotesEvent>()

    private val drafts = listOf(
        DraftLabel("shopping", "Einkauf", "Einkauf"),
        DraftLabel("ideas", "Ideen", "Ideen")
    )

    private fun setContent(
        editor: LabelEditorState = LabelEditorState(drafts = drafts),
        usage: Map<String, Int> = emptyMap()
    ) {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                ManageLabelsContent(editor = editor, labelUsage = usage, onEvent = { events += it })
            }
        }
    }

    @Test
    fun showsEveryLabelWithItsUsage() {
        setContent(usage = mapOf("shopping" to 3))

        composeRule.onNodeWithText("Einkauf").assertIsDisplayed()
        composeRule.onNodeWithText("Ideen").assertIsDisplayed()
        composeRule.onNodeWithText(context.resources.getQuantityString(R.plurals.label_usage, 3, 3)).assertIsDisplayed()
    }

    @Test
    fun emptyListShowsAHint() {
        setContent(LabelEditorState())

        composeRule.onNodeWithText(context.getString(R.string.manage_labels_empty)).assertIsDisplayed()
    }

    @Test
    fun typingChangesOnlyTheDraft() {
        setContent()

        composeRule.onNodeWithTag("label_field_shopping").performTextReplacement("Supermarkt")

        assertEquals(listOf<NotesEvent>(NotesEvent.DraftRenameLabel("shopping", "Supermarkt")), events)
    }

    @Test
    fun duplicateNamesShowAnError() {
        setContent(LabelEditorState(drafts = listOf(drafts[0].copy(name = "ideen"), drafts[1])))

        composeRule.onAllNodesWithText(context.getString(R.string.label_name_taken)).assertCountEquals(2)
    }

    @Test
    fun emptyNameShowsAnError() {
        setContent(LabelEditorState(drafts = listOf(drafts[0], drafts[1].copy(name = ""))))

        composeRule.onNodeWithText(context.getString(R.string.label_name_empty)).assertIsDisplayed()
    }

    @Test
    fun deleteButtonOnlyMarksTheLabel() {
        setContent()

        composeRule.onNodeWithTag("delete_label_ideas").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.DraftToggleDeleteLabel("ideas")), events)
    }

    @Test
    fun markedLabelShowsTheConsequenceAndCanBeRestored() {
        setContent(
            LabelEditorState(drafts = listOf(drafts[0], drafts[1].copy(isDeleted = true))),
            usage = mapOf("ideas" to 2)
        )

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.label_pending_delete_usage, 2, 2)
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("restore_label_ideas").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.DraftToggleDeleteLabel("ideas")), events)
    }

    @Test
    fun newRowsAreMarkedAsUnsaved() {
        setContent(LabelEditorState(drafts = drafts + DraftLabel("new", "Lissabon", originalName = null)))

        composeRule.onNodeWithText(context.getString(R.string.label_new_hint)).assertIsDisplayed()
    }

    @Test
    fun newLabelIsTypedIntoTheDraftAndAddedWithEnter() {
        setContent()

        composeRule.onNodeWithTag("new_label_field").performTextInput("Lissabon")
        composeRule.onNodeWithTag("new_label_field").performImeAction()

        assertEquals(
            listOf(NotesEvent.DraftNewLabelNameChanged("Lissabon"), NotesEvent.DraftAddLabel),
            events
        )
    }

    @Test
    fun duplicateNewLabelCannotBeAdded() {
        setContent(LabelEditorState(drafts = drafts, newName = "einkauf"))

        composeRule.onNodeWithTag("add_label_button").assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.label_name_taken)).assertIsDisplayed()
        composeRule.onNodeWithTag("new_label_field").performImeAction()
        assertTrue(events.isEmpty())
    }
}
