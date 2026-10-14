package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialogState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Assert.assertEquals
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
    fun moreOptionsMenuHasCorrectContentDescriptionAndShowsDisconnectBehindDivider() {
        setContent(
            DashboardUiState(
                space = PreviewData.space,
                metrics = sampleMetrics,
                dialogs = DashboardDialogState(isMenuExpanded = true)
            )
        )

        composeRule.onNodeWithContentDescription("Weitere Optionen").assertIsDisplayed()
        composeRule.onNodeWithTag("menu_disconnect_item").assertIsDisplayed()
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
}
