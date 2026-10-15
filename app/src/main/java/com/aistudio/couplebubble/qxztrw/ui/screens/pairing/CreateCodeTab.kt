package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.aistudio.couplebubble.qxztrw.ui.components.LocalReducedMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.BuildConfig
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.components.minutesLeft
import com.aistudio.couplebubble.qxztrw.ui.components.spokenDigits
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.SunsetTerracottaLight
import java.util.Locale

@Composable
internal fun CreateCodeTabContent(
    state: PairingUiState,
    onCopyCode: () -> Unit,
    onGenerateNewCode: () -> Unit,
    onOpenDemoSpace: () -> Unit,
    modifier: Modifier = Modifier,
    onShareCode: () -> Unit = {},
    showDeveloperTools: Boolean = BuildConfig.DEBUG
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = SunsetTerracottaLight.copy(alpha = 0.2f)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.create_code_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Code Display Container with Warm Ambient Inner Glow
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                color = Color.Transparent
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.your_pairing_code_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val spokenCode = stringResource(R.string.pairing_code_spoken, spokenDigits(state.generatedCode))
                    Text(
                        text = state.generatedCode,
                        modifier = Modifier
                            .semantics { contentDescription = spokenCode }
                            .testTag("generated_code"),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        // May wrap at large font scales instead of being clipped
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Countdown Timer Row
            CountdownRow(
                formattedCountdown = formatCountdown(state.countdownSeconds),
                countdownSeconds = state.countdownSeconds,
                onRefresh = onGenerateNewCode
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Copy to Clipboard Action Button
            val copyButtonColor by animateColorAsState(
                targetValue = if (state.isCopied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondary,
                label = "copyButtonColor"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCopyCode,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .testTag("copy_code_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = copyButtonColor)
                ) {
                    Icon(
                        imageVector = if (state.isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = null,
                        tint = if (state.isCopied) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (state.isCopied) stringResource(R.string.code_copied) else stringResource(R.string.copy_code),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.isCopied) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onShareCode,
                    enabled = state.generatedCode.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                        .testTag("share_code_button"),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.share_code),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showDeveloperTools) {
                Spacer(modifier = Modifier.height(14.dp))

                // Demo space shortcut, debug builds only
                TextButton(
                    onClick = onOpenDemoSpace,
                    modifier = Modifier.testTag("open_demo_button")
                ) {
                    Text(
                        text = stringResource(R.string.open_demo_space),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/** "12:34": minutes and seconds left, independent of the device locale. */
internal fun formatCountdown(totalSeconds: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)

@Composable
private fun CountdownRow(
    formattedCountdown: String,
    countdownSeconds: Int,
    onRefresh: () -> Unit
) {
    // Read inside graphicsLayer, so the pulse redraws the icon instead of recomposing the row every frame
    val alpha: State<Float> = if (LocalReducedMotion.current) {
        remember { mutableFloatStateOf(1f) }
    } else {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alphaPulse"
        )
    }

    val minutes = minutesLeft(countdownSeconds)
    val spokenCountdown = pluralStringResource(R.plurals.code_valid_minutes, minutes, minutes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp).graphicsLayer { this.alpha = alpha.value }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.code_valid_for, formattedCountdown),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Speaks whole minutes only, a seconds ticker would interrupt TalkBack every second
            modifier = Modifier
                .testTag("code_countdown")
                .clearAndSetSemantics {
                    contentDescription = spokenCountdown
                    liveRegion = LiveRegionMode.Polite
                }
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onRefresh
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.renew_code),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(name = "CreateCodeTabContent Light", showBackground = true)
@Composable
private fun CreateCodeTabContentLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        CreateCodeTabContent(
            state = PreviewData.pairing,
            onCopyCode = {},
            onGenerateNewCode = {},
            onOpenDemoSpace = {}
        )
    }
}

@Preview(name = "CreateCodeTabContent Dark", showBackground = true)
@Composable
private fun CreateCodeTabContentDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        CreateCodeTabContent(
            state = PreviewData.pairing.copy(isCopied = true),
            onCopyCode = {},
            onGenerateNewCode = {},
            onOpenDemoSpace = {}
        )
    }
}
