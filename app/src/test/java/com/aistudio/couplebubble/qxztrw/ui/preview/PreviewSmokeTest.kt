package com.aistudio.couplebubble.qxztrw.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.ui.components.GoogleAuthCard
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.ReunionCard
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.SpaceTopHeader
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.CreateCodeTabContent
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.EnterCodeTabContent
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Renders the preview content of non-dialog components in light and dark mode, so a broken preview fails a test. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PreviewSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent { CoupleBubbleTheme(darkTheme = darkTheme) { content() } }
        composeRule.waitForIdle()
    }

    @Test
    fun googleAuthCardWithProfile() = render(darkTheme = false) {
        GoogleAuthCard(userProfile = PreviewData.profile)
    }

    @Test
    fun spaceTopHeaderShowsBothNames() {
        render(darkTheme = true) {
            SpaceTopHeader(
                space = PreviewData.space,
                editableRole = PartnerRole.PARTNER_1,
                isUploadingProfilePhoto = true,
                isMenuExpanded = false,
                onMenuExpandedChanged = {},
                onPartnerAvatarDenied = {},
                onEditNamesClick = {},
                onGoogleBackupClick = {},
                onMilestonesClick = {},
                onDisconnectClick = {}
            )
        }
        composeRule.onNodeWithText("Mia & Leo").assertIsDisplayed()
    }

    @Test
    fun createCodeTabShowsTheCode() {
        render(darkTheme = false) {
            CreateCodeTabContent(state = PreviewData.pairing, onCopyCode = {}, onGenerateNewCode = {}, onOpenDemoSpace = {})
        }
        composeRule.onNodeWithText("482-913", substring = true).assertIsDisplayed()
    }

    @Test
    fun enterCodeTabWhileConnecting() = render(darkTheme = true) {
        EnterCodeTabContent(
            state = PreviewData.pairing.copy(enteredCode = "482913", isLoading = true),
            onEnteredCodeChanged = {},
            onConnectClicked = {},
            onOpenDemoSpace = {}
        )
    }

    @Test
    fun reunionCardShowsCountdownAndDistance() {
        render(darkTheme = true) {
            ReunionCard(
                state = PreviewData.reunion,
                partnerName = PreviewData.space.partner2Name,
                ownColor = Color(0xFFE65D2E),
                partnerColor = Color(0xFF5B8DB8),
                onClick = {},
                onCaptureMoment = {}
            )
        }
        composeRule.onNodeWithText("12").assertIsDisplayed()
        composeRule.onNodeWithText("≈ 476 km voneinander entfernt").assertIsDisplayed()
        composeRule.onNodeWithText("Standort, vor 3 Std.").assertIsDisplayed()
    }

    @Test
    fun settingsScreenSmokeTest() {
        render(darkTheme = false) {
            com.aistudio.couplebubble.qxztrw.ui.screens.settings.SettingsScreen(
                state = com.aistudio.couplebubble.qxztrw.ui.screens.settings.SettingsUiState(
                    isOpen = true,
                    space = PreviewData.space,
                    appVersion = "1.4.0"
                ),
                onEvent = {}
            )
        }
        composeRule.onNodeWithText("Einstellungen").assertIsDisplayed()
    }
}
