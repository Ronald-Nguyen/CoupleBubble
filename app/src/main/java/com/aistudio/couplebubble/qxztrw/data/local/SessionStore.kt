package com.aistudio.couplebubble.qxztrw.data.local

import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import kotlinx.coroutines.flow.Flow

/** Device-local session and display settings; survives app updates and is restored by Auto Backup. */
interface SessionStore {
    val activeCoupleIdFlow: Flow<String?>
    suspend fun saveActiveCoupleId(coupleId: String)

    /** Stored partner role: "1" or "2" (legacy installs: "A" or "B"), read through [PartnerRole]. */
    val partnerRoleFlow: Flow<String?>
    suspend fun savePartnerRole(role: PartnerRole)

    /** Device-only counter settings; they survive [clearSession]. */
    val counterPreferencesFlow: Flow<CounterPreferences>
    suspend fun saveCounterDisplayMode(mode: CounterDisplayMode)
    suspend fun saveEnabledMilestoneKinds(kinds: Set<MilestoneKind>)

    /** Key of the last milestone a notification was shown for, so each one notifies only once per device. */
    val lastNotifiedMilestoneFlow: Flow<String?>
    suspend fun saveLastNotifiedMilestone(key: String)

    /** Forgets the active space and role, keeping the device-only settings. */
    suspend fun clearSession()
}
