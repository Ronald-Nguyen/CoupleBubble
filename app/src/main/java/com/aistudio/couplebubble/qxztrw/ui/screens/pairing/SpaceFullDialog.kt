package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/** Shown when the entered code belongs to a space that already has two partners. */
@Composable
internal fun SpaceFullDialog(
    onRecheckCode: () -> Unit,
    onCreateOwnSpace: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRecheckCode,
        title = {
            Text(
                text = stringResource(R.string.space_full_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.space_full_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onCreateOwnSpace,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.testTag("space_full_create_own")
            ) {
                Text(
                    text = stringResource(R.string.space_full_create_own),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onRecheckCode,
                modifier = Modifier.testTag("space_full_recheck_code")
            ) {
                Text(
                    text = stringResource(R.string.space_full_recheck_code),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Preview(name = "Pairing - Space Full Dialog (Light)", showBackground = true)
@Composable
private fun SpaceFullDialogLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        SpaceFullDialog(
            onRecheckCode = {},
            onCreateOwnSpace = {}
        )
    }
}

@Preview(name = "Pairing - Space Full Dialog (Dark)", showBackground = true)
@Composable
private fun SpaceFullDialogDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        SpaceFullDialog(
            onRecheckCode = {},
            onCreateOwnSpace = {}
        )
    }
}
