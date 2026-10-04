package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.asString
import com.aistudio.couplebubble.qxztrw.ui.components.GoogleAuthCard
import com.aistudio.couplebubble.qxztrw.ui.components.SpaceSetupDialog
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToLong
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val dialogs = state.dialogs
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    // Collapse the FAB to its icon once the user scrolls into the content
    val isFabExpanded by remember { derivedStateOf { scrollState.value < 120 } }
    val partner1Color = parseColorHexToLong(state.space.partner1ColorHex, state.space.partner1AvatarColor)
    val partner2Color = parseColorHexToLong(state.space.partner2ColorHex, state.space.partner2AvatarColor)

    fun showSnackbar(@StringRes messageRes: Int) {
        scope.launch { snackbarHostState.showSnackbar(context.getString(messageRes)) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = stringResource(R.string.add_memory)) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isFabExpanded) null else stringResource(R.string.add_memory)
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onEvent(DashboardEvent.ShowAddMemoryDialog(true))
                },
                expanded = isFabExpanded,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                modifier = Modifier.testTag("add_memory_fab")
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpaceTopHeader(
                space = state.space,
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                isUploadingProfilePhoto = dialogs.isUploadingProfilePhoto,
                isMenuExpanded = dialogs.isMenuExpanded,
                onMenuExpandedChanged = { onEvent(DashboardEvent.SetMenuExpanded(it)) },
                onPartnerAvatarDenied = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSnackbar(R.string.only_own_avatar_editable)
                },
                onUploadProfilePhotoBytes = { isPartner1, bytes -> onEvent(DashboardEvent.UploadProfilePhoto(isPartner1, bytes)) },
                onEditNamesClick = { onEvent(DashboardEvent.ShowEditNamesDialog(true)) },
                onGoogleBackupClick = { onEvent(DashboardEvent.ShowGoogleBackupDialog(true)) },
                onMilestonesClick = { onEvent(DashboardEvent.ShowMilestoneSettingsDialog(true)) },
                onDisconnectClick = { onEvent(DashboardEvent.ShowDisconnectDialog(true)) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Hero element: "Zusammen seit" counter with next milestone
            TogetherCard(
                metrics = state.metrics,
                displayMode = state.counterPreferences.displayMode,
                onDisplayModeSelected = { onEvent(DashboardEvent.SelectCounterDisplayMode(it)) },
                onMilestoneClick = { onEvent(DashboardEvent.ShowMilestoneSettingsDialog(true)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            MemoryTimelineSection(
                memories = state.memories,
                partner1Name = state.space.partner1Name,
                partner2Name = state.space.partner2Name,
                partner1Color = partner1Color,
                partner2Color = partner2Color,
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                photoSyncStates = state.photoSyncStates,
                onAddMemoryClick = { onEvent(DashboardEvent.ShowAddMemoryDialog(true)) },
                onEditMemory = { onEvent(DashboardEvent.ShowEditMemoryDialog(it)) },
                onDeleteMemory = { onEvent(DashboardEvent.ShowDeleteMemoryDialog(it)) },
                onSaveToGallery = { onEvent(DashboardEvent.SaveToGallery(it)) },
                onImageClick = { imageUrl -> fullscreenImageUrl = imageUrl },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            )

            Spacer(modifier = Modifier.height(84.dp))
        }

        fullscreenImageUrl?.let { imageUrl ->
            FullscreenImageViewer(
                imageUrl = imageUrl,
                isSaving = dialogs.isSavingToGallery,
                onSaveToGallery = { onEvent(DashboardEvent.SaveToGallery(listOf(imageUrl))) },
                onDismiss = { fullscreenImageUrl = null }
            )
        }

        if (dialogs.showSetupSpaceDialog) {
            SpaceSetupDialog(
                currentPartner1 = state.space.partner1Name,
                currentPartner2 = state.space.partner2Name,
                initialDate = LocalDate.of(state.space.anniversaryYear, state.space.anniversaryMonth, state.space.anniversaryDay),
                onDismiss = { /* Not dismissable: the space needs names and a date */ },
                onSave = { name1, name2, date -> onEvent(DashboardEvent.SaveSpaceSetup(name1, name2, date)) }
            )
        }

        if (dialogs.showMilestoneSettingsDialog) {
            MilestoneSettingsDialog(
                enabledKinds = state.counterPreferences.enabledMilestoneKinds,
                customMilestones = state.customMilestones,
                onToggleKind = { kind, enabled -> onEvent(DashboardEvent.ToggleMilestoneKind(kind, enabled)) },
                onAddCustomClick = { onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(true)) },
                onDeleteCustom = { onEvent(DashboardEvent.DeleteCustomMilestone(it)) },
                onDismiss = { onEvent(DashboardEvent.ShowMilestoneSettingsDialog(false)) }
            )
        }

        if (dialogs.showAddCustomMilestoneDialog) {
            AddCustomMilestoneDialog(
                onDismiss = { onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(false)) },
                onSave = { title, date -> onEvent(DashboardEvent.AddCustomMilestone(title, date)) }
            )
        }

        if (dialogs.showGoogleBackupDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(DashboardEvent.ShowGoogleBackupDialog(false)) },
                title = {
                    Text(
                        text = stringResource(R.string.google_backup_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    GoogleAuthCard(
                        userProfile = state.userProfile,
                        isLoading = state.googleAuth.isLoading,
                        errorMessage = state.googleAuth.error?.asString(),
                        onSignInClick = { onEvent(DashboardEvent.SignInWithGoogle(context)) },
                        onSignOutGoogle = {
                            onEvent(DashboardEvent.SignOutGoogle)
                            onEvent(DashboardEvent.ShowGoogleBackupDialog(false))
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { onEvent(DashboardEvent.ShowGoogleBackupDialog(false)) }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (dialogs.showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { onEvent(DashboardEvent.ShowDisconnectDialog(false)) },
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
                    Text(
                        text = stringResource(R.string.disconnect_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { onEvent(DashboardEvent.ConfirmDisconnect) },
                        modifier = Modifier.testTag("confirm_disconnect_button")
                    ) {
                        Text(
                            text = stringResource(R.string.confirm_disconnect),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onEvent(DashboardEvent.ShowDisconnectDialog(false)) },
                        modifier = Modifier.testTag("cancel_disconnect_button")
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (dialogs.showEditNamesDialog) {
            EditNamesDialog(
                currentPartner1 = state.space.partner1Name,
                currentPartner2 = state.space.partner2Name,
                currentPartner1ColorHex = state.space.partner1ColorHex,
                currentPartner2ColorHex = state.space.partner2ColorHex,
                onDismiss = { onEvent(DashboardEvent.ShowEditNamesDialog(false)) },
                onSave = { newName1, newName2, newColor1, newColor2 ->
                    onEvent(DashboardEvent.UpdatePartnerNames(newName1, newName2))
                    if (newColor1 != state.space.partner1ColorHex) {
                        onEvent(DashboardEvent.UpdatePartnerColor(newColor1, isPartner1 = true))
                    }
                    if (newColor2 != state.space.partner2ColorHex) {
                        onEvent(DashboardEvent.UpdatePartnerColor(newColor2, isPartner1 = false))
                    }
                    onEvent(DashboardEvent.ShowEditNamesDialog(false))
                    showSnackbar(R.string.names_and_colors_updated)
                },
                onSwapRoles = { onEvent(DashboardEvent.SwapPartnerRoles) }
            )
        }

        if (dialogs.showAddMemoryDialog) {
            MemoryFormDialog(
                memory = null,
                partner1Name = state.space.partner1Name,
                partner2Name = state.space.partner2Name,
                partner1Color = partner1Color,
                partner2Color = partner2Color,
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                onDismiss = { onEvent(DashboardEvent.ShowAddMemoryDialog(false)) },
                onSave = { memory, image1Uri, image2Uri ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(DashboardEvent.AddMemory(memory.title, memory.date, memory.note, image1Uri, image2Uri))
                    onEvent(DashboardEvent.ShowAddMemoryDialog(false))
                    showSnackbar(R.string.memory_added)
                }
            )
        }

        dialogs.memoryToEdit?.let { memory ->
            MemoryFormDialog(
                memory = memory,
                partner1Name = state.space.partner1Name,
                partner2Name = state.space.partner2Name,
                partner1Color = partner1Color,
                partner2Color = partner2Color,
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                onDismiss = { onEvent(DashboardEvent.ShowEditMemoryDialog(null)) },
                onSave = { updatedMemory, image1Uri, image2Uri ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEvent(DashboardEvent.UpdateMemory(updatedMemory, image1Uri, image2Uri))
                    onEvent(DashboardEvent.ShowEditMemoryDialog(null))
                    showSnackbar(R.string.memory_updated)
                }
            )
        }

        dialogs.memoryToDelete?.let { memory ->
            DeleteMemoryConfirmationDialog(
                onDismiss = { onEvent(DashboardEvent.ShowDeleteMemoryDialog(null)) },
                onConfirmDelete = {
                    onEvent(DashboardEvent.DeleteMemory(memory.id))
                    onEvent(DashboardEvent.ShowDeleteMemoryDialog(null))
                    showSnackbar(R.string.memory_deleted)
                }
            )
        }
    }
}


@Preview(name = "Dashboard Light", showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        DashboardScreen(
            state = DashboardUiState(
                space = CoupleSpace(
                    partner1Name = "Alex",
                    partner2Name = "Sam",
                    anniversaryYear = 2025,
                    anniversaryMonth = 6,
                    anniversaryDay = 25
                ),
                metrics = RelationshipDateCalculator.calculate(2025, 6, 25),
                memories = listOf(
                    Memory(
                        id = "1",
                        title = "Erster Urlaub am Meer",
                        date = LocalDate.of(2025, 9, 18),
                        note = "Magischer Sommerurlaub am Meer. Zwei Perspektiven unseres Lieblingsmoments.",
                        partner1ImageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800",
                        partner2ImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800"
                    )
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Dashboard Dark", showBackground = true)
@Composable
private fun DashboardScreenDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        DashboardScreen(
            state = DashboardUiState(
                space = CoupleSpace(
                    partner1Name = "Alex",
                    partner2Name = "Sam",
                    anniversaryYear = 2025,
                    anniversaryMonth = 6,
                    anniversaryDay = 25
                ),
                metrics = RelationshipDateCalculator.calculate(2025, 6, 25),
                memories = listOf(
                    Memory(
                        id = "1",
                        title = "Erster Urlaub am Meer",
                        date = LocalDate.of(2025, 9, 18),
                        note = "Magischer Sommerurlaub am Meer. Zwei Perspektiven unseres Lieblingsmoments.",
                        partner1ImageUrl = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800",
                        partner2ImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800"
                    )
                )
            ),
            onEvent = {}
        )
    }
}
