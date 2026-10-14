package com.aistudio.couplebubble.qxztrw.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SettingsEvent>()

    private fun setContent(state: SettingsUiState) {
        composeRule.setContent {
            CoupleBubbleTheme {
                SettingsScreen(
                    state = state,
                    onEvent = { events += it }
                )
            }
        }
    }

    @Test
    fun displaysAllSectionsAndItems() {
        setContent(
            SettingsUiState(
                isOpen = true,
                space = PreviewData.space,
                appVersion = "1.4.0"
            )
        )

        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Einstellungen").assertIsDisplayed()

        // Sections
        composeRule.onNodeWithText("RAUM").assertExists()
        composeRule.onNodeWithText("KONTO").assertExists()
        composeRule.onNodeWithText("ÜBER").assertExists()

        // Items
        composeRule.onNodeWithTag("settings_item_names_colors").assertExists()
        composeRule.onNodeWithText("Spitznamen & Farben").assertExists()

        composeRule.onNodeWithTag("settings_item_share_code").assertExists()
        composeRule.onNodeWithText("Raum-Code teilen").assertExists()

        composeRule.onNodeWithTag("settings_item_milestones").assertExists()
        composeRule.onNodeWithText("Meilensteine").assertExists()

        composeRule.onNodeWithTag("settings_item_disconnect").assertExists()
        composeRule.onNodeWithText("Verbindung trennen").assertExists()

        composeRule.onNodeWithTag("settings_item_google_backup").assertExists()
        composeRule.onNodeWithText("Google-Sicherung").assertExists()

        composeRule.onNodeWithTag("settings_item_app_version").assertExists()
        composeRule.onNodeWithText("Version").assertExists()
        composeRule.onNodeWithText("1.4.0").assertExists()

        composeRule.onNodeWithTag("settings_item_privacy").assertExists()
        composeRule.onNodeWithText("Datenschutz").assertExists()
    }

    @Test
    fun clickingBackButtonEmitsCloseEvent() {
        setContent(SettingsUiState(isOpen = true, space = PreviewData.space))

        composeRule.onNodeWithTag("settings_back_button").performClick()
        assertEquals(listOf(SettingsEvent.Close), events)
    }

    @Test
    fun clickingItemsEmitsRespectiveEvents() {
        setContent(SettingsUiState(isOpen = true, space = PreviewData.space))

        composeRule.onNodeWithTag("settings_item_names_colors").performScrollTo().performClick()
        assertEquals(listOf(SettingsEvent.ShowEditNamesDialog(true)), events)

        events.clear()
        composeRule.onNodeWithTag("settings_item_milestones").performScrollTo().performClick()
        assertEquals(listOf(SettingsEvent.ShowMilestoneSettingsDialog(true)), events)

        events.clear()
        composeRule.onNodeWithTag("settings_item_google_backup").performScrollTo().performClick()
        assertEquals(listOf(SettingsEvent.ShowGoogleBackupDialog(true)), events)

        events.clear()
        composeRule.onNodeWithTag("settings_item_disconnect").performScrollTo().performClick()
        assertEquals(listOf(SettingsEvent.ShowDisconnectDialog(true)), events)

        events.clear()
        composeRule.onNodeWithTag("settings_item_privacy").performScrollTo().performClick()
        assertEquals(listOf(SettingsEvent.ShowPrivacySummaryDialog(true)), events)
    }

    @Test
    fun privacySummaryDialogShowsPointsAndCloses() {
        setContent(
            SettingsUiState(
                isOpen = true,
                space = PreviewData.space,
                dialog = SettingsDialog.PrivacySummary
            )
        )

        composeRule.onNodeWithTag("privacy_summary_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Datenschutz bei CoupleBubble").assertIsDisplayed()
        composeRule.onNodeWithText("100 % privat für euch zwei:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Sichere Cloud-Speicherung:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Standort nur optional:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Volle Kontrolle & Löschung:", substring = true).assertIsDisplayed()

        composeRule.onNodeWithTag("privacy_summary_close_button").performClick()
        assertEquals(listOf(SettingsEvent.ShowPrivacySummaryDialog(false)), events)
    }

    @Test
    fun disconnectDialogShowsAndEmitsEvents() {
        setContent(
            SettingsUiState(
                isOpen = true,
                space = PreviewData.space,
                dialog = SettingsDialog.Disconnect
            )
        )

        composeRule.onNodeWithText("Verbindung trennen?").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_disconnect_button").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_disconnect_button").assertIsDisplayed()

        composeRule.onNodeWithTag("cancel_disconnect_button").performClick()
        assertEquals(listOf(SettingsEvent.ShowDisconnectDialog(false)), events)

        events.clear()
        composeRule.onNodeWithTag("confirm_disconnect_button").performClick()
        assertEquals(listOf(SettingsEvent.ConfirmDisconnect), events)
    }
}
