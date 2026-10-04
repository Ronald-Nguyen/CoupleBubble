package com.aistudio.couplebubble.qxztrw.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.NoteLabel
import com.aistudio.couplebubble.qxztrw.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.ui.NotesEvent
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/** Rename, delete and add the labels both partners share. Renames are saved while typing. */
@Composable
fun ManageLabelsDialog(
    labels: List<NoteLabel>,
    labelUsage: Map<String, Int>,
    onEvent: (NotesEvent) -> Unit
) {
    val close = { onEvent(NotesEvent.ShowLabelManager(false)) }

    AlertDialog(
        onDismissRequest = close,
        title = {
            Text(
                text = stringResource(R.string.manage_labels_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = { ManageLabelsContent(labels = labels, labelUsage = labelUsage, onEvent = onEvent) },
        confirmButton = {
            TextButton(
                onClick = close,
                modifier = Modifier.testTag("manage_labels_done")
            ) {
                Text(stringResource(R.string.manage_labels_done))
            }
        }
    )
}

/** The dialog's body, separate from [AlertDialog] so it can be tested without a dialog window. */
@Composable
internal fun ManageLabelsContent(
    labels: List<NoteLabel>,
    labelUsage: Map<String, Int>,
    onEvent: (NotesEvent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
            .testTag("manage_labels_dialog")
    ) {
        Text(
            text = stringResource(R.string.manage_labels_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (labels.isEmpty()) {
            Text(
                text = stringResource(R.string.manage_labels_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        labels.forEach { label ->
            key(label.id) {
                LabelRow(
                    label = label,
                    labels = labels,
                    usage = labelUsage[label.id] ?: 0,
                    onEvent = onEvent
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        NewLabelRow(
            labels = labels,
            onAddLabel = { onEvent(NotesEvent.AddLabel(it)) }
        )
    }
}

@Composable
private fun LabelRow(
    label: NoteLabel,
    labels: List<NoteLabel>,
    usage: Int,
    onEvent: (NotesEvent) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var name by remember(label.id) { mutableStateOf(label.name) }
    var isFocused by remember { mutableStateOf(false) }
    val errorRes = remember(name, labels) {
        val normalized = NoteOrganizer.normalizeLabelName(name)
        when {
            normalized.isEmpty() -> R.string.label_name_empty
            NoteOrganizer.isLabelNameTaken(labels, normalized, exceptId = label.id) -> R.string.label_name_taken
            else -> null
        }
    }

    // Take over the partner's rename, but never while you are typing here
    LaunchedEffect(label.name) {
        if (!isFocused) name = label.name
    }

    OutlinedTextField(
        value = name,
        onValueChange = { newName ->
            val limited = newName.take(NoteOrganizer.MAX_LABEL_NAME_LENGTH)
            if (limited != name) {
                name = limited
                onEvent(NotesEvent.RenameLabel(label.id, limited))
            }
        },
        singleLine = true,
        isError = errorRes != null,
        supportingText = when {
            errorRes != null -> {
                { Text(stringResource(errorRes)) }
            }
            usage > 0 -> {
                { Text(pluralStringResource(R.plurals.label_usage, usage, usage)) }
            }
            else -> null
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(NotesEvent.DeleteLabel(label.id))
                },
                modifier = Modifier.testTag("delete_label_${label.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_label, label.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .testTag("label_field_${label.id}")
    )
}

@Composable
private fun NewLabelRow(
    labels: List<NoteLabel>,
    onAddLabel: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var name by rememberSaveable { mutableStateOf("") }
    val normalized = remember(name) { NoteOrganizer.normalizeLabelName(name) }
    val isTaken = remember(normalized, labels) { NoteOrganizer.isLabelNameTaken(labels, normalized) }
    val canAdd = normalized.isNotEmpty() && !isTaken
    val submit = {
        if (canAdd) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onAddLabel(name)
            name = ""
        }
    }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(NoteOrganizer.MAX_LABEL_NAME_LENGTH) },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.new_label_placeholder)) },
        isError = isTaken,
        supportingText = if (isTaken) {
            { Text(stringResource(R.string.label_name_taken)) }
        } else {
            null
        },
        trailingIcon = {
            IconButton(
                onClick = submit,
                enabled = canAdd,
                modifier = Modifier.testTag("add_label_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_label),
                    tint = if (canAdd) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("new_label_field")
    )
}

@Preview(name = "Manage Labels Light", showBackground = true)
@Composable
private fun ManageLabelsDialogPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        ManageLabelsDialog(
            labels = previewLabels,
            labelUsage = mapOf(previewLabels[0].id to 3, previewLabels[1].id to 1),
            onEvent = {}
        )
    }
}

@Preview(name = "Manage Labels Dark", showBackground = true)
@Composable
private fun ManageLabelsDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        ManageLabelsDialog(labels = emptyList(), labelUsage = emptyMap(), onEvent = {})
    }
}
