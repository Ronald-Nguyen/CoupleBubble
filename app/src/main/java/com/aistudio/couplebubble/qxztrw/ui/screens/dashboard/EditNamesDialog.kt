package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.components.CustomColorPickerDialog
import com.aistudio.couplebubble.qxztrw.ui.theme.getContrastingTextColor
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToCompose

@Composable
internal fun EditNamesDialog(
    currentPartner1: String,
    currentPartner2: String,
    currentPartner1ColorHex: String,
    currentPartner2ColorHex: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onSwapRoles: () -> Unit
) {
    var fields by remember {
        mutableStateOf(
            EditNamesFields(
                name1 = currentPartner1,
                name2 = currentPartner2,
                color1 = currentPartner1ColorHex,
                color2 = currentPartner2ColorHex
            )
        )
    }
    val haptic = LocalHapticFeedback.current
    val swapIconRotation by animateFloatAsState(
        targetValue = fields.swapCount * 180f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "swapIconRotation"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_names),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(spring(stiffness = Spring.StiffnessLow)),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Partner 1 slides in from below, Partner 2 from above, so a swap reads as the two crossing.
                AnimatedContent(
                    targetState = fields,
                    contentKey = { it.swapCount },
                    transitionSpec = { partnerSwapTransition(fromBelow = true) },
                    label = "partner1Fields"
                ) { state ->
                    EditPartnerFields(
                        name = state.name1,
                        onNameChange = { fields = fields.copy(name1 = it) },
                        partnerLabel = stringResource(R.string.partner_a_label),
                        colorHex = state.color1,
                        onColorSelected = { fields = fields.copy(color1 = it) }
                    )
                }

                // Swap Roles Divider with Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            fields = fields.copy(
                                name1 = fields.name2,
                                name2 = fields.name1,
                                color1 = fields.color2,
                                color2 = fields.color1,
                                swapCount = fields.swapCount + 1
                            )
                            onSwapRoles()
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = stringResource(R.string.swap_partners_tooltip),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.graphicsLayer { rotationZ = swapIconRotation }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                AnimatedContent(
                    targetState = fields,
                    contentKey = { it.swapCount },
                    transitionSpec = { partnerSwapTransition(fromBelow = false) },
                    label = "partner2Fields"
                ) { state ->
                    EditPartnerFields(
                        name = state.name2,
                        onNameChange = { fields = fields.copy(name2 = it) },
                        partnerLabel = stringResource(R.string.partner_b_label),
                        colorHex = state.color2,
                        onColorSelected = { fields = fields.copy(color2 = it) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fields.name1.isNotBlank() && fields.name2.isNotBlank()) {
                        onSave(fields.name1.trim(), fields.name2.trim(), fields.color1, fields.color2)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private data class EditNamesFields(
    val name1: String,
    val name2: String,
    val color1: String,
    val color2: String,
    val swapCount: Int = 0
)

private fun partnerSwapTransition(fromBelow: Boolean): ContentTransform {
    val direction = if (fromBelow) 1 else -1
    val offsetSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    )
    return ContentTransform(
        targetContentEnter = slideInVertically(offsetSpec) { height -> direction * height } +
            fadeIn(spring(stiffness = Spring.StiffnessLow)),
        initialContentExit = slideOutVertically(offsetSpec) { height -> direction * height } +
            fadeOut(spring(stiffness = Spring.StiffnessMedium)),
        sizeTransform = SizeTransform(clip = false)
    )
}

@Composable
private fun EditPartnerFields(
    name: String,
    onNameChange: (String) -> Unit,
    partnerLabel: String,
    colorHex: String,
    onColorSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(partnerLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.choose_accent_color) + " ($partnerLabel)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ColorPaletteSelector(
            selectedColorHex = colorHex,
            partnerName = name,
            onColorSelected = onColorSelected
        )
    }
}

@Composable
private fun ColorPaletteSelector(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    partnerName: String = "",
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        "#E65D2E" to R.string.color_terracotta,
        "#8FA89B" to R.string.color_sage,
        "#E8A598" to R.string.color_rose,
        "#4A7C92" to R.string.color_ocean,
        "#DDA15E" to R.string.color_amber,
        "#9A5865" to R.string.color_berry,
        "#4ECDC4" to R.string.color_mint,
        "#FF6B6B" to R.string.color_coral
    )

    var showCustomColorPicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isPresetSelected = palette.any { it.first.equals(selectedColorHex, ignoreCase = true) }

    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorHex = selectedColorHex,
            partnerName = partnerName.ifBlank { stringResource(R.string.partner_fallback_name) },
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { newHex ->
                onColorSelected(newHex)
                showCustomColorPicker = false
            }
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        palette.forEach { (hex, nameResId) ->
            val color = parseColorHexToCompose(hex)
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .clickable {
                        if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onColorSelected(hex)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(nameResId),
                        tint = getContrastingTextColor(color),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Custom Color Picker Button
        val customColor = if (!isPresetSelected) parseColorHexToCompose(selectedColorHex) else MaterialTheme.colorScheme.surfaceContainerHigh
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(customColor)
                .border(
                    width = if (!isPresetSelected) 3.dp else 1.dp,
                    color = if (!isPresetSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable { showCustomColorPicker = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = stringResource(R.string.custom_color_button),
                tint = if (!isPresetSelected) getContrastingTextColor(customColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
