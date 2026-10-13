package com.aistudio.couplebubble.qxztrw.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.rethrowIfCancellation
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleSignInClient
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipCalendar
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
import com.aistudio.couplebubble.qxztrw.domain.session.MilestoneReminder
import com.aistudio.couplebubble.qxztrw.domain.session.PartnerMomentWatcher
import com.aistudio.couplebubble.qxztrw.domain.session.PartnerRoleSync
import com.aistudio.couplebubble.qxztrw.domain.error.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerSlotUnknownException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceFullException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceUnavailableException
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import com.aistudio.couplebubble.qxztrw.widget.widgetSnapshotOf
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class CoupleViewModel(
    private val repository: CoupleRepository,
    private val preferences: SessionStore? = null,
    private val localImageStore: LocalImageStore? = null,
    private val googleSignInClient: GoogleSignInClient? = null,
    private val momentNotifier: MomentNotifier? = null,
    private val widgetPublisher: WidgetPublisher? = null,
    private val started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _pairingState = MutableStateFlow(PairingUiState())
    private val _dialogState = MutableStateFlow(DashboardDialogState())
    private val _googleAuthState = MutableStateFlow(GoogleAuthState())
    private val _isSessionRestored = MutableStateFlow(preferences == null)

    private val uploads = MemoryPhotoUploadTracker()
    private val partnerMoments = momentNotifier?.let(::PartnerMomentWatcher)
    private val milestoneReminder = momentNotifier?.let { MilestoneReminder(it, preferences) }
    private val roleSync = PartnerRoleSync(repository, preferences)
    private val _counterPreferences = MutableStateFlow(CounterPreferences())

    private val _effects = Channel<CoupleEffect>(Channel.BUFFERED)

    /** One-shot toasts, snackbars and haptic confirmations; collected once by the activity. */
    val effects: Flow<CoupleEffect> = _effects.receiveAsFlow()

    // Shared with NotesViewModel, which lives in the same ViewModelStore
    val currentSpace: StateFlow<CoupleSpace?> = repository.currentSpace
    val currentUserProfile: StateFlow<UserProfile?> = repository.currentUserProfile

    private var countdownJob: Job? = null
    private var silentRestoreAttempted = false
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

    // Keyed on the id only: names or photos changing on the space document must not re-open the listeners
    private val activeSpaceId = repository.currentSpace.map { it?.id }.distinctUntilChanged()

    /** One Firestore listener per space, shared by the timeline and the partner notifications. */
    private val spaceMemoriesFlow: Flow<Pair<String, List<Memory>>?> = activeSpaceId.flatMapLatest { spaceId ->
        if (spaceId != null) repository.getMemories(spaceId).map { spaceId to it } else flowOf(null)
    }.shareIn(viewModelScope, started, replay = 1)

    private val memoriesFlow = spaceMemoriesFlow.map { it?.second.orEmpty() }

    init {
        if (partnerMoments != null) {
            viewModelScope.launch {
                spaceMemoriesFlow.collect { update ->
                    val space = repository.currentSpace.value
                    when {
                        update == null -> partnerMoments.reset()
                        space?.id == update.first ->
                            partnerMoments.onMemories(space, update.second, repository.currentUserProfile.value?.uid)
                    }
                }
            }
        }
    }

    private val visibleMemoriesFlow = combine(memoriesFlow, uploads.pendingMemories, uploads::visibleMemories)

    /** Shared by the dashboard, the widgets and the milestone reminder, so the collection is listened to once. */
    private val customMilestonesFlow = activeSpaceId.flatMapLatest { spaceId ->
        if (spaceId != null) repository.getCustomMilestones(spaceId) else flowOf(emptyList())
    }.shareIn(viewModelScope, started, replay = 1)

    init {
        if (widgetPublisher != null) {
            viewModelScope.launch {
                // A null space only means "still loading"; leaving the space clears the widgets explicitly
                combine(repository.currentSpace.filterNotNull(), _counterPreferences, customMilestonesFlow, ::widgetSnapshotOf)
                    .distinctUntilChanged()
                    .collect { widgetPublisher.publish(it) }
            }
        }
        if (milestoneReminder != null) {
            viewModelScope.launch {
                combine(repository.currentSpace, _counterPreferences, customMilestonesFlow) { space, counter, custom ->
                    space?.takeIf { it.isActive }?.let { it.id to RelationshipCalendar.of(it, counter, custom).metrics() }
                }.collect { update -> update?.let { (spaceId, metrics) -> milestoneReminder.onMetrics(spaceId, metrics) } }
            }
        }
    }

    // `null` until DataStore answers, so a not yet loaded preference never decides which photo is editable
    private val partnerRoleFlow: Flow<PartnerRole?> = preferences?.partnerRoleFlow
        ?.map<String?, PartnerRole?> { PartnerRole.displayFallback(it) }
        ?.onStart { emit(null) }
        ?: flowOf(PartnerRole.PARTNER_1)

    private data class Session(
        val space: CoupleSpace?,
        val userProfile: UserProfile?,
        val role: PartnerRole?,
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

    init {
        viewModelScope.launch {
            sessionFlow.collect { roleSync.sync(it.space, it.userProfile?.uid, it.role) }
        }
    }

    private val dashboardContentFlow = combine(
        visibleMemoriesFlow,
        uploads.syncStates,
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
            val calendar = RelationshipCalendar.of(space, content.counterPreferences, content.customMilestones)
            CoupleMainState.Paired(
                DashboardUiState(
                    space = space,
                    metrics = calendar.metrics(),
                    memories = content.memories,
                    userProfile = session.userProfile,
                    isCurrentUserPartner1 = space.isPartner1(session.userProfile?.uid, session.role ?: PartnerRole.PARTNER_1),
                    currentUserRole = space.editableRole(session.userProfile?.uid, session.role),
                    dialogs = dialogs,
                    googleAuth = googleAuth,
                    photoSyncStates = content.photoSyncStates,
                    counterPreferences = content.counterPreferences,
                    customMilestones = content.customMilestones,
                    memoryMilestones = calendar.memoryMilestones(content.memories),
                    milestoneOptions = calendar.reachedMilestones()
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
            is DashboardEvent.UploadProfilePhoto -> onUploadProfilePhoto(event.bytes)
            is DashboardEvent.ShowAddMemoryDialog -> setShowAddMemoryDialog(event.show)
            is DashboardEvent.AddMemory ->
                onAddMemoryFromUri(event.title, event.date, event.note, event.milestoneKey, event.ownImageUri)
            is DashboardEvent.ShowEditMemoryDialog -> setMemoryToEdit(event.memory)
            is DashboardEvent.UpdateMemory -> onUpdateMemoryFromEdit(event.memory, event.ownPhoto)
            is DashboardEvent.ShowDeleteMemoryDialog -> setMemoryToDelete(event.memory)
            is DashboardEvent.DeleteMemory -> onDeleteMemory(event.memoryId)
            is DashboardEvent.SaveToGallery -> onSaveToGallery(event.imageUrls)
            is DashboardEvent.ShowGoogleBackupDialog -> setShowGoogleBackupDialog(event.show)
            is DashboardEvent.SignInWithGoogle -> onSignInWithGoogleClicked(event.activityContext)
            is DashboardEvent.SaveSpaceSetup -> onSaveSpaceSetup(event.partner1Name, event.partner2Name, event.date)
            is DashboardEvent.SelectCounterDisplayMode -> onCounterDisplayModeSelected(event.mode)
            is DashboardEvent.ShowMilestoneSettingsDialog -> setShowMilestoneSettingsDialog(event.show)
            is DashboardEvent.ShowAddCustomMilestoneDialog -> setShowAddCustomMilestoneDialog(event.show)
            is DashboardEvent.ToggleMilestoneKind -> onMilestoneKindToggled(event.kind, event.enabled)
            is DashboardEvent.AddCustomMilestone -> onAddCustomMilestone(event.title, event.date, event.recurringYearly)
            is DashboardEvent.SetCustomMilestoneEnabled -> onCustomMilestoneEnabledChanged(event.milestoneId, event.enabled)
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
                    _pairingState.update { it.copy(isLoading = false, errorMessage = null) }
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

    fun onSaveSpaceSetup(partner1Name: String, partner2Name: String, date: LocalDate) {
        val trimmed1 = partner1Name.trim()
        val trimmed2 = partner2Name.trim()
        if (trimmed1.length < 2 || trimmed2.length < 2) return
        if (date.isAfter(LocalDate.now()) || _dialogState.value.isSavingSpaceSetup) return

        val currentSpaceId = repository.currentSpace.value?.id ?: return

        viewModelScope.launch {
            _dialogState.update { it.copy(isSavingSpaceSetup = true) }
            repository.updateSpaceDetails(
                coupleId = currentSpaceId,
                partner1Name = trimmed1,
                partner2Name = trimmed2,
                anniversaryYear = date.year,
                anniversaryMonth = date.monthValue,
                anniversaryDay = date.dayOfMonth
            )
            preferences?.saveActiveCoupleId(currentSpaceId)
            _dialogState.update { it.copy(isSavingSpaceSetup = false) }
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
                signInWithGoogle { client.signIn(activityContext) }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
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

    /** Runs [signIn] and restores the space anchored at `/users/{uid}`; returns the failure or `null`. */
    private suspend fun signInWithGoogle(signIn: suspend () -> Result<FirebaseUser>): Throwable? {
        val previousUid = repository.currentAuthUid
        val result = signIn()
        result.getOrNull()?.let { firebaseUser ->
            val profile = repository.signInWithGoogleUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                previousUid = previousUid
            ).getOrNull()
            profile?.coupleId?.takeIf { it.isNotBlank() }?.let { preferences?.saveActiveCoupleId(it) }
        }
        return if (result.isSuccess) null else result.exceptionOrNull()
    }

    /**
     * After a reinstall the anonymous UID is gone, but a Google account that already used the app still
     * points to the space via `/users/{uid}`. Once per app start, an unpaired device without a Google
     * login tries a picker-free sign-in with that account and returns to the space on its own.
     */
    fun tryRestoreWithGoogle(activityContext: Context) {
        val client = googleSignInClient ?: return
        if (silentRestoreAttempted) return
        silentRestoreAttempted = true
        viewModelScope.launch {
            _isSessionRestored.first { it }
            if (repository.currentSpace.value != null || repository.currentUserProfile.value?.email != null) return@launch
            _googleAuthState.value = GoogleAuthState(isLoading = true)
            try {
                signInWithGoogle { client.signInSilently(activityContext) }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                // No authorized account: the user pairs or signs in manually
            }
            _googleAuthState.update { it.copy(isLoading = false) }
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

    fun onUploadProfilePhoto(imageBytes: ByteArray) {
        val current = repository.currentSpace.value ?: return
        val dialogs = _dialogState.value
        if (dialogs.isUploadingProfilePhoto) return
        if (dialogs.isSwappingRoles) {
            _effects.trySend(CoupleEffect.Snackbar(UiText.Resource(R.string.profile_photo_wait_for_swap)))
            return
        }
        _dialogState.update { it.copy(isUploadingProfilePhoto = true) }
        viewModelScope.launch {
            val result = try {
                repository.uploadProfilePhoto(current.id, partnerRoleFlow.first(), imageBytes)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                Result.failure(e)
            } finally {
                _dialogState.update { it.copy(isUploadingProfilePhoto = false) }
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

    fun swapPartnerRoles() {
        val current = repository.currentSpace.value ?: return
        val dialogs = _dialogState.value
        if (dialogs.isSwappingRoles) return
        // A memory added while the swap reads the list would keep the old slot layout
        if (dialogs.isUploadingProfilePhoto || uploads.hasPendingUploads) {
            _effects.trySend(CoupleEffect.Snackbar(UiText.Resource(R.string.swap_partners_wait_for_upload)))
            return
        }
        _dialogState.update { it.copy(isSwappingRoles = true) }
        viewModelScope.launch {
            val result = try {
                repository.swapPartners(current.id)
            } finally {
                _dialogState.update { it.copy(isSwappingRoles = false) }
            }
            if (result.isSuccess) {
                // With bound members syncPartnerRole follows the swapped IDs; without them only the preference knows
                if (!current.hasBoundMembers) {
                    preferences?.let { prefs ->
                        PartnerRole.fromStored(prefs.partnerRoleFlow.firstOrNull())?.let { role ->
                            prefs.savePartnerRole(role.swapped())
                        }
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
            widgetPublisher?.publish(null)
            _pairingState.update { it.copy(enteredCode = "", isLoading = false, errorMessage = null) }
            // The old code points at the space just left
            refreshPairingCode()
        }
    }

    // endregion

    // region Memories

    /** Stores a local-first copy of every slot that gets new bytes; slots without new bytes keep [current]. */
    private suspend fun compress(uri: Uri?): ByteArray? = uri?.let { localImageStore?.compress(it) }

    private fun onAddMemoryFromUri(title: String, date: LocalDate, note: String, milestoneKey: String?, ownImageUri: Uri?) {
        viewModelScope.launch {
            onAddMemory(title, date, note, compress(ownImageUri), milestoneKey)
        }
    }

    private fun onUpdateMemoryFromEdit(memory: Memory, ownPhoto: OwnPhotoEdit) {
        viewModelScope.launch {
            val change = when (ownPhoto) {
                OwnPhotoEdit.Keep -> MemoryPhotoChange.Keep
                OwnPhotoEdit.Remove -> MemoryPhotoChange.Remove
                is OwnPhotoEdit.Replace -> compress(ownPhoto.uri)?.let { MemoryPhotoChange.Replace(it) } ?: MemoryPhotoChange.Keep
            }
            onUpdateMemory(memory, change)
        }
    }

    /**
     * The slot a photo change of this user goes to, for the local-first copy and the upload badge (the repository
     * resolves it again when writing). Without a slot the change is dropped with a hint and only the text is saved.
     */
    private suspend fun ownSlotFor(change: MemoryPhotoChange): Pair<MemoryPhotoChange, PartnerRole?> {
        if (change == MemoryPhotoChange.Keep) return change to null
        val role = repository.currentSpace.value?.editableRole(repository.currentUserProfile.value?.uid, partnerRoleFlow.first())
        if (role == null) {
            _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.memory_photo_slot_unknown)))
            return MemoryPhotoChange.Keep to null
        }
        return change to role
    }

    private fun Memory.withOwnPhoto(role: PartnerRole?, change: MemoryPhotoChange): Memory {
        if (role == null) return this
        val url = when (change) {
            MemoryPhotoChange.Keep -> return this
            MemoryPhotoChange.Remove -> null
            is MemoryPhotoChange.Replace ->
                localImageStore?.saveMemoryImage(id, if (role == PartnerRole.PARTNER_1) "a" else "b", change.bytes)
                    ?: return this
        }
        return if (role == PartnerRole.PARTNER_1) copy(partner1ImageUrl = url) else copy(partner2ImageUrl = url)
    }

    fun onAddMemory(
        title: String,
        date: LocalDate,
        note: String,
        ownImageBytes: ByteArray? = null,
        milestoneKey: String? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val requested = ownImageBytes?.takeIf { it.isNotEmpty() }?.let { MemoryPhotoChange.Replace(it) } ?: MemoryPhotoChange.Keep
            val (change, role) = ownSlotFor(requested)
            val newMemory = Memory(id = UUID.randomUUID().toString(), title = title, date = date, note = note, milestoneKey = milestoneKey)
                .withOwnPhoto(role, change)
            _dialogState.update { it.copy(showAddMemoryDialog = false) }
            val result = uploads.track(newMemory, role.takeIf { change is MemoryPhotoChange.Replace }) {
                repository.addMemory(current.id, newMemory, partnerRoleFlow.first(), change)
            }
            reportSlotUnknown(result)
        }
    }

    /** Saves title, date, note and milestone link of [memory]; photos only change through [ownPhoto], never the partner's slot. */
    fun onUpdateMemory(
        memory: Memory,
        ownPhoto: MemoryPhotoChange = MemoryPhotoChange.Keep
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val (change, role) = ownSlotFor(ownPhoto)
            val memoryId = memory.id.ifBlank { UUID.randomUUID().toString() }
            // Photos shown meanwhile come from the latest stored version, not from when the dialog opened
            val latest = (uiState.value as? CoupleMainState.Paired)?.state?.memories?.firstOrNull { it.id == memoryId } ?: memory
            val updatedMemory = latest.copy(
                id = memoryId,
                title = memory.title,
                date = memory.date,
                note = memory.note,
                milestoneKey = memory.milestoneKey
            )
                .withOwnPhoto(role, change)
            _dialogState.update { it.copy(memoryToEdit = null) }
            val result = uploads.track(updatedMemory, role.takeIf { change is MemoryPhotoChange.Replace }) {
                repository.updateMemory(current.id, updatedMemory, partnerRoleFlow.first(), change)
            }
            reportSlotUnknown(result)
        }
    }

    private suspend fun reportSlotUnknown(result: Result<Memory>) {
        if (result.exceptionOrNull() is PartnerSlotUnknownException) {
            _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.memory_photo_slot_unknown)))
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

    fun onAddCustomMilestone(title: String, date: LocalDate, recurringYearly: Boolean = false) {
        val current = repository.currentSpace.value ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        _dialogState.update { it.copy(showAddCustomMilestoneDialog = false) }
        viewModelScope.launch {
            repository.addCustomMilestone(current.id, CustomMilestone(title = trimmed, date = date, recurringYearly = recurringYearly))
        }
    }

    fun onCustomMilestoneEnabledChanged(milestoneId: String, enabled: Boolean) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.setCustomMilestoneEnabled(current.id, milestoneId, enabled)
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
        const val SPACE_LOAD_TIMEOUT_MS = 10_000L
        const val COPY_FEEDBACK_MILLIS = 2_500L
    }
}
