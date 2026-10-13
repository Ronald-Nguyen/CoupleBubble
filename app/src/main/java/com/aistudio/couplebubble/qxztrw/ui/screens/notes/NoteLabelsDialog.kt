package com.aistudio.couplebubble.qxztrw.ui.screens.notes

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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.DraftLabel
import com.aistudio.couplebubble.qxztrw.domain.model.LabelNameError
import com.aistudio.couplebubble.qxztrw.domain.model.NoteOrganizer
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/**
 * Rename, delete and add the labels both partners share. Every change is a draft: "Fertig" writes them all at
 * once (including a typed label that was never added with "+"), "Abbrechen" throws them away, and Back or a tap
 * outside asks first when something would be lost.
 */
@Composable
fun ManageLabelsDialog(
    editor: LabelEditorState,
    labelUsage: Map<String, Int>,
    onEvent: (NotesEvent) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = { onEvent(NotesEvent.RequestCloseLabels) },
        title = {
            Text(
                text = stringResource(R.string.manage_labels_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = { ManageLabelsContent(editor = editor, labelUsage = labelUsage, onEvent = onEvent) },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editor.hasChanges) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(NotesEvent.SaveLabels)
                },
                enabled = editor.canSave,
                modifier = Modifier.testTag("manage_labels_done")
            ) {
                Text(stringResource(R.string.manage_labels_done))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(NotesEvent.CancelLabels) },
                modifier = Modifier.testTag("manage_labels_cancel")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (editor.showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { onEvent(NotesEvent.KeepEditingLabels) },
            title = { Text(stringResource(R.string.discard_label_changes_title)) },
            text = { Text(stringResource(R.string.discard_label_changes_text)) },
            confirmButton = {
                TextButton(onClick = { onEvent(NotesEvent.ConfirmDiscardLabels) }) {
                    Text(stringResource(R.string.discard_changes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(NotesEvent.KeepEditingLabels) }) {
                    Text(stringResource(R.string.keep_editing))
                }
            }
        )
    }
}

/** The dialog's body, separate from [AlertDialog] so it can be tested without a dialog window. */
@Composable
internal fun ManageLabelsContent(
    editor: LabelEditorState,
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
        if (editor.drafts.isEmpty()) {
            Text(
                text = stringResource(R.string.manage_labels_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        editor.drafts.forEach { draft ->
            key(draft.id) {
                LabelRow(
                    draft = draft,
                    error = editor.errors[draft.id],
                    usage = labelUsage[draft.id] ?: 0,
                    onEvent = onEvent
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        NewLabelRow(editor = editor, onEvent = onEvent)
    }
}

@Composable
private fun LabelNameError.message(): String = stringResource(
    when (this) {
        LabelNameError.EMPTY -> R.string.label_name_empty
        LabelNameError.TAKEN -> R.string.label_name_taken
    }
)

@Composable
private fun LabelRow(
    draft: DraftLabel,
    error: LabelNameError?,
    usage: Int,
    onEvent: (NotesEvent) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    // Local text, so typing never waits for the ViewModel; a partner's rename arrives via originalName
    var name by remember(draft.id) { mutableStateOf(draft.name) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(draft.originalName) {
        if (!isFocused) name = draft.name
    }

    val supportingText: String? = when {
        draft.isDeleted && usage > 0 -> pluralStringResource(R.plurals.label_pending_delete_usage, usage, usage)
        draft.isDeleted -> stringResource(R.string.label_pending_delete)
        error != null -> error.message()
        draft.isNew -> stringResource(R.string.label_new_hint)
        usage > 0 -> pluralStringResource(R.plurals.label_usage, usage, usage)
        else -> null
    }

    OutlinedTextField(
        value = name,
        onValueChange = { newName ->
            val limited = newName.take(NoteOrganizer.MAX_LABEL_NAME_LENGTH)
            if (limited != name) {
                name = limited
                onEvent(NotesEvent.DraftRenameLabel(draft.id, limited))
            }
        },
        enabled = !draft.isDeleted,
        singleLine = true,
        isError = error != null && !draft.isDeleted,
        textStyle = LocalTextStyle.current.copy(
            textDecoration = if (draft.isDeleted) TextDecoration.LineThrough else null
        ),
        supportingText = supportingText?.let { { Text(it) } },
        trailingIcon = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(NotesEvent.DraftToggleDeleteLabel(draft.id))
                },
                modifier = Modifier.testTag(
                    if (draft.isDeleted) "restore_label_${draft.id}" else "delete_label_${draft.id}"
                )
            ) {
                Icon(
                    imageVector = if (draft.isDeleted) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.Delete,
                    contentDescription = stringResource(
                        if (draft.isDeleted) R.string.restore_label else R.string.delete_label,
                        name
                    ),
                    tint = if (draft.isDeleted) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
            .testTag("label_field_${draft.id}")
    )
}

@Composable
private fun NewLabelRow(
    editor: LabelEditorState,
    onEvent: (NotesEvent) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var name by rememberSaveable { mutableStateOf(editor.newName) }
    val error = editor.newNameError
    val canAdd = NoteOrganizer.normalizeLabelName(name).isNotEmpty() && error == null
    val submit = {
        if (canAdd) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEvent(NotesEvent.DraftAddLabel)
        }
    }

    // The ViewModel moved the typed name into the list
    LaunchedEffect(editor.addedCount) {
        if (editor.addedCount > 0) name = ""
    }

    OutlinedTextField(
        value = name,
        onValueChange = {
            val limited = it.take(NoteOrganizer.MAX_LABEL_NAME_LENGTH)
            if (limited != name) {
                name = limited
                onEvent(NotesEvent.DraftNewLabelNameChanged(limited))
            }
        },
        singleLine = true,
        placeholder = { Text(stringResource(R.string.new_label_placeholder)) },
        isError = error != null,
        supportingText = error?.let { { Text(it.message()) } },
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
            editor = LabelEditorState(
                drafts = previewLabels.mapIndexed { index, label ->
                    DraftLabel(label.id, label.name, label.name, isDeleted = index == 1)
                } + DraftLabel("new", "Lissabon", originalName = null),
                newName = "Filme"
            ),
            labelUsage = mapOf(previewLabels[0].id to 3, previewLabels[1].id to 1),
            onEvent = {}
        )
    }
}

@Preview(name = "Manage Labels Dark", showBackground = true)
@Composable
private fun ManageLabelsDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        ManageLabelsDialog(editor = LabelEditorState(), labelUsage = emptyMap(), onEvent = {})
    }
}
