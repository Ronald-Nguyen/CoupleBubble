package com.aistudio.couplebubble.qxztrw.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.PairingTab
import com.aistudio.couplebubble.qxztrw.ui.PairingUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.GoldenSunsetLight
import com.aistudio.couplebubble.qxztrw.ui.theme.MyApplicationTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.OceanBluePrimaryLight
import com.aistudio.couplebubble.qxztrw.ui.theme.SunsetTerracottaLight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PairingScreen(
    state: PairingUiState,
    onTabSelected: (PairingTab) -> Unit,
    onEnteredCodeChanged: (String) -> Unit,
    onCopyCodeClicked: () -> Unit,
    onGenerateNewCode: () -> Unit,
    onConnectClicked: () -> Unit,
    onOpenDemoSpace: () -> Unit,
    onSaveSpaceSetup: (partnerAName: String, partnerBName: String, date: LocalDate) -> Unit = { _, _, _ -> },
    onDismissSetupDialog: () -> Unit = {},
    onSignInWithGoogle: (uid: String, email: String?, displayName: String?) -> Unit = { _, _, _ -> },
    onSignInWithGoogleClick: (Context) -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                isLoading = state.isGoogleAuthLoading,
                errorMessage = state.googleAuthError,
                onSignInClick = { onSignInWithGoogleClick(context) },
                onSignOutGoogle = onSignOutGoogle,
                onSignInWithGoogle = onSignInWithGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Tab Selector
            PairingTabRow(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
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
                            copyToClipboard(context, state.generatedCode)
                            onCopyCodeClicked()
                        },
                        onGenerateNewCode = onGenerateNewCode,
                        onOpenDemoSpace = onOpenDemoSpace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 480.dp)
                    )
                }
                PairingTab.ENTER -> {
                    EnterCodeTabContent(
                        state = state,
                        onEnteredCodeChanged = onEnteredCodeChanged,
                        onConnectClicked = onConnectClicked,
                        onOpenDemoSpace = onOpenDemoSpace,
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
                onDismiss = onDismissSetupDialog,
                onSave = onSaveSpaceSetup
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceSetupDialog(
    currentPartnerA: String = "Alex",
    currentPartnerB: String = "Sam",
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (partnerAName: String, partnerBName: String, date: LocalDate) -> Unit
) {
    var partnerA by remember { mutableStateOf(currentPartnerA) }
    var partnerB by remember { mutableStateOf(currentPartnerB) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isPartnerAValid = partnerA.trim().length >= 2
    val isPartnerBValid = partnerB.trim().length >= 2
    val isValid = isPartnerAValid && isPartnerBValid

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.setup_space_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_space_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = partnerA,
                    onValueChange = { partnerA = it },
                    label = { Text(stringResource(R.string.partner_a_nickname)) },
                    singleLine = true,
                    isError = partnerA.isNotBlank() && !isPartnerAValid,
                    supportingText = {
                        if (partnerA.isNotBlank() && !isPartnerAValid) {
                            Text(stringResource(R.string.nickname_too_short), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = partnerB,
                    onValueChange = { partnerB = it },
                    label = { Text(stringResource(R.string.partner_b_nickname)) },
                    singleLine = true,
                    isError = partnerB.isNotBlank() && !isPartnerBValid,
                    supportingText = {
                        if (partnerB.isNotBlank() && !isPartnerBValid) {
                            Text(stringResource(R.string.nickname_too_short), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate.format(dateFormatter))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onSave(partnerA.trim(), partnerB.trim(), selectedDate)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(R.string.save_space_setup))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
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

@Composable
fun GoogleAuthCard(
    userProfile: UserProfile?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSignInClick: () -> Unit = {},
    onSignOutGoogle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onSignInWithGoogle: ((uid: String, email: String?, displayName: String?) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = stringResource(R.string.google_icon_content_description),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.google_backup_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (userProfile?.email != null) {
                            "${stringResource(R.string.google_signed_in_as)}: ${userProfile.email}"
                        } else {
                            stringResource(R.string.google_backup_optional)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (userProfile?.email != null) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSignOutGoogle()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.google_sign_out),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSignInClick()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.google_sign_in_loading),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.google_sign_in_btn),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
                onClick = { onTabSelected(PairingTab.CREATE) },
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
                onClick = { onTabSelected(PairingTab.ENTER) },
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

@Composable
private fun CreateCodeTabContent(
    state: PairingUiState,
    onCopyCode: () -> Unit,
    onGenerateNewCode: () -> Unit,
    onOpenDemoSpace: () -> Unit,
    modifier: Modifier = Modifier
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
                lineHeight = 22.sp
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
                            text = "DEIN KOPPLUNGSCODE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = state.generatedCode,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 6.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Countdown Timer Row
            CountdownRow(
                formattedCountdown = state.formattedCountdown,
                onRefresh = onGenerateNewCode
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Copy to Clipboard Action Button
            val copyButtonColor by animateColorAsState(
                targetValue = if (state.isCopied) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondary,
                label = "copyButtonColor"
            )

            Button(
                onClick = onCopyCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("copy_code_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = copyButtonColor)
            ) {
                Icon(
                    imageVector = if (state.isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = if (state.isCopied) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state.isCopied) stringResource(R.string.code_copied) else stringResource(R.string.copy_code),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.isCopied) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Demo Space Quick Button for tester convenience
            TextButton(
                onClick = onOpenDemoSpace,
                modifier = Modifier.testTag("open_demo_button")
            ) {
                Text(
                    text = "Oder Demo-Raum direkt öffnen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CountdownRow(
    formattedCountdown: String,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Gültig für $formattedCountdown",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onRefresh,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Code erneuern",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EnterCodeTabContent(
    state: PairingUiState,
    onEnteredCodeChanged: (String) -> Unit,
    onConnectClicked: () -> Unit,
    onOpenDemoSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val pinDescription = stringResource(R.string.pin_input_description)

    // Cursor is pinned to the end so Backspace always removes the previous digit
    val pinFieldValue = remember(state.enteredCode) {
        TextFieldValue(
            text = state.enteredCode,
            selection = TextRange(state.enteredCode.length)
        )
    }

    val onPinChanged: (String) -> Unit = { raw ->
        val digits = raw.filter { it in '0'..'9' }.take(PIN_LENGTH)
        val completesPin = digits.length == PIN_LENGTH && state.enteredCode.length < PIN_LENGTH
        onEnteredCodeChanged(digits)
        if (completesPin && !state.isLoading) {
            keyboardController?.hide()
            focusManager.clearFocus()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onConnectClicked()
        }
    }

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
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_code_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6-digit PIN: evenly spaced boxes with a transparent BasicTextField overlay for IME input
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val code = state.enteredCode
                    for (i in 0 until PIN_LENGTH) {
                        PinDigitSlot(
                            char = code.getOrNull(i)?.toString() ?: "",
                            isActive = i == code.length,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                BasicTextField(
                    value = pinFieldValue,
                    onValueChange = { onPinChanged(it.text) },
                    textStyle = TextStyle(color = Color.Transparent),
                    cursorBrush = SolidColor(Color.Transparent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (state.isInputReady && !state.isLoading) {
                                onConnectClicked()
                            }
                        }
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = pinDescription }
                        .testTag("pin_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick preset for the demo code
            OutlinedButton(
                onClick = { onPinChanged("482913") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(R.string.enter_code_demo_fill),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Error Message Display
            AnimatedVisibility(
                visible = state.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.errorMessage?.let { errorText ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CTA Button: "Verbinden" in Warm Sunset Terracotta
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onConnectClicked()
                },
                enabled = state.isInputReady && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("connect_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.connecting),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.connect),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Demo Space shortcut
            TextButton(
                onClick = onOpenDemoSpace,
                modifier = Modifier.testTag("open_demo_button_enter")
            ) {
                Text(
                    text = "Oder Demo-Raum direkt öffnen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PinDigitSlot(
    char: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.secondary
            char.isNotEmpty() -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        },
        label = "slotBorder"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.surfaceContainerHigh
            char.isNotEmpty() -> MaterialTheme.colorScheme.surfaceContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        label = "slotBg"
    )

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isActive || char.isNotEmpty()) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

private const val PIN_LENGTH = 6

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText("CoupleBubble Code", text)
    clipboard?.setPrimaryClip(clip)
}

// -------------------------------------------------------------
// Previews with Mock Data
// -------------------------------------------------------------

@Preview(name = "Pairing - Create Code (Light)", showBackground = true)
@Composable
private fun PairingScreenCreatePreview() {
    MyApplicationTheme(darkTheme = false) {
        PairingScreen(
            state = PairingUiState(
                selectedTab = PairingTab.CREATE,
                generatedCode = "482-913",
                countdownSeconds = 884
            ),
            onTabSelected = {},
            onEnteredCodeChanged = {},
            onCopyCodeClicked = {},
            onGenerateNewCode = {},
            onConnectClicked = {},
            onOpenDemoSpace = {}
        )
    }
}

@Preview(name = "Pairing - Enter Code (Dark)", showBackground = true)
@Composable
private fun PairingScreenEnterPreview() {
    MyApplicationTheme(darkTheme = true) {
        PairingScreen(
            state = PairingUiState(
                selectedTab = PairingTab.ENTER,
                enteredCode = "48291",
                countdownSeconds = 720
            ),
            onTabSelected = {},
            onEnteredCodeChanged = {},
            onCopyCodeClicked = {},
            onGenerateNewCode = {},
            onConnectClicked = {},
            onOpenDemoSpace = {}
        )
    }
}
