package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.copyToClipboard
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

/** Opens the system share sheet with the invitation text for the room code. */
internal fun shareRoomCode(context: Context, message: String, chooserTitle: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}

/** Shown on the dashboard while only one partner has joined, so the code can still be sent. */
@Composable
internal fun InvitePartnerCard(
    formattedCode: String,
    modifier: Modifier = Modifier,
    onCopied: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val shareMessage = stringResource(R.string.share_code_message, formattedCode)
    val chooserTitle = stringResource(R.string.share_code_chooser)

    Card(
        modifier = modifier.testTag("invite_partner_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.invite_partner_label),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formattedCode,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                // The code wraps at the hyphen instead of being cut to "482-9…"
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.invite_partner_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { shareRoomCode(context, shareMessage, chooserTitle) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("invite_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.share_code))
                }
                OutlinedButton(
                    onClick = {
                        if (copyToClipboard(context, formattedCode)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCopied()
                        }
                    },
                    modifier = Modifier.testTag("invite_copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_code))
                }
            }
        }
    }
}

@Preview(name = "Invite Light", showBackground = true)
@Composable
private fun InvitePartnerCardPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        InvitePartnerCard(formattedCode = "482-913", modifier = Modifier.fillMaxWidth())
    }
}

@Preview(name = "Invite Dark", showBackground = true)
@Composable
private fun InvitePartnerCardDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        InvitePartnerCard(formattedCode = "482-913", modifier = Modifier.fillMaxWidth())
    }
}
