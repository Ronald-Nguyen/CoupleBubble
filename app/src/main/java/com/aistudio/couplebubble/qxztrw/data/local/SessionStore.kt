package com.aistudio.couplebubble.qxztrw.data.local

import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Device-local session and display settings; survives app updates and is restored by Auto Backup. */
interface SessionStore {
    val activeCoupleIdFlow: Flow<String?>
    suspend fun saveActiveCoupleId(coupleId: String)

    /** Stored partner role: "1" or "2" (legacy installs: "A" or "B"), read through [PartnerRole]. */
    val partnerRoleFlow: Flow<String?>
    suspend fun savePartnerRole(role: PartnerRole)

    /** Whether the onboarding has been completed or skipped on this device; survives [clearSession]. */
    val onboardingCompletedFlow: Flow<Boolean>
    suspend fun saveOnboardingCompleted(completed: Boolean = true)

    /** Device-only counter settings; they survive [clearSession]. */
    val counterPreferencesFlow: Flow<CounterPreferences>
    suspend fun saveCounterDisplayMode(mode: CounterDisplayMode)
    suspend fun saveEnabledMilestoneKinds(kinds: Set<MilestoneKind>)

    /** Key of the last milestone a notification was shown for, so each one notifies only once per device. */
    val lastNotifiedMilestoneFlow: Flow<String?>
    suspend fun saveLastNotifiedMilestone(key: String)

    /** Device-local timestamp (epoch millis) when the notes tab was last seen for [coupleId]. */
    fun notesLastSeenFlow(coupleId: String): Flow<Long>
    suspend fun saveNotesLastSeen(coupleId: String, timestamp: Long)

    /** Forgets the active space and role, keeping the device-only settings. */
    suspend fun clearSession()
}

/**
 * The stored role for display decisions; `null` until DataStore answers, so a not yet loaded preference never
 * decides which photo is editable.
 */
val SessionStore.displayRoleFlow: Flow<PartnerRole?>
    get() = partnerRoleFlow.map<String?, PartnerRole?> { PartnerRole.displayFallback(it) }.onStart { emit(null) }
