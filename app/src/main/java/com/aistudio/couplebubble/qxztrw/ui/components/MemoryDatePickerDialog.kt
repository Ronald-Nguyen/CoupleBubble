package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toPickerMillis(),
        selectableDates = selectableDates
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(datePickerState) {
        snapshotFlow { datePickerState.selectedDateMillis }
            .drop(1)
            .collect { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // The picker works in UTC millis; converting in the local zone could shift the day
                    onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) {
                Text(stringResource(R.string.date_picker_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Only days between [min] and [max] (both inclusive, `null` = open) can be picked. */
@OptIn(ExperimentalMaterial3Api::class)
internal fun selectableDatesBetween(min: LocalDate? = null, max: LocalDate? = null): SelectableDates {
    val minMillis = min?.toPickerMillis()
    val maxMillis = max?.toPickerMillis()
    return object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
            (minMillis == null || utcTimeMillis >= minMillis) && (maxMillis == null || utcTimeMillis <= maxMillis)

        override fun isSelectableYear(year: Int): Boolean =
            (min == null || year >= min.year) && (max == null || year <= max.year)
    }
}

/** A read-only date field that opens a picker; TalkBack reads it as a button with its label and date. */
@Composable
internal fun DateFieldButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp
) {
    val description = stringResource(R.string.date_field_description, label, value)
    val actionLabel = stringResource(R.string.date_field_action)
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(iconSize)) },
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onClick)
                .semantics { contentDescription = description }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "MemoryDatePickerDialog Light", showBackground = true)
@Composable
private fun MemoryDatePickerDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        MemoryDatePickerDialog(initialDate = LocalDate.of(2023, 6, 25), onDismiss = {}, onDateSelected = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "MemoryDatePickerDialog Dark", showBackground = true)
@Composable
private fun MemoryDatePickerDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        MemoryDatePickerDialog(initialDate = LocalDate.of(2023, 6, 25), onDismiss = {}, onDateSelected = {})
    }
}
