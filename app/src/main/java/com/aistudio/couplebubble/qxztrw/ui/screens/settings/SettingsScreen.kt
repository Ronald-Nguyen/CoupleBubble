package com.aistudio.couplebubble.qxztrw.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.asString
import com.aistudio.couplebubble.qxztrw.domain.model.canSwapPartners
import com.aistudio.couplebubble.qxztrw.ui.components.DialogScrollColumn
import com.aistudio.couplebubble.qxztrw.ui.components.GoogleAuthCard
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.AddCustomMilestoneDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DeleteCustomMilestoneDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.EditNamesDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.MilestoneSettingsDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.shareRoomCode
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val space = state.space

    var systemNotificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        // Android stops showing the dialog after repeated refusals; then the system settings are the only way
        val canAskAgain = (context as? Activity)?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
        } ?: true
        if (!granted && !canAskAgain) openAppNotificationSettings(context)
    }
    LifecycleResumeEffect(Unit) {
        systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose {}
    }

    BackHandler(enabled = true) {
        onEvent(SettingsEvent.Close)
    }

    Scaffold(
        modifier = modifier.testTag("settings_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(SettingsEvent.Close) },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Abschnitt: Raum
            SettingsSectionHeader(title = stringResource(R.string.settings_section_space))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_names_colors),
                icon = Icons.Outlined.Palette,
                onClick = { onEvent(SettingsEvent.ShowEditNamesDialog(true)) },
                modifier = Modifier.testTag("settings_item_names_colors")
            )

            if (space != null && space.pairingCode.length == 6) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                val shareMessage = stringResource(R.string.share_code_message, space.formattedPairingCode)
                val shareChooserTitle = stringResource(R.string.share_code_chooser)
                SettingsActionItem(
                    title = stringResource(R.string.settings_item_share_code),
                    icon = Icons.Outlined.Share,
                    onClick = { shareRoomCode(context, shareMessage, shareChooserTitle) },
                    modifier = Modifier.testTag("settings_item_share_code")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_milestones),
                icon = Icons.Outlined.Flag,
                onClick = { onEvent(SettingsEvent.ShowMilestoneSettingsDialog(true)) },
                modifier = Modifier.testTag("settings_item_milestones")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_disconnect),
                icon = Icons.Outlined.LinkOff,
                tint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = { onEvent(SettingsEvent.ShowDisconnectDialog(true)) },
                modifier = Modifier.testTag("settings_item_disconnect")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Abschnitt: Darstellung
            SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = state.themeMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(SettingsEvent.SetThemeMode(mode))
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(mode.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            selectedBorderColor = Color.Transparent,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 0.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_chip_${mode.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Abschnitt: Benachrichtigungen
            SettingsSectionHeader(title = stringResource(R.string.settings_section_notifications))

            if (!systemNotificationsEnabled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .testTag("notification_permission_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.notification_permission_warning_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.notification_permission_warning_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openAppNotificationSettings(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("notification_permission_allow_button")
                        ) {
                            Text(
                                text = stringResource(R.string.notification_permission_allow),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            SettingsSwitchItem(
                title = stringResource(R.string.settings_item_notify_moments),
                icon = Icons.Outlined.Collections,
                checked = state.notificationPreferences.partnerMomentsEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetPartnerMomentsNotificationEnabled(it)) },
                modifier = Modifier.testTag("settings_switch_moments")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingsSwitchItem(
                title = stringResource(R.string.settings_item_notify_milestones),
                icon = Icons.Outlined.Flag,
                checked = state.notificationPreferences.milestonesEnabled,
                onCheckedChange = { onEvent(SettingsEvent.SetMilestonesNotificationEnabled(it)) },
                modifier = Modifier.testTag("settings_switch_milestones")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_system_notifications),
                icon = Icons.Outlined.Notifications,
                onClick = { openAppNotificationSettings(context) },
                modifier = Modifier.testTag("settings_item_system_notifications")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Abschnitt: Konto
            SettingsSectionHeader(title = stringResource(R.string.settings_section_account))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_google_backup),
                icon = Icons.Outlined.Shield,
                onClick = { onEvent(SettingsEvent.ShowGoogleBackupDialog(true)) },
                modifier = Modifier.testTag("settings_item_google_backup")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Abschnitt: Über
            SettingsSectionHeader(title = stringResource(R.string.settings_section_about))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_app_version),
                icon = Icons.Outlined.Info,
                onClick = null,
                trailingContent = {
                    Text(
                        text = state.appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.testTag("settings_item_app_version")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            SettingsActionItem(
                title = stringResource(R.string.settings_item_privacy),
                icon = Icons.Outlined.Security,
                onClick = { onEvent(SettingsEvent.ShowPrivacySummaryDialog(true)) },
                modifier = Modifier.testTag("settings_item_privacy")
            )

            Spacer(modifier = Modifier.height(36.dp))
        }

        // Dialogs
        when (val dialog = state.dialog) {
            SettingsDialog.None -> Unit

            SettingsDialog.EditNames -> {
                if (space != null) {
                    EditNamesDialog(
                        currentPartner1 = space.partner1Name,
                        currentPartner2 = space.partner2Name,
                        currentPartner1ColorHex = space.partner1ColorHex,
                        currentPartner2ColorHex = space.partner2ColorHex,
                        onDismiss = { onEvent(SettingsEvent.ShowEditNamesDialog(false)) },
                        onSave = { newName1, newName2, newColor1, newColor2 ->
                            onEvent(SettingsEvent.SavePartnerDetails(newName1, newName2, newColor1, newColor2))
                            onEvent(SettingsEvent.ShowEditNamesDialog(false))
                        },
                        canSwapRoles = space.canSwapPartners,
                        isSwappingRoles = state.isSwappingRoles,
                        completedRoleSwaps = state.completedRoleSwaps,
                        onSwapRoles = { onEvent(SettingsEvent.SwapPartnerRoles) }
                    )
                }
            }

            is SettingsDialog.MilestoneSettings -> {
                MilestoneSettingsDialog(
                    enabledKinds = state.counterPreferences.enabledMilestoneKinds,
                    customMilestones = state.customMilestones,
                    memories = state.memories,
                    onToggleKind = { kind, enabled -> onEvent(SettingsEvent.ToggleMilestoneKind(kind, enabled)) },
                    onAddCustomClick = { onEvent(SettingsEvent.ShowAddCustomMilestoneDialog(true)) },
                    onToggleCustomEnabled = { id, enabled -> onEvent(SettingsEvent.SetCustomMilestoneEnabled(id, enabled)) },
                    onDeleteCustomClick = { onEvent(SettingsEvent.ShowDeleteCustomMilestoneDialog(it)) },
                    onToggleMoment = { id, enabled -> onEvent(SettingsEvent.SetMomentMilestoneEnabled(id, enabled)) },
                    onDismiss = { onEvent(SettingsEvent.ShowMilestoneSettingsDialog(false)) }
                )
                if (dialog.addingCustom) {
                    AddCustomMilestoneDialog(
                        onDismiss = { onEvent(SettingsEvent.ShowAddCustomMilestoneDialog(false)) },
                        onSave = { title, date, recurring -> onEvent(SettingsEvent.AddCustomMilestone(title, date, recurring)) }
                    )
                }
                dialog.deletingCustom?.let { milestone ->
                    DeleteCustomMilestoneDialog(
                        milestone = milestone,
                        onDismiss = { onEvent(SettingsEvent.ShowDeleteCustomMilestoneDialog(null)) },
                        onConfirmDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(SettingsEvent.DeleteCustomMilestone(milestone.id))
                        }
                    )
                }
            }

            SettingsDialog.GoogleBackup -> {
                AlertDialog(
                    onDismissRequest = { onEvent(SettingsEvent.ShowGoogleBackupDialog(false)) },
                    title = {
                        Text(
                            text = stringResource(R.string.google_backup_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        DialogScrollColumn {
                            GoogleAuthCard(
                            userProfile = state.userProfile,
                            isLoading = state.googleAuth.isLoading,
                            errorMessage = state.googleAuth.error?.asString(),
                            onSignInClick = { onEvent(SettingsEvent.SignInWithGoogle(context)) },
                            onSignOutGoogle = null
                        )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { onEvent(SettingsEvent.ShowGoogleBackupDialog(false)) },
                            modifier = Modifier.testTag("google_backup_close_button")
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            SettingsDialog.Disconnect -> {
                AlertDialog(
                    onDismissRequest = { onEvent(SettingsEvent.ShowDisconnectDialog(false)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.LinkOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.disconnect_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        DialogScrollColumn {
                            Text(
                            text = stringResource(R.string.disconnect_dialog_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { onEvent(SettingsEvent.ConfirmDisconnect) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.testTag("confirm_disconnect_button")
                        ) {
                            Text(
                                text = stringResource(R.string.confirm_disconnect),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { onEvent(SettingsEvent.ShowDisconnectDialog(false)) },
                            modifier = Modifier.testTag("cancel_disconnect_button")
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            SettingsDialog.PrivacySummary -> {
                AlertDialog(
                    onDismissRequest = { onEvent(SettingsEvent.ShowPrivacySummaryDialog(false)) },
                    title = {
                        Text(
                            text = stringResource(R.string.privacy_summary_title),
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
                                text = stringResource(R.string.privacy_summary_point_private),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.privacy_summary_point_storage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.privacy_summary_point_location),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.privacy_summary_point_rights),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { onEvent(SettingsEvent.ShowPrivacySummaryDialog(false)) },
                            modifier = Modifier.testTag("privacy_summary_close_button")
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    },
                    modifier = Modifier.testTag("privacy_summary_dialog")
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsActionItem(
    title: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val clickableModifier = if (onClick != null) {
        Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { newValue ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(newValue)
                }
            )
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.secondary
            )
        )
    }
}

fun openAppNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }

@Preview(name = "SettingsScreen Light", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(
                isOpen = true,
                space = PreviewData.space,
                appVersion = "1.4.0"
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "SettingsScreen Dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(
                isOpen = true,
                space = PreviewData.space,
                appVersion = "1.4.0"
            ),
            onEvent = {}
        )
    }
}
