package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.core.guardedScope
import com.aistudio.couplebubble.qxztrw.core.backgroundGuard
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.error.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceFullException
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceUnavailableException
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository
import com.aistudio.couplebubble.qxztrw.domain.session.SessionRestorer
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.GoogleAuthController
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingTab
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The pairing screen: this device's own code with its countdown, entering the partner's code, the demo space. */
class PairingViewModel(
    private val repository: SpaceRepository,
    private val sessionStore: SessionStore,
    private val restorer: SessionRestorer,
    private val googleAuth: GoogleAuthController,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    private val _effects = Channel<CoupleEffect>(Channel.BUFFERED)

    /** One-shot toasts; collected once by the activity. */
    val effects: Flow<CoupleEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<PairingUiState> = combine(
        _state,
        repository.currentUserProfile,
        googleAuth.state
    ) { state, profile, auth ->
        state.copy(userProfile = profile, googleAuth = auth)
    }.stateIn(guardedScope, started, PairingUiState())

    private var countdownJob: Job? = null
    private var copyFeedbackJob: Job? = null
    private var generateJob: Job? = null

    init {
        viewModelScope.launch(backgroundGuard) {
            // Every unpaired device gets its own fresh code once the session check found no space ...
            restorer.isRestored.first { it }
            if (restorer.lostAccess) {
                _effects.send(CoupleEffect.Toast(UiText.Resource(R.string.session_access_lost), isLong = true))
                restorer.acknowledgeLostAccess()
            }
            // A stored space that could not be read offline is still waiting for its listener; a fresh code
            // would replace that listener and leave the device unpaired once the network returns
            val hasStoredSpace = !sessionStore.activeCoupleIdFlow.first().isNullOrBlank()
            if (repository.currentSpace.value == null && !hasStoredSpace) refreshPairingCode()

            // ... and again after leaving a space, because the old code points at the space just left
            var wasPaired = repository.currentSpace.value != null
            repository.currentSpace.collect { space ->
                if (wasPaired && space == null) {
                    _state.update { it.copy(enteredCode = "", isLoading = false, errorMessage = null) }
                    refreshPairingCode()
                }
                wasPaired = space != null
            }
        }
    }

    fun onEvent(event: PairingEvent) {
        when (event) {
            is PairingEvent.SelectTab -> _state.update { it.copy(selectedTab = event.tab, errorMessage = null) }
            is PairingEvent.EnteredCodeChanged -> {
                val cleaned = event.rawInput.filter { it in '0'..'9' }.take(6)
                _state.update { it.copy(enteredCode = cleaned, errorMessage = null) }
            }
            PairingEvent.CodeCopied -> {
                onCodeCopied()
                onCodeShared()
            }
            PairingEvent.CodeShared -> onCodeShared()
            PairingEvent.GenerateNewCode -> onGenerateNewCode()
            PairingEvent.Connect -> onConnect()
            PairingEvent.OpenDemoSpace -> onOpenDemoSpace()
            PairingEvent.RecheckCodeAfterSpaceFull ->
                _state.update { it.copy(showSpaceFullDialog = false, enteredCode = "", errorMessage = null) }
            PairingEvent.CreateOwnSpaceAfterSpaceFull -> {
                _state.update {
                    it.copy(showSpaceFullDialog = false, enteredCode = "", selectedTab = PairingTab.CREATE, errorMessage = null)
                }
                onGenerateNewCode()
            }
            is PairingEvent.SignInWithGoogle -> onSignInWithGoogle(event.activityContext)
            PairingEvent.SignOutGoogle -> viewModelScope.launch(backgroundGuard) { googleAuth.signOut() }
            PairingEvent.ClearGoogleAuthError -> googleAuth.clearError()
        }
    }

    /** Creates a fresh random code and listens on its space; every unpaired device gets its own. */
    private suspend fun refreshPairingCode() {
        val newCode = repository.generateNewPairingCode()
        _state.update {
            it.copy(generatedCode = newCode.code, countdownSeconds = newCode.totalValidSeconds, errorMessage = null)
        }
        startCountdownTimer()
    }

    private fun onGenerateNewCode() {
        // Only the latest tap counts: two overlapping draws could show one code while listening on another
        generateJob?.cancel()
        generateJob = viewModelScope.launch(backgroundGuard) {
            sessionStore.savePartnerRole(PartnerRole.PARTNER_1)
            refreshPairingCode()
        }
    }

    /** The code is already in the clipboard; only the "copied" feedback is shown for a moment. */
    private fun onCodeCopied() {
        _state.update { it.copy(isCopied = true) }
        copyFeedbackJob?.cancel()
        copyFeedbackJob = viewModelScope.launch(backgroundGuard) {
            delay(COPY_FEEDBACK_MILLIS)
            _state.update { it.copy(isCopied = false) }
        }
    }

    private fun onCodeShared() {
        val code = _state.value.generatedCode.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch(backgroundGuard) { repository.onPairingCodeShared(code) }
    }

    private fun connectErrorText(error: Throwable): UiText = when (error) {
        is InvalidPairingCodeException -> UiText.Resource(R.string.pairing_code_invalid)
        is SpaceUnavailableException -> UiText.Resource(R.string.space_unavailable)
        else -> UiText.Resource(R.string.pairing_connection_failed)
    }

    private fun onConnect() {
        val codeToTest = _state.value.enteredCode
        if (codeToTest.length != 6) {
            _state.update { it.copy(errorMessage = UiText.Resource(R.string.pairing_code_incomplete)) }
            return
        }

        // Set synchronously so a second trigger (paste plus Done) cannot start a second connect
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(backgroundGuard) {
            val result = repository.connectWithCode(codeToTest)
            val error = result.exceptionOrNull()
            when {
                error is SpaceFullException -> _state.update { it.copy(isLoading = false, showSpaceFullDialog = true) }
                error != null -> _state.update { it.copy(isLoading = false, errorMessage = connectErrorText(error)) }
                else -> {
                    val space = result.getOrNull() ?: return@launch
                    sessionStore.saveActiveCoupleId(space.id)
                    sessionStore.savePartnerRole(PartnerRole.PARTNER_2)
                    _state.update { it.copy(isLoading = false, errorMessage = null) }
                }
            }
        }
    }

    private fun onOpenDemoSpace() {
        viewModelScope.launch(backgroundGuard) {
            _state.update { it.copy(isLoading = true) }
            sessionStore.savePartnerRole(PartnerRole.PARTNER_1)
            val demoSpace = repository.openDemoSpace()
            sessionStore.saveActiveCoupleId(demoSpace.id)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun onSignInWithGoogle(activityContext: Context) {
        viewModelScope.launch(backgroundGuard) {
            googleAuth.signIn(activityContext)?.let { _effects.send(CoupleEffect.Toast(it, isLong = true)) }
        }
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch(backgroundGuard) {
            while (_state.value.countdownSeconds > 0) {
                delay(1000)
                _state.update { state ->
                    if (state.countdownSeconds > 0) state.copy(countdownSeconds = state.countdownSeconds - 1) else state
                }
            }
        }
    }

    private companion object {
        const val COPY_FEEDBACK_MILLIS = 2_500L
    }
}
