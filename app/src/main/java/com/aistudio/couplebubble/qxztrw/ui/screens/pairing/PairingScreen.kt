package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.ui.PairingEvent
import com.aistudio.couplebubble.qxztrw.ui.PairingTab
import com.aistudio.couplebubble.qxztrw.ui.PairingUiState
import com.aistudio.couplebubble.qxztrw.ui.asString
import com.aistudio.couplebubble.qxztrw.ui.components.GoogleAuthCard
import com.aistudio.couplebubble.qxztrw.ui.components.SpaceSetupDialog
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.GoldenSunsetLight
import com.aistudio.couplebubble.qxztrw.ui.theme.OceanBluePrimaryLight
import com.aistudio.couplebubble.qxztrw.ui.theme.SunsetTerracottaLight

@Composable
fun PairingScreen(
    state: PairingUiState,
    onEvent: (PairingEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header Hero with App Icon / Brand
            HeaderBrand()

            Spacer(modifier = Modifier.height(20.dp))

            // Google Backup Anchor Card
            GoogleAuthCard(
                userProfile = state.userProfile,
                isLoading = state.googleAuth.isLoading,
                errorMessage = state.googleAuth.error?.asString(),
                onSignInClick = { onEvent(PairingEvent.SignInWithGoogle(context)) },
                onSignOutGoogle = { onEvent(PairingEvent.SignOutGoogle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Tab Selector
            PairingTabRow(
                selectedTab = state.selectedTab,
                onTabSelected = { onEvent(PairingEvent.SelectTab(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Content
            when (state.selectedTab) {
                PairingTab.CREATE -> {
                    CreateCodeTabContent(
                        state = state,
                        onCopyCode = {
                            if (state.generatedCode.isNotBlank() && copyToClipboard(context, state.generatedCode)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEvent(PairingEvent.CodeCopied)
                            }
                        },
                        onGenerateNewCode = { onEvent(PairingEvent.GenerateNewCode) },
                        onOpenDemoSpace = { onEvent(PairingEvent.OpenDemoSpace) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                    )
                }
                PairingTab.ENTER -> {
                    EnterCodeTabContent(
                        state = state,
                        onEnteredCodeChanged = { onEvent(PairingEvent.EnteredCodeChanged(it)) },
                        onConnectClicked = { onEvent(PairingEvent.Connect) },
                        onOpenDemoSpace = { onEvent(PairingEvent.OpenDemoSpace) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Room Setup / Onboarding Dialog if room details are not complete
        if (state.showSetupSpaceDialog) {
            SpaceSetupDialog(
                onDismiss = { onEvent(PairingEvent.DismissSetupDialog) },
                onSave = { name1, name2, date -> onEvent(PairingEvent.SaveSpaceSetup(name1, name2, date)) }
            )
        }

        if (state.showSpaceFullDialog) {
            SpaceFullDialog(
                onRecheckCode = { onEvent(PairingEvent.RecheckCodeAfterSpaceFull) },
                onCreateOwnSpace = { onEvent(PairingEvent.CreateOwnSpaceAfterSpaceFull) }
            )
        }
    }
}

@Composable
private fun HeaderBrand() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // App Icon Badge with Mediterranean Ocean Blue & Terracotta Blend
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    spotColor = SunsetTerracottaLight.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GoldenSunsetLight,
                            SunsetTerracottaLight,
                            OceanBluePrimaryLight
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PairingTabRow(
    selectedTab: PairingTab,
    onTabSelected: (PairingTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val selectTab: (PairingTab) -> Unit = { tab ->
        if (tab != selectedTab) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onTabSelected(tab)
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(18.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == PairingTab.CREATE,
                onClick = { selectTab(PairingTab.CREATE) },
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .testTag("tab_create_code"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tab_create_code),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == PairingTab.CREATE) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            )

            Tab(
                selected = selectedTab == PairingTab.ENTER,
                onClick = { selectTab(PairingTab.ENTER) },
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .testTag("tab_enter_code"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Password,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tab_enter_code),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedTab == PairingTab.ENTER) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            )
        }
    }
}

/** Returns false when no clipboard service is available, so the UI never confirms a copy that didn't happen. */
private fun copyToClipboard(context: Context, text: String): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText("CoupleBubble Code", text))
    return true
}

@Preview(name = "Pairing - Create Code (Light)", showBackground = true)
@Composable
private fun PairingScreenCreatePreview() {
    CoupleBubbleTheme(darkTheme = false) {
        PairingScreen(
            state = PairingUiState(
                selectedTab = PairingTab.CREATE,
                generatedCode = "482-913",
                countdownSeconds = 884
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Pairing - Enter Code (Dark)", showBackground = true)
@Composable
private fun PairingScreenEnterPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        PairingScreen(
            state = PairingUiState(
                selectedTab = PairingTab.ENTER,
                enteredCode = "48291",
                countdownSeconds = 720
            ),
            onEvent = {}
        )
    }
}
