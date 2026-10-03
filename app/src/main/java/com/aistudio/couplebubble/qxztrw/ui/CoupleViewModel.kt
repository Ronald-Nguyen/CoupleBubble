package com.aistudio.couplebubble.qxztrw.ui

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.auth.GoogleAuthClient
import com.aistudio.couplebubble.qxztrw.data.CoupleSessionPreferences
import com.aistudio.couplebubble.qxztrw.data.LocalImageStorage
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.model.RelationshipMetrics
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.repository.SpaceFullException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.UUID

/** Upload progress of one memory photo; absent from the map once nothing needs to be shown. */
enum class PhotoSyncState {
    UPLOADING,
    SYNCED
}

data class PhotoSlotKey(val memoryId: String, val isPartnerA: Boolean)

enum class PairingTab {
    CREATE,
    ENTER
}

data class PairingUiState(
    val selectedTab: PairingTab = PairingTab.CREATE,
    val generatedCode: String = "482-913",
    val countdownSeconds: Int = 890,
    val enteredCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCopied: Boolean = false,
    val userProfile: UserProfile? = null,
    val showSetupSpaceDialog: Boolean = false,
    val showSpaceFullDialog: Boolean = false,
    val pendingSpaceId: String? = null,
    val isGoogleAuthLoading: Boolean = false,
    val googleAuthError: String? = null
) {
    val formattedCountdown: String
        get() {
            val minutes = countdownSeconds / 60
            val seconds = countdownSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val isInputReady: Boolean
        get() = enteredCode.replace("-", "").trim().length == 6
}

data class DashboardDialogState(
    val isMenuExpanded: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val showEditNamesDialog: Boolean = false,
    val showAddMemoryDialog: Boolean = false,
    val showSetupSpaceDialog: Boolean = false,
    val showGoogleBackupDialog: Boolean = false,
    val isGoogleAuthLoading: Boolean = false,
    val isUploadingProfilePhoto: Boolean = false,
    val googleAuthError: String? = null,
    val memoryToEdit: Memory? = null,
    val memoryToDelete: Memory? = null
)

data class DashboardUiState(
    val space: CoupleSpace,
    val metrics: RelationshipMetrics,
    val memories: List<Memory> = emptyList(),
    val userProfile: UserProfile? = null,
    val isCurrentUserPartner1: Boolean = true,
    val isUploadingProfilePhoto: Boolean = false,
    val isGoogleAuthLoading: Boolean = false,
    val googleAuthError: String? = null,
    val isMenuExpanded: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val showEditNamesDialog: Boolean = false,
    val showAddMemoryDialog: Boolean = false,
    val showSetupSpaceDialog: Boolean = false,
    val showGoogleBackupDialog: Boolean = false,
    val memoryToEdit: Memory? = null,
    val memoryToDelete: Memory? = null,
    val photoSyncStates: Map<PhotoSlotKey, PhotoSyncState> = emptyMap(),
    val loveNoteText: String = "Du bist mein liebster Gedanke am Morgen und meine schönste Ruhe am Abend. Schön, dass wir diesen Raum teilen."
)

sealed interface CoupleMainState {
    data object Loading : CoupleMainState
    data class Unpaired(val state: PairingUiState) : CoupleMainState
    data class Paired(val state: DashboardUiState) : CoupleMainState
}

@OptIn(ExperimentalCoroutinesApi::class)
class CoupleViewModel(
    private val repository: CoupleRepository = FirebaseCoupleRepository(),
    private val preferences: CoupleSessionPreferences? = null,
    private val started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _pairingState = MutableStateFlow(PairingUiState())
    private val _dialogState = MutableStateFlow(DashboardDialogState())
    private val _isSessionRestored = MutableStateFlow(preferences == null)

    // Local-first copies of memories whose photos are still uploading, shown in place of the remote version
    private val _pendingMemories = MutableStateFlow<Map<String, Memory>>(emptyMap())
    private val _photoSyncStates = MutableStateFlow<Map<PhotoSlotKey, PhotoSyncState>>(emptyMap())

    private var countdownJob: Job? = null
    private var copyFeedbackJob: Job? = null

    init {
        startCountdownTimer()
        initSessionCheck()
        initPairingListener()
    }

    private fun initPairingListener() {
        viewModelScope.launch {
            repository.listenToPairingCode(_pairingState.value.generatedCode)
        }
    }

    private fun initSessionCheck() {
        viewModelScope.launch {
            try {
                val currentAuth = FirebaseAuth.getInstance()
                if (currentAuth.currentUser == null) {
                    try {
                        currentAuth.signInAnonymously().await()
                    } catch (e: Exception) {
                        // Anonymous auth fallback
                    }
                }
                val authUser = currentAuth.currentUser
                if (authUser != null) {
                    val userRes = repository.restoreSessionForUser(authUser.uid)
                    if (userRes.isSuccess && userRes.getOrNull() != null) {
                        val space = userRes.getOrNull()!!
                        preferences?.saveActiveCoupleId(space.id)
                        _isSessionRestored.value = true
                        return@launch
                    }
                }
            } catch (e: Exception) {
                // Ignore if Firebase uninitialized
            }

            if (preferences != null) {
                val activeCoupleId = preferences.activeCoupleIdFlow.firstOrNull()
                if (!activeCoupleId.isNullOrBlank()) {
                    val result = repository.restoreSession(activeCoupleId)
                    if (result.isFailure) {
                        preferences.clearSession()
                    }
                }
            }
            _isSessionRestored.value = true
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

        viewModelScope.launch {
            repository.currentUserProfile.collect { profile ->
                _pairingState.value = _pairingState.value.copy(userProfile = profile)
            }
        }
    }

    private val memoriesFlow = repository.currentSpace.flatMapLatest { space ->
        if (space != null) {
            repository.getMemories(space.id)
        } else {
            flowOf(emptyList())
        }
    }

    private val visibleMemoriesFlow = combine(memoriesFlow, _pendingMemories) { remote, pending ->
        if (pending.isEmpty()) {
            remote
        } else {
            (remote.filterNot { it.id in pending } + pending.values).sortedByDescending { it.date }
        }
    }

    private val partnerRoleFlow: Flow<String> = preferences?.partnerRoleFlow?.map { it ?: "1" }?.onStart { emit("1") } ?: flowOf("1")

    val uiState: StateFlow<CoupleMainState> = combine(
        repository.currentSpace,
        _pairingState,
        _dialogState,
        visibleMemoriesFlow,
        _isSessionRestored,
        repository.currentUserProfile,
        partnerRoleFlow,
        _photoSyncStates
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val space = flows[0] as CoupleSpace?
        val pairing = flows[1] as PairingUiState
        val dialogs = flows[2] as DashboardDialogState
        @Suppress("UNCHECKED_CAST")
        val memories = flows[3] as List<Memory>
        val sessionRestored = flows[4] as Boolean
        val userProfile = flows[5] as UserProfile?
        val role = flows[6] as String
        @Suppress("UNCHECKED_CAST")
        val photoSyncStates = flows[7] as Map<PhotoSlotKey, PhotoSyncState>

        // Explicit partner IDs decide the role; array order and the stored preference are only fallbacks
        val uid = userProfile?.uid
        val isPartner1 = when {
            space == null || uid == null -> role != "2"
            uid == space.partner1Id -> true
            uid == space.partner2Id -> false
            uid == space.userUids.getOrNull(0) -> true
            uid == space.userUids.getOrNull(1) -> false
            else -> role != "2"
        }

        if (!sessionRestored && repository.currentSpace.value == null) {
            CoupleMainState.Loading
        } else if (space != null && space.isActive) {
            val metrics = RelationshipDateCalculator.calculate(
                year = space.anniversaryYear,
                month = space.anniversaryMonth,
                day = space.anniversaryDay
            )
            CoupleMainState.Paired(
                DashboardUiState(
                    space = space,
                    metrics = metrics,
                    memories = memories,
                    userProfile = userProfile,
                    isCurrentUserPartner1 = isPartner1,
                    isUploadingProfilePhoto = dialogs.isUploadingProfilePhoto,
                    isGoogleAuthLoading = dialogs.isGoogleAuthLoading,
                    googleAuthError = dialogs.googleAuthError,
                    isMenuExpanded = dialogs.isMenuExpanded,
                    showDisconnectDialog = dialogs.showDisconnectDialog,
                    showEditNamesDialog = dialogs.showEditNamesDialog,
                    showAddMemoryDialog = dialogs.showAddMemoryDialog,
                    showSetupSpaceDialog = dialogs.showSetupSpaceDialog,
                    showGoogleBackupDialog = dialogs.showGoogleBackupDialog,
                    memoryToEdit = dialogs.memoryToEdit,
                    photoSyncStates = photoSyncStates,
                    memoryToDelete = dialogs.memoryToDelete
                )
            )
        } else {
            CoupleMainState.Unpaired(pairing)
        }
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = if (preferences == null) CoupleMainState.Unpaired(_pairingState.value) else CoupleMainState.Loading
    )

    fun onTabSelected(tab: PairingTab) {
        _pairingState.value = _pairingState.value.copy(
            selectedTab = tab,
            errorMessage = null
        )
    }

    fun onEnteredCodeChanged(rawInput: String) {
        val cleaned = rawInput.filter { it in '0'..'9' }.take(6)
        _pairingState.value = _pairingState.value.copy(
            enteredCode = cleaned,
            errorMessage = null
        )
    }

    fun onGenerateNewCode() {
        viewModelScope.launch {
            preferences?.savePartnerRole("1")
            val newCode = repository.generateNewPairingCode()
            _pairingState.value = _pairingState.value.copy(
                generatedCode = newCode.code,
                countdownSeconds = newCode.totalValidSeconds,
                errorMessage = null
            )
            startCountdownTimer()
        }
    }

    fun onCopyCodeSuccess() {
        _pairingState.value = _pairingState.value.copy(isCopied = true)
        copyFeedbackJob?.cancel()
        copyFeedbackJob = viewModelScope.launch {
            delay(2500)
            _pairingState.value = _pairingState.value.copy(isCopied = false)
        }
    }

    fun onConnectClicked() {
        val codeToTest = _pairingState.value.enteredCode
        if (codeToTest.length != 6) {
            _pairingState.value = _pairingState.value.copy(
                errorMessage = "Bitte gib alle 6 Stellen deines Codes ein."
            )
            return
        }

        viewModelScope.launch {
            _pairingState.value = _pairingState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            val result = repository.connectWithCode(codeToTest)
            val error = result.exceptionOrNull()
            if (error is SpaceFullException) {
                _pairingState.value = _pairingState.value.copy(
                    isLoading = false,
                    showSpaceFullDialog = true
                )
            } else if (error != null) {
                _pairingState.value = _pairingState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Verbindung fehlgeschlagen."
                )
            } else {
                val space = result.getOrNull()
                if (space != null) {
                    preferences?.saveActiveCoupleId(space.id)
                    preferences?.savePartnerRole("2")
                    if (!space.isSetupComplete) {
                        _pairingState.value = _pairingState.value.copy(
                            isLoading = false,
                            showSetupSpaceDialog = true,
                            pendingSpaceId = space.id
                        )
                    } else {
                        _pairingState.value = _pairingState.value.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun onRecheckCodeAfterSpaceFull() {
        _pairingState.value = _pairingState.value.copy(
            showSpaceFullDialog = false,
            enteredCode = "",
            errorMessage = null
        )
    }

    fun onCreateOwnSpaceAfterSpaceFull() {
        _pairingState.value = _pairingState.value.copy(
            showSpaceFullDialog = false,
            enteredCode = "",
            selectedTab = PairingTab.CREATE,
            errorMessage = null
        )
        onGenerateNewCode()
    }

    fun onOpenDemoSpace() {
        viewModelScope.launch {
            _pairingState.value = _pairingState.value.copy(isLoading = true)
            preferences?.savePartnerRole("1")
            val demoSpace = repository.openDemoSpace()
            preferences?.saveActiveCoupleId(demoSpace.id)
            _isSessionRestored.value = true
            _pairingState.value = _pairingState.value.copy(isLoading = false)
        }
    }

    fun setShowSetupSpaceDialog(show: Boolean) {
        _pairingState.value = _pairingState.value.copy(showSetupSpaceDialog = show)
        _dialogState.value = _dialogState.value.copy(showSetupSpaceDialog = show)
    }

    fun setShowGoogleBackupDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(
            isMenuExpanded = false,
            showGoogleBackupDialog = show
        )
    }

    fun onSaveSpaceSetup(
        partnerAName: String,
        partnerBName: String,
        date: LocalDate
    ) {
        val trimmedA = partnerAName.trim()
        val trimmedB = partnerBName.trim()
        if (trimmedA.length < 2 || trimmedB.length < 2) return

        val currentSpaceId = repository.currentSpace.value?.id ?: _pairingState.value.pendingSpaceId ?: return

        viewModelScope.launch {
            repository.updateSpaceDetails(
                coupleId = currentSpaceId,
                partnerAName = trimmedA,
                partnerBName = trimmedB,
                anniversaryYear = date.year,
                anniversaryMonth = date.monthValue,
                anniversaryDay = date.dayOfMonth
            )
            preferences?.saveActiveCoupleId(currentSpaceId)
            _pairingState.value = _pairingState.value.copy(
                showSetupSpaceDialog = false,
                pendingSpaceId = null
            )
            _dialogState.value = _dialogState.value.copy(showSetupSpaceDialog = false)
        }
    }

    fun onSignInWithGoogle(uid: String, email: String?, displayName: String?) {
        viewModelScope.launch {
            val result = repository.signInWithGoogleUser(uid, email, displayName)
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null && !profile.coupleId.isNullOrBlank()) {
                    preferences?.saveActiveCoupleId(profile.coupleId)
                }
            }
        }
    }

    private fun isCancellationException(throwable: Throwable?): Boolean =
        throwable is GetCredentialCancellationException || throwable?.cause is GetCredentialCancellationException

    fun onSignInWithGoogleClicked(
        activityContext: Context,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _pairingState.value = _pairingState.value.copy(
                isGoogleAuthLoading = true,
                googleAuthError = null
            )
            _dialogState.value = _dialogState.value.copy(
                isGoogleAuthLoading = true,
                googleAuthError = null
            )

            try {
                val previousUid = try {
                    FirebaseAuth.getInstance().currentUser?.uid
                } catch (_: Exception) {
                    null
                }
                val googleAuthClient = GoogleAuthClient(activityContext.applicationContext)
                val result = googleAuthClient.signIn(activityContext)

                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    if (firebaseUser != null) {
                        val repoResult = repository.signInWithGoogleUser(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = firebaseUser.displayName,
                            previousUid = previousUid
                        )
                        if (repoResult.isSuccess) {
                            val profile = repoResult.getOrNull()
                            if (profile != null && !profile.coupleId.isNullOrBlank()) {
                                preferences?.saveActiveCoupleId(profile.coupleId)
                            }
                        }
                    }
                    _pairingState.value = _pairingState.value.copy(isGoogleAuthLoading = false)
                    _dialogState.value = _dialogState.value.copy(isGoogleAuthLoading = false)
                    onSuccess?.invoke()
                } else {
                    val exception = result.exceptionOrNull()
                    _pairingState.value = _pairingState.value.copy(isGoogleAuthLoading = false)
                    _dialogState.value = _dialogState.value.copy(isGoogleAuthLoading = false)

                    if (!isCancellationException(exception)) {
                        val errorMsg = resolveGoogleAuthErrorMessage(activityContext, exception)
                        _pairingState.value = _pairingState.value.copy(googleAuthError = errorMsg)
                        _dialogState.value = _dialogState.value.copy(googleAuthError = errorMsg)
                        onError?.invoke(errorMsg)
                    }
                }
            } catch (e: Exception) {
                _pairingState.value = _pairingState.value.copy(isGoogleAuthLoading = false)
                _dialogState.value = _dialogState.value.copy(isGoogleAuthLoading = false)
                if (!isCancellationException(e)) {
                    val errorMsg = resolveGoogleAuthErrorMessage(activityContext, e)
                    _pairingState.value = _pairingState.value.copy(googleAuthError = errorMsg)
                    _dialogState.value = _dialogState.value.copy(googleAuthError = errorMsg)
                    onError?.invoke(errorMsg)
                }
            }
        }
    }

    private fun resolveGoogleAuthErrorMessage(context: Context, throwable: Throwable?): String {
        if (throwable == null) return context.getString(R.string.google_sign_in_failed)
        val msg = throwable.message ?: throwable.localizedMessage ?: ""
        return when {
            throwable is NoCredentialException -> {
                context.getString(R.string.google_error_no_credentials)
            }
            throwable is GetCredentialException &&
                (msg.contains("[10]") || msg.contains("[16]") || msg.contains("[28444]") || msg.contains("DEVELOPER_ERROR")) -> {
                context.getString(R.string.google_error_sha1_missing)
            }
            throwable is FirebaseAuthException &&
                (throwable.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || msg.contains("CONFIGURATION_NOT_FOUND")) -> {
                context.getString(R.string.google_error_provider_disabled)
            }
            throwable is FirebaseNetworkException -> {
                context.getString(R.string.google_error_network)
            }
            msg.isNotBlank() -> {
                context.getString(R.string.google_sign_in_error_prefix, msg)
            }
            else -> {
                context.getString(R.string.google_sign_in_failed)
            }
        }
    }

    fun clearGoogleAuthError() {
        _pairingState.value = _pairingState.value.copy(googleAuthError = null)
        _dialogState.value = _dialogState.value.copy(googleAuthError = null)
    }

    fun onSignOutGoogle(context: Context? = null) {
        viewModelScope.launch {
            if (context != null) {
                try {
                    GoogleAuthClient(context.applicationContext).signOut()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            repository.signOutUser()
        }
    }

    fun setMenuExpanded(expanded: Boolean) {
        _dialogState.value = _dialogState.value.copy(isMenuExpanded = expanded)
    }

    fun setShowDisconnectDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(
            isMenuExpanded = false,
            showDisconnectDialog = show
        )
    }

    fun setShowEditNamesDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(
            isMenuExpanded = false,
            showEditNamesDialog = show
        )
    }

    fun setShowAddMemoryDialog(show: Boolean) {
        _dialogState.value = _dialogState.value.copy(showAddMemoryDialog = show)
    }

    fun setMemoryToEdit(memory: Memory?) {
        _dialogState.value = _dialogState.value.copy(memoryToEdit = memory)
    }

    fun setMemoryToDelete(memory: Memory?) {
        _dialogState.value = _dialogState.value.copy(memoryToDelete = memory)
    }

    fun onUpdatePartnerNames(partnerAName: String, partnerBName: String) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.updatePartnerNames(current.id, partnerAName, partnerBName)
            _dialogState.value = _dialogState.value.copy(showEditNamesDialog = false)
        }
    }

    fun onAddMemory(
        title: String,
        date: LocalDate,
        note: String,
        imageABytes: ByteArray? = null,
        imageBBytes: ByteArray? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val memoryId = UUID.randomUUID().toString()
            var localUrlA: String? = null
            var localUrlB: String? = null

            preferences?.let { prefs ->
                if (imageABytes != null && imageABytes.isNotEmpty()) {
                    localUrlA = LocalImageStorage.saveImage(prefs.context, memoryId, "a", imageABytes)
                }
                if (imageBBytes != null && imageBBytes.isNotEmpty()) {
                    localUrlB = LocalImageStorage.saveImage(prefs.context, memoryId, "b", imageBBytes)
                }
            }

            val newMemory = Memory(
                id = memoryId,
                title = title,
                date = date,
                note = note,
                partnerAImageUrl = localUrlA,
                partnerBImageUrl = localUrlB
            )
            _dialogState.value = _dialogState.value.copy(showAddMemoryDialog = false)
            trackPhotoUpload(newMemory, imageABytes, imageBBytes) {
                repository.addMemory(current.id, newMemory, imageABytes, imageBBytes)
            }
        }
    }

    fun onUpdateMemory(
        memory: Memory,
        imageABytes: ByteArray? = null,
        imageBBytes: ByteArray? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val memoryId = if (memory.id.isNotBlank()) memory.id else UUID.randomUUID().toString()
            var updatedUrlA = memory.partnerAImageUrl
            var updatedUrlB = memory.partnerBImageUrl

            preferences?.let { prefs ->
                if (imageABytes != null && imageABytes.isNotEmpty()) {
                    val local = LocalImageStorage.saveImage(prefs.context, memoryId, "a", imageABytes)
                    if (local != null) updatedUrlA = local
                }
                if (imageBBytes != null && imageBBytes.isNotEmpty()) {
                    val local = LocalImageStorage.saveImage(prefs.context, memoryId, "b", imageBBytes)
                    if (local != null) updatedUrlB = local
                }
            }

            val updatedMemory = memory.copy(
                id = memoryId,
                imageUrl = null,
                partnerAImageUrl = updatedUrlA,
                partnerBImageUrl = updatedUrlB
            )
            _dialogState.value = _dialogState.value.copy(memoryToEdit = null)
            trackPhotoUpload(updatedMemory, imageABytes, imageBBytes) {
                repository.updateMemory(current.id, updatedMemory, imageABytes, imageBBytes)
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
        imageABytes: ByteArray?,
        imageBBytes: ByteArray?,
        upload: suspend () -> Result<Memory>
    ) {
        val slots = buildSet {
            if (imageABytes?.isNotEmpty() == true) add(PhotoSlotKey(localMemory.id, isPartnerA = true))
            if (imageBBytes?.isNotEmpty() == true) add(PhotoSlotKey(localMemory.id, isPartnerA = false))
        }
        if (slots.isEmpty()) {
            upload()
            return
        }

        _pendingMemories.update { it + (localMemory.id to localMemory) }
        _photoSyncStates.update { states -> states + slots.associateWith { PhotoSyncState.UPLOADING } }

        val saved = upload().getOrNull()
        val confirmed = slots.filterTo(mutableSetOf()) { key ->
            val url = if (key.isPartnerA) saved?.partnerAImageUrl else saved?.partnerBImageUrl
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
            _dialogState.value = _dialogState.value.copy(memoryToDelete = null)
        }
    }

    fun onUploadProfilePhoto(
        isPartner1: Boolean,
        imageBytes: ByteArray,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val current = repository.currentSpace.value ?: return
        _dialogState.value = _dialogState.value.copy(isUploadingProfilePhoto = true)
        viewModelScope.launch {
            try {
                val result = repository.uploadProfilePhoto(current.id, isPartner1, imageBytes)
                if (result.isSuccess) {
                    onSuccess?.invoke()
                } else {
                    onError?.invoke(result.exceptionOrNull()?.message ?: "Upload fehlgeschlagen")
                }
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Upload fehlgeschlagen")
            } finally {
                _dialogState.value = _dialogState.value.copy(isUploadingProfilePhoto = false)
            }
        }
    }

    fun onUploadProfilePhoto(
        isPartner1: Boolean,
        uri: android.net.Uri,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val current = repository.currentSpace.value ?: return
        _dialogState.value = _dialogState.value.copy(isUploadingProfilePhoto = true)
        viewModelScope.launch {
            try {
                val result = repository.uploadProfilePhoto(current.id, isPartner1, uri)
                if (result.isSuccess) {
                    onSuccess?.invoke()
                } else {
                    onError?.invoke(result.exceptionOrNull()?.message ?: "Upload fehlgeschlagen")
                }
            } catch (e: Exception) {
                onError?.invoke(e.message ?: "Upload fehlgeschlagen")
            } finally {
                _dialogState.value = _dialogState.value.copy(isUploadingProfilePhoto = false)
            }
        }
    }

    fun onUpdatePartnerColor(
        colorHex: String,
        isPartner1: Boolean,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val result = repository.updatePartnerColor(current.id, isPartner1, colorHex)
            if (result.isSuccess) {
                onSuccess?.invoke()
            } else {
                onError?.invoke(result.exceptionOrNull()?.message ?: "Farbe konnte nicht aktualisiert werden")
            }
        }
    }

    fun swapPartnerRoles(
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable?) -> Unit)? = null
    ) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            val result = repository.swapPartners(current.id)
            if (result.isSuccess) {
                preferences?.let { prefs ->
                    val currentRole = prefs.partnerRoleFlow.firstOrNull()
                    if (currentRole == "1" || currentRole == "A") {
                        prefs.savePartnerRole("2")
                    } else if (currentRole == "2" || currentRole == "B") {
                        prefs.savePartnerRole("1")
                    }
                }
                onSuccess?.invoke()
            } else {
                onError?.invoke(result.exceptionOrNull())
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
            _pairingState.value = _pairingState.value.copy(
                enteredCode = "",
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_pairingState.value.countdownSeconds > 0) {
                delay(1000)
                val current = _pairingState.value.countdownSeconds
                if (current > 0) {
                    _pairingState.value = _pairingState.value.copy(countdownSeconds = current - 1)
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
    }
}
