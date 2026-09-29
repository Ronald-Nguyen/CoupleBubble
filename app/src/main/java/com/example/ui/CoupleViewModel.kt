package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.CoupleSpace
import com.example.model.RelationshipDateCalculator
import com.example.model.RelationshipMetrics
import com.example.repository.CoupleRepository
import com.example.repository.MockCoupleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val isCopied: Boolean = false
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

data class DashboardUiState(
    val space: CoupleSpace,
    val metrics: RelationshipMetrics,
    val isMenuExpanded: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val loveNoteText: String = "Du bist mein liebster Gedanke am Morgen und meine schönste Ruhe am Abend. Schön, dass wir diesen Raum teilen."
)

sealed interface CoupleMainState {
    data class Unpaired(val state: PairingUiState) : CoupleMainState
    data class Paired(val state: DashboardUiState) : CoupleMainState
}

class CoupleViewModel(
    private val repository: CoupleRepository = MockCoupleRepository()
) : ViewModel() {

    private val _pairingState = MutableStateFlow(PairingUiState())
    private val _dashboardExtras = MutableStateFlow(
        Pair(false, false) // isMenuExpanded, showDisconnectDialog
    )

    private var countdownJob: Job? = null
    private var copyFeedbackJob: Job? = null

    init {
        startCountdownTimer()
    }

    val uiState: StateFlow<CoupleMainState> = combine(
        repository.currentSpace,
        _pairingState,
        _dashboardExtras
    ) { space, pairing, extras ->
        if (space != null) {
            val metrics = RelationshipDateCalculator.calculate(
                year = space.anniversaryYear,
                month = space.anniversaryMonth,
                day = space.anniversaryDay
            )
            CoupleMainState.Paired(
                DashboardUiState(
                    space = space,
                    metrics = metrics,
                    isMenuExpanded = extras.first,
                    showDisconnectDialog = extras.second
                )
            )
        } else {
            CoupleMainState.Unpaired(pairing)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CoupleMainState.Unpaired(_pairingState.value)
    )

    fun onTabSelected(tab: PairingTab) {
        _pairingState.value = _pairingState.value.copy(
            selectedTab = tab,
            errorMessage = null
        )
    }

    fun onEnteredCodeChanged(rawInput: String) {
        // Strip non-alphanumeric, limit to 6 chars, uppercase
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
                _pairingState.value = _pairingState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    fun onOpenDemoSpace() {
        viewModelScope.launch {
            _pairingState.value = _pairingState.value.copy(isLoading = true)
            repository.openDemoSpace()
            _pairingState.value = _pairingState.value.copy(isLoading = false)
        }
    }

    fun setMenuExpanded(expanded: Boolean) {
        _dashboardExtras.value = _dashboardExtras.value.copy(first = expanded)
    }

    fun setShowDisconnectDialog(show: Boolean) {
        _dashboardExtras.value = _dashboardExtras.value.copy(
            first = false,
            second = show
        )
    }

    fun onConfirmDisconnect() {
        viewModelScope.launch {
            _dashboardExtras.value = Pair(false, false)
            repository.disconnect()
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
            while (true) {
                delay(1000)
                val current = _pairingState.value.countdownSeconds
                if (current > 0) {
                    _pairingState.value = _pairingState.value.copy(countdownSeconds = current - 1)
                } else {
                    onGenerateNewCode()
                    break
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
