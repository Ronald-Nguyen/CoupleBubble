package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialogState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
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
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleMetrics = RelationshipDateCalculator.calculate(2023, 6, 25)
    private val events = mutableListOf<DashboardEvent>()

    private fun setContent(state: DashboardUiState) {
        events.clear()
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                DashboardScreen(state = state, onEvent = { events += it })
            }
        }
        composeRule.waitForIdle()
        events.clear()
    }

    @Test
    fun planReunionButtonShownWhenNoReunionPlanned() {
        setContent(DashboardUiState(space = PreviewData.space, metrics = sampleMetrics, reunion = null))

        val button = composeRule.onNodeWithTag("plan_reunion_button")
        button.assertIsDisplayed()
        button.performClick()

        assertEquals(listOf(DashboardEvent.ShowReunionDialog(true)), events)
    }

    @Test
    fun planReunionButtonHiddenWhenReunionPlanned() {
        setContent(DashboardUiState(space = PreviewData.space, metrics = sampleMetrics, reunion = PreviewData.reunion))

        composeRule.onNodeWithTag("plan_reunion_button").assertDoesNotExist()
        composeRule.onNodeWithText("≈ 476 km voneinander entfernt").assertIsDisplayed()
    }

    @Test
    fun moreOptionsMenuHasCorrectContentDescriptionAndShowsDailyActions() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                dialogs = DashboardDialogState(isMenuExpanded = true)
            )
        )

        composeRule.onNodeWithContentDescription("Weitere Optionen").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_reunion_item").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_share_code_item").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_settings_item").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_disconnect_item").assertDoesNotExist()
    }

    @Test
    fun disconnectDialogExplainsConsequencesAndEmitsEvents() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                dialogs = DashboardDialogState(dialog = DashboardDialog.Disconnect)
            )
        )

        composeRule.onNodeWithText("Verbindung trennen?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Dein Partner wird ebenfalls getrennt und gelangt zurück zur Kopplung. Eure gemeinsamen Momente, Notizen und Fotos bleiben gespeichert, sind aber für euch beide erst nach einer erneuten Verbindung wieder sichtbar."
        ).assertIsDisplayed()

        composeRule.onNodeWithTag("cancel_disconnect_button").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_disconnect_button").assertIsDisplayed()

        composeRule.onNodeWithTag("cancel_disconnect_button").performClick()
        assertEquals(listOf(DashboardEvent.ShowDisconnectDialog(false)), events)

        events.clear()
        composeRule.onNodeWithTag("confirm_disconnect_button").performClick()
        assertEquals(listOf(DashboardEvent.ConfirmDisconnect), events)
    }

    @Test
    fun googleBackupDialogSaysSchliessenInsteadOfAbbrechen() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                dialogs = DashboardDialogState(dialog = DashboardDialog.GoogleBackup)
            )
        )

        composeRule.onAllNodesWithText("Google-Datensicherung").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Schließen").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").assertDoesNotExist()

        composeRule.onNodeWithText("Schließen").performClick()
        assertEquals(listOf(DashboardEvent.ShowGoogleBackupDialog(false)), events)
    }

    @Test
    fun addMemoryFabUsesMomentText() {
        setContent(DashboardUiState(space = PreviewData.space, metrics = sampleMetrics))

        composeRule.onNodeWithTag("add_memory_fab").assertIsDisplayed()
        composeRule.onNodeWithText("Moment festhalten", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun longClickOnMemoryCardOpensOptionsMenu() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                memories = listOf(PreviewData.memory)
            )
        )

        composeRule.onNodeWithTag("memory_card_${PreviewData.memory.id}").assertIsDisplayed()
        composeRule.onNodeWithText("Moment bearbeiten").assertDoesNotExist()

        composeRule.onNodeWithTag("memory_card_${PreviewData.memory.id}").performTouchInput { longClick() }

        composeRule.onNodeWithText("Moment bearbeiten").assertIsDisplayed()
        composeRule.onNodeWithText("Moment löschen").assertIsDisplayed()
    }

    @Test
    fun threeDotsMenuButtonOpensSameOptionsMenu() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                memories = listOf(PreviewData.memory)
            )
        )

        composeRule.onNodeWithTag("memory_menu_button_${PreviewData.memory.id}").performClick()

        composeRule.onNodeWithText("Moment bearbeiten").assertIsDisplayed()
        composeRule.onNodeWithText("Moment löschen").assertIsDisplayed()
    }

    @Test
    fun yearCardToggleCollapseHidesMemories() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                memories = listOf(PreviewData.memory)
            )
        )

        val memoryTitle = PreviewData.memory.title
        composeRule.onNodeWithText(memoryTitle).assertIsDisplayed()

        // Toggle year to collapse
        composeRule.onNodeWithTag("year_header_${PreviewData.memory.date.year}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(memoryTitle).assertDoesNotExist()

        // Toggle year to expand again
        composeRule.onNodeWithTag("year_header_${PreviewData.memory.date.year}").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(memoryTitle).assertIsDisplayed()
    }

    @Test
    fun searchButtonInHeaderEmitsOpenSearch() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                isSearchOpen = false
            )
        )

        val searchButton = composeRule.onNodeWithTag("memories_search_button")
        searchButton.assertIsDisplayed()
        searchButton.performClick()

        assertEquals(listOf(DashboardEvent.OpenSearch), events)
    }

    @Test
    fun searchBarCloseButtonEmitsCloseSearch() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                isSearchOpen = true,
                searchQuery = "Urlaub"
            )
        )

        composeRule.onNodeWithTag("memories_search_bar").assertIsDisplayed()
        val closeButton = composeRule.onNodeWithTag("memories_search_close")
        closeButton.assertIsDisplayed()
        closeButton.performClick()

        assertEquals(listOf(DashboardEvent.CloseSearch), events)
    }

    @Test
    fun searchBarInputEmitsSearchQueryChanged() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                isSearchOpen = true,
                searchQuery = ""
            )
        )

        val searchInput = composeRule.onNodeWithTag("memories_search_input")
        searchInput.assertIsDisplayed()
        searchInput.performTextInput("Sommer")

        assertEquals(listOf(DashboardEvent.SearchQueryChanged("Sommer")), events)
    }

    @Test
    fun searchStateDisplaysHitCount() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                isSearchOpen = true,
                memories = PreviewData.memories,
                filteredMemories = listOf(PreviewData.memory)
            )
        )

        composeRule.onNodeWithTag("memories_search_count").assertIsDisplayed()
        composeRule.onNodeWithText("1 Moment gefunden").assertIsDisplayed()
    }

    @Test
    fun emptySearchDisplaysEmptySearchCard() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                isSearchOpen = true,
                searchQuery = "Fantasiestadt",
                memories = PreviewData.memories,
                filteredMemories = emptyList()
            )
        )

        composeRule.onNodeWithTag("memories_search_empty").assertIsDisplayed()
        composeRule.onNodeWithText("Keine Momente für „Fantasiestadt“ gefunden").assertIsDisplayed()
    }

    @Test
    fun yearJumpRowDisplaysWhenTimelineHasMultipleYears() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                memories = PreviewData.memories,
                filteredMemories = PreviewData.memories // 2024 and 2023
            )
        )

        composeRule.onNodeWithTag("timeline_year_jump_row").assertIsDisplayed()
        composeRule.onNodeWithTag("year_chip_2024").assertIsDisplayed()
        composeRule.onNodeWithTag("year_chip_2023").assertIsDisplayed()
    }

    @Test
    fun yearJumpRowHiddenWhenSingleYear() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                memories = listOf(PreviewData.memory),
                filteredMemories = listOf(PreviewData.memory) // only 2023
            )
        )

        composeRule.onNodeWithTag("timeline_year_jump_row").assertDoesNotExist()
        composeRule.onNodeWithTag("year_chip_2023").assertDoesNotExist()
    }
}
