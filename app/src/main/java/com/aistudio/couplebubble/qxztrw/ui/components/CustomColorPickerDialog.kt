package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.getContrastingTextColor
import java.util.Locale

@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    partnerName: String = "Alex",
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Parse initial color to HSV
    val initialHsv = remember(initialColorHex) {
        val hsv = FloatArray(3)
        try {
            val cleanHex = if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex"
            val parsedColor = android.graphics.Color.parseColor(cleanHex)
            android.graphics.Color.colorToHSV(parsedColor, hsv)
        } catch (e: Exception) {
            hsv[0] = 16f
            hsv[1] = 0.8f
            hsv[2] = 0.9f
        }
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun currentComposeColor(): Color {
        val intColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        return Color(intColor)
    }

    fun currentHex(): String {
        val intColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val r = android.graphics.Color.red(intColor)
        val g = android.graphics.Color.green(intColor)
        val b = android.graphics.Color.blue(intColor)
        return String.format(Locale.US, "#%02X%02X%02X", r, g, b)
    }

    var hexText by remember { mutableStateOf(currentHex()) }
    var hexError by remember { mutableStateOf(false) }

    fun updateFromHex(input: String) {
        val clean = if (input.startsWith("#")) input else "#$input"
        hexText = clean.uppercase()
        if (clean.length == 7 && clean.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            try {
                val parsed = android.graphics.Color.parseColor(clean)
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(parsed, hsv)
                hue = hsv[0]
                saturation = hsv[1]
                value = hsv[2]
                hexError = false
            } catch (e: Exception) {
                hexError = true
            }
        } else {
            hexError = input.length >= 7
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.custom_color_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Avatar Preview Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Preview Ring
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(3.dp, currentComposeColor(), CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(currentComposeColor()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = partnerName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = getContrastingTextColor(currentComposeColor())
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = partnerName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentHex(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Curated Quick Presets Row
                Text(
                    text = stringResource(R.string.custom_color_preset),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val presets = listOf(
                    "#E65D2E", "#8FA89B", "#E8A598", "#4A7C92",
                    "#DDA15E", "#9A5865", "#4ECDC4", "#FF6B6B"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.forEach { hex ->
                        val parsed = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        val isCurrent = currentHex().equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    updateFromHex(hex)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = getContrastingTextColor(parsed),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Hue Slider with Rainbow Track
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.custom_color_hue),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                    )
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            hexText = currentHex()
                            hexError = false
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentComposeColor(),
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                // Saturation Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.custom_color_saturation),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            hexText = currentHex()
                            hexError = false
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentComposeColor(),
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                // Brightness / Value Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.custom_color_value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            hexText = currentHex()
                            hexError = false
                        },
                        valueRange = 0.2f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = currentComposeColor(),
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                // Hex Code Field
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { updateFromHex(it) },
                    label = { Text(stringResource(R.string.custom_color_hex)) },
                    singleLine = true,
                    isError = hexError,
                    supportingText = if (hexError) {
                        { Text(stringResource(R.string.custom_color_invalid_hex), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onColorSelected(currentHex())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.custom_color_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Preview(name = "Custom Color Picker Preview")
@Composable
private fun CustomColorPickerDialogPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        CustomColorPickerDialog(
            initialColorHex = "#E65D2E",
            partnerName = "Alex",
            onDismiss = {},
            onColorSelected = {}
        )
    }
}
