package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.model.RelationshipMetrics
import com.aistudio.couplebubble.qxztrw.ui.GermanLongDate
import com.aistudio.couplebubble.qxztrw.ui.components.MemoryDatePickerDialog
import com.aistudio.couplebubble.qxztrw.ui.milestoneCountdown
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
internal fun TogetherCard(
    metrics: RelationshipMetrics,
    displayMode: CounterDisplayMode,
    onDisplayModeSelected: (CounterDisplayMode) -> Unit,
    onMilestoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val resources = LocalContext.current.resources
    val nextMilestoneTitle = remember(metrics.nextMilestone, resources) {
        metrics.nextMilestone?.let { milestoneTitle(resources, it) }
    }
    val nextMilestoneCountdown = remember(metrics.daysUntilNextMilestone, resources) {
        milestoneCountdown(resources, metrics.daysUntilNextMilestone)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.together_since_date, metrics.formattedStartDate).uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 10.dp)
            ) {
                CounterDisplayMode.entries.forEach { mode ->
                    val isSelected = mode == displayMode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDisplayModeSelected(mode)
                            }
                        },
                        label = { Text(stringResource(mode.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            AnimatedContent(
                targetState = displayMode,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(stiffness = Spring.StiffnessLow)) { it / 4 })
                        .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "counter_display_mode"
            ) { mode ->
                CounterHero(metrics = metrics, mode = mode)
            }

            if (nextMilestoneTitle != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onMilestoneClick)
                ) {
                    Text(
                        text = stringResource(R.string.next_milestone).uppercase(),
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp)
                    ) {
                        Text(
                            text = nextMilestoneTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = nextMilestoneCountdown,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { metrics.progressToNextMilestone },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private val CounterDisplayMode.labelRes: Int
    get() = when (this) {
        CounterDisplayMode.DAYS -> R.string.counter_mode_days
        CounterDisplayMode.WEEKS -> R.string.counter_mode_weeks
        CounterDisplayMode.MONTHS -> R.string.counter_mode_months
        CounterDisplayMode.YEARS -> R.string.counter_mode_years
    }

@Composable
private fun CounterHero(
    metrics: RelationshipMetrics,
    mode: CounterDisplayMode
) {
    val (value, unitRes) = when (mode) {
        CounterDisplayMode.DAYS -> metrics.totalDays to R.plurals.counter_unit_days
        CounterDisplayMode.WEEKS -> metrics.totalWeeks to R.plurals.counter_unit_weeks
        CounterDisplayMode.MONTHS -> metrics.totalMonths to R.plurals.counter_unit_months
        CounterDisplayMode.YEARS -> metrics.years.toLong() to R.plurals.counter_unit_years
    }
    val formattedValue = remember(value) {
        NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)
    }
    val subline = when (mode) {
        CounterDisplayMode.DAYS -> null
        CounterDisplayMode.WEEKS -> metrics.weekRemainderDays.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.counter_and_days, it, it)
        }
        CounterDisplayMode.MONTHS -> metrics.monthRemainderDays.takeIf { it > 0 }?.let {
            pluralStringResource(R.plurals.counter_and_days, it, it)
        }
        CounterDisplayMode.YEARS -> listOf(
            pluralStringResource(R.plurals.months_count, metrics.months, metrics.months),
            pluralStringResource(R.plurals.days_count, metrics.days, metrics.days)
        ).joinToString(separator = " · ")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = formattedValue,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = pluralStringResource(unitRes, value.toInt()).uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alignByBaseline()
            )
        }

        if (subline != null) {
            Text(
                text = subline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun MilestoneSettingsDialog(
    enabledKinds: Set<MilestoneKind>,
    customMilestones: List<CustomMilestone>,
    onToggleKind: (MilestoneKind, Boolean) -> Unit,
    onAddCustomClick: () -> Unit,
    onDeleteCustom: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val pastLabel = stringResource(R.string.custom_milestone_past)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.milestone_settings_title),
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

                MilestoneKind.entries.forEach { kind ->
                    val checked = kind in enabledKinds
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
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
                        val isPast = milestone.date.isBefore(today)
                        val formattedDate = milestone.date.format(GermanLongDate)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = milestone.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isPast) 0.5f else 1f)
                                )
                                Text(
                                    text = if (isPast) "$formattedDate · $pastLabel" else formattedDate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteCustom(milestone.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete_custom_milestone, milestone.title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private val MilestoneKind.labelRes: Int
    get() = when (this) {
        MilestoneKind.DAYS -> R.string.milestone_kind_days
        MilestoneKind.WEEKS -> R.string.milestone_kind_weeks
        MilestoneKind.MONTHS -> R.string.milestone_kind_months
        MilestoneKind.YEARS -> R.string.milestone_kind_years
        MilestoneKind.CUSTOM -> R.string.milestone_kind_custom
    }

@Composable
internal fun AddCustomMilestoneDialog(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now(ZoneId.systemDefault()).plusMonths(1)) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.add_custom_milestone),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.custom_milestone_name_label)) },
                    placeholder = { Text(stringResource(R.string.custom_milestone_name_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate.format(GermanLongDate),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.memory_date_label)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.memory_date_label),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(title.trim(), selectedDate)
                },
                enabled = title.isNotBlank(),
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
    CustomMilestone(id = "c2", title = "Erste Wohnung", date = LocalDate.of(2026, 2, 12))
)

@Preview(name = "Together Card Light", showBackground = true)
@Composable
private fun TogetherCardPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        TogetherCard(
            metrics = RelationshipDateCalculator.calculate(2025, 6, 25, today = LocalDate.of(2026, 9, 25)),
            displayMode = CounterDisplayMode.DAYS,
            onDisplayModeSelected = {},
            onMilestoneClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Together Card Dark (Wochen)", showBackground = true)
@Composable
private fun TogetherCardDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        TogetherCard(
            metrics = RelationshipDateCalculator.calculate(
                2025, 6, 25,
                today = LocalDate.of(2026, 9, 26),
                enabledKinds = setOf(MilestoneKind.MONTHS, MilestoneKind.YEARS)
            ),
            displayMode = CounterDisplayMode.WEEKS,
            onDisplayModeSelected = {},
            onMilestoneClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Milestone Settings Light", showBackground = true)
@Composable
private fun MilestoneSettingsDialogPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        MilestoneSettingsDialog(
            enabledKinds = setOf(MilestoneKind.DAYS, MilestoneKind.YEARS, MilestoneKind.CUSTOM),
            customMilestones = previewCustomMilestones,
            onToggleKind = { _, _ -> },
            onAddCustomClick = {},
            onDeleteCustom = {},
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
            onToggleKind = { _, _ -> },
            onAddCustomClick = {},
            onDeleteCustom = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Add Custom Milestone Light", showBackground = true)
@Composable
private fun AddCustomMilestoneDialogPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        AddCustomMilestoneDialog(onDismiss = {}, onSave = { _, _ -> })
    }
}

@Preview(name = "Add Custom Milestone Dark", showBackground = true)
@Composable
private fun AddCustomMilestoneDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        AddCustomMilestoneDialog(onDismiss = {}, onSave = { _, _ -> })
    }
}
