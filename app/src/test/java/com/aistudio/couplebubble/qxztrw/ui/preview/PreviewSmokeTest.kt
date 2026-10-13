package com.aistudio.couplebubble.qxztrw.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.ui.components.GoogleAuthCard
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
}
