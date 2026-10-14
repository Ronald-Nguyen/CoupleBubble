package com.aistudio.couplebubble.qxztrw.ui.screens.settings

import android.content.Context
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import java.time.LocalDate

sealed interface SettingsEvent {
    data object Open : SettingsEvent
    data object Close : SettingsEvent

    data class ShowEditNamesDialog(val show: Boolean) : SettingsEvent
    data class SavePartnerDetails(
        val partner1Name: String,
        val partner2Name: String,
        val partner1ColorHex: String,
        val partner2ColorHex: String
    ) : SettingsEvent
    data object SwapPartnerRoles : SettingsEvent

    data class ShowMilestoneSettingsDialog(val show: Boolean) : SettingsEvent
    data class ShowAddCustomMilestoneDialog(val show: Boolean) : SettingsEvent
    data class ToggleMilestoneKind(val kind: MilestoneKind, val enabled: Boolean) : SettingsEvent
    data class AddCustomMilestone(val title: String, val date: LocalDate, val recurringYearly: Boolean = false) : SettingsEvent
    data class SetCustomMilestoneEnabled(val milestoneId: String, val enabled: Boolean) : SettingsEvent
    data class ShowDeleteCustomMilestoneDialog(val milestone: CustomMilestone?) : SettingsEvent
    data class DeleteCustomMilestone(val milestoneId: String) : SettingsEvent
    data class SetMomentMilestoneEnabled(val memoryId: String, val enabled: Boolean) : SettingsEvent

    data class ShowGoogleBackupDialog(val show: Boolean) : SettingsEvent
    data class SignInWithGoogle(val activityContext: Context) : SettingsEvent

    data class ShowDisconnectDialog(val show: Boolean) : SettingsEvent
    data object ConfirmDisconnect : SettingsEvent

    data class ShowPrivacySummaryDialog(val show: Boolean) : SettingsEvent
}
