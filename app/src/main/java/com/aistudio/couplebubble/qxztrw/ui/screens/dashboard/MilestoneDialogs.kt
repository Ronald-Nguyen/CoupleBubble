package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.foundation.layout.heightIn
import com.aistudio.couplebubble.qxztrw.ui.components.MinTouchTarget
import com.aistudio.couplebubble.qxztrw.ui.components.rememberInitialFocus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.InputRules
import com.aistudio.couplebubble.qxztrw.ui.components.ValidatedTextField
import com.aistudio.couplebubble.qxztrw.core.ui.GermanLongDate
import com.aistudio.couplebubble.qxztrw.ui.components.DateFieldButton
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone
import com.aistudio.couplebubble.qxztrw.ui.components.DiscardChangesDialog
import com.aistudio.couplebubble.qxztrw.ui.components.MemoryDatePickerDialog
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.LocalDate
import java.time.ZoneId

@Composable
internal fun MilestoneSettingsDialog(
    enabledKinds: Set<MilestoneKind>,
    customMilestones: List<CustomMilestone>,
    memories: List<Memory>,
    onToggleKind: (MilestoneKind, Boolean) -> Unit,
    onAddCustomClick: () -> Unit,
    onToggleCustomEnabled: (String, Boolean) -> Unit,
    onDeleteCustomClick: (CustomMilestone) -> Unit,
    onToggleMoment: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val yearlyLabel = stringResource(R.string.custom_milestone_yearly)
    val disabledLabel = stringResource(R.string.custom_milestone_disabled)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.milestone_settings_title),
                modifier = Modifier.semantics { heading() },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.milestone_settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Moments are switched on one by one in their own section below
                MilestoneKind.entries.filter { it != MilestoneKind.MOMENT }.forEach { kind ->
                    val checked = kind in enabledKinds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouchTarget)
                            .toggleable(
                                value = checked,
                                role = Role.Switch,
                                onValueChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleKind(kind, enabled)
                                }
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(kind.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                if (customMilestones.isEmpty()) {
                    Text(
                        text = stringResource(R.string.custom_milestones_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    customMilestones.forEach { milestone ->
                        val nextDate = milestone.nextOccurrence(today)
                        val formattedDate = nextDate.format(GermanLongDate)
                        val subline = when {
                            !milestone.enabled -> "$formattedDate · $disabledLabel"
                            milestone.recurringYearly -> "$formattedDate · $yearlyLabel"
                            else -> formattedDate
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val enabledDescription = stringResource(R.string.custom_milestone_enabled_toggle, milestone.title)
                            Checkbox(
                                checked = milestone.enabled,
                                onCheckedChange = { enabled ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleCustomEnabled(milestone.id, enabled)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.semantics { contentDescription = enabledDescription }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = milestone.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (!milestone.enabled) 0.5f else 1f)
                                )
                                Text(
                                    text = subline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteCustomClick(milestone) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete_custom_milestone, milestone.title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onAddCustomClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.add_custom_milestone))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                MomentMilestoneSection(
                    memories = memories,
                    today = today,
                    onToggleMoment = onToggleMoment
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

/** Asks before [milestone] is deleted for both partners. */
@Composable
internal fun DeleteCustomMilestoneDialog(
    milestone: CustomMilestone,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.delete_custom_milestone_title),
                modifier = Modifier.semantics { heading() },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_custom_milestone_desc, milestone.title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = stringResource(R.string.confirm_delete),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/** Collapsible list of all moments, each with its own shared switch to count its yearly anniversaries. */
@Composable
private fun MomentMilestoneSection(
    memories: List<Memory>,
    today: LocalDate,
    onToggleMoment: (String, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "momentsArrow"
    )
    val sortedMemories = remember(memories) { memories.sortedByDescending { it.date } }
    val yearlyLabel = stringResource(R.string.custom_milestone_yearly)
    val disabledLabel = stringResource(R.string.custom_milestone_disabled)
    val toggleDescription = stringResource(if (expanded) R.string.milestone_moments_collapse else R.string.milestone_moments_expand)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clickable { expanded = !expanded }
            .semantics { contentDescription = toggleDescription }
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.milestone_moments_section, memories.size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(arrowRotation)
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(spring(stiffness = Spring.StiffnessLow)),
        exit = shrinkVertically(spring(stiffness = Spring.StiffnessLow))
    ) {
        Column {
            if (sortedMemories.isEmpty()) {
                Text(
                    text = stringResource(R.string.milestone_moments_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            sortedMemories.forEach { memory ->
                val subline = if (memory.countsAsMilestone) {
                    val next = MomentMilestone(memory.id, memory.title, memory.date).nextAnniversary(today)
                    "${next.format(GermanLongDate)} · $yearlyLabel"
                } else {
                    "${memory.date.format(GermanLongDate)} · $disabledLabel"
                }
                val enabledDescription = stringResource(R.string.custom_milestone_enabled_toggle, memory.title)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = memory.countsAsMilestone,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleMoment(memory.id, enabled)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.semantics { contentDescription = enabledDescription }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memory.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (memory.countsAsMilestone) 1f else 0.5f)
                        )
                        Text(
                            text = subline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private val MilestoneKind.labelRes: Int
    get() = when (this) {
        MilestoneKind.DAYS -> R.string.milestone_kind_days
        MilestoneKind.WEEKS -> R.string.milestone_kind_weeks
        MilestoneKind.MONTHS -> R.string.milestone_kind_months
        MilestoneKind.YEARS -> R.string.milestone_kind_years
        MilestoneKind.MOMENT -> R.string.milestone_kind_moments
        MilestoneKind.CUSTOM -> R.string.milestone_kind_custom
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddCustomMilestoneDialog(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now(ZoneId.systemDefault()).plusMonths(1)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var recurringYearly by remember { mutableStateOf(false) }
    val initialDraft = remember { CustomMilestoneDraft(title, selectedDate, recurringYearly) }
    var showDiscardPrompt by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            val hasChanges = CustomMilestoneDraft(title, selectedDate, recurringYearly).hasChangesFrom(initialDraft)
            if (hasChanges) showDiscardPrompt = true else onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.add_custom_milestone),
                modifier = Modifier.semantics { heading() },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val titleFocus = rememberInitialFocus(enabled = true)
            val focusManager = LocalFocusManager.current
            Column(
                modifier = Modifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ValidatedTextField(
                    field = InputField.MILESTONE_TITLE,
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.custom_milestone_name_label),
                    placeholder = stringResource(R.string.custom_milestone_name_placeholder),
                    modifier = Modifier.focusRequester(titleFocus),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                DateFieldButton(
                    label = stringResource(R.string.memory_date_label),
                    value = selectedDate.format(GermanLongDate),
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = recurringYearly,
                            role = Role.Switch,
                            onValueChange = { enabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                recurringYearly = enabled
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.custom_milestone_recurring_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.custom_milestone_recurring_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = recurringYearly,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(title.trim(), selectedDate, recurringYearly)
                },
                enabled = InputRules.isValid(InputField.MILESTONE_TITLE, title),
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

    if (showDiscardPrompt) {
        DiscardChangesDialog(onDiscard = onDismiss, onKeepEditing = { showDiscardPrompt = false })
    }

    if (showDatePicker) {
        MemoryDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate = it }
        )
    }
}

private val previewCustomMilestones = listOf(
    CustomMilestone(id = "c1", title = "Hochzeit", date = LocalDate.of(2027, 8, 12)),
    CustomMilestone(id = "c3", title = "Erster Kuss", date = LocalDate.of(2025, 6, 20), recurringYearly = true),
    CustomMilestone(id = "c4", title = "Umzug", date = LocalDate.of(2026, 12, 1), enabled = false),
    CustomMilestone(id = "c2", title = "Erste Wohnung", date = LocalDate.of(2026, 2, 12))
)

@Preview(name = "Milestone Settings Light", showBackground = true)
@Composable
private fun MilestoneSettingsDialogPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        MilestoneSettingsDialog(
            enabledKinds = setOf(MilestoneKind.DAYS, MilestoneKind.YEARS, MilestoneKind.CUSTOM),
            customMilestones = previewCustomMilestones,
            memories = PreviewData.memories,
            onToggleKind = { _, _ -> },
            onAddCustomClick = {},
            onToggleCustomEnabled = { _, _ -> },
            onDeleteCustomClick = {},
            onToggleMoment = { _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview(name = "Milestone Settings Dark", showBackground = true)
@Composable
private fun MilestoneSettingsDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        MilestoneSettingsDialog(
            enabledKinds = setOf(MilestoneKind.MONTHS, MilestoneKind.YEARS),
            customMilestones = emptyList(),
            memories = PreviewData.memories,
            onToggleKind = { _, _ -> },
            onAddCustomClick = {},
            onToggleCustomEnabled = { _, _ -> },
            onDeleteCustomClick = {},
            onToggleMoment = { _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview(name = "Add Custom Milestone Light", showBackground = true)
@Composable
private fun AddCustomMilestoneDialogPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        AddCustomMilestoneDialog(onDismiss = {}, onSave = { _, _, _ -> })
    }
}

@Preview(name = "Add Custom Milestone Dark", showBackground = true)
@Composable
private fun AddCustomMilestoneDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        AddCustomMilestoneDialog(onDismiss = {}, onSave = { _, _, _ -> })
    }
}

@Preview(name = "Delete Custom Milestone Light", showBackground = true)
@Composable
private fun DeleteCustomMilestoneDialogPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        DeleteCustomMilestoneDialog(milestone = previewCustomMilestones.first(), onDismiss = {}, onConfirmDelete = {})
    }
}

@Preview(name = "Delete Custom Milestone Dark", showBackground = true)
@Composable
private fun DeleteCustomMilestoneDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        DeleteCustomMilestoneDialog(milestone = previewCustomMilestones.first(), onDismiss = {}, onConfirmDelete = {})
    }
}
