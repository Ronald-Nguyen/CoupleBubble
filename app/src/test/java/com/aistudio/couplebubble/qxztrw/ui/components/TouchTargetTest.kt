package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TouchTargetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun smallVisualGetsFullTouchTargetAndKeepsItsDrawnSize() {
        var clicks = 0
        composeRule.setContent {
            Box(
                Modifier
                    .testTag("target")
                    .circularTouchTarget(visualSize = 28.dp) { clicks++ }
            ) {
                Box(Modifier.size(28.dp).testTag("visual"))
            }
        }

        composeRule.onNodeWithTag("target").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("visual", useUnmergedTree = true).assertWidthIsEqualTo(28.dp)
        // A tap near the edge of the 48 dp area, outside the 28 dp visual, still counts
        composeRule.onNodeWithTag("target").performTouchInput {
            down(center.copy(x = center.x + 18.dp.toPx())); up()
        }
        assertEquals(1, clicks)
    }

    @Test
    fun visualLargerThanMinimumKeepsItsSize() {
        composeRule.setContent {
            Box(Modifier.testTag("target").circularTouchTarget(visualSize = 56.dp) {})
        }

        composeRule.onNodeWithTag("target").assertWidthIsEqualTo(56.dp)
    }
}
