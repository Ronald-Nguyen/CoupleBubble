package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.GermanShortDate
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.InputRules
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * First-run setup of a freshly paired space, shown to both partners until one of them saves.
 * The form speaks from the current user's view ("Du" / partner) and maps the names back to the slots,
 * so the joiner never names the creator as themselves. The date has to be picked explicitly and can't lie
 * in the future; the dialog can't be dismissed because the counter needs a real date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSetupDialog(
    ownName: String,
    partnerName: String,
    isCurrentUserPartner1: Boolean,
    isSaving: Boolean,
    onSave: (partner1Name: String, partner2Name: String, date: LocalDate) -> Unit
) {
    var own by rememberSaveable { mutableStateOf(ownName) }
    var partner by rememberSaveable { mutableStateOf(partnerName) }
    var selectedEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val selectedDate = selectedEpochDay?.let(LocalDate::ofEpochDay)
    val isValid = InputRules.isValid(InputField.NICKNAME, own) && InputRules.isValid(InputField.NICKNAME, partner) && selectedDate != null && !isSaving

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                text = stringResource(R.string.setup_space_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            val ownFocus = rememberInitialFocus(enabled = true)
            val partnerFocus = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_space_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ValidatedTextField(
                    field = InputField.NICKNAME,
                    value = own,
                    onValueChange = { own = it },
                    label = stringResource(R.string.partner_a_nickname),
                    modifier = Modifier.focusRequester(ownFocus),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { partnerFocus.requestFocus() })
                )
                ValidatedTextField(
                    field = InputField.NICKNAME,
                    value = partner,
                    onValueChange = { partner = it },
                    label = stringResource(R.string.partner_b_nickname),
                    modifier = Modifier.focusRequester(partnerFocus),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.anniversary_date_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    border = if (selectedDate == null) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    } else {
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate?.format(GermanShortDate) ?: stringResource(R.string.setup_space_pick_date))
                }

                Text(
                    text = stringResource(R.string.setup_space_date_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val date = selectedDate ?: return@Button
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val ownTrimmed = own.trim()
                    val partnerTrimmed = partner.trim()
                    if (isCurrentUserPartner1) {
                        onSave(ownTrimmed, partnerTrimmed, date)
                    } else {
                        onSave(partnerTrimmed, ownTrimmed, date)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.save_space_setup))
                }
            }
        }
    )

    if (showDatePicker) {
        // The Material date picker works on UTC midnight, so convert with UTC to avoid an off-by-one day
        val todayUtcMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialDisplayedMonthMillis = selectedDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
                ?: todayUtcMillis,
            yearRange = 1950..LocalDate.now().year,
            selectableDates = remember(todayUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            if (picked.toEpochDay() != selectedEpochDay) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            selectedEpochDay = picked.toEpochDay()
                        }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(name = "Space Setup Dialog (Light)", showBackground = true)
@Composable
private fun SpaceSetupDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        SpaceSetupDialog(
            ownName = "Lena",
            partnerName = "",
            isCurrentUserPartner1 = true,
            isSaving = false,
            onSave = { _, _, _ -> }
        )
    }
}

@Preview(name = "Space Setup Dialog (Dark)", showBackground = true)
@Composable
private fun SpaceSetupDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        SpaceSetupDialog(
            ownName = "Jonas",
            partnerName = "Lena",
            isCurrentUserPartner1 = false,
            isSaving = false,
            onSave = { _, _, _ -> }
        )
    }
}
