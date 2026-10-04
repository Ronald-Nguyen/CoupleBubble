package com.aistudio.couplebubble.qxztrw.ui

import android.content.Context
import android.net.Uri
import com.aistudio.couplebubble.qxztrw.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import java.time.LocalDate

/** Everything the pairing screen can ask the ViewModel to do. */
sealed interface PairingEvent {
    data class SelectTab(val tab: PairingTab) : PairingEvent
    data class EnteredCodeChanged(val rawInput: String) : PairingEvent

    /** The code is already in the clipboard; the ViewModel only shows the "copied" feedback. */
    data object CodeCopied : PairingEvent
    data object GenerateNewCode : PairingEvent
    data object Connect : PairingEvent
    data object OpenDemoSpace : PairingEvent
    data class SaveSpaceSetup(val partner1Name: String, val partner2Name: String, val date: LocalDate) : PairingEvent
    data object DismissSetupDialog : PairingEvent
    data object RecheckCodeAfterSpaceFull : PairingEvent
    data object CreateOwnSpaceAfterSpaceFull : PairingEvent

    /** Credential Manager shows its account picker on top of [activityContext]. */
    data class SignInWithGoogle(val activityContext: Context) : PairingEvent
    data object SignOutGoogle : PairingEvent
}

/** Everything the "Wir" tab (dashboard) can ask the ViewModel to do. */
sealed interface DashboardEvent {
    data class SetMenuExpanded(val expanded: Boolean) : DashboardEvent
    data class ShowDisconnectDialog(val show: Boolean) : DashboardEvent
    data object ConfirmDisconnect : DashboardEvent
    data class ShowEditNamesDialog(val show: Boolean) : DashboardEvent
    data class UpdatePartnerNames(val partner1Name: String, val partner2Name: String) : DashboardEvent
    data class UpdatePartnerColor(val colorHex: String, val isPartner1: Boolean) : DashboardEvent
    data object SwapPartnerRoles : DashboardEvent
    data class UploadProfilePhoto(val isPartner1: Boolean, val bytes: ByteArray) : DashboardEvent
    data class ShowAddMemoryDialog(val show: Boolean) : DashboardEvent
    data class AddMemory(
        val title: String,
        val date: LocalDate,
        val note: String,
        val image1Uri: Uri?,
        val image2Uri: Uri?
    ) : DashboardEvent
    data class ShowEditMemoryDialog(val memory: Memory?) : DashboardEvent
    data class UpdateMemory(val memory: Memory, val image1Uri: Uri?, val image2Uri: Uri?) : DashboardEvent
    data class ShowDeleteMemoryDialog(val memory: Memory?) : DashboardEvent
    data class DeleteMemory(val memoryId: String) : DashboardEvent
    data class SaveToGallery(val imageUrls: List<String>) : DashboardEvent
    data class ShowGoogleBackupDialog(val show: Boolean) : DashboardEvent
    data class SignInWithGoogle(val activityContext: Context) : DashboardEvent
    data object SignOutGoogle : DashboardEvent
    data class SaveSpaceSetup(val partner1Name: String, val partner2Name: String, val date: LocalDate) : DashboardEvent
    data class SelectCounterDisplayMode(val mode: CounterDisplayMode) : DashboardEvent
    data class ShowMilestoneSettingsDialog(val show: Boolean) : DashboardEvent
    data class ShowAddCustomMilestoneDialog(val show: Boolean) : DashboardEvent
    data class ToggleMilestoneKind(val kind: MilestoneKind, val enabled: Boolean) : DashboardEvent
    data class AddCustomMilestone(val title: String, val date: LocalDate) : DashboardEvent
    data class DeleteCustomMilestone(val milestoneId: String) : DashboardEvent
}

/** One-shot feedback from the ViewModel; the host shows it once and plays haptics where noted. */
sealed interface CoupleEffect {
    data class Toast(val text: UiText, val isLong: Boolean = false) : CoupleEffect
    data class Snackbar(val text: UiText) : CoupleEffect

    /** The role swap went through: long-press haptic plus a snackbar. */
    data object PartnersSwapped : CoupleEffect

    /** Photos were exported to the gallery: long-press haptic plus a toast. */
    data class SavedToGallery(val photoCount: Int) : CoupleEffect
}
