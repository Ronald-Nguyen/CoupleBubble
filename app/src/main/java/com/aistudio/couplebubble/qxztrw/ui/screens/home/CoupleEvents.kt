package com.aistudio.couplebubble.qxztrw.ui.screens.home

import android.content.Context
import android.net.Uri
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.GestureHint
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
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
    data object RecheckCodeAfterSpaceFull : PairingEvent
    data object CreateOwnSpaceAfterSpaceFull : PairingEvent

    /** Credential Manager shows its account picker on top of [activityContext]. */
    data class SignInWithGoogle(val activityContext: Context) : PairingEvent
    data object SignOutGoogle : PairingEvent
    data object ClearGoogleAuthError : PairingEvent
}

/** What the memory form did with the current user's own photo slot; the partner's slot is never sent. */
sealed interface OwnPhotoEdit {
    data object Keep : OwnPhotoEdit
    data object Remove : OwnPhotoEdit
    data class Replace(val uri: Uri) : OwnPhotoEdit
}

/** Everything the "Uns" tab (dashboard) and the bottom navigation can ask the ViewModel to do. */
sealed interface DashboardEvent {
    data class SelectMainTab(val tab: MainTab) : DashboardEvent
    data class SetMenuExpanded(val expanded: Boolean) : DashboardEvent
    data class ShowDisconnectDialog(val show: Boolean) : DashboardEvent
    data object ConfirmDisconnect : DashboardEvent
    data class ShowEditNamesDialog(val show: Boolean) : DashboardEvent

    /** "Speichern" in the names dialog: names plus both accent colors; only changed colors are written. */
    data class SavePartnerDetails(
        val partner1Name: String,
        val partner2Name: String,
        val partner1ColorHex: String,
        val partner2ColorHex: String
    ) : DashboardEvent
    data object SwapPartnerRoles : DashboardEvent
    data class UploadProfilePhoto(val bytes: ByteArray) : DashboardEvent
    data class ShowAddMemoryDialog(val show: Boolean) : DashboardEvent
    data class AddMemory(
        val title: String,
        val date: LocalDate,
        val note: String,
        val countsAsMilestone: Boolean,
        val ownImageUri: Uri?
    ) : DashboardEvent
    data class ShowEditMemoryDialog(val memory: Memory?) : DashboardEvent
    data class UpdateMemory(val memory: Memory, val ownPhoto: OwnPhotoEdit) : DashboardEvent
    /** Hides the moment right away; it is only deleted in Firestore once the undo snackbar is gone. */
    data class DeleteMemory(val memoryId: String) : DashboardEvent

    /** "Rückgängig" on the delete snackbar: the hidden moment comes back. */
    data class UndoDeleteMemory(val memoryId: String) : DashboardEvent

    /** The delete snackbar went away without undo: the moment is deleted for good. */
    data class CommitDeleteMemory(val memoryId: String) : DashboardEvent
    data class SaveToGallery(val imageUrls: List<String>) : DashboardEvent
    data class ShowGoogleBackupDialog(val show: Boolean) : DashboardEvent
    data class SignInWithGoogle(val activityContext: Context) : DashboardEvent
    data class SaveSpaceSetup(val partner1Name: String, val partner2Name: String, val date: LocalDate) : DashboardEvent
    data class SelectCounterDisplayMode(val mode: CounterDisplayMode) : DashboardEvent
    data class ShowMilestoneSettingsDialog(val show: Boolean) : DashboardEvent
    data class ShowAddCustomMilestoneDialog(val show: Boolean) : DashboardEvent
    data class ToggleMilestoneKind(val kind: MilestoneKind, val enabled: Boolean) : DashboardEvent
    data class AddCustomMilestone(val title: String, val date: LocalDate, val recurringYearly: Boolean = false) : DashboardEvent
    data class SetCustomMilestoneEnabled(val milestoneId: String, val enabled: Boolean) : DashboardEvent
    data class ShowDeleteCustomMilestoneDialog(val milestone: CustomMilestone?) : DashboardEvent
    data class DeleteCustomMilestone(val milestoneId: String) : DashboardEvent
    data class SetMomentMilestoneEnabled(val memoryId: String, val enabled: Boolean) : DashboardEvent
    data class ShowReunionDialog(val show: Boolean) : DashboardEvent

    /** [shareLocation] is only honoured while location access is granted. */
    data class SaveReunion(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val shareLocation: Boolean
    ) : DashboardEvent
    data object DeleteReunion : DashboardEvent

    /** The "Uns" tab became visible: refresh the own approximate location if it is shared and older than an hour. */
    data object RefreshReunionLocation : DashboardEvent

    data object OpenSearch : DashboardEvent
    data object CloseSearch : DashboardEvent
    data class SearchQueryChanged(val query: String) : DashboardEvent
    data class DismissGestureHint(val hint: GestureHint) : DashboardEvent
}

/** One-shot feedback from the ViewModel; the host shows it once and plays haptics where noted. */
sealed interface CoupleEffect {
    data class Toast(val text: UiText, val isLong: Boolean = false) : CoupleEffect
    /** A snackbar; with an [action] it stays longer and its result goes back to the sending ViewModel as an event. */
    data class Snackbar(val text: UiText, val action: SnackbarAction<*>? = null) : CoupleEffect

    /** The role swap went through: long-press haptic plus a snackbar. */
    data object PartnersSwapped : CoupleEffect

    /** A moment was saved by the repository: long-press haptic plus a snackbar with [text]. */
    data class MemorySaved(val text: UiText) : CoupleEffect

    /** Photos were exported to the gallery: long-press haptic plus a toast. */
    data class SavedToGallery(val photoCount: Int) : CoupleEffect
}

/**
 * The button of a snackbar plus the events sent back once the user pressed it or the snackbar went away
 * ([onDismissed] `null`: nothing to do then). [E] is the event type of the ViewModel that sent it.
 */
data class SnackbarAction<out E : Any>(
    val label: UiText,
    val onActionPerformed: E,
    val onDismissed: E?
)
