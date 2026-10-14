package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "de")
class TimelineSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun stateIs(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun yearHeaderIsHeadingButtonWithChangingState() {
        val memory = PreviewData.memory
        composeRule.setContent {
            CoupleBubbleTheme {
                YearMemoryGroupCard(
                    year = memory.date.year,
                    memories = listOf(memory),
                    partner1Name = "Mia",
                    partner2Name = "Jonas",
                    partner1Color = 0xFFE65D2E,
                    partner2Color = 0xFF4A7C92,
                    currentUserRole = null,
                    photoSyncStates = emptyMap(),
                    onEditMemory = {},
                    onDeleteMemory = {},
                    onSaveToGallery = {},
                    onImageClick = {}
                )
            }
        }

        val header = composeRule.onNodeWithTag("year_header_${memory.date.year}")
        header
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(stateIs("ausgeklappt"))
        composeRule.onNodeWithTag("memory_card_${memory.id}").assert(stateIs("ausgeklappt"))

        header.performClick()
        header.assert(stateIs("eingeklappt"))
    }
}
