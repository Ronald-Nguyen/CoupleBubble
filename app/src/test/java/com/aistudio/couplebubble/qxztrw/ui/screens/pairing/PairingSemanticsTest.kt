package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de")
class PairingSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinFieldReadsProgressDigitByDigit() {
        composeRule.setContent {
            CoupleBubbleTheme {
                EnterCodeTabContent(
                    state = PreviewData.pairing.copy(enteredCode = "482"),
                    onEnteredCodeChanged = {},
                    onConnectClicked = {},
                    onOpenDemoSpace = {},
                    showDeveloperTools = false
                )
            }
        }

        composeRule.onNodeWithTag("pin_input").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3 von 6 Ziffern eingegeben: 4, 8, 2")
        )
    }

    @Test
    fun pinErrorIsAnnouncedAsLiveRegion() {
        composeRule.setContent {
            CoupleBubbleTheme {
                EnterCodeTabContent(
                    state = PreviewData.pairing.copy(errorMessage = UiText.Resource(R.string.pairing_code_invalid)),
                    onEnteredCodeChanged = {},
                    onConnectClicked = {},
                    onOpenDemoSpace = {},
                    showDeveloperTools = false
                )
            }
        }

        composeRule.onNodeWithTag("pairing_error", useUnmergedTree = true).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)
        )
    }

    @Test
    fun generatedCodeIsSpelledAndCountdownSpeaksMinutes() {
        composeRule.setContent {
            CoupleBubbleTheme {
                CreateCodeTabContent(
                    state = PreviewData.pairing,
                    onCopyCode = {},
                    onGenerateNewCode = {},
                    onOpenDemoSpace = {},
                    showDeveloperTools = false
                )
            }
        }

        composeRule.onNodeWithTag("generated_code", useUnmergedTree = true)
            .assertContentDescriptionEquals("Dein Kopplungscode: 4, 8, 2, 9, 1, 3")
        composeRule.onNodeWithTag("code_countdown", useUnmergedTree = true)
            .assertContentDescriptionEquals("Code noch höchstens 13 Minuten gültig")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }
}
