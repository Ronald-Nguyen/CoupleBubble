package com.aistudio.couplebubble.qxztrw.ui

import com.aistudio.couplebubble.qxztrw.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.RelationshipMetrics
import com.aistudio.couplebubble.qxztrw.model.UserProfile

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
data class GoogleAuthState(
    val isLoading: Boolean = false,
    val error: UiText? = null
)

data class PairingUiState(
    val selectedTab: PairingTab = PairingTab.CREATE,
    val generatedCode: String = "482-913",
    val countdownSeconds: Int = 890,
    val enteredCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isCopied: Boolean = false,
    val userProfile: UserProfile? = null,
    val showSetupSpaceDialog: Boolean = false,
    val showSpaceFullDialog: Boolean = false,
    val pendingSpaceId: String? = null,
    val googleAuth: GoogleAuthState = GoogleAuthState()
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

/** Which tab, menu and dialogs of the paired home are open; owned by the ViewModel. */
data class DashboardDialogState(
    val selectedTab: MainTab = MainTab.US,
    val isMenuExpanded: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val showEditNamesDialog: Boolean = false,
    val showAddMemoryDialog: Boolean = false,
    val showSetupSpaceDialog: Boolean = false,
    val showGoogleBackupDialog: Boolean = false,
    val showMilestoneSettingsDialog: Boolean = false,
    val showAddCustomMilestoneDialog: Boolean = false,
    val isUploadingProfilePhoto: Boolean = false,
    val isSavingToGallery: Boolean = false,
    val memoryToEdit: Memory? = null,
    val memoryToDelete: Memory? = null
)

data class DashboardUiState(
    val space: CoupleSpace,
    val metrics: RelationshipMetrics,
    val memories: List<Memory> = emptyList(),
    val userProfile: UserProfile? = null,
    val isCurrentUserPartner1: Boolean = true,
    val dialogs: DashboardDialogState = DashboardDialogState(),
    val googleAuth: GoogleAuthState = GoogleAuthState(),
    val photoSyncStates: Map<PhotoSlotKey, PhotoSyncState> = emptyMap(),
    val counterPreferences: CounterPreferences = CounterPreferences(),
    val customMilestones: List<CustomMilestone> = emptyList()
)

sealed interface CoupleMainState {
    data object Loading : CoupleMainState
    data class Unpaired(val state: PairingUiState) : CoupleMainState
    data class Paired(val state: DashboardUiState) : CoupleMainState
}
