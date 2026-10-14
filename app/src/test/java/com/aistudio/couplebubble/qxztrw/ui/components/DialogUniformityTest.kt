package com.aistudio.couplebubble.qxztrw.ui.components

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DeleteCustomMilestoneDialog
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DialogUniformityTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun stringResourcesConsistentlyUseMomentInsteadOfErinnerung() {
        val addMemory = context.getString(R.string.add_memory)
        val memoryUpdated = context.getString(R.string.memory_updated)
        val memoryOptions = context.getString(R.string.memory_options)
        val close = context.getString(R.string.close)

        assertEquals("Moment festhalten", addMemory)
        assertEquals("Moment aktualisiert", memoryUpdated)
        assertEquals("Moment-Optionen", memoryOptions)
        assertEquals("Schließen", close)

        assertFalse(addMemory.contains("Erinnerung"))
        assertFalse(memoryUpdated.contains("Erinnerung"))
        assertFalse(memoryOptions.contains("Erinnerung"))
    }

    @Test
    fun discardChangesDialogRendersButtonsAndHandlesClicks() {
        var discarded = false
        var keptEditing = false

        composeRule.setContent {
            CoupleBubbleTheme {
                DiscardChangesDialog(
                    onDiscard = { discarded = true },
                    onKeepEditing = { keptEditing = true }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.discard_changes_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.discard_changes)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.keep_editing)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.keep_editing)).performClick()
        assertTrue(keptEditing)
        assertFalse(discarded)

        composeRule.onNodeWithText(context.getString(R.string.discard_changes)).performClick()
        assertTrue(discarded)
    }

    @Test
    fun deleteCustomMilestoneDialogRendersButtonsAndHandlesClicks() {
        var deleted = false
        var dismissed = false
        val milestone = CustomMilestone(id = "m1", title = "Jahrestag", date = LocalDate.of(2025, 5, 1))

        composeRule.setContent {
            CoupleBubbleTheme {
                DeleteCustomMilestoneDialog(
                    milestone = milestone,
                    onDismiss = { dismissed = true },
                    onConfirmDelete = { deleted = true }
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.delete_custom_milestone_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.confirm_delete)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.cancel)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.cancel)).performClick()
        assertTrue(dismissed)
        assertFalse(deleted)

        composeRule.onNodeWithText(context.getString(R.string.confirm_delete)).performClick()
        assertTrue(deleted)
    }
}
