package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.PairingUiState
import com.aistudio.couplebubble.qxztrw.ui.asString
import com.aistudio.couplebubble.qxztrw.ui.theme.SunsetTerracottaLight

@Composable
internal fun EnterCodeTabContent(
    state: PairingUiState,
    onEnteredCodeChanged: (String) -> Unit,
    onConnectClicked: () -> Unit,
    onOpenDemoSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val pinDescription = stringResource(R.string.pin_input_description)

    // Cursor is pinned to the end so Backspace always removes the previous digit
    val pinFieldValue = remember(state.enteredCode) {
        TextFieldValue(
            text = state.enteredCode,
            selection = TextRange(state.enteredCode.length)
        )
    }

    // Re-read on every resume so a code copied in a messenger shows up when the user comes back
    val clipboardManager = LocalClipboardManager.current
    var clipboardCode by remember { mutableStateOf<String?>(null) }
    LifecycleResumeEffect(clipboardManager) {
        clipboardCode = extractPairingCode(clipboardManager.getText()?.text)
        onPauseOrDispose {}
    }

    val onPinChanged: (String) -> Unit = { raw ->
        val digits = raw.filter { it in '0'..'9' }.take(PIN_LENGTH)
        val completesPin = digits.length == PIN_LENGTH && digits != state.enteredCode
        onEnteredCodeChanged(digits)
        if (completesPin && !state.isLoading) {
            keyboardController?.hide()
            focusManager.clearFocus()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onConnectClicked()
        }
    }

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = SunsetTerracottaLight.copy(alpha = 0.2f)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_code_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6-digit PIN: evenly spaced boxes with a transparent BasicTextField overlay for IME input
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val code = state.enteredCode
                    for (i in 0 until PIN_LENGTH) {
                        PinDigitSlot(
                            char = code.getOrNull(i)?.toString() ?: "",
                            isActive = i == code.length,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                BasicTextField(
                    value = pinFieldValue,
                    onValueChange = { onPinChanged(it.text) },
                    textStyle = TextStyle(color = Color.Transparent),
                    cursorBrush = SolidColor(Color.Transparent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (state.isInputReady && !state.isLoading) {
                                onConnectClicked()
                            }
                        }
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = pinDescription }
                        .testTag("pin_input")
                )
            }

            val pasteCandidate = clipboardCode?.takeIf { it != state.enteredCode }
            AnimatedVisibility(
                visible = pasteCandidate != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AssistChip(
                    onClick = { pasteCandidate?.let(onPinChanged) },
                    enabled = !state.isLoading,
                    label = {
                        Text(
                            text = stringResource(R.string.paste_code_from_clipboard, clipboardCode.orEmpty()),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .testTag("paste_clipboard_code")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick preset for the demo code
            OutlinedButton(
                onClick = { onPinChanged("482913") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(R.string.enter_code_demo_fill),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Error Message Display
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.errorMessage?.asString()?.let { errorText ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CTA Button: "Verbinden" in Warm Sunset Terracotta
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onConnectClicked()
                },
                enabled = state.isInputReady && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("connect_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.connecting),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.connect),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Demo Space shortcut
            TextButton(
                onClick = onOpenDemoSpace,
                modifier = Modifier.testTag("open_demo_button_enter")
            ) {
                Text(
                    text = stringResource(R.string.open_demo_space),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PinDigitSlot(
    char: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.secondary
            char.isNotEmpty() -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        },
        label = "slotBorder"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.surfaceContainerHigh
            char.isNotEmpty() -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "slotBg"
    )

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isActive || char.isNotEmpty()) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

private const val PIN_LENGTH = 6

private val PAIRING_CODE_REGEX = Regex("^[0-9]{6}$")

/** Returns the 6-digit code from clipboard text; tolerates the "482-913" format the app itself copies. */
private fun extractPairingCode(text: String?): String? =
    text?.filterNot { it == '-' || it.isWhitespace() }?.takeIf { PAIRING_CODE_REGEX.matches(it) }
