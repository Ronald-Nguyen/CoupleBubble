package com.aistudio.couplebubble.qxztrw.data.fake

import com.aistudio.couplebubble.qxztrw.data.network.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleSignInClient
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import com.aistudio.couplebubble.qxztrw.widget.WidgetSnapshot
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SessionStore] with the same semantics as the DataStore one. */
class FakeSessionStore(initialOnboardingCompleted: Boolean = false) : SessionStore {
    override val activeCoupleIdFlow = MutableStateFlow<String?>(null)
    override val partnerRoleFlow = MutableStateFlow<String?>(null)
    override val onboardingCompletedFlow = MutableStateFlow(initialOnboardingCompleted)
    override val counterPreferencesFlow = MutableStateFlow(CounterPreferences())
    override val themeModeFlow = MutableStateFlow(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.SYSTEM)
    override val notificationPreferencesFlow = MutableStateFlow(com.aistudio.couplebubble.qxztrw.domain.model.NotificationPreferences())
    override val lastNotifiedMilestoneFlow = MutableStateFlow<String?>(null)
    override val dismissedGestureHintsFlow = MutableStateFlow<Set<com.aistudio.couplebubble.qxztrw.domain.model.GestureHint>>(emptySet())

    override suspend fun dismissGestureHint(hint: com.aistudio.couplebubble.qxztrw.domain.model.GestureHint) {
        dismissedGestureHintsFlow.update { it + hint }
    }

    override suspend fun saveActiveCoupleId(coupleId: String) {
        activeCoupleIdFlow.value = coupleId
    }

    override suspend fun savePartnerRole(role: PartnerRole) {
        partnerRoleFlow.value = role.storedValue
    }

    override suspend fun saveOnboardingCompleted(completed: Boolean) {
        onboardingCompletedFlow.value = completed
    }

    override suspend fun saveCounterDisplayMode(mode: CounterDisplayMode) {
        counterPreferencesFlow.update { it.copy(displayMode = mode) }
    }

    override suspend fun saveEnabledMilestoneKinds(kinds: Set<MilestoneKind>) {
        counterPreferencesFlow.update { it.copy(enabledMilestoneKinds = kinds) }
    }

    override suspend fun saveLastNotifiedMilestone(key: String) {
        lastNotifiedMilestoneFlow.value = key
    }

    override suspend fun saveThemeMode(mode: com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode) {
        themeModeFlow.value = mode
    }

    override suspend fun savePartnerMomentsNotificationEnabled(enabled: Boolean) {
        notificationPreferencesFlow.update { it.copy(partnerMomentsEnabled = enabled) }
    }

    override suspend fun saveMilestonesNotificationEnabled(enabled: Boolean) {
        notificationPreferencesFlow.update { it.copy(milestonesEnabled = enabled) }
    }

    val notesLastSeenFlows = mutableMapOf<String, MutableStateFlow<Long>>()

    override fun notesLastSeenFlow(coupleId: String): kotlinx.coroutines.flow.Flow<Long> =
        notesLastSeenFlows.getOrPut(coupleId) { MutableStateFlow(0L) }

    override suspend fun saveNotesLastSeen(coupleId: String, timestamp: Long) {
        notesLastSeenFlows.getOrPut(coupleId) { MutableStateFlow(0L) }.value = timestamp
    }

    override suspend fun clearSession() {
        activeCoupleIdFlow.value = null
        partnerRoleFlow.value = null
    }
}

/** Every picked photo "compresses" to [compressedBytes]; local copies get a `file://` URL without touching disk. */
class FakeLocalImageStore(var compressedBytes: ByteArray? = byteArrayOf(1, 2, 3)) : LocalImageStore {
    val galleryExports = mutableListOf<String>()

    override fun saveMemoryImage(memoryId: String, slot: String, bytes: ByteArray): String =
        "file:///local/memory_${memoryId}_$slot.jpg"

    override fun saveProfilePhoto(coupleId: String, ownerUid: String?, bytes: ByteArray): String =
        "file:///local/profile_${coupleId}_$ownerUid.jpg"

    override suspend fun compress(uri: Uri): ByteArray? = compressedBytes

    override suspend fun loadSampledBitmap(uri: Uri, maxDimension: Int): Bitmap? = null

    override suspend fun saveToGallery(imageUrl: String): Result<Unit> {
        galleryExports += imageUrl
        return Result.success(Unit)
    }
}

class RecordingMomentNotifier : MomentNotifier {
    val moments = mutableListOf<String>()
    val milestones = mutableListOf<Milestone>()

    override fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String) {
        moments += memoryId
    }

    override fun notifyMilestoneReached(milestone: Milestone) {
        milestones += milestone
    }
}

class RecordingWidgetPublisher : WidgetPublisher {
    val published = mutableListOf<WidgetSnapshot?>()

    override suspend fun publish(snapshot: WidgetSnapshot?) {
        published += snapshot
    }
}

/** No Google account on the test device: every sign-in fails like a missing credential would. */
class FakeGoogleSignInClient : GoogleSignInClient {
    var signOutCount = 0
        private set

    override suspend fun signIn(activityContext: Context): Result<FirebaseUser> =
        Result.failure(IllegalStateException("No Google account"))

    override suspend fun signOut() {
        signOutCount++
    }
}

/** Online until a test sets [online] to `false`. */
class FakeConnectivityObserver : ConnectivityObserver {
    val online = MutableStateFlow(true)

    override val isOnline: Flow<Boolean> = online
}
