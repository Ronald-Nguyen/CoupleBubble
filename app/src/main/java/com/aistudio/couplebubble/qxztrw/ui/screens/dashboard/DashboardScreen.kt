package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.asString
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.canSwapPartners
import com.aistudio.couplebubble.qxztrw.ui.components.GoogleAuthCard
import com.aistudio.couplebubble.qxztrw.ui.components.SpaceSetupDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.OwnPhotoEdit
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import com.aistudio.couplebubble.qxztrw.ui.theme.parseColorHexToLong
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    listState: LazyListState = rememberLazyListState()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val dialogs = state.dialogs
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    // Collapse the FAB to its icon once the user scrolls into the content
    val isFabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 120 }
    }

    BackHandler(enabled = state.isSearchOpen) {
        onEvent(DashboardEvent.CloseSearch)
    }

    val groupedMemories = remember(state.filteredMemories) {
        state.filteredMemories.groupBy { it.date.year }.toSortedMap(compareByDescending { it })
    }
    val years = remember(groupedMemories) { groupedMemories.keys.toList() }

    val yearIndices = remember(
        state.isSearchOpen,
        state.space.isWaitingForPartner,
        state.reunion,
        years
    ) {
        var idx = 0
        idx++ // "header"
        if (!state.isSearchOpen) {
            if (state.space.isWaitingForPartner) idx++
            idx++ // "together_card"
            idx++ // "reunion"
        } else {
            idx++ // "search_status"
        }
        if (years.size > 1) {
            idx++ // "year_jump"
        }
        years.associateWith { year ->
            val currentIdx = idx
            idx++
            currentIdx
        }
    }

    val partner1Color = parseColorHexToLong(state.space.partner1ColorHex, state.space.partner1AvatarColor)
    val partner2Color = parseColorHexToLong(state.space.partner2ColorHex, state.space.partner2AvatarColor)

    fun showSnackbar(@StringRes messageRes: Int) {
        scope.launch { snackbarHostState.showSnackbar(context.getString(messageRes)) }
    }

    // The shared approximate location is only refreshed while this tab is in front, never in the background
    LifecycleResumeEffect(Unit) {
        onEvent(DashboardEvent.RefreshReunionLocation)
        onPauseOrDispose {}
    }
    val ownColor = if (state.isCurrentUserPartner1) partner1Color else partner2Color
    val reunionPartnerColor = if (state.isCurrentUserPartner1) partner2Color else partner1Color
    val reunionPartnerName = if (state.isCurrentUserPartner1) state.space.partner2Name else state.space.partner1Name

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
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            item(key = "header") {
                SpaceTopHeader(
                    space = state.space,
                    editableRole = state.currentUserRole.takeUnless { dialogs.isSwappingRoles },
                    isUploadingProfilePhoto = dialogs.isUploadingProfilePhoto,
                    isMenuExpanded = dialogs.isMenuExpanded,
                    onMenuExpandedChanged = { onEvent(DashboardEvent.SetMenuExpanded(it)) },
                    isSearchOpen = state.isSearchOpen,
                    searchQuery = state.searchQuery,
                    onOpenSearch = { onEvent(DashboardEvent.OpenSearch) },
                    onCloseSearch = { onEvent(DashboardEvent.CloseSearch) },
                    onSearchQueryChange = { onEvent(DashboardEvent.SearchQueryChanged(it)) },
                    onPartnerAvatarDenied = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSnackbar(R.string.only_own_avatar_editable)
                    },
                    onProfileNotLinked = { showSnackbar(R.string.profile_photo_slot_unknown) },
                    onUploadProfilePhotoBytes = { bytes -> onEvent(DashboardEvent.UploadProfilePhoto(bytes)) },
                    onViewProfilePhoto = { photoUrl -> fullscreenImageUrl = photoUrl },
                    onEditNamesClick = { onEvent(DashboardEvent.ShowEditNamesDialog(true)) },
                    onGoogleBackupClick = { onEvent(DashboardEvent.ShowGoogleBackupDialog(true)) },
                    onMilestonesClick = { onEvent(DashboardEvent.ShowMilestoneSettingsDialog(true)) },
                    onReunionClick = { onEvent(DashboardEvent.ShowReunionDialog(true)) },
                    onDisconnectClick = { onEvent(DashboardEvent.ShowDisconnectDialog(true)) }
                )
                Spacer(modifier = Modifier.height(if (state.isSearchOpen) 10.dp else 20.dp))
            }

            if (!state.isSearchOpen) {
                if (state.space.isWaitingForPartner) {
                    item(key = "invite_partner") {
                        InvitePartnerCard(
                            formattedCode = state.space.formattedPairingCode,
                            onCopied = { showSnackbar(R.string.code_copied) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // Hero element: "Zusammen seit" counter with next milestone
                item(key = "together_card") {
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
                }

                if (state.reunion != null) {
                    item(key = "reunion") {
                        ReunionCard(
                            state = state.reunion,
                            partnerName = reunionPartnerName,
                            ownColor = Color(ownColor),
                            partnerColor = Color(reunionPartnerColor),
                            onClick = { onEvent(DashboardEvent.ShowReunionDialog(true)) },
                            onCaptureMoment = { onEvent(DashboardEvent.ShowAddMemoryDialog(true)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                } else {
                    item(key = "reunion") {
                        TextButton(
                            onClick = { onEvent(DashboardEvent.ShowReunionDialog(true)) },
                            modifier = Modifier.testTag("plan_reunion_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.reunion_menu))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else {
                item(key = "search_status") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.memory_search_results_count,
                                state.filteredMemories.size,
                                state.filteredMemories.size
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("memories_search_count")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (groupedMemories.isEmpty()) {
                if (state.isSearchOpen) {
                    item(key = "empty_timeline") {
                        EmptySearchTimelineCard(
                            query = state.searchQuery,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                        )
                    }
                } else {
                    item(key = "empty_timeline") {
                        EmptyTimelineCard(
                            onAddMemoryClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEvent(DashboardEvent.ShowAddMemoryDialog(true))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                        )
                    }
                }
            } else {
                if (years.size > 1) {
                    item(key = "year_jump") {
                        TimelineYearJumpRow(
                            years = years,
                            onYearClick = { year ->
                                val targetIdx = yearIndices[year]
                                if (targetIdx != null) {
                                    scope.launch {
                                        listState.animateScrollToItem(targetIdx)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                items(
                    items = groupedMemories.entries.toList(),
                    key = { (year, _) -> "year_$year" }
                ) { (year, yearMemories) ->
                    YearMemoryGroupCard(
                        year = year,
                        memories = yearMemories,
                        partner1Name = state.space.partner1Name,
                        partner2Name = state.space.partner2Name,
                        partner1Color = partner1Color,
                        partner2Color = partner2Color,
                        currentUserRole = state.currentUserRole,
                        photoSyncStates = state.photoSyncStates,
                        onEditMemory = { onEvent(DashboardEvent.ShowEditMemoryDialog(it)) },
                        onDeleteMemory = { onEvent(DashboardEvent.DeleteMemory(it.id)) },
                        onSaveToGallery = { onEvent(DashboardEvent.SaveToGallery(it)) },
                        onImageClick = { imageUrl -> fullscreenImageUrl = imageUrl },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        fullscreenImageUrl?.let { imageUrl ->
            FullscreenImageViewer(
                imageUrl = imageUrl,
                isSaving = dialogs.isSavingToGallery,
                onSaveToGallery = { onEvent(DashboardEvent.SaveToGallery(listOf(imageUrl))) },
                onDismiss = { fullscreenImageUrl = null }
            )
        }

        if (state.needsSpaceSetup) {
            SpaceSetupDialog(
                ownName = state.setupOwnName,
                partnerName = state.setupPartnerName,
                isCurrentUserPartner1 = state.isCurrentUserPartner1,
                isSaving = dialogs.isSavingSpaceSetup,
                onSave = { name1, name2, date -> onEvent(DashboardEvent.SaveSpaceSetup(name1, name2, date)) }
            )
        }

        when (val dialog = dialogs.dialog) {
            DashboardDialog.None -> Unit
            is DashboardDialog.MilestoneSettings -> {
                MilestoneSettingsDialog(
                    enabledKinds = state.counterPreferences.enabledMilestoneKinds,
                    customMilestones = state.customMilestones,
                    memories = state.memories,
                    onToggleKind = { kind, enabled -> onEvent(DashboardEvent.ToggleMilestoneKind(kind, enabled)) },
                    onAddCustomClick = { onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(true)) },
                    onToggleCustomEnabled = { id, enabled -> onEvent(DashboardEvent.SetCustomMilestoneEnabled(id, enabled)) },
                    onDeleteCustomClick = { onEvent(DashboardEvent.ShowDeleteCustomMilestoneDialog(it)) },
                    onToggleMoment = { id, enabled -> onEvent(DashboardEvent.SetMomentMilestoneEnabled(id, enabled)) },
                    onDismiss = { onEvent(DashboardEvent.ShowMilestoneSettingsDialog(false)) }
                )
                if (dialog.addingCustom) {
                    AddCustomMilestoneDialog(
                        onDismiss = { onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(false)) },
                        onSave = { title, date, recurring -> onEvent(DashboardEvent.AddCustomMilestone(title, date, recurring)) }
                    )
                }
                dialog.deletingCustom?.let { milestone ->
                    DeleteCustomMilestoneDialog(
                        milestone = milestone,
                        onDismiss = { onEvent(DashboardEvent.ShowDeleteCustomMilestoneDialog(null)) },
                        onConfirmDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(DashboardEvent.DeleteCustomMilestone(milestone.id))
                        }
                    )
                }
            }

            DashboardDialog.Reunion -> {
                ReunionDialog(
                    current = state.reunion,
                    partnerName = reunionPartnerName,
                    ownColor = Color(ownColor),
                    partnerColor = Color(reunionPartnerColor),
                    onDismiss = { onEvent(DashboardEvent.ShowReunionDialog(false)) },
                    onSave = { start, end, shareLocation -> onEvent(DashboardEvent.SaveReunion(start, end, shareLocation)) },
                    onDelete = { onEvent(DashboardEvent.DeleteReunion) }
                )
            }

            DashboardDialog.GoogleBackup -> {
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
                            onSignOutGoogle = null
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { onEvent(DashboardEvent.ShowGoogleBackupDialog(false)) }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            DashboardDialog.Disconnect -> {
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
                        Button(
                            onClick = { onEvent(DashboardEvent.ConfirmDisconnect) },
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
                            onClick = { onEvent(DashboardEvent.ShowDisconnectDialog(false)) },
                            modifier = Modifier.testTag("cancel_disconnect_button")
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            DashboardDialog.EditNames -> {
                EditNamesDialog(
                    currentPartner1 = state.space.partner1Name,
                    currentPartner2 = state.space.partner2Name,
                    currentPartner1ColorHex = state.space.partner1ColorHex,
                    currentPartner2ColorHex = state.space.partner2ColorHex,
                    onDismiss = { onEvent(DashboardEvent.ShowEditNamesDialog(false)) },
                    onSave = { newName1, newName2, newColor1, newColor2 ->
                        onEvent(DashboardEvent.SavePartnerDetails(newName1, newName2, newColor1, newColor2))
                        onEvent(DashboardEvent.ShowEditNamesDialog(false))
                    },
                    canSwapRoles = state.space.canSwapPartners,
                    isSwappingRoles = dialogs.isSwappingRoles,
                    completedRoleSwaps = dialogs.completedRoleSwaps,
                    onSwapRoles = { onEvent(DashboardEvent.SwapPartnerRoles) }
                )
            }

            DashboardDialog.AddMemory -> {
                MemoryFormDialog(
                    memory = null,
                    partner1Name = state.space.partner1Name,
                    partner2Name = state.space.partner2Name,
                    partner1Color = partner1Color,
                    partner2Color = partner2Color,
                    editableRole = state.currentUserRole.takeUnless { dialogs.isSwappingRoles },
                    onDismiss = { onEvent(DashboardEvent.ShowAddMemoryDialog(false)) },
                    onSave = { memory, ownPhoto ->
                        val ownImageUri = (ownPhoto as? OwnPhotoEdit.Replace)?.uri
                        onEvent(DashboardEvent.AddMemory(memory.title, memory.date, memory.note, memory.countsAsMilestone, ownImageUri))
                        onEvent(DashboardEvent.ShowAddMemoryDialog(false))
                    }
                )
            }

            is DashboardDialog.EditMemory -> {
                val memory = dialog.memory
                MemoryFormDialog(
                    memory = memory,
                    partner1Name = state.space.partner1Name,
                    partner2Name = state.space.partner2Name,
                    partner1Color = partner1Color,
                    partner2Color = partner2Color,
                    editableRole = state.currentUserRole.takeUnless { dialogs.isSwappingRoles },
                    onDismiss = { onEvent(DashboardEvent.ShowEditMemoryDialog(null)) },
                    onSave = { updatedMemory, ownPhoto ->
                        onEvent(DashboardEvent.UpdateMemory(updatedMemory, ownPhoto))
                        onEvent(DashboardEvent.ShowEditMemoryDialog(null))
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySearchTimelineCard(
    query: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.testTag("memories_search_empty")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (query.isNotBlank()) {
                    stringResource(R.string.memories_search_empty, query)
                } else {
                    stringResource(R.string.memories_search_empty_no_query)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun TimelineYearJumpRow(
    years: List<Int>,
    onYearClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .testTag("timeline_year_jump_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        years.forEach { year ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onYearClick(year)
                    }
                    .testTag("year_chip_$year")
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
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
