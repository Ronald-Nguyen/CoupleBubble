package com.aistudio.couplebubble.qxztrw.ui

import android.content.Context
import android.net.Uri
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.auth.GoogleSignInClient
import com.aistudio.couplebubble.qxztrw.data.CoupleSessionPreferences
import com.aistudio.couplebubble.qxztrw.data.LocalImageStore
import com.aistudio.couplebubble.qxztrw.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.model.isPartner1
import com.aistudio.couplebubble.qxztrw.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.repository.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.repository.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.repository.SpaceFullException
import com.aistudio.couplebubble.qxztrw.repository.SpaceUnavailableException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CoupleViewModel(
    private val repository: CoupleRepository,
    private val preferences: CoupleSessionPreferences? = null,
    private val localImageStore: LocalImageStore? = null,
    private val googleSignInClient: GoogleSignInClient? = null,
    private val started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _pairingState = MutableStateFlow(PairingUiState())
    private val _dialogState = MutableStateFlow(DashboardDialogState())
    private val _googleAuthState = MutableStateFlow(GoogleAuthState())
    private val _isSessionRestored = MutableStateFlow(preferences == null)

    // Local-first copies of memories whose photos are still uploading, shown in place of the remote version
    private val _pendingMemories = MutableStateFlow<Map<String, Memory>>(emptyMap())
    private val _photoSyncStates = MutableStateFlow<Map<PhotoSlotKey, PhotoSyncState>>(emptyMap())
    private val _counterPreferences = MutableStateFlow(CounterPreferences())

    private val _effects = Channel<CoupleEffect>(Channel.BUFFERED)

    /** One-shot toasts, snackbars and haptic confirmations; collected once by the activity. */
    val effects: Flow<CoupleEffect> = _effects.receiveAsFlow()

    // Shared with NotesViewModel, which lives in the same ViewModelStore
    val currentSpace: StateFlow<CoupleSpace?> = repository.currentSpace
    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile

    private var countdownJob: Job? = null
    private var copyFeedbackJob: Job? = null

    init {
        initSessionCheck()
    }

    /** Creates a fresh random code and listens on its space; every unpaired device gets its own. */
    private suspend fun refreshPairingCode() {
        val newCode = repository.generateNewPairingCode()
        _pairingState.update {
            it.copy(
                generatedCode = newCode.code,
                countdownSeconds = newCode.totalValidSeconds,
                errorMessage = null
            )
        }
        startCountdownTimer()
    }

    private fun initSessionCheck() {
        viewModelScope.launch {
            val uid = repository.ensureSignedIn()
            if (uid != null) {
                val space = repository.restoreSessionForUser(uid).getOrNull()
                if (space != null) {
                    preferences?.saveActiveCoupleId(space.id)
                    _isSessionRestored.value = true
                    return@launch
                }
            }

            if (preferences != null) {
                val activeCoupleId = preferences.activeCoupleIdFlow.firstOrNull()
                if (!activeCoupleId.isNullOrBlank()) {
                    val result = repository.restoreSession(activeCoupleId)
                    if (result.exceptionOrNull() is SpaceUnavailableException) {
                        // Offline without cache: keep the session and the loading screen until the listener delivers
                        withTimeoutOrNull(SPACE_LOAD_TIMEOUT_MS) {
                            repository.currentSpace.first { it != null }
                        }
                    } else if (result.isFailure) {
                        preferences.clearSession()
                    }
                }
            }
            _isSessionRestored.value = true
            if (repository.currentSpace.value == null) refreshPairingCode()
        }

        viewModelScope.launch {
            repository.currentSpace.collect { space ->
                if (space != null) {
                    if (space.isActive) {
                        preferences?.saveActiveCoupleId(space.id)
                    } else {
                        preferences?.clearSession()
                    }
                }
            }
        }

        if (preferences != null) {
            viewModelScope.launch {
                preferences.counterPreferencesFlow.collect { _counterPreferences.value = it }
            }
        }

        viewModelScope.launch {
            repository.currentUserProfile.collect { profile ->
                _pairingState.update { it.copy(userProfile = profile) }
            }
        }
    }

    private val memoriesFlow = repository.currentSpace.flatMapLatest { space ->
        if (space != null) repository.getMemories(space.id) else flowOf(emptyList())
    }

    private val visibleMemoriesFlow = combine(memoriesFlow, _pendingMemories) { remote, pending ->
        if (pending.isEmpty()) {
            remote
        } else {
            (remote.filterNot { it.id in pending } + pending.values).sortedByDescending { it.date }
        }
    }

    private val customMilestonesFlow = repository.currentSpace.flatMapLatest { space ->
        if (space != null) repository.getCustomMilestones(space.id) else flowOf(emptyList())
    }

    private val partnerRoleFlow: Flow<PartnerRole> = preferences?.partnerRoleFlow
        ?.map { PartnerRole.displayFallback(it) }
        ?.onStart { emit(PartnerRole.PARTNER_1) }
        ?: flowOf(PartnerRole.PARTNER_1)

    private data class Session(
        val space: CoupleSpace?,
        val userProfile: UserProfile?,
        val role: PartnerRole,
        val isRestored: Boolean
    )

    private data class DashboardContent(
        val memories: List<Memory>,
        val photoSyncStates: Map<PhotoSlotKey, PhotoSyncState>,
        val counterPreferences: CounterPreferences,
        val customMilestones: List<CustomMilestone>
    )

    private val sessionFlow = combine(
        repository.currentSpace,
        repository.currentUserProfile,
        partnerRoleFlow,
        _isSessionRestored,
        ::Session
    )

    private val dashboardContentFlow = combine(
        visibleMemoriesFlow,
        _photoSyncStates,
        _counterPreferences,
        customMilestonesFlow,
        ::DashboardContent
    )

    val uiState: StateFlow<CoupleMainState> = combine(
        sessionFlow,
        _pairingState,
        _dialogState,
        _googleAuthState,
        dashboardContentFlow
    ) { session, pairing, dialogs, googleAuth, content ->
        val space = session.space
        if (!session.isRestored && space == null) {
            CoupleMainState.Loading
        } else if (space != null && space.isActive) {
            val metrics = RelationshipDateCalculator.calculate(
                year = space.anniversaryYear,
                month = space.anniversaryMonth,
                day = space.anniversaryDay,
                enabledKinds = content.counterPreferences.enabledMilestoneKinds,
                customMilestones = content.customMilestones
            )
            CoupleMainState.Paired(
                DashboardUiState(
                    space = space,
                    metrics = metrics,
                    memories = content.memories,
                    userProfile = session.userProfile,
                    isCurrentUserPartner1 = space.isPartner1(session.userProfile?.uid, session.role),
                    dialogs = dialogs,
                    googleAuth = googleAuth,
                    photoSyncStates = content.photoSyncStates,
                    counterPreferences = content.counterPreferences,
                    customMilestones = content.customMilestones
                )
            )
        } else {
            CoupleMainState.Unpaired(pairing.copy(googleAuth = googleAuth))
        }
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = if (preferences == null) CoupleMainState.Unpaired(_pairingState.value) else CoupleMainState.Loading
    )

    fun onPairingEvent(event: PairingEvent) {
        when (event) {
            is PairingEvent.SelectTab -> onTabSelected(event.tab)
            is PairingEvent.EnteredCodeChanged -> onEnteredCodeChanged(event.rawInput)
            PairingEvent.CodeCopied -> onCopyCodeSuccess()
            PairingEvent.GenerateNewCode -> onGenerateNewCode()
            PairingEvent.Connect -> onConnectClicked()
            PairingEvent.OpenDemoSpace -> onOpenDemoSpace()
            is PairingEvent.SaveSpaceSetup -> onSaveSpaceSetup(event.partner1Name, event.partner2Name, event.date)
            PairingEvent.DismissSetupDialog -> setShowSetupSpaceDialog(false)
            PairingEvent.RecheckCodeAfterSpaceFull -> onRecheckCodeAfterSpaceFull()
            PairingEvent.CreateOwnSpaceAfterSpaceFull -> onCreateOwnSpaceAfterSpaceFull()
            is PairingEvent.SignInWithGoogle -> onSignInWithGoogleClicked(event.activityContext)
            PairingEvent.SignOutGoogle -> onSignOutGoogle()
        }
    }

    fun onDashboardEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.SetMenuExpanded -> setMenuExpanded(event.expanded)
            is DashboardEvent.ShowDisconnectDialog -> setShowDisconnectDialog(event.show)
            DashboardEvent.ConfirmDisconnect -> onConfirmDisconnect()
            is DashboardEvent.ShowEditNamesDialog -> setShowEditNamesDialog(event.show)
            is DashboardEvent.UpdatePartnerNames -> onUpdatePartnerNames(event.partner1Name, event.partner2Name)
            is DashboardEvent.UpdatePartnerColor -> onUpdatePartnerColor(event.colorHex, event.isPartner1)
            DashboardEvent.SwapPartnerRoles -> swapPartnerRoles()
            is DashboardEvent.UploadProfilePhoto -> onUploadProfilePhoto(event.isPartner1, event.bytes)
            is DashboardEvent.ShowAddMemoryDialog -> setShowAddMemoryDialog(event.show)
            is DashboardEvent.AddMemory -> onAddMemoryFromUris(event.title, event.date, event.note, event.image1Uri, event.image2Uri)
            is DashboardEvent.ShowEditMemoryDialog -> setMemoryToEdit(event.memory)
            is DashboardEvent.UpdateMemory -> onUpdateMemoryFromUris(event.memory, event.image1Uri, event.image2Uri)
            is DashboardEvent.ShowDeleteMemoryDialog -> setMemoryToDelete(event.memory)
            is DashboardEvent.DeleteMemory -> onDeleteMemory(event.memoryId)
            is DashboardEvent.SaveToGallery -> onSaveToGallery(event.imageUrls)
            is DashboardEvent.ShowGoogleBackupDialog -> setShowGoogleBackupDialog(event.show)
            is DashboardEvent.SignInWithGoogle -> onSignInWithGoogleClicked(event.activityContext)
            DashboardEvent.SignOutGoogle -> onSignOutGoogle()
            is DashboardEvent.SaveSpaceSetup -> onSaveSpaceSetup(event.partner1Name, event.partner2Name, event.date)
            is DashboardEvent.SelectCounterDisplayMode -> onCounterDisplayModeSelected(event.mode)
            is DashboardEvent.ShowMilestoneSettingsDialog -> setShowMilestoneSettingsDialog(event.show)
            is DashboardEvent.ShowAddCustomMilestoneDialog -> setShowAddCustomMilestoneDialog(event.show)
            is DashboardEvent.ToggleMilestoneKind -> onMilestoneKindToggled(event.kind, event.enabled)
            is DashboardEvent.AddCustomMilestone -> onAddCustomMilestone(event.title, event.date)
            is DashboardEvent.DeleteCustomMilestone -> onDeleteCustomMilestone(event.milestoneId)
        }
    }

    // region Pairing

    fun onTabSelected(tab: PairingTab) {
        _pairingState.update { it.copy(selectedTab = tab, errorMessage = null) }
    }

    fun onEnteredCodeChanged(rawInput: String) {
        val cleaned = rawInput.filter { it in '0'..'9' }.take(6)
        _pairingState.update { it.copy(enteredCode = cleaned, errorMessage = null) }
    }

    fun onGenerateNewCode() {
        viewModelScope.launch {
            preferences?.savePartnerRole(PartnerRole.PARTNER_1)
            refreshPairingCode()
        }
    }

    fun onCopyCodeSuccess() {
        _pairingState.update { it.copy(isCopied = true) }
        copyFeedbackJob?.cancel()
        copyFeedbackJob = viewModelScope.launch {
            delay(COPY_FEEDBACK_MILLIS)
            _pairingState.update { it.copy(isCopied = false) }
        }
    }

    private fun connectErrorText(error: Throwable): UiText = when (error) {
        is InvalidPairingCodeException -> UiText.Resource(R.string.pairing_code_invalid)
        is SpaceUnavailableException -> UiText.Resource(R.string.space_unavailable)
        else -> error.message?.let { UiText.Raw(it) } ?: UiText.Resource(R.string.pairing_connection_failed)
    }

    fun onConnectClicked() {
        val codeToTest = _pairingState.value.enteredCode
        if (codeToTest.length != 6) {
            _pairingState.update { it.copy(errorMessage = UiText.Resource(R.string.pairing_code_incomplete)) }
            return
        }

        viewModelScope.launch {
            _pairingState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.connectWithCode(codeToTest)
            val error = result.exceptionOrNull()
            when {
                error is SpaceFullException -> _pairingState.update {
                    it.copy(isLoading = false, showSpaceFullDialog = true)
                }
                error != null -> _pairingState.update {
                    it.copy(isLoading = false, errorMessage = connectErrorText(error))
                }
                else -> {
                    val space = result.getOrNull() ?: return@launch
                    preferences?.saveActiveCoupleId(space.id)
                    preferences?.savePartnerRole(PartnerRole.PARTNER_2)
                    _pairingState.update {
                        if (!space.isSetupComplete) {
                            it.copy(isLoading = false, showSetupSpaceDialog = true, pendingSpaceId = space.id)
                        } else {
                            it.copy(isLoading = false, errorMessage = null)
                        }
                    }
                }
            }
        }
    }

    fun onRecheckCodeAfterSpaceFull() {
        _pairingState.update { it.copy(showSpaceFullDialog = false, enteredCode = "", errorMessage = null) }
    }

    fun onCreateOwnSpaceAfterSpaceFull() {
        _pairingState.update {
            it.copy(
                showSpaceFullDialog = false,
                enteredCode = "",
                selectedTab = PairingTab.CREATE,
                errorMessage = null
            )
        }
        onGenerateNewCode()
    }

    fun onOpenDemoSpace() {
        viewModelScope.launch {
            _pairingState.update { it.copy(isLoading = true) }
            preferences?.savePartnerRole(PartnerRole.PARTNER_1)
            val demoSpace = repository.openDemoSpace()
            preferences?.saveActiveCoupleId(demoSpace.id)
            _isSessionRestored.value = true
            _pairingState.update { it.copy(isLoading = false) }
        }
    }

    fun setShowSetupSpaceDialog(show: Boolean) {
        _pairingState.update { it.copy(showSetupSpaceDialog = show) }
        _dialogState.update { it.copy(showSetupSpaceDialog = show) }
    }

    fun onSaveSpaceSetup(partner1Name: String, partner2Name: String, date: LocalDate) {
        val trimmed1 = partner1Name.trim()
        val trimmed2 = partner2Name.trim()
        if (trimmed1.length < 2 || trimmed2.length < 2) return

        val currentSpaceId = repository.currentSpace.value?.id ?: _pairingState.value.pendingSpaceId ?: return

        viewModelScope.launch {
            repository.updateSpaceDetails(
                coupleId = currentSpaceId,
                partner1Name = trimmed1,
                partner2Name = trimmed2,
                anniversaryYear = date.year,
                anniversaryMonth = date.monthValue,
                anniversaryDay = date.dayOfMonth
            )
            preferences?.saveActiveCoupleId(currentSpaceId)
            _pairingState.update { it.copy(showSetupSpaceDialog = false, pendingSpaceId = null) }
            _dialogState.update { it.copy(showSetupSpaceDialog = false) }
        }
    }

    // endregion

    // region Google sign-in

    private fun isCancellationException(throwable: Throwable?): Boolean =
        throwable is GetCredentialCancellationException || throwable?.cause is GetCredentialCancellationException

    fun onSignInWithGoogleClicked(activityContext: Context) {
        val client = googleSignInClient ?: return
        viewModelScope.launch {
            _googleAuthState.value = GoogleAuthState(isLoading = true)

            val failure = try {
                val previousUid = repository.currentAuthUid
                val result = client.signIn(activityContext)
                result.getOrNull()?.let { firebaseUser ->
                    val profile = repository.signInWithGoogleUser(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        previousUid = previousUid
                    ).getOrNull()
                    profile?.coupleId?.takeIf { it.isNotBlank() }?.let { preferences?.saveActiveCoupleId(it) }
                }
                if (result.isSuccess) null else result.exceptionOrNull()
            } catch (e: Exception) {
                e
            }

            if (failure == null || isCancellationException(failure)) {
                _googleAuthState.update { it.copy(isLoading = false) }
            } else {
                val errorText = googleAuthErrorText(failure)
                _googleAuthState.value = GoogleAuthState(isLoading = false, error = errorText)
                _effects.send(CoupleEffect.Toast(errorText, isLong = true))
            }
        }
    }

    private fun googleAuthErrorText(throwable: Throwable?): UiText {
        if (throwable == null) return UiText.Resource(R.string.google_sign_in_failed)
        val msg = throwable.message ?: throwable.localizedMessage ?: ""
        return when {
            throwable is NoCredentialException -> UiText.Resource(R.string.google_error_no_credentials)
            throwable is GetCredentialException &&
                (msg.contains("[10]") || msg.contains("[16]") || msg.contains("[28444]") || msg.contains("DEVELOPER_ERROR")) ->
                UiText.Resource(R.string.google_error_sha1_missing)
            throwable is FirebaseAuthException &&
                (throwable.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || msg.contains("CONFIGURATION_NOT_FOUND")) ->
                UiText.Resource(R.string.google_error_provider_disabled)
            throwable is FirebaseNetworkException -> UiText.Resource(R.string.google_error_network)
            msg.isNotBlank() -> UiText.Resource(R.string.google_sign_in_error_prefix, listOf(msg))
            else -> UiText.Resource(R.string.google_sign_in_failed)
        }
    }

    fun clearGoogleAuthError() {
        _googleAuthState.update { it.copy(error = null) }
    }

    fun onSignOutGoogle() {
        viewModelScope.launch {
            try {
                googleSignInClient?.signOut()
            } catch (_: Exception) {
                // Ignore
            }
            repository.signOutUser()
        }
    }

    // endregion

    // region Dashboard dialogs

    fun onMainTabSelected(tab: MainTab) {
        _dialogState.update { it.copy(selectedTab = tab, isMenuExpanded = false) }
    }

    fun setMenuExpanded(expanded: Boolean) {
        _dialogState.update { it.copy(isMenuExpanded = expanded) }
    }

    fun setShowGoogleBackupDialog(show: Boolean) {
        _dialogState.update { it.copy(isMenuExpanded = false, showGoogleBackupDialog = show) }
    }

    fun setShowDisconnectDialog(show: Boolean) {
        _dialogState.update { it.copy(isMenuExpanded = false, showDisconnectDialog = show) }
    }

    fun setShowEditNamesDialog(show: Boolean) {
        _dialogState.update { it.copy(isMenuExpanded = false, showEditNamesDialog = show) }
    }

    fun setShowAddMemoryDialog(show: Boolean) {
        _dialogState.update { it.copy(showAddMemoryDialog = show) }
    }

    fun setMemoryToEdit(memory: Memory?) {
        _dialogState.update { it.copy(memoryToEdit = memory) }
    }

    fun setMemoryToDelete(memory: Memory?) {
        _dialogState.update { it.copy(memoryToDelete = memory) }
    }

    fun setShowMilestoneSettingsDialog(show: Boolean) {
        _dialogState.update { it.copy(isMenuExpanded = false, showMilestoneSettingsDialog = show) }
    }

    fun setShowAddCustomMilestoneDialog(show: Boolean) {
        _dialogState.update { it.copy(showAddCustomMilestoneDialog = show) }
    }

    // endregion

    // region Space & partners

    fun onUpdatePartnerNames(partner1Name: String, partner2Name: String) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.updatePartnerNames(current.id, partner1Name, partner2Name)
            _dialogState.update { it.copy(showEditNamesDialog = false) }
        }
    }

    fun onUpdatePartnerColor(colorHex: String, isPartner1: Boolean) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.updatePartnerColor(current.id, isPartner1, colorHex)
        }
    }

    fun onUploadProfilePhoto(isPartner1: Boolean, imageBytes: ByteArray) {
        val current = repository.currentSpace.value ?: return
        _dialogState.update { it.copy(isUploadingProfilePhoto = true) }
        viewModelScope.launch {
            val succeeded = try {
                repository.uploadProfilePhoto(current.id, isPartner1, imageBytes).isSuccess
            } catch (_: Exception) {
                false
            } finally {
                _dialogState.update { it.copy(isUploadingProfilePhoto = false) }
            }
            _effects.send(
                if (succeeded) {
                    CoupleEffect.Toast(UiText.Resource(R.string.profile_photo_saved))
                } else {
                    CoupleEffect.Toast(UiText.Resource(R.string.profile_photo_upload_failed), isLong = true)
                }
            )
        }
    }

    fun swapPartnerRoles() {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val result = repository.swapPartners(current.id)
            if (result.isSuccess) {
                preferences?.let { prefs ->
                    PartnerRole.fromStored(prefs.partnerRoleFlow.firstOrNull())?.let { role ->
                        prefs.savePartnerRole(role.swapped())
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

    fun onConfirmDisconnect() {
        val current = repository.currentSpace.value
        viewModelScope.launch {
            _dialogState.value = DashboardDialogState()
            if (current != null) {
                repository.disconnectCouple(current.id)
            } else {
                repository.disconnect()
            }
            preferences?.clearSession()
            _pairingState.update { it.copy(enteredCode = "", isLoading = false, errorMessage = null) }
            // The old code points at the space just left
            refreshPairingCode()
        }
    }

    // endregion

    // region Memories

    /** Stores a local-first copy of every slot that gets new bytes; slots without new bytes keep [current]. */
    private fun saveLocalCopies(
        memoryId: String,
        image1Bytes: ByteArray?,
        image2Bytes: ByteArray?,
        current: Pair<String?, String?>
    ): Pair<String?, String?> {
        val store = localImageStore ?: return current
        fun save(slot: String, bytes: ByteArray?, fallback: String?): String? =
            bytes?.takeIf { it.isNotEmpty() }?.let { store.saveMemoryImage(memoryId, slot, it) } ?: fallback
        return save("a", image1Bytes, current.first) to save("b", image2Bytes, current.second)
    }

    private suspend fun compress(uri: Uri?): ByteArray? = uri?.let { localImageStore?.compress(it) }

    private fun onAddMemoryFromUris(title: String, date: LocalDate, note: String, image1Uri: Uri?, image2Uri: Uri?) {
        viewModelScope.launch {
            onAddMemory(title, date, note, compress(image1Uri), compress(image2Uri))
        }
    }

    private fun onUpdateMemoryFromUris(memory: Memory, image1Uri: Uri?, image2Uri: Uri?) {
        viewModelScope.launch {
            onUpdateMemory(memory, compress(image1Uri), compress(image2Uri))
        }
    }

    fun onAddMemory(
        title: String,
        date: LocalDate,
        note: String,
        image1Bytes: ByteArray? = null,
        image2Bytes: ByteArray? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val memoryId = UUID.randomUUID().toString()
            val (localUrl1, localUrl2) = saveLocalCopies(memoryId, image1Bytes, image2Bytes, current = null to null)

            val newMemory = Memory(
                id = memoryId,
                title = title,
                date = date,
                note = note,
                partner1ImageUrl = localUrl1,
                partner2ImageUrl = localUrl2
            )
            _dialogState.update { it.copy(showAddMemoryDialog = false) }
            trackPhotoUpload(newMemory, image1Bytes, image2Bytes) {
                repository.addMemory(current.id, newMemory, image1Bytes, image2Bytes)
            }
        }
    }

    fun onUpdateMemory(
        memory: Memory,
        image1Bytes: ByteArray? = null,
        image2Bytes: ByteArray? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val memoryId = memory.id.ifBlank { UUID.randomUUID().toString() }
            val (url1, url2) = saveLocalCopies(
                memoryId,
                image1Bytes,
                image2Bytes,
                current = memory.partner1ImageUrl to memory.partner2ImageUrl
            )

            val updatedMemory = memory.copy(
                id = memoryId,
                imageUrl = null,
                partner1ImageUrl = url1,
                partner2ImageUrl = url2
            )
            _dialogState.update { it.copy(memoryToEdit = null) }
            trackPhotoUpload(updatedMemory, image1Bytes, image2Bytes) {
                repository.updateMemory(current.id, updatedMemory, image1Bytes, image2Bytes)
            }
        }
    }

    /**
     * Shows [localMemory] with an upload badge on every slot that gets new bytes until [upload] returns.
     * Slots that end up with a Firebase Storage HTTPS URL briefly switch to SYNCED; a Base64 fallback or
     * a failure just drops the badge.
     */
    private suspend fun trackPhotoUpload(
        localMemory: Memory,
        image1Bytes: ByteArray?,
        image2Bytes: ByteArray?,
        upload: suspend () -> Result<Memory>
    ) {
        val slots = buildSet {
            if (image1Bytes?.isNotEmpty() == true) add(PhotoSlotKey(localMemory.id, isPartner1 = true))
            if (image2Bytes?.isNotEmpty() == true) add(PhotoSlotKey(localMemory.id, isPartner1 = false))
        }
        if (slots.isEmpty()) {
            upload()
            return
        }

        _pendingMemories.update { it + (localMemory.id to localMemory) }
        _photoSyncStates.update { states -> states + slots.associateWith { PhotoSyncState.UPLOADING } }

        val saved = upload().getOrNull()
        val confirmed = slots.filterTo(mutableSetOf()) { key ->
            val url = if (key.isPartner1) saved?.partner1ImageUrl else saved?.partner2ImageUrl
            url?.startsWith("https://") == true
        }

        _pendingMemories.update { pending ->
            if (pending[localMemory.id] === localMemory) pending - localMemory.id else pending
        }
        _photoSyncStates.update { states -> states - slots + confirmed.associateWith { PhotoSyncState.SYNCED } }

        if (confirmed.isNotEmpty()) {
            delay(SYNC_CONFIRMATION_MILLIS)
            // A newer upload of the same slot may have started meanwhile; leave its badge alone
            _photoSyncStates.update { states ->
                states.filterNot { (key, state) -> key in confirmed && state == PhotoSyncState.SYNCED }
            }
        }
    }

    fun onDeleteMemory(memoryId: String) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.deleteMemory(current.id, memoryId)
            _dialogState.update { it.copy(memoryToDelete = null) }
        }
    }

    /** Exports [imageUrls] to the gallery; a second request while one is running is ignored. */
    fun onSaveToGallery(imageUrls: List<String>) {
        val store = localImageStore ?: return
        val urls = imageUrls.filter { it.isNotBlank() }.distinct()
        if (urls.isEmpty() || _dialogState.value.isSavingToGallery) return
        _dialogState.update { it.copy(isSavingToGallery = true) }
        viewModelScope.launch {
            val allSaved = urls.map { store.saveToGallery(it) }.all { it.isSuccess }
            _dialogState.update { it.copy(isSavingToGallery = false) }
            _effects.send(
                if (allSaved) {
                    CoupleEffect.SavedToGallery(urls.size)
                } else {
                    CoupleEffect.Toast(UiText.Resource(R.string.photo_save_failed))
                }
            )
        }
    }

    // endregion

    // region Counter & milestones

    fun onCounterDisplayModeSelected(mode: CounterDisplayMode) {
        _counterPreferences.update { it.copy(displayMode = mode) }
        viewModelScope.launch { preferences?.saveCounterDisplayMode(mode) }
    }

    fun onMilestoneKindToggled(kind: MilestoneKind, enabled: Boolean) {
        val kinds = _counterPreferences.value.enabledMilestoneKinds.let { if (enabled) it + kind else it - kind }
        _counterPreferences.update { it.copy(enabledMilestoneKinds = kinds) }
        viewModelScope.launch { preferences?.saveEnabledMilestoneKinds(kinds) }
    }

    fun onAddCustomMilestone(title: String, date: LocalDate) {
        val current = repository.currentSpace.value ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        _dialogState.update { it.copy(showAddCustomMilestoneDialog = false) }
        viewModelScope.launch {
            repository.addCustomMilestone(current.id, CustomMilestone(title = trimmed, date = date))
        }
    }

    fun onDeleteCustomMilestone(milestoneId: String) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.deleteCustomMilestone(current.id, milestoneId)
        }
    }

    // endregion

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_pairingState.value.countdownSeconds > 0) {
                delay(1000)
                _pairingState.update { state ->
                    if (state.countdownSeconds > 0) state.copy(countdownSeconds = state.countdownSeconds - 1) else state
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        copyFeedbackJob?.cancel()
    }

    private companion object {
        const val SYNC_CONFIRMATION_MILLIS = 1_500L
        const val SPACE_LOAD_TIMEOUT_MS = 10_000L
        const val COPY_FEEDBACK_MILLIS = 2_500L
    }
}
