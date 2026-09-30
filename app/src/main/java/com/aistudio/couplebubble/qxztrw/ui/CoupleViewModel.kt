package com.aistudio.couplebubble.qxztrw.ui

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class PairingTab {
    CREATE,
    ENTER
}

data class PairingUiState(
    val selectedTab: PairingTab = PairingTab.CREATE,
    val generatedCode: String = "BLU-789",
    val countdownSeconds: Int = 890,
    val enteredCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isCopied: Boolean = false,
    val userProfile: UserProfile? = null,
    val showSetupSpaceDialog: Boolean = false,
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
    val memoryToEdit: Memory? = null,
    val memoryToDelete: Memory? = null
)

data class DashboardUiState(
    val space: CoupleSpace,
    val metrics: RelationshipMetrics,
    val memories: List<Memory> = emptyList(),
    val userProfile: UserProfile? = null,
    val isGoogleAuthLoading: Boolean = false,
    val isMenuExpanded: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val showEditNamesDialog: Boolean = false,
    val showAddMemoryDialog: Boolean = false,
    val showSetupSpaceDialog: Boolean = false,
    val showGoogleBackupDialog: Boolean = false,
    val memoryToEdit: Memory? = null,
    val memoryToDelete: Memory? = null,
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
                val authUser = FirebaseAuth.getInstance().currentUser
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

    val uiState: StateFlow<CoupleMainState> = combine(
        repository.currentSpace,
        _pairingState,
        _dialogState,
        memoriesFlow,
        _isSessionRestored,
        repository.currentUserProfile
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val space = flows[0] as CoupleSpace?
        val pairing = flows[1] as PairingUiState
        val dialogs = flows[2] as DashboardDialogState
        @Suppress("UNCHECKED_CAST")
        val memories = flows[3] as List<Memory>
        val sessionRestored = flows[4] as Boolean
        val userProfile = flows[5] as UserProfile?

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
                    isGoogleAuthLoading = dialogs.isGoogleAuthLoading,
                    isMenuExpanded = dialogs.isMenuExpanded,
                    showDisconnectDialog = dialogs.showDisconnectDialog,
                    showEditNamesDialog = dialogs.showEditNamesDialog,
                    showAddMemoryDialog = dialogs.showAddMemoryDialog,
                    showSetupSpaceDialog = dialogs.showSetupSpaceDialog,
                    showGoogleBackupDialog = dialogs.showGoogleBackupDialog,
                    memoryToEdit = dialogs.memoryToEdit,
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
        val cleaned = rawInput.filter { it.isLetterOrDigit() }.take(6).uppercase()
        _pairingState.value = _pairingState.value.copy(
            enteredCode = cleaned,
            errorMessage = null
        )
    }

    fun onGenerateNewCode() {
        viewModelScope.launch {
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
            if (result.isFailure) {
                _pairingState.value = _pairingState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Verbindung fehlgeschlagen."
                )
            } else {
                val space = result.getOrNull()
                if (space != null) {
                    preferences?.saveActiveCoupleId(space.id)
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

    fun onOpenDemoSpace() {
        viewModelScope.launch {
            _pairingState.value = _pairingState.value.copy(isLoading = true)
            val demoSpace = repository.openDemoSpace()
            preferences?.saveActiveCoupleId(demoSpace.id)
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
            _dialogState.value = _dialogState.value.copy(isGoogleAuthLoading = true)

            try {
                val googleAuthClient = GoogleAuthClient(activityContext.applicationContext)
                val result = googleAuthClient.signIn(activityContext)

                if (result.isSuccess) {
                    val firebaseUser = result.getOrNull()
                    if (firebaseUser != null) {
                        val repoResult = repository.signInWithGoogleUser(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email,
                            displayName = firebaseUser.displayName
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

                    if (exception !is GetCredentialCancellationException) {
                        val errorMsg = exception?.localizedMessage ?: "Google-Anmeldung fehlgeschlagen"
                        _pairingState.value = _pairingState.value.copy(googleAuthError = errorMsg)
                        onError?.invoke(errorMsg)
                    }
                }
            } catch (e: Exception) {
                _pairingState.value = _pairingState.value.copy(
                    isGoogleAuthLoading = false,
                    googleAuthError = e.localizedMessage
                )
                _dialogState.value = _dialogState.value.copy(isGoogleAuthLoading = false)
                if (e !is GetCredentialCancellationException) {
                    onError?.invoke(e.localizedMessage ?: "Google-Anmeldung fehlgeschlagen")
                }
            }
        }
    }

    fun clearGoogleAuthError() {
        _pairingState.value = _pairingState.value.copy(googleAuthError = null)
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
            if (preferences != null) {
                if (imageABytes != null && imageABytes.isNotEmpty()) {
                    localUrlA = LocalImageStorage.saveImage(preferences.context, memoryId, "a", imageABytes)
                }
                if (imageBBytes != null && imageBBytes.isNotEmpty()) {
                    localUrlB = LocalImageStorage.saveImage(preferences.context, memoryId, "b", imageBBytes)
                }
            }
            val newMemory = Memory(
                id = memoryId,
                title = title,
                date = date,
                note = note,
                imageUrl = localUrlA,
                partnerAImageUrl = localUrlA,
                partnerBImageUrl = localUrlB
            )
            repository.addMemory(current.id, newMemory, imageABytes, imageBBytes)
            _dialogState.value = _dialogState.value.copy(showAddMemoryDialog = false)
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
            var updatedUrlA = memory.partnerAImageUrl ?: memory.imageUrl
            var updatedUrlB = memory.partnerBImageUrl

            if (preferences != null) {
                if (imageABytes != null && imageABytes.isNotEmpty()) {
                    val local = LocalImageStorage.saveImage(preferences.context, memoryId, "a", imageABytes)
                    if (local != null) updatedUrlA = local
                }
                if (imageBBytes != null && imageBBytes.isNotEmpty()) {
                    val local = LocalImageStorage.saveImage(preferences.context, memoryId, "b", imageBBytes)
                    if (local != null) updatedUrlB = local
                }
            }

            val updatedMemory = memory.copy(
                id = memoryId,
                imageUrl = updatedUrlA,
                partnerAImageUrl = updatedUrlA,
                partnerBImageUrl = updatedUrlB
            )
            repository.updateMemory(current.id, updatedMemory, imageABytes, imageBBytes)
            _dialogState.value = _dialogState.value.copy(memoryToEdit = null)
        }
    }

    fun onDeleteMemory(memoryId: String) {
        val current = repository.currentSpace.value ?: return
        viewModelScope.launch {
            repository.deleteMemory(current.id, memoryId)
            _dialogState.value = _dialogState.value.copy(memoryToDelete = null)
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
}
