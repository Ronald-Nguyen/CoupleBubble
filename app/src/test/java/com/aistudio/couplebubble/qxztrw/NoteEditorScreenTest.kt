package com.aistudio.couplebubble.qxztrw

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.model.NoteCategory
import com.aistudio.couplebubble.qxztrw.model.NoteItem
import com.aistudio.couplebubble.qxztrw.model.NoteType
import com.aistudio.couplebubble.qxztrw.model.SharedNote
import com.aistudio.couplebubble.qxztrw.ui.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.NoteEditorScreen
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
class NoteEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val events = mutableListOf<NotesEvent>()

    private val checklist = SharedNote(
        id = "list",
        title = "Wocheneinkauf",
        type = NoteType.CHECKLIST,
        category = NoteCategory.SHOPPING,
        items = listOf(
            NoteItem("milk", "Hafermilch", checked = true, createdBy = "uid_alex", position = 1),
            NoteItem("feta", "Feta", createdBy = "uid_sam", position = 2)
        )
    )
    private val textNote = SharedNote(id = "text", title = "WLAN", body = "Passwort steht am Router")

    private fun setContent(note: SharedNote, showDeleteDialog: Boolean = false) {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                NoteEditorScreen(
                    note = note,
                    authorColors = mapOf("uid_alex" to "#E65D2E", "uid_sam" to "#4ECDC4"),
                    showDeleteDialog = showDeleteDialog,
                    onEvent = { events += it }
                )
            }
        }
    }

    @Test
    fun checkedItemsSitInTheirOwnSectionWithAClearAction() {
        setContent(checklist)

        composeRule.onNodeWithText(context.getString(R.string.note_checked_section, 1).uppercase()).assertIsDisplayed()
        composeRule.onNodeWithTag("clear_checked_button").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.ClearCheckedItems), events)
    }

    @Test
    fun clearActionIsHiddenWithoutCheckedItems() {
        setContent(checklist.copy(items = checklist.items.map { it.copy(checked = false) }))

        composeRule.onNodeWithTag("clear_checked_button").assertDoesNotExist()
    }

    @Test
    fun checkingAnItemEmitsTheNewState() {
        setContent(checklist)

        composeRule.onNodeWithTag("note_item_checkbox_feta").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.ItemCheckedChanged("feta", true)), events)
    }

    @Test
    fun enterAddsTheItemAndKeepsTheEmptiedFieldFocused() {
        setContent(checklist)

        composeRule.onNodeWithTag("add_item_field").performTextInput("Brot")
        composeRule.onNodeWithTag("add_item_field").performImeAction()

        assertEquals(listOf<NotesEvent>(NotesEvent.AddItem("Brot")), events)
        composeRule.onNodeWithTag("add_item_field")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            .assertIsFocused()
    }

    @Test
    fun blankItemIsNotAdded() {
        setContent(checklist)

        composeRule.onNodeWithTag("add_item_field").performTextInput("   ")
        composeRule.onNodeWithTag("add_item_field").performImeAction()

        assertTrue(events.isEmpty())
    }

    @Test
    fun itemsCanBeRemoved() {
        setContent(checklist)

        composeRule.onNodeWithTag("note_item_delete_feta").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.DeleteItem("feta")), events)
    }

    @Test
    fun editingAnItemEmitsItsText() {
        setContent(checklist)

        composeRule.onNodeWithText("Feta").performTextReplacement("Feta & Oliven")

        assertEquals(listOf<NotesEvent>(NotesEvent.ItemTextChanged("feta", "Feta & Oliven")), events)
    }

    @Test
    fun typingTheTitleAndBodyEmitsChanges() {
        setContent(textNote)

        composeRule.onNodeWithTag("note_title_field").performTextReplacement("WLAN Eltern")
        composeRule.onNodeWithTag("note_body_field").performTextReplacement("Gartenhaus")

        assertEquals(
            listOf(NotesEvent.TitleChanged("WLAN Eltern"), NotesEvent.BodyChanged("Gartenhaus")),
            events
        )
    }

    @Test
    fun topBarActionsEmitEvents() {
        setContent(textNote)

        composeRule.onNodeWithTag("note_pin_button").performClick()
        composeRule.onNodeWithTag("note_convert_button").performClick()
        composeRule.onNodeWithTag("note_back_button").performClick()

        assertEquals(
            listOf(NotesEvent.TogglePinned, NotesEvent.ConvertNoteType, NotesEvent.CloseNote),
            events
        )
    }

    @Test
    fun categoryMenuOnlyEmitsAChangedCategory() {
        setContent(checklist)

        composeRule.onNodeWithTag("note_category_button").performClick()
        composeRule.onNodeWithTag("note_category_SHOPPING").performClick()
        composeRule.onNodeWithTag("note_category_button").performClick()
        composeRule.onNodeWithTag("note_category_IDEAS").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.CategoryChanged(NoteCategory.IDEAS)), events)
    }

    @Test
    fun deleteIsRequestedFromTheMoreMenu() {
        setContent(textNote)

        composeRule.onNodeWithTag("note_more_button").performClick()
        composeRule.onNodeWithTag("note_delete_menu_item").performClick()

        assertEquals(listOf<NotesEvent>(NotesEvent.RequestDeleteNote), events)
    }

    @Test
    fun deleteDialogConfirmsOrDismisses() {
        setContent(textNote, showDeleteDialog = true)

        composeRule.onNodeWithTag("cancel_delete_note").performClick()
        composeRule.onNodeWithTag("confirm_delete_note").performClick()

        assertEquals(listOf(NotesEvent.DismissDeleteNote, NotesEvent.ConfirmDeleteNote), events)
    }
}
