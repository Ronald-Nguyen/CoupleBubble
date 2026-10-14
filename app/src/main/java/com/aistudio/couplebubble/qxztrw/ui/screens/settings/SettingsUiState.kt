package com.aistudio.couplebubble.qxztrw.ui.screens.settings

import androidx.compose.runtime.Immutable
import com.aistudio.couplebubble.qxztrw.BuildConfig
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.GoogleAuthState

/** Dialogs of the settings screen; sealed type so two dialogs can never be open at once. */
sealed interface SettingsDialog {
    data object None : SettingsDialog
    data object EditNames : SettingsDialog
    data class MilestoneSettings(
        val addingCustom: Boolean = false,
        val deletingCustom: CustomMilestone? = null
    ) : SettingsDialog
    data object GoogleBackup : SettingsDialog
    data object Disconnect : SettingsDialog
    data object PrivacySummary : SettingsDialog
}

@Immutable
data class SettingsUiState(
    val isOpen: Boolean = false,
    val space: CoupleSpace? = null,
    val userProfile: UserProfile? = null,
    val isCurrentUserPartner1: Boolean = true,
    val currentUserRole: PartnerRole? = PartnerRole.PARTNER_1,
    val isSwappingRoles: Boolean = false,
    val completedRoleSwaps: Int = 0,
    val counterPreferences: CounterPreferences = CounterPreferences(),
    val customMilestones: List<CustomMilestone> = emptyList(),
    val memories: List<Memory> = emptyList(),
    val googleAuth: GoogleAuthState = GoogleAuthState(),
    val dialog: SettingsDialog = SettingsDialog.None,
    val appVersion: String = BuildConfig.VERSION_NAME
)
