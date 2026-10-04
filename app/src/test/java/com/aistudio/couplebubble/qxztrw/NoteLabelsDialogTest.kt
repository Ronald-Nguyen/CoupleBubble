package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.ui.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.ManageLabelsContent
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

    private val labels = listOf(
        NoteLabel("shopping", "Einkauf", 1),
        NoteLabel("ideas", "Ideen", 2)
    )

    private fun setContent(labels: List<NoteLabel> = this.labels, usage: Map<String, Int> = emptyMap()) {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                ManageLabelsContent(labels = labels, labelUsage = usage, onEvent = { events += it })
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
        setContent(labels = emptyList())

        composeRule.onNodeWithText(context.getString(R.string.manage_labels_empty)).assertIsDisplayed()
    }

    @Test
    fun typingRenamesTheLabel() {
        setContent()

        composeRule.onNodeWithTag("label_field_shopping").performTextReplacement("Supermarkt")

        assertEquals(listOf<NotesEvent>(NotesEvent.RenameLabel("shopping", "Supermarkt")), events)
    }

    @Test
    fun renamingToATakenNameShowsAnError() {
        setContent()

        composeRule.onNodeWithTag("label_field_shopping").performTextReplacement("ideen")

        composeRule.onNodeWithText(context.getString(R.string.label_name_taken)).assertIsDisplayed()
    }

    @Test
    fun clearingANameShowsAnError() {
        setContent()

        composeRule.onNodeWithTag("label_field_ideas").performTextReplacement("")

        composeRule.onNodeWithText(context.getString(R.string.label_name_empty)).assertIsDisplayed()
    }

    @Test
    fun deleteButtonRemovesTheLabel() {
        setContent()

        composeRule.onNodeWithTag("delete_label_ideas").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.DeleteLabel("ideas")), events)
    }

    @Test
    fun newLabelIsAddedWithEnterAndTheButton() {
        setContent()

        composeRule.onNodeWithTag("new_label_field").performTextInput("Lissabon")
        composeRule.onNodeWithTag("new_label_field").performImeAction()
        composeRule.onNodeWithTag("new_label_field").performTextInput("Filme")
        composeRule.onNodeWithTag("add_label_button").performClick()

        assertEquals(listOf(NotesEvent.AddLabel("Lissabon"), NotesEvent.AddLabel("Filme")), events)
    }

    @Test
    fun duplicateNewLabelCannotBeAdded() {
        setContent()

        composeRule.onNodeWithTag("new_label_field").performTextInput("einkauf")

        composeRule.onNodeWithTag("add_label_button").assertIsNotEnabled()
        composeRule.onNodeWithText(context.getString(R.string.label_name_taken)).assertIsDisplayed()
        composeRule.onNodeWithTag("new_label_field").performImeAction()
        assertTrue(events.isEmpty())
    }
}
