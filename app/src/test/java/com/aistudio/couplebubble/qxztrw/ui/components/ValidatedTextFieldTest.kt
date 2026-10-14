package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.text.input.ImeAction
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ValidatedTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setForm(focusFirst: Boolean) {
        composeRule.setContent {
            CoupleBubbleTheme {
                val first = rememberInitialFocus(enabled = focusFirst)
                val second = remember { FocusRequester() }
                var a by remember { mutableStateOf("") }
                var b by remember { mutableStateOf("") }
                Column {
                    ValidatedTextField(
                        field = InputField.NICKNAME, value = a, onValueChange = { a = it }, label = "A",
                        modifier = Modifier.focusRequester(first).testTag("first"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { second.requestFocus() })
                    )
                    ValidatedTextField(
                        field = InputField.NICKNAME, value = b, onValueChange = { b = it }, label = "B",
                        modifier = Modifier.focusRequester(second).testTag("second"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun newEntryFocusesFirstField() {
        setForm(focusFirst = true)
        composeRule.onNodeWithTag("first").assertIsFocused()
    }

    @Test
    fun editingFocusesNothing() {
        setForm(focusFirst = false)
        composeRule.onNodeWithTag("first").assertIsNotFocused()
        composeRule.onNodeWithTag("second").assertIsNotFocused()
    }

    @Test
    fun nextMovesFocusToSecondField() {
        setForm(focusFirst = true)
        composeRule.onNodeWithTag("first").performImeAction()
        composeRule.onNodeWithTag("second").assertIsFocused()
    }
}
