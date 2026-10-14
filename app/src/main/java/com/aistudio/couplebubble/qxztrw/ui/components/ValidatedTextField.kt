package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.InputError
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.InputRules
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/**
 * An [OutlinedTextField] checked against [InputRules] for [field]. The error only shows once the field was
 * touched (typed into or left after focusing); the counter appears from 80 % of the limit on.
 */
@Composable
fun ValidatedTextField(
    field: InputField,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var touched by rememberSaveable { mutableStateOf(false) }
    var hadFocus by rememberSaveable { mutableStateOf(false) }
    val error = InputRules.validate(field, value).takeIf { touched }
    val showCounter = InputRules.showsCounter(field, value)

    OutlinedTextField(
        value = value,
        onValueChange = {
            touched = true
            onValueChange(it)
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        isError = error != null,
        supportingText = if (error != null || showCounter) {
            { InputSupportingText(field, value, error, showCounter) }
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (it.isFocused) hadFocus = true else if (hadFocus) touched = true
            }
    )
}

/** Error reason on the left and, near the limit, the counter `n/max`. */
@Composable
fun InputSupportingText(field: InputField, value: String, error: InputError?, showCounter: Boolean) {
    Column {
        if (error != null) Text(inputErrorMessage(field, error))
        if (showCounter) {
            Text(
                text = stringResource(R.string.input_counter, value.trim().length, field.maxLength),
                color = if (error == InputError.TOO_LONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun inputErrorMessage(field: InputField, error: InputError): String = when (error) {
    InputError.EMPTY -> stringResource(R.string.input_error_empty)
    InputError.TOO_SHORT -> pluralStringResource(R.plurals.input_error_too_short, field.minLength, field.minLength)
    InputError.TOO_LONG -> pluralStringResource(R.plurals.input_error_too_long, field.maxLength, field.maxLength)
}

@Preview(name = "Validated Text Field Light", showBackground = true)
@Composable
private fun ValidatedTextFieldPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        Column {
            ValidatedTextField(
                field = InputField.MILESTONE_TITLE,
                value = "Unser erster gemeinsamer Urlaub am Meer in Portugal, endlich",
                onValueChange = {},
                label = "Titel"
            )
            InputSupportingText(InputField.NICKNAME, "L", InputError.TOO_SHORT, showCounter = false)
        }
    }
}

@Preview(name = "Validated Text Field Dark", showBackground = true)
@Composable
private fun ValidatedTextFieldDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        ValidatedTextField(field = InputField.NICKNAME, value = "Lena", onValueChange = {}, label = "Dein Spitzname")
    }
}
