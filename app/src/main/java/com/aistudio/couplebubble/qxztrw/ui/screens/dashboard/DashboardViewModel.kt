package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.data.local.displayRoleFlow
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipCalendar
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.hasBoundMembers
import com.aistudio.couplebubble.qxztrw.domain.model.isPartner1
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.session.SpaceContent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialogState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardUiState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.GoogleAuthController
import com.aistudio.couplebubble.qxztrw.ui.screens.home.MemoryPhotoUploadTracker
import com.aistudio.couplebubble.qxztrw.ui.screens.home.OwnPhotoEdit
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PhotoSlotKey
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PhotoSyncState
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The "Uns" tab of a paired couple: counter, milestones, moments, partner names, photos and colors. */
class DashboardViewModel(
    private val repository: CoupleRepository,
    private val sessionStore: SessionStore,
    private val localImageStore: LocalImageStore,
    private val googleAuth: GoogleAuthController,
    private val widgetPublisher: WidgetPublisher,
    spaceContent: SpaceContent,
    private val clock: Clock = Clock.systemDefaultZone(),
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DashboardDialogState())
    private val _counterPreferences = MutableStateFlow(CounterPreferences())
    private val uploads = MemoryPhotoUploadTracker()
    private val _effects = Channel<CoupleEffect>(Channel.BUFFERED)

    /** One-shot toasts, snackbars and haptic confirmations; collected once by the activity. */
    val effects: Flow<CoupleEffect> = _effects.receiveAsFlow()

    private val partnerRoleFlow: Flow<PartnerRole?> = sessionStore.displayRoleFlow

    /**
     * The stored role right now, for a single write. [partnerRoleFlow] starts with `null` until DataStore answers,
     * so its first value must not be used here.
     */
    private suspend fun storedRole(): PartnerRole = PartnerRole.displayFallback(sessionStore.partnerRoleFlow.first())

    init {
        viewModelScope.launch {
            sessionStore.counterPreferencesFlow.collect { _counterPreferences.value = it }
        }
    }

    private data class Session(val space: CoupleSpace?, val userProfile: UserProfile?, val role: PartnerRole?)

    private data class Content(
        val memories: List<Memory>,
        val photoSyncStates: Map<PhotoSlotKey, PhotoSyncState>,
        val counterPreferences: CounterPreferences,
        val customMilestones: List<CustomMilestone>
    )

    private val sessionFlow = combine(repository.currentSpace, repository.currentUserProfile, partnerRoleFlow, ::Session)

    private val contentFlow = combine(
        combine(spaceContent.memories, uploads.pendingMemories, uploads::visibleMemories),
        uploads.syncStates,
        _counterPreferences,
        spaceContent.customMilestones,
        ::Content
    )

    /** `null` while no space is active; the app then shows the pairing or loading screen instead. */
    val uiState: StateFlow<DashboardUiState?> = combine(
        sessionFlow,
        _dialogState,
        googleAuth.state,
        contentFlow
    ) { session, dialogs, auth, content ->
        val space = session.space?.takeIf { it.isActive } ?: return@combine null
        val uid = session.userProfile?.uid
        val calendar = RelationshipCalendar.of(space, content.counterPreferences, content.customMilestones)
        val today = LocalDate.now(clock)
        DashboardUiState(
            space = space,
            metrics = calendar.metrics(today),
            memories = content.memories,
            userProfile = session.userProfile,
            isCurrentUserPartner1 = space.isPartner1(uid, session.role ?: PartnerRole.PARTNER_1),
            currentUserRole = space.editableRole(uid, session.role),
            dialogs = dialogs,
            googleAuth = auth,
            photoSyncStates = content.photoSyncStates,
            counterPreferences = content.counterPreferences,
            customMilestones = content.customMilestones,
            memoryMilestones = calendar.memoryMilestones(content.memories, today),
            milestoneOptions = calendar.reachedMilestones(today)
        )
    }.stateIn(viewModelScope, started, null)

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.SelectMainTab -> updateDialogs { it.copy(selectedTab = event.tab, isMenuExpanded = false) }
            is DashboardEvent.SetMenuExpanded -> updateDialogs { it.copy(isMenuExpanded = event.expanded) }
            is DashboardEvent.ShowDisconnectDialog -> toggleDialog<DashboardDialog.Disconnect>(DashboardDialog.Disconnect, event.show)
            DashboardEvent.ConfirmDisconnect -> onConfirmDisconnect()
            is DashboardEvent.ShowEditNamesDialog -> toggleDialog<DashboardDialog.EditNames>(DashboardDialog.EditNames, event.show)
            is DashboardEvent.UpdatePartnerNames -> onUpdatePartnerNames(event.partner1Name, event.partner2Name)
            is DashboardEvent.UpdatePartnerColor -> onUpdatePartnerColor(event.colorHex, event.isPartner1)
            DashboardEvent.SwapPartnerRoles -> swapPartnerRoles()
            is DashboardEvent.UploadProfilePhoto -> onUploadProfilePhoto(event.bytes)
            is DashboardEvent.ShowAddMemoryDialog -> toggleDialog<DashboardDialog.AddMemory>(DashboardDialog.AddMemory, event.show)
            is DashboardEvent.AddMemory -> viewModelScope.launch {
                addMemory(event.title, event.date, event.note, compress(event.ownImageUri), event.milestoneKey)
            }
            is DashboardEvent.ShowEditMemoryDialog -> toggleDialog<DashboardDialog.EditMemory>(event.memory?.let(DashboardDialog::EditMemory), event.memory != null)
            is DashboardEvent.UpdateMemory -> viewModelScope.launch {
                val change = when (val photo = event.ownPhoto) {
                    OwnPhotoEdit.Keep -> MemoryPhotoChange.Keep
                    OwnPhotoEdit.Remove -> MemoryPhotoChange.Remove
                    is OwnPhotoEdit.Replace -> compress(photo.uri)?.let { MemoryPhotoChange.Replace(it) } ?: MemoryPhotoChange.Keep
                }
                updateMemory(event.memory, change)
            }
            is DashboardEvent.ShowDeleteMemoryDialog -> toggleDialog<DashboardDialog.DeleteMemory>(event.memory?.let(DashboardDialog::DeleteMemory), event.memory != null)
            is DashboardEvent.DeleteMemory -> onDeleteMemory(event.memoryId)
            is DashboardEvent.SaveToGallery -> onSaveToGallery(event.imageUrls)
            is DashboardEvent.ShowGoogleBackupDialog -> toggleDialog<DashboardDialog.GoogleBackup>(DashboardDialog.GoogleBackup, event.show)
            is DashboardEvent.SignInWithGoogle -> onSignInWithGoogle(event.activityContext)
            is DashboardEvent.SaveSpaceSetup -> onSaveSpaceSetup(event.partner1Name, event.partner2Name, event.date)
            is DashboardEvent.SelectCounterDisplayMode -> onCounterDisplayModeSelected(event.mode)
            is DashboardEvent.ShowMilestoneSettingsDialog ->
                toggleDialog<DashboardDialog.MilestoneSettings>(DashboardDialog.MilestoneSettings(), event.show)
            is DashboardEvent.ShowAddCustomMilestoneDialog -> setAddingCustomMilestone(event.show)
            is DashboardEvent.ToggleMilestoneKind -> onMilestoneKindToggled(event.kind, event.enabled)
            is DashboardEvent.AddCustomMilestone -> onAddCustomMilestone(event.title, event.date, event.recurringYearly)
            is DashboardEvent.SetCustomMilestoneEnabled -> onCustomMilestoneEnabledChanged(event.milestoneId, event.enabled)
        }
    }

    private fun updateDialogs(transform: (DashboardDialogState) -> DashboardDialogState) = _dialogState.update(transform)

    /** Opens [dialog] (closing the menu), or closes the open dialog if it is a [T]. */
    private inline fun <reified T : DashboardDialog> toggleDialog(dialog: DashboardDialog?, show: Boolean) = updateDialogs {
        when {
            show && dialog != null -> it.copy(dialog = dialog, isMenuExpanded = false)
            it.dialog is T -> it.copy(dialog = DashboardDialog.None)
            else -> it
        }
    }

    /** The custom-milestone dialog sits on top of the milestone settings. */
    private fun setAddingCustomMilestone(adding: Boolean) = updateDialogs {
        it.copy(dialog = DashboardDialog.MilestoneSettings(addingCustom = adding))
    }

    private val currentSpace: CoupleSpace? get() = repository.currentSpace.value

    // region Space & partners

    private fun onSaveSpaceSetup(partner1Name: String, partner2Name: String, date: LocalDate) {
        val trimmed1 = partner1Name.trim()
        val trimmed2 = partner2Name.trim()
        if (trimmed1.length < 2 || trimmed2.length < 2) return
        if (date.isAfter(LocalDate.now(clock)) || _dialogState.value.isSavingSpaceSetup) return
        val spaceId = currentSpace?.id ?: return

        viewModelScope.launch {
            updateDialogs { it.copy(isSavingSpaceSetup = true) }
            repository.updateSpaceDetails(
                coupleId = spaceId,
                partner1Name = trimmed1,
                partner2Name = trimmed2,
                anniversaryYear = date.year,
                anniversaryMonth = date.monthValue,
                anniversaryDay = date.dayOfMonth
            )
            sessionStore.saveActiveCoupleId(spaceId)
            updateDialogs { it.copy(isSavingSpaceSetup = false) }
        }
    }

    private fun onUpdatePartnerNames(partner1Name: String, partner2Name: String) {
        val space = currentSpace ?: return
        viewModelScope.launch {
            repository.updatePartnerNames(space.id, partner1Name, partner2Name)
            toggleDialog<DashboardDialog.EditNames>(null, show = false)
        }
    }

    private fun onUpdatePartnerColor(colorHex: String, isPartner1: Boolean) {
        val space = currentSpace ?: return
        viewModelScope.launch { repository.updatePartnerColor(space.id, isPartner1, colorHex) }
    }

    private fun onUploadProfilePhoto(imageBytes: ByteArray) {
        val space = currentSpace ?: return
        val dialogs = _dialogState.value
        if (dialogs.isUploadingProfilePhoto) return
        if (dialogs.isSwappingRoles) {
            _effects.trySend(CoupleEffect.Snackbar(UiText.Resource(R.string.profile_photo_wait_for_swap)))
            return
        }
        updateDialogs { it.copy(isUploadingProfilePhoto = true) }
        viewModelScope.launch {
            val result = try {
                repository.uploadProfilePhoto(space.id, storedRole(), imageBytes)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                Result.failure(e)
            } finally {
                updateDialogs { it.copy(isUploadingProfilePhoto = false) }
            }
            _effects.send(
                when {
                    result.isSuccess -> CoupleEffect.Toast(UiText.Resource(R.string.profile_photo_saved))
                    result.exceptionOrNull() is PartnerSlotUnknownException ->
                        CoupleEffect.Snackbar(UiText.Resource(R.string.profile_photo_slot_unknown))
                    else -> CoupleEffect.Toast(UiText.Resource(R.string.profile_photo_upload_failed), isLong = true)
                }
            )
        }
    }

    private fun swapPartnerRoles() {
        val space = currentSpace ?: return
        val dialogs = _dialogState.value
        if (dialogs.isSwappingRoles) return
        // A memory added while the swap reads the list would keep the old slot layout
        if (dialogs.isUploadingProfilePhoto || uploads.hasPendingUploads) {
            _effects.trySend(CoupleEffect.Snackbar(UiText.Resource(R.string.swap_partners_wait_for_upload)))
            return
        }
        updateDialogs { it.copy(isSwappingRoles = true) }
        viewModelScope.launch {
            val result = try {
                repository.swapPartners(space.id)
            } finally {
                updateDialogs { it.copy(isSwappingRoles = false) }
            }
            if (result.isSuccess) {
                // With bound members the stored role follows the swapped IDs; without them only the preference knows
                if (!space.hasBoundMembers) {
                    PartnerRole.fromStored(sessionStore.partnerRoleFlow.firstOrNull())?.let {
                        sessionStore.savePartnerRole(it.swapped())
                    }
                }
                _effects.send(CoupleEffect.PartnersSwapped)
            } else {
                val messageRes = if (result.exceptionOrNull() is PartnerNotConnectedException) {
                    R.string.swap_partners_partner_missing
                } else {
                    R.string.swap_partners_failed
                }
                _effects.send(CoupleEffect.Snackbar(UiText.Resource(messageRes)))
            }
        }
    }

    private fun onConfirmDisconnect() {
        val space = currentSpace
        viewModelScope.launch {
            _dialogState.value = DashboardDialogState()
            if (space != null) repository.disconnectCouple(space.id) else repository.disconnect()
            sessionStore.clearSession()
            widgetPublisher.publish(null)
        }
    }

    private fun onSignInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            googleAuth.signIn(activityContext)?.let { _effects.send(CoupleEffect.Toast(it, isLong = true)) }
        }
    }

    // endregion

    // region Memories

    private suspend fun compress(uri: Uri?): ByteArray? = uri?.let { localImageStore.compress(it) }

    /**
     * The slot a photo change of this user goes to, for the local-first copy and the upload badge (the repository
     * resolves it again when writing). Without a slot the change is dropped with a hint and only the text is saved.
     */
    private suspend fun ownSlotFor(change: MemoryPhotoChange): Pair<MemoryPhotoChange, PartnerRole?> {
        if (change == MemoryPhotoChange.Keep) return change to null
        val role = currentSpace?.editableRole(repository.currentUserProfile.value?.uid, storedRole())
        if (role == null) {
            _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.memory_photo_slot_unknown)))
            return MemoryPhotoChange.Keep to null
        }
        return change to role
    }

    /** Stores a local-first copy of a new own photo, so the moment shows it before the upload is done. */
    private fun Memory.withOwnPhoto(role: PartnerRole?, change: MemoryPhotoChange): Memory {
        if (role == null) return this
        val url = when (change) {
            MemoryPhotoChange.Keep -> return this
            MemoryPhotoChange.Remove -> null
            is MemoryPhotoChange.Replace ->
                localImageStore.saveMemoryImage(id, if (role == PartnerRole.PARTNER_1) "a" else "b", change.bytes) ?: return this
        }
        return if (role == PartnerRole.PARTNER_1) copy(partner1ImageUrl = url) else copy(partner2ImageUrl = url)
    }

    private suspend fun addMemory(title: String, date: LocalDate, note: String, ownImageBytes: ByteArray?, milestoneKey: String?) {
        val space = currentSpace ?: return
        val requested = ownImageBytes?.takeIf { it.isNotEmpty() }?.let { MemoryPhotoChange.Replace(it) } ?: MemoryPhotoChange.Keep
        val (change, role) = ownSlotFor(requested)
        val newMemory = Memory(id = UUID.randomUUID().toString(), title = title, date = date, note = note, milestoneKey = milestoneKey)
            .withOwnPhoto(role, change)
        toggleDialog<DashboardDialog.AddMemory>(null, show = false)
        val result = uploads.track(newMemory, role.takeIf { change is MemoryPhotoChange.Replace }) {
            repository.addMemory(space.id, newMemory, storedRole(), change)
        }
        reportSlotUnknown(result)
    }

    /** Saves title, date, note and milestone link of [memory]; photos only change through [ownPhoto], never the partner's slot. */
    private suspend fun updateMemory(memory: Memory, ownPhoto: MemoryPhotoChange) {
        val space = currentSpace ?: return
        val (change, role) = ownSlotFor(ownPhoto)
        val memoryId = memory.id.ifBlank { UUID.randomUUID().toString() }
        // Photos shown meanwhile come from the latest stored version, not from when the dialog opened
        val latest = uiState.value?.memories?.firstOrNull { it.id == memoryId } ?: memory
        val updatedMemory = latest.copy(
            id = memoryId,
            title = memory.title,
            date = memory.date,
            note = memory.note,
            milestoneKey = memory.milestoneKey
        ).withOwnPhoto(role, change)
        toggleDialog<DashboardDialog.EditMemory>(null, show = false)
        val result = uploads.track(updatedMemory, role.takeIf { change is MemoryPhotoChange.Replace }) {
            repository.updateMemory(space.id, updatedMemory, storedRole(), change)
        }
        reportSlotUnknown(result)
    }

    private suspend fun reportSlotUnknown(result: Result<Memory>) {
        if (result.exceptionOrNull() is PartnerSlotUnknownException) {
            _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.memory_photo_slot_unknown)))
        }
    }

    private fun onDeleteMemory(memoryId: String) {
        val space = currentSpace ?: return
        viewModelScope.launch {
            repository.deleteMemory(space.id, memoryId)
            toggleDialog<DashboardDialog.DeleteMemory>(null, show = false)
        }
    }

    /** Exports [imageUrls] to the gallery; a second request while one is running is ignored. */
    private fun onSaveToGallery(imageUrls: List<String>) {
        val urls = imageUrls.filter { it.isNotBlank() }.distinct()
        if (urls.isEmpty() || _dialogState.value.isSavingToGallery) return
        updateDialogs { it.copy(isSavingToGallery = true) }
        viewModelScope.launch {
            val allSaved = urls.map { localImageStore.saveToGallery(it) }.all { it.isSuccess }
            updateDialogs { it.copy(isSavingToGallery = false) }
            _effects.send(
                if (allSaved) CoupleEffect.SavedToGallery(urls.size) else CoupleEffect.Toast(UiText.Resource(R.string.photo_save_failed))
            )
        }
    }

    // endregion

    // region Counter & milestones

    private fun onCounterDisplayModeSelected(mode: CounterDisplayMode) {
        _counterPreferences.update { it.copy(displayMode = mode) }
        viewModelScope.launch { sessionStore.saveCounterDisplayMode(mode) }
    }

    private fun onMilestoneKindToggled(kind: MilestoneKind, enabled: Boolean) {
        val kinds = _counterPreferences.value.enabledMilestoneKinds.let { if (enabled) it + kind else it - kind }
        _counterPreferences.update { it.copy(enabledMilestoneKinds = kinds) }
        viewModelScope.launch { sessionStore.saveEnabledMilestoneKinds(kinds) }
    }

    private fun onAddCustomMilestone(title: String, date: LocalDate, recurringYearly: Boolean) {
        val space = currentSpace ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        setAddingCustomMilestone(false)
        viewModelScope.launch {
            repository.addCustomMilestone(space.id, CustomMilestone(title = trimmed, date = date, recurringYearly = recurringYearly))
        }
    }

    private fun onCustomMilestoneEnabledChanged(milestoneId: String, enabled: Boolean) {
        val space = currentSpace ?: return
        viewModelScope.launch { repository.setCustomMilestoneEnabled(space.id, milestoneId, enabled) }
    }

    // endregion
}
