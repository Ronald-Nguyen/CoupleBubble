package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.GermanShortDate
import com.aistudio.couplebubble.qxztrw.ui.components.DateFieldButton
import com.aistudio.couplebubble.qxztrw.ui.components.selectableDatesBetween
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlaceSource
import com.aistudio.couplebubble.qxztrw.domain.model.source
import com.aistudio.couplebubble.qxztrw.ui.components.DiscardChangesDialog
import com.aistudio.couplebubble.qxztrw.ui.components.MemoryDatePickerDialog
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.home.ReunionUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private enum class ReunionDateField { START, END }

/**
 * Plans or edits the reunion. The switch only covers the user's own location and is optional; the partner's row just
 * says whether they share theirs. Location access is asked for when the switch goes on; a refusal flips it back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReunionDialog(
    current: ReunionUiState?,
    partnerName: String,
    ownColor: Color,
    partnerColor: Color,
    onDismiss: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate, shareLocation: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    var startDate by remember { mutableStateOf(current?.reunion?.startDate ?: today.plusWeeks(2)) }
    var endDate by remember { mutableStateOf(current?.reunion?.endDate ?: today.plusWeeks(2).plusDays(2)) }
    var pickingDate by remember { mutableStateOf<ReunionDateField?>(null) }
    var shareLocation by remember { mutableStateOf(current?.ownPlace?.sharesLocation == true) }
    var permissionDenied by remember { mutableStateOf(false) }
    val initialDraft = remember { ReunionDraft(startDate, endDate, shareLocation) }
    var showDiscardPrompt by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        shareLocation = granted
        permissionDenied = !granted
    }

    AlertDialog(
        onDismissRequest = {
            val hasChanges = ReunionDraft(startDate, endDate, shareLocation).hasChangesFrom(initialDraft)
            if (hasChanges) showDiscardPrompt = true else onDismiss()
        },
        title = { Text(text = stringResource(R.string.reunion_dialog_title), fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Stacked: side by side, the dates are cut off on narrow screens and large font scales
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField(
                        label = stringResource(R.string.reunion_from),
                        date = startDate,
                        onClick = { pickingDate = ReunionDateField.START },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DateField(
                        label = stringResource(R.string.reunion_to),
                        date = endDate,
                        onClick = { pickingDate = ReunionDateField.END },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Text(
                    text = stringResource(R.string.reunion_distance_label).uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reunion_share_location")
                        .toggleable(
                            value = shareLocation,
                            role = Role.Switch,
                            onValueChange = { enabled ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                permissionDenied = false
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                when {
                                    !enabled -> shareLocation = false
                                    granted -> shareLocation = true
                                    else -> permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    ColorDot(ownColor)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            text = stringResource(R.string.reunion_share_location),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                if (permissionDenied) R.string.reunion_location_denied else R.string.reunion_share_location_hint,
                                partnerName
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionDenied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = shareLocation,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.secondary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    ColorDot(partnerColor)
                    Column(modifier = Modifier.padding(horizontal = 10.dp)) {
                        Text(
                            text = partnerName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = partnerStatus(current?.partnerPlace, current?.nowMillis ?: 0L),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (current != null) {
                    TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.testTag("reunion_delete")) {
                        Text(text = stringResource(R.string.reunion_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSave(startDate, endDate, shareLocation)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("reunion_save")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDiscardPrompt) {
        DiscardChangesDialog(onDiscard = onDismiss, onKeepEditing = { showDiscardPrompt = false })
    }

    // Removing the reunion also removes both partners' shared places, so it is confirmed first
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.reunion_remove_confirm_title), modifier = Modifier.semantics { heading() }) },
            text = { Text(stringResource(R.string.reunion_remove_confirm_text, partnerName)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("reunion_delete_confirm")
                ) {
                    Text(stringResource(R.string.reunion_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    pickingDate?.let { field ->
        MemoryDatePickerDialog(
            initialDate = if (field == ReunionDateField.START) startDate else endDate,
            onDismiss = { pickingDate = null },
            onDateSelected = { picked ->
                if (field == ReunionDateField.START) {
                    // Keep the length of the stay when the start moves
                    val length = ChronoUnit.DAYS.between(startDate, endDate)
                    startDate = picked
                    if (endDate.isBefore(picked)) endDate = picked.plusDays(length.coerceAtLeast(0))
                } else {
                    endDate = if (picked.isBefore(startDate)) startDate else picked
                }
            },
            // An end in the past would make the reunion vanish right after saving
            selectableDates = remember(field) {
                if (field == ReunionDateField.END) selectableDatesBetween(min = LocalDate.now()) else selectableDatesBetween()
            }
        )
    }
}

@Composable
private fun DateField(label: String, date: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    DateFieldButton(label = label, value = date.format(GermanShortDate), onClick = onClick, modifier = modifier, iconSize = 18.dp)
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun partnerStatus(place: ReunionPlace?, nowMillis: Long): String = when (val source = place.source(nowMillis)) {
    is ReunionPlaceSource.Location -> stringResource(R.string.reunion_partner_shares)
    ReunionPlaceSource.None -> stringResource(R.string.reunion_partner_nothing)
}

@Preview(name = "ReunionDialog Light", showBackground = true)
@Composable
private fun ReunionDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        ReunionDialog(
            current = PreviewData.reunion,
            partnerName = PreviewData.space.partner2Name,
            ownColor = Color(0xFFE65D2E),
            partnerColor = Color(0xFF5B8DB8),
            onDismiss = {},
            onSave = { _, _, _ -> },
            onDelete = {}
        )
    }
}

@Preview(name = "ReunionDialog Dark", showBackground = true)
@Composable
private fun ReunionDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        ReunionDialog(
            current = null,
            partnerName = PreviewData.space.partner2Name,
            ownColor = Color(0xFFE65D2E),
            partnerColor = Color(0xFF5B8DB8),
            onDismiss = {},
            onSave = { _, _, _ -> },
            onDelete = {}
        )
    }
}
