package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/**
 * Asks „Änderungen verwerfen?“ when a form with unsaved input is closed by Back or a tap outside.
 * Dismissing this prompt itself keeps editing.
 */
@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    text: String = stringResource(R.string.discard_changes_text)
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.discard_changes_title)) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard_changes), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text(stringResource(R.string.keep_editing))
            }
        }
    )
}

@Preview(name = "DiscardChangesDialog Light", showBackground = true)
@Composable
private fun DiscardChangesDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        DiscardChangesDialog(onDiscard = {}, onKeepEditing = {})
    }
}

@Preview(name = "DiscardChangesDialog Dark", showBackground = true)
@Composable
private fun DiscardChangesDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        DiscardChangesDialog(onDiscard = {}, onKeepEditing = {})
    }
}
