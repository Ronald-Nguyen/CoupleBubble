package com.aistudio.couplebubble.qxztrw.ui.screens.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsFirstPageWithSkipIndicatorAndNextButton() {
        var completed = false
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        val context = composeRule.activity
        composeRule.onNodeWithText(context.getString(R.string.onboarding_step_1)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_title_1)).assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_page_indicator").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next_button").assertIsDisplayed()
    }

    @Test
    fun pagingThroughAllThreePagesShowsStartButtonAndCompletes() {
        var completed = false
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        val context = composeRule.activity

        // Page 1 -> Next
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()

        // Page 2
        composeRule.onNodeWithText(context.getString(R.string.onboarding_title_2)).assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        composeRule.waitForIdle()

        // Page 3: Last page has "Los geht's"
        composeRule.onNodeWithText(context.getString(R.string.onboarding_title_3)).assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()
        composeRule.onNodeWithTag("onboarding_start_button").assertIsDisplayed()

        composeRule.onNodeWithTag("onboarding_start_button").performClick()
        assertTrue(completed)
    }

    @Test
    fun skipButtonImmediatelyInvokesOnComplete() {
        var completed = false
        composeRule.setContent {
            CoupleBubbleTheme(darkTheme = false) {
                OnboardingScreen(onComplete = { completed = true })
            }
        }

        composeRule.onNodeWithTag("onboarding_skip_button").performClick()
        assertTrue(completed)
    }
}
