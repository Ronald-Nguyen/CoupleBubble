package com.aistudio.couplebubble.qxztrw.ui.screens.home

import androidx.compose.runtime.Immutable
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlaceSource
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionStatus
import com.aistudio.couplebubble.qxztrw.domain.model.SpaceDefaults
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile

/** Upload progress of one memory photo; absent from the map once nothing needs to be shown. */
enum class PhotoSyncState {
    UPLOADING,
    SYNCED
}

data class PhotoSlotKey(val memoryId: String, val isPartner1: Boolean)

/** Tabs of the bottom navigation once a couple is paired. */
enum class MainTab {
    US,
    NOTES
}

enum class PairingTab {
    CREATE,
    ENTER
}

/** Google sign-in progress, shared by the pairing screen and the dashboard's backup dialog. */
@Immutable
data class GoogleAuthState(
    val isLoading: Boolean = false,
    val error: UiText? = null
)

@Immutable
data class PairingUiState(
    val selectedTab: PairingTab = PairingTab.CREATE,
    /** Empty until the ViewModel has generated this device's own random code. */
    val generatedCode: String = "",
    val countdownSeconds: Int = 0,
    val enteredCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isCopied: Boolean = false,
    val userProfile: UserProfile? = null,
    val showSpaceFullDialog: Boolean = false,
    val googleAuth: GoogleAuthState = GoogleAuthState()
) {
    val isInputReady: Boolean
        get() = enteredCode.replace("-", "").trim().length == 6
}

/** The one dialog of the paired home that is open; a sealed type, so two dialogs can never be open at once. */
sealed interface DashboardDialog {
    data object None : DashboardDialog
    data object Disconnect : DashboardDialog
    data object EditNames : DashboardDialog
    data object AddMemory : DashboardDialog
    data class EditMemory(val memory: Memory) : DashboardDialog
    data class DeleteMemory(val memory: Memory) : DashboardDialog
    data object GoogleBackup : DashboardDialog

    /** The milestone settings, with the "add custom milestone" dialog on top while [addingCustom]. */
    data class MilestoneSettings(val addingCustom: Boolean = false) : DashboardDialog

    /** Planning or editing the reunion countdown. */
    data object Reunion : DashboardDialog
}

/** Which tab, menu and dialog of the paired home are open, and which long-running actions are busy. */
@Immutable
data class DashboardDialogState(
    val selectedTab: MainTab = MainTab.US,
    val isMenuExpanded: Boolean = false,
    val dialog: DashboardDialog = DashboardDialog.None,
    val isSavingSpaceSetup: Boolean = false,
    val isUploadingProfilePhoto: Boolean = false,
    val isSwappingRoles: Boolean = false,
    val isSavingToGallery: Boolean = false
)

@Immutable
data class DashboardUiState(
    val space: CoupleSpace,
    val metrics: RelationshipMetrics,
    val memories: List<Memory> = emptyList(),
    val userProfile: UserProfile? = null,
    val isCurrentUserPartner1: Boolean = true,
    /** Slot whose profile photo this user may change; `null` while their UID is not bound to the space. */
    val currentUserRole: PartnerRole? = PartnerRole.PARTNER_1,
    val dialogs: DashboardDialogState = DashboardDialogState(),
    val googleAuth: GoogleAuthState = GoogleAuthState(),
    val photoSyncStates: Map<PhotoSlotKey, PhotoSyncState> = emptyMap(),
    val counterPreferences: CounterPreferences = CounterPreferences(),
    val customMilestones: List<CustomMilestone> = emptyList(),
    /** Memory id → milestone the moment was linked to. */
    val memoryMilestones: Map<String, Milestone> = emptyMap(),
    /** Reached milestones offered in the memory dialog's dropdown, newest first. */
    val milestoneOptions: List<Milestone> = emptyList(),
    /** The planned reunion as seen by this user; `null` while nothing is planned or it is over. */
    val reunion: ReunionUiState? = null
) {
    /** A fresh space still carries placeholder names and date; both partners are asked until one of them saves. */
    val needsSpaceSetup: Boolean
        get() = !space.isSetupComplete

    /** The current user's own name for the setup form: the stored one, else the first name of the Google account. */
    val setupOwnName: String
        get() {
            val stored = if (isCurrentUserPartner1) space.partner1Name else space.partner2Name
            val placeholder = if (isCurrentUserPartner1) SpaceDefaults.PARTNER_1_NAME else SpaceDefaults.PARTNER_2_NAME
            return if (stored != placeholder) stored
            else userProfile?.displayName?.trim()?.substringBefore(' ').orEmpty()
        }

    /** The partner's name for the setup form; placeholders are left empty instead of suggesting "Alex"/"Sam". */
    val setupPartnerName: String
        get() {
            val stored = if (isCurrentUserPartner1) space.partner2Name else space.partner1Name
            val placeholder = if (isCurrentUserPartner1) SpaceDefaults.PARTNER_2_NAME else SpaceDefaults.PARTNER_1_NAME
            return if (stored != placeholder) stored else ""
        }
}

/** The reunion countdown from the current user's side: own and partner place are resolved by UID. */
@Immutable
data class ReunionUiState(
    val reunion: Reunion,
    val status: ReunionStatus,
    val distanceKm: Int?,
    val ownPlace: ReunionPlace?,
    val partnerPlace: ReunionPlace?,
    val ownSource: ReunionPlaceSource,
    val partnerSource: ReunionPlaceSource,
    /** The time the sources were judged at, for "vor 2 Std." labels. */
    val nowMillis: Long
)

/** Which screen the app shows: a spinner while the session is restored, pairing, or the paired home. */
enum class AppRoute {
    LOADING,
    UNPAIRED,
    PAIRED
}
