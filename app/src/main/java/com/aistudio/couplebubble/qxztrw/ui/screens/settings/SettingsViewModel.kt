package com.aistudio.couplebubble.qxztrw.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.BuildConfig
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.data.local.displayRoleFlow
import com.aistudio.couplebubble.qxztrw.data.network.ConnectivityObserver
import com.aistudio.couplebubble.qxztrw.domain.error.PartnerNotConnectedException
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.InputField
import com.aistudio.couplebubble.qxztrw.domain.model.InputRules
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.domain.model.canSwapPartners
import com.aistudio.couplebubble.qxztrw.domain.model.editableRole
import com.aistudio.couplebubble.qxztrw.domain.model.hasBoundMembers
import com.aistudio.couplebubble.qxztrw.domain.model.isPartner1
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
import com.aistudio.couplebubble.qxztrw.domain.session.SpaceContent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.GoogleAuthController
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: CoupleRepository,
    private val sessionStore: SessionStore,
    private val googleAuth: GoogleAuthController,
    spaceContent: SpaceContent,
    private val reunionRepository: ReunionRepository,
    connectivity: ConnectivityObserver,
    private val widgetPublisher: WidgetPublisher,
    private val appScope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val _isOpen = MutableStateFlow(false)
    private val _dialog = MutableStateFlow<SettingsDialog>(SettingsDialog.None)
    private val _isSwappingRoles = MutableStateFlow(false)
    private val _completedRoleSwaps = MutableStateFlow(0)
    private val _counterPreferences = MutableStateFlow(CounterPreferences())

    private val _effects = Channel<CoupleEffect>(Channel.BUFFERED)
    val effects: Flow<CoupleEffect> = _effects.receiveAsFlow()

    val isOnline: StateFlow<Boolean> = connectivity.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch {
            sessionStore.counterPreferencesFlow.collect { _counterPreferences.value = it }
        }
    }

    private data class LocalState(
        val isOpen: Boolean,
        val dialog: SettingsDialog,
        val isSwappingRoles: Boolean,
        val completedRoleSwaps: Int
    )

    private data class Session(
        val space: CoupleSpace?,
        val userProfile: UserProfile?,
        val role: PartnerRole?
    )

    private data class Content(
        val counterPreferences: CounterPreferences,
        val customMilestones: List<CustomMilestone>,
        val memories: List<Memory>
    )

    private val localFlow = combine(
        _isOpen,
        _dialog,
        _isSwappingRoles,
        _completedRoleSwaps,
        ::LocalState
    )

    private val sessionFlow = combine(
        repository.currentSpace,
        repository.currentUserProfile,
        sessionStore.displayRoleFlow,
        ::Session
    )

    private val contentFlow = combine(
        _counterPreferences,
        spaceContent.customMilestones,
        spaceContent.memories,
        ::Content
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        localFlow,
        sessionFlow,
        googleAuth.state,
        contentFlow
    ) { local, session, auth, content ->
        val space = session.space
        val uid = session.userProfile?.uid
        val isPartner1 = if (space != null) space.isPartner1(uid, session.role ?: PartnerRole.PARTNER_1) else true
        val currentRole = space?.editableRole(uid, session.role)

        SettingsUiState(
            isOpen = local.isOpen,
            space = space,
            userProfile = session.userProfile,
            isCurrentUserPartner1 = isPartner1,
            currentUserRole = currentRole,
            isSwappingRoles = local.isSwappingRoles,
            completedRoleSwaps = local.completedRoleSwaps,
            counterPreferences = content.counterPreferences,
            customMilestones = content.customMilestones,
            memories = content.memories,
            googleAuth = auth,
            dialog = local.dialog,
            appVersion = BuildConfig.VERSION_NAME
        )
    }.stateIn(viewModelScope, started, SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Open -> _isOpen.value = true
            SettingsEvent.Close -> {
                _isOpen.value = false
                _dialog.value = SettingsDialog.None
            }
            is SettingsEvent.ShowEditNamesDialog -> toggleDialog<SettingsDialog.EditNames>(SettingsDialog.EditNames, event.show)
            is SettingsEvent.SavePartnerDetails -> onSavePartnerDetails(event)
            SettingsEvent.SwapPartnerRoles -> swapPartnerRoles()
            is SettingsEvent.ShowMilestoneSettingsDialog -> toggleDialog<SettingsDialog.MilestoneSettings>(SettingsDialog.MilestoneSettings(), event.show)
            is SettingsEvent.ShowAddCustomMilestoneDialog -> setAddingCustomMilestone(event.show)
            is SettingsEvent.ToggleMilestoneKind -> onMilestoneKindToggled(event.kind, event.enabled)
            is SettingsEvent.AddCustomMilestone -> onAddCustomMilestone(event.title, event.date, event.recurringYearly)
            is SettingsEvent.SetCustomMilestoneEnabled -> onCustomMilestoneEnabledChanged(event.milestoneId, event.enabled)
            is SettingsEvent.ShowDeleteCustomMilestoneDialog -> _dialog.value = SettingsDialog.MilestoneSettings(deletingCustom = event.milestone)
            is SettingsEvent.DeleteCustomMilestone -> onDeleteCustomMilestone(event.milestoneId)
            is SettingsEvent.SetMomentMilestoneEnabled -> onMomentMilestoneEnabledChanged(event.memoryId, event.enabled)
            is SettingsEvent.ShowGoogleBackupDialog -> toggleDialog<SettingsDialog.GoogleBackup>(SettingsDialog.GoogleBackup, event.show)
            is SettingsEvent.SignInWithGoogle -> onSignInWithGoogle(event.activityContext)
            is SettingsEvent.ShowDisconnectDialog -> toggleDialog<SettingsDialog.Disconnect>(SettingsDialog.Disconnect, event.show)
            SettingsEvent.ConfirmDisconnect -> onConfirmDisconnect()
            is SettingsEvent.ShowPrivacySummaryDialog -> toggleDialog<SettingsDialog.PrivacySummary>(SettingsDialog.PrivacySummary, event.show)
        }
    }

    private inline fun <reified T : SettingsDialog> toggleDialog(dialog: SettingsDialog, show: Boolean) {
        _dialog.update { current ->
            when {
                show -> dialog
                current is T -> SettingsDialog.None
                else -> current
            }
        }
    }

    private fun setAddingCustomMilestone(adding: Boolean) {
        _dialog.update { SettingsDialog.MilestoneSettings(addingCustom = adding) }
    }

    private val currentSpace: CoupleSpace? get() = repository.currentSpace.value

    private fun onSavePartnerDetails(event: SettingsEvent.SavePartnerDetails) {
        val trimmed1 = event.partner1Name.trim()
        val trimmed2 = event.partner2Name.trim()
        if (!InputRules.isValid(InputField.NICKNAME, trimmed1) || !InputRules.isValid(InputField.NICKNAME, trimmed2)) return
        val space = currentSpace ?: return

        viewModelScope.launch {
            val results = buildList {
                add(repository.updatePartnerNames(space.id, trimmed1, trimmed2))
                if (event.partner1ColorHex != space.partner1ColorHex) {
                    add(repository.updatePartnerColor(space.id, isPartner1 = true, colorHex = event.partner1ColorHex))
                }
                if (event.partner2ColorHex != space.partner2ColorHex) {
                    add(repository.updatePartnerColor(space.id, isPartner1 = false, colorHex = event.partner2ColorHex))
                }
            }
            val text = if (results.all { it.isSuccess }) {
                if (isOnline.value) UiText.Resource(R.string.names_and_colors_updated) else UiText.Resource(R.string.saved_offline)
            } else {
                UiText.Resource(R.string.names_save_failed)
            }
            _effects.send(CoupleEffect.Snackbar(text))
        }
    }

    private fun swapPartnerRoles() {
        val space = currentSpace ?: return
        if (_isSwappingRoles.value) return
        _isSwappingRoles.value = true
        viewModelScope.launch {
            val result = try {
                repository.swapPartners(space.id)
            } finally {
                _isSwappingRoles.value = false
            }
            if (result.isSuccess) {
                if (!space.hasBoundMembers) {
                    PartnerRole.fromStored(sessionStore.partnerRoleFlow.firstOrNull())?.let {
                        sessionStore.savePartnerRole(it.swapped())
                    }
                }
                _completedRoleSwaps.update { it + 1 }
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

    private fun onMilestoneKindToggled(kind: MilestoneKind, enabled: Boolean) {
        val current = _counterPreferences.value.enabledMilestoneKinds
        val updated = if (enabled) current + kind else current - kind
        _counterPreferences.value = _counterPreferences.value.copy(enabledMilestoneKinds = updated)
        viewModelScope.launch {
            sessionStore.saveEnabledMilestoneKinds(updated)
        }
    }

    private fun onAddCustomMilestone(title: String, date: LocalDate, recurringYearly: Boolean) {
        val trimmed = title.trim()
        if (!InputRules.isValid(InputField.MILESTONE_TITLE, trimmed)) return
        val spaceId = currentSpace?.id ?: return
        val milestone = CustomMilestone(
            title = trimmed,
            date = date,
            recurringYearly = recurringYearly
        )
        viewModelScope.launch {
            _dialog.value = SettingsDialog.MilestoneSettings()
            val result = repository.addCustomMilestone(spaceId, milestone)
            if (result.isFailure) {
                _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.milestone_save_failed)))
            }
        }
    }

    private fun onCustomMilestoneEnabledChanged(milestoneId: String, enabled: Boolean) {
        val spaceId = currentSpace?.id ?: return
        viewModelScope.launch {
            val result = repository.setCustomMilestoneEnabled(spaceId, milestoneId, enabled)
            if (result.isFailure) {
                _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.milestone_save_failed)))
            }
        }
    }

    private fun onDeleteCustomMilestone(milestoneId: String) {
        val spaceId = currentSpace?.id ?: return
        viewModelScope.launch {
            _dialog.value = SettingsDialog.MilestoneSettings()
            val result = repository.deleteCustomMilestone(spaceId, milestoneId)
            if (result.isFailure) {
                _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.milestone_delete_failed)))
            }
        }
    }

    private fun onMomentMilestoneEnabledChanged(memoryId: String, enabled: Boolean) {
        val spaceId = currentSpace?.id ?: return
        viewModelScope.launch {
            val result = repository.setMemoryCountsAsMilestone(spaceId, memoryId, enabled)
            if (result.isFailure) {
                _effects.send(CoupleEffect.Snackbar(UiText.Resource(R.string.milestone_save_failed)))
            }
        }
    }

    private fun onSignInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            googleAuth.signIn(activityContext)?.let { _effects.send(CoupleEffect.Toast(it, isLong = true)) }
        }
    }

    private fun onConfirmDisconnect() {
        val space = currentSpace
        viewModelScope.launch {
            _dialog.value = SettingsDialog.None
            _isOpen.value = false
            val uid = repository.currentUserProfile.value?.uid
            if (space != null && uid != null) reunionRepository.removeOwnPlace(space.id, uid)
            if (space != null) repository.disconnectCouple(space.id) else repository.disconnect()
            sessionStore.clearSession()
            widgetPublisher.publish(null)
        }
    }
}
