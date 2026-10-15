package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.nameOf
import com.aistudio.couplebubble.qxztrw.domain.model.roleOf
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import kotlinx.coroutines.flow.first

/**
 * Notifies about moments the partner adds. The first memory list of a space is only the baseline, so existing
 * moments are no news; moments without `createdBy` (older app versions) never notify.
 */
class PartnerMomentWatcher(
    private val notifier: MomentNotifier,
    private val sessionStore: SessionStore? = null,
    /** Lists arriving this long after the baseline still count as baseline: the cache snapshot comes before the server one. */
    private val settleMillis: Long = 0L,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {

    /** Space id and memory ids already seen. */
    private var known: Pair<String, Set<String>>? = null
    private var baselineAt = 0L

    /** Leaving the space forgets the baseline, so rejoining doesn't report old moments. */
    fun reset() {
        known = null
        baselineAt = 0L
    }

    suspend fun onMemories(space: CoupleSpace, memories: List<Memory>, ownUid: String?) {
        val ids = memories.mapTo(HashSet()) { it.id }
        val seen = known?.takeIf { it.first == space.id }?.second
        known = space.id to ids
        if (seen == null) baselineAt = nowMillis()
        if (seen == null || ownUid == null) return
        if (nowMillis() - baselineAt < settleMillis) return

        val notificationsEnabled = sessionStore?.notificationPreferencesFlow?.first()?.partnerMomentsEnabled ?: true
        if (!notificationsEnabled) return

        memories.filter { it.id !in seen && it.createdBy != null && it.createdBy != ownUid }.forEach { memory ->
            // An author no longer bound to the space is still "the other one"
            val authorRole = space.roleOf(memory.createdBy) ?: space.roleOf(ownUid)?.swapped() ?: PartnerRole.PARTNER_2
            notifier.notifyPartnerMoment(memory.id, space.nameOf(authorRole), memory.title)
        }
    }
}
