package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.data.location.LocationProvider
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
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionRules
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionStatus
import com.aistudio.couplebubble.qxztrw.domain.model.source
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.hasBoundMembers
import com.aistudio.couplebubble.qxztrw.domain.model.isPartner1
import com.aistudio.couplebubble.qxztrw.domain.model.momentMilestones
import com.aistudio.couplebubble.qxztrw.domain.model.withPhoto
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
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
import com.aistudio.couplebubble.qxztrw.ui.screens.home.ReunionUiState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.SnackbarAction
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
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

/** The "Uns" tab of a paired couple: counter, reunion, milestones, moments, partner names, photos and colors. */
class DashboardViewModel(
    private val repository: CoupleRepository,
    private val sessionStore: SessionStore,
    private val localImageStore: LocalImageStore,
    private val googleAuth: GoogleAuthController,
    private val widgetPublisher: WidgetPublisher,
    spaceContent: SpaceContent,
    private val reunionRepository: ReunionRepository,
    private val locationProvider: LocationProvider,
    private val appScope: CoroutineScope,
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

    /** The reunion of the active space with the space id it belongs to; `null` while nothing is planned. */
    private val reunionFlow: StateFlow<Pair<String, Reunion>?> = spaceContent.reunion.stateIn(viewModelScope, started, null)

    private var isRefreshingLocation = false

    /** A moment the user deleted whose undo snackbar is still showing. */
    private data class PendingDeletion(val spaceId: String, val memoryId: String)

    private var pendingDeletion: PendingDeletion? = null

    /** Moments hidden from the timeline: waiting for the undo snackbar or being deleted in Firestore. */
    private val _hiddenMemoryIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            sessionStore.counterPreferencesFlow.collect { _counterPreferences.value = it }
        }
        // A reunion that is over is removed, together with both stored places
        viewModelScope.launch {
            reunionFlow.collect { entry ->
                val (spaceId, reunion) = entry ?: return@collect
                if (reunion.status(LocalDate.now(clock)) == ReunionStatus.Over) reunionRepository.deleteReunion(spaceId)
            }
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
        combine(spaceContent.memories, uploads.pendingMemories, _hiddenMemoryIds) { remote, pending, hidden ->
            uploads.visibleMemories(remote, pending).filterNot { it.id in hidden }
        },
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
        contentFlow,
        reunionFlow
    ) { session, dialogs, auth, content, reunionEntry ->
        val space = session.space?.takeIf { it.isActive } ?: return@combine null
        val uid = session.userProfile?.uid
        val calendar = RelationshipCalendar.of(space, content.counterPreferences, content.customMilestones, content.memories.momentMilestones())
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
            reunion = reunionEntry?.takeIf { it.first == space.id }?.second?.let { reunionUiState(it, uid, today) }
        )
    }.stateIn(viewModelScope, started, null)

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.SelectMainTab -> updateDialogs { it.copy(selectedTab = event.tab, isMenuExpanded = false) }
            is DashboardEvent.SetMenuExpanded -> updateDialogs { it.copy(isMenuExpanded = event.expanded) }
            is DashboardEvent.ShowDisconnectDialog -> toggleDialog<DashboardDialog.Disconnect>(DashboardDialog.Disconnect, event.show)
            DashboardEvent.ConfirmDisconnect -> onConfirmDisconnect()
            is DashboardEvent.ShowEditNamesDialog -> toggleDialog<DashboardDialog.EditNames>(DashboardDialog.EditNames, event.show)
            is DashboardEvent.SavePartnerDetails -> onSavePartnerDetails(event)
            DashboardEvent.SwapPartnerRoles -> swapPartnerRoles()
            is DashboardEvent.UploadProfilePhoto -> onUploadProfilePhoto(event.bytes)
            is DashboardEvent.ShowAddMemoryDialog -> toggleDialog<DashboardDialog.AddMemory>(DashboardDialog.AddMemory, event.show)
            is DashboardEvent.AddMemory -> viewModelScope.launch {
                addMemory(event.title, event.date, event.note, compress(event.ownImageUri), event.countsAsMilestone)
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
            is DashboardEvent.DeleteMemory -> onDeleteMemory(event.memoryId)
            is DashboardEvent.UndoDeleteMemory -> onUndoDeleteMemory(event.memoryId)
            is DashboardEvent.CommitDeleteMemory -> pendingDeletion?.takeIf { it.memoryId == event.memoryId }?.let(::commitDeletion)
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
            is DashboardEvent.ShowDeleteCustomMilestoneDialog ->
                updateDialogs { it.copy(dialog = DashboardDialog.MilestoneSettings(deletingCustom = event.milestone)) }
            is DashboardEvent.DeleteCustomMilestone -> onDeleteCustomMilestone(event.milestoneId)
            is DashboardEvent.SetMomentMilestoneEnabled -> onMomentMilestoneEnabledChanged(event.memoryId, event.enabled)
            is DashboardEvent.ShowReunionDialog -> toggleDialog<DashboardDialog.Reunion>(DashboardDialog.Reunion, event.show)
            is DashboardEvent.SaveReunion -> onSaveReunion(event.startDate, event.endDate, event.shareLocation)
            DashboardEvent.DeleteReunion -> onDeleteReunion()
            DashboardEvent.RefreshReunionLocation -> refreshReunionLocation()
        }
    }

    private fun reunionUiState(reunion: Reunion, uid: String?, today: LocalDate): ReunionUiState? {
        val status = reunion.status(today)
        if (status == ReunionStatus.Over) return null
        val now = clock.millis()
        val own = reunion.placeOf(uid)
        val partner = reunion.partnerPlaceOf(uid)
        return ReunionUiState(
            reunion = reunion,
            status = status,
            distanceKm = reunion.distanceKm(uid, now),
            ownPlace = own,
            partnerPlace = partner,
            ownSource = own.source(now),
            partnerSource = partner.source(now),
            nowMillis = now
        )
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
            val result = repository.updateSpaceDetails(
                coupleId = spaceId,
                partner1Name = trimmed1,
                partner2Name = trimmed2,
                anniversary = date
            )
            // On failure the dialog stays open, so the same form can simply be sent again
            if (result.isSuccess) sessionStore.saveActiveCoupleId(spaceId) else showSnackbar(R.string.space_setup_save_failed)
            updateDialogs { it.copy(isSavingSpaceSetup = false) }
        }
    }

    /** Writes names and the changed colors; the confirmation only comes once every write went through. */
    private fun onSavePartnerDetails(event: DashboardEvent.SavePartnerDetails) {
        val space = currentSpace ?: return
        toggleDialog<DashboardDialog.EditNames>(null, show = false)
        viewModelScope.launch {
            val results = buildList {
                add(repository.updatePartnerNames(space.id, event.partner1Name, event.partner2Name))
                if (event.partner1ColorHex != space.partner1ColorHex) {
                    add(repository.updatePartnerColor(space.id, isPartner1 = true, colorHex = event.partner1ColorHex))
                }
                if (event.partner2ColorHex != space.partner2ColorHex) {
                    add(repository.updatePartnerColor(space.id, isPartner1 = false, colorHex = event.partner2ColorHex))
                }
            }
            showSnackbar(if (results.all { it.isSuccess }) R.string.names_and_colors_updated else R.string.names_save_failed)
        }
    }

    private suspend fun showSnackbar(@StringRes messageRes: Int) {
        _effects.send(CoupleEffect.Snackbar(UiText.Resource(messageRes)))
    }

    /** Shows [messageRes] when [result] failed; returns whether it succeeded. */
    private suspend fun reportFailure(result: Result<*>, @StringRes messageRes: Int): Boolean {
        if (result.isFailure) showSnackbar(messageRes)
        return result.isSuccess
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
                updateDialogs { it.copy(completedRoleSwaps = it.completedRoleSwaps + 1) }
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
            // Leaving the space takes the own reunion place with it
            val uid = repository.currentUserProfile.value?.uid
            if (space != null && uid != null) reunionRepository.removeOwnPlace(space.id, uid)
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
                localImageStore.saveMemoryImage(id, role.fileSlot, change.bytes) ?: return this
        }
        return withPhoto(role, url)
    }

    private suspend fun addMemory(title: String, date: LocalDate, note: String, ownImageBytes: ByteArray?, countsAsMilestone: Boolean) {
        val space = currentSpace ?: return
        val requested = ownImageBytes?.takeIf { it.isNotEmpty() }?.let { MemoryPhotoChange.Replace(it) } ?: MemoryPhotoChange.Keep
        val (change, role) = ownSlotFor(requested)
        val newMemory = Memory(id = UUID.randomUUID().toString(), title = title, date = date, note = note, countsAsMilestone = countsAsMilestone)
            .withOwnPhoto(role, change)
        toggleDialog<DashboardDialog.AddMemory>(null, show = false)
        val result = uploads.track(newMemory, role.takeIf { change is MemoryPhotoChange.Replace }) {
            repository.addMemory(space.id, newMemory, storedRole(), change)
        }
        reportMemorySaved(result, R.string.memory_added)
    }

    /**
     * Saves title, date and note of [memory]; photos only change through [ownPhoto], never the partner's slot.
     * The milestone switch is written on its own and only when it differs from the stored moment.
     */
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
            countsAsMilestone = memory.countsAsMilestone
        ).withOwnPhoto(role, change)
        toggleDialog<DashboardDialog.EditMemory>(null, show = false)
        if (memory.countsAsMilestone != latest.countsAsMilestone) {
            onMomentMilestoneEnabledChanged(memoryId, memory.countsAsMilestone)
        }
        val result = uploads.track(updatedMemory, role.takeIf { change is MemoryPhotoChange.Replace }) {
            repository.updateMemory(space.id, updatedMemory, storedRole(), change)
        }
        reportMemorySaved(result, R.string.memory_updated)
    }

    /** The confirmation (with its haptic) only comes once the repository saved the moment. */
    private suspend fun reportMemorySaved(result: Result<Memory>, @StringRes successRes: Int) {
        _effects.send(
            when {
                result.isSuccess -> CoupleEffect.MemorySaved(UiText.Resource(successRes))
                result.exceptionOrNull() is PartnerSlotUnknownException ->
                    CoupleEffect.Snackbar(UiText.Resource(R.string.memory_photo_slot_unknown))
                else -> CoupleEffect.Snackbar(UiText.Resource(R.string.memory_save_failed))
            }
        )
    }

    /** Hides the moment and offers undo; a deletion still waiting for its snackbar is carried out first. */
    private fun onDeleteMemory(memoryId: String) {
        val space = currentSpace ?: return
        pendingDeletion?.let(::commitDeletion)
        pendingDeletion = PendingDeletion(space.id, memoryId)
        _hiddenMemoryIds.update { it + memoryId }
        _effects.trySend(
            CoupleEffect.Snackbar(
                text = UiText.Resource(R.string.memory_deleted),
                action = SnackbarAction(
                    label = UiText.Resource(R.string.memory_delete_undo),
                    onActionPerformed = DashboardEvent.UndoDeleteMemory(memoryId),
                    onDismissed = DashboardEvent.CommitDeleteMemory(memoryId)
                )
            )
        )
    }

    private fun onUndoDeleteMemory(memoryId: String) {
        if (pendingDeletion?.memoryId != memoryId) return
        pendingDeletion = null
        _hiddenMemoryIds.update { it - memoryId }
    }

    /** Deletes in the app scope so the write survives the ViewModel; on failure the moment shows up again. */
    private fun commitDeletion(deletion: PendingDeletion) {
        if (pendingDeletion == deletion) pendingDeletion = null
        appScope.launch {
            val result = repository.deleteMemory(deletion.spaceId, deletion.memoryId)
            _hiddenMemoryIds.update { it - deletion.memoryId }
            if (result.isFailure) _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.memory_delete_failed)))
        }
    }

    override fun onCleared() {
        pendingDeletion?.let(::commitDeletion)
        super.onCleared()
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
            val milestone = CustomMilestone(title = trimmed, date = date, recurringYearly = recurringYearly)
            reportFailure(repository.addCustomMilestone(space.id, milestone), R.string.milestone_save_failed)
        }
    }

    private fun onCustomMilestoneEnabledChanged(milestoneId: String, enabled: Boolean) {
        val space = currentSpace ?: return
        viewModelScope.launch {
            reportFailure(repository.setCustomMilestoneEnabled(space.id, milestoneId, enabled), R.string.milestone_save_failed)
        }
    }

    private fun onDeleteCustomMilestone(milestoneId: String) {
        val space = currentSpace ?: return
        updateDialogs { it.copy(dialog = DashboardDialog.MilestoneSettings()) }
        viewModelScope.launch {
            reportFailure(repository.deleteCustomMilestone(space.id, milestoneId), R.string.milestone_delete_failed)
        }
    }

    private fun onMomentMilestoneEnabledChanged(memoryId: String, enabled: Boolean) {
        val space = currentSpace ?: return
        viewModelScope.launch {
            reportFailure(repository.setMemoryCountsAsMilestone(space.id, memoryId, enabled), R.string.milestone_save_failed)
        }
    }

    // endregion

    // region Reunion

    private fun onSaveReunion(startDate: LocalDate, endDate: LocalDate, shareLocation: Boolean) {
        val space = currentSpace ?: return
        val uid = repository.currentUserProfile.value?.uid
        if (endDate.isBefore(startDate)) {
            _effects.trySend(CoupleEffect.Snackbar(UiText.Resource(R.string.reunion_end_before_start)))
            return
        }
        val existing = reunionFlow.value?.takeIf { it.first == space.id }?.second
        // The waiting progress keeps running from the first planning when only the dates move
        val plannedAt = existing?.plannedAtDate ?: LocalDate.now(clock)
        toggleDialog<DashboardDialog.Reunion>(null, show = false)
        viewModelScope.launch {
            val datesSaved = reportFailure(
                reunionRepository.saveReunionDates(space.id, startDate, endDate, plannedAt),
                R.string.reunion_save_failed
            )
            if (!datesSaved) return@launch
            if (uid == null) return@launch
            val wantsLocation = shareLocation && locationProvider.hasPermission()
            val location = if (wantsLocation) locationProvider.currentApproximateLocation()?.rounded() else null
            if (wantsLocation && location == null) {
                _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.reunion_location_unavailable)))
            }
            // Sharing stays on without a fix, so the next resume of the dashboard tries again
            val place = ReunionPlace(
                sharesLocation = wantsLocation,
                location = location,
                locatedAtMillis = location?.let { clock.millis() }
            )
            val placeResult = if (place.isEmpty) {
                reunionRepository.removeOwnPlace(space.id, uid)
            } else {
                reunionRepository.saveOwnPlace(space.id, uid, place)
            }
            reportFailure(placeResult, R.string.reunion_place_save_failed)
        }
    }

    private fun onDeleteReunion() {
        val space = currentSpace ?: return
        toggleDialog<DashboardDialog.Reunion>(null, show = false)
        viewModelScope.launch {
            reportFailure(reunionRepository.deleteReunion(space.id), R.string.reunion_delete_failed)
        }
    }

    /** Asks for a new approximate fix only while the app is open, sharing is on and the stored one is older than an hour. */
    private fun refreshReunionLocation() {
        val (spaceId, reunion) = reunionFlow.value ?: return
        if (spaceId != currentSpace?.id || isRefreshingLocation) return
        val uid = repository.currentUserProfile.value?.uid ?: return
        val own = reunion.placeOf(uid)?.takeIf { it.sharesLocation } ?: return
        if (reunion.status(LocalDate.now(clock)) == ReunionStatus.Over || !locationProvider.hasPermission()) return
        val locatedAt = own.locatedAtMillis
        if (locatedAt != null && clock.millis() - locatedAt < ReunionRules.LOCATION_REFRESH_MILLIS) return
        isRefreshingLocation = true
        viewModelScope.launch {
            try {
                val location = locationProvider.currentApproximateLocation()?.rounded() ?: return@launch
                // A background refresh stays silent: the next resume simply tries again
                reunionRepository.saveOwnPlace(spaceId, uid, own.copy(location = location, locatedAtMillis = clock.millis()))
            } finally {
                isRefreshingLocation = false
            }
        }
    }

    // endregion
}
