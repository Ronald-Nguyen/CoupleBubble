package com.aistudio.couplebubble.qxztrw

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.aistudio.couplebubble.qxztrw.ui.MainTab
import com.aistudio.couplebubble.qxztrw.ui.screens.PairedHomeScreen
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PairedHomeScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedTabs = mutableListOf<MainTab>()

    private fun setContent(selectedTab: MainTab, isNoteOpen: Boolean = false) {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                PairedHomeScreen(
                    selectedTab = selectedTab,
                    isNoteOpen = isNoteOpen,
                    onTabSelected = { selectedTabs += it },
                    usContent = { Text("Dashboard", modifier = it.testTag("us_content")) },
                    notesContent = { Text("Notizen", modifier = it.testTag("notes_content")) }
                )
            }
        }
    }

    @Test
    fun showsTheContentOfTheSelectedTab() {
        setContent(MainTab.NOTES)

        composeRule.onNodeWithTag("notes_content").assertIsDisplayed()
        composeRule.onNodeWithTag("us_content").assertDoesNotExist()
    }

    @Test
    fun tappingAnotherTabSelectsIt() {
        setContent(MainTab.US)

        composeRule.onNodeWithTag("bottom_nav_us").performClick()
        composeRule.onNodeWithTag("bottom_nav_notes").performClick()

        assertEquals(listOf(MainTab.NOTES), selectedTabs)
    }

    @Test
    fun bottomBarHidesWhileANoteIsOpen() {
        setContent(MainTab.NOTES, isNoteOpen = true)

        composeRule.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        composeRule.onNodeWithTag("notes_content").assertIsDisplayed()
    }

    @Test
    fun backFromTheNotesOverviewReturnsToUs() {
        setContent(MainTab.NOTES)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        assertEquals(listOf(MainTab.US), selectedTabs)
    }

    @Test
    fun backIsLeftToTheEditorWhileANoteIsOpen() {
        setContent(MainTab.NOTES, isNoteOpen = true)

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        assertEquals(emptyList<MainTab>(), selectedTabs)
    }
}
