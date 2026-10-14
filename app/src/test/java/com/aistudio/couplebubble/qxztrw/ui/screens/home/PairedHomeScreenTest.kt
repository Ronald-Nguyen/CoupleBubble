package com.aistudio.couplebubble.qxztrw.ui.screens.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

    private fun setContent(
        selectedTab: MainTab,
        isNoteOpen: Boolean = false,
        isOffline: Boolean = false,
        hasUnreadNotes: Boolean = false
    ) {
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                PairedHomeScreen(
                    selectedTab = selectedTab,
                    isNoteOpen = isNoteOpen,
                    isOffline = isOffline,
                    hasUnreadNotes = hasUnreadNotes,
                    onTabSelected = { selectedTabs += it },
                    usContent = { Text("Dashboard", modifier = it.testTag("us_content")) },
                    notesContent = { Text("Notizen", modifier = it.testTag("notes_content")) }
                )
            }
        }
    }

    @Test
    fun offlineBannerOnlyShowsWhileOffline() {
        setContent(MainTab.US, isOffline = true)

        composeRule.onNodeWithTag("offline_banner").assertIsDisplayed()
    }

    @Test
    fun noOfflineBannerWhileOnline() {
        setContent(MainTab.US)

        composeRule.onNodeWithTag("offline_banner").assertDoesNotExist()
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

    @Test
    fun tappingActiveTabCallsOnTabReselected() {
        var reselectedTab: MainTab? = null
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                PairedHomeScreen(
                    selectedTab = MainTab.US,
                    isNoteOpen = false,
                    isOffline = false,
                    onTabSelected = { selectedTabs += it },
                    onTabReselected = { reselectedTab = it },
                    usContent = { Text("Dashboard", modifier = it.testTag("us_content")) },
                    notesContent = { Text("Notizen", modifier = it.testTag("notes_content")) }
                )
            }
        }

        composeRule.onNodeWithTag("bottom_nav_us").performClick()
        assertEquals(MainTab.US, reselectedTab)
        assertEquals(emptyList<MainTab>(), selectedTabs)
    }

    @Test
    fun switchingTabsPreservesScrollPositions() {
        var currentTab by mutableStateOf(MainTab.US)
        val usListState = LazyListState()
        val notesGridState = LazyStaggeredGridState()

        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                PairedHomeScreen(
                    selectedTab = currentTab,
                    isNoteOpen = false,
                    isOffline = false,
                    onTabSelected = { currentTab = it },
                    usContent = {
                        LazyColumn(state = usListState, modifier = it.testTag("us_column")) {
                            items(50) { index -> Text("US Item $index") }
                        }
                    },
                    notesContent = {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            state = notesGridState,
                            modifier = it.testTag("notes_grid")
                        ) {
                            items(50) { index -> Text("Note Item $index") }
                        }
                    }
                )
            }
        }

        // Scroll US list
        composeRule.runOnIdle {
            kotlinx.coroutines.runBlocking {
                usListState.scrollToItem(15)
            }
        }
        composeRule.waitForIdle()
        assertEquals(15, usListState.firstVisibleItemIndex)

        // Switch to Notes tab
        currentTab = MainTab.NOTES
        composeRule.waitForIdle()

        // Scroll Notes grid
        composeRule.runOnIdle {
            kotlinx.coroutines.runBlocking {
                notesGridState.scrollToItem(20)
            }
        }
        composeRule.waitForIdle()
        assertEquals(20, notesGridState.firstVisibleItemIndex)

        // Switch back to US tab
        currentTab = MainTab.US
        composeRule.waitForIdle()

        // Verify US list position is preserved
        assertEquals(15, usListState.firstVisibleItemIndex)

        // Switch back to Notes tab
        currentTab = MainTab.NOTES
        composeRule.waitForIdle()

        // Verify Notes grid position is preserved
        assertEquals(20, notesGridState.firstVisibleItemIndex)
    }

    @Test
    fun notesTabShowsBadgeWhenHasUnreadNotesIsTrue() {
        setContent(MainTab.US, hasUnreadNotes = true)

        composeRule.onNodeWithTag("notes_tab_badge", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun notesTabHidesBadgeWhenHasUnreadNotesIsFalse() {
        setContent(MainTab.US, hasUnreadNotes = false)

        composeRule.onNodeWithTag("notes_tab_badge", useUnmergedTree = true).assertDoesNotExist()
    }
}
