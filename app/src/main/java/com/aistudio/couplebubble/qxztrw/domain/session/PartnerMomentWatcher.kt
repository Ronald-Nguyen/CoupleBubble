package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.nameOf
import com.aistudio.couplebubble.qxztrw.domain.model.roleOf
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier

/**
 * Notifies about moments the partner adds. The first memory list of a space is only the baseline, so existing
 * moments are no news; moments without `createdBy` (older app versions) never notify.
 */
class PartnerMomentWatcher(private val notifier: MomentNotifier) {

    /** Space id and memory ids already seen. */
    private var known: Pair<String, Set<String>>? = null

    /** Leaving the space forgets the baseline, so rejoining doesn't report old moments. */
    fun reset() {
        known = null
    }

    fun onMemories(space: CoupleSpace, memories: List<Memory>, ownUid: String?) {
        val ids = memories.mapTo(HashSet()) { it.id }
        val seen = known?.takeIf { it.first == space.id }?.second
        known = space.id to ids
        if (seen == null || ownUid == null) return

        memories.filter { it.id !in seen && it.createdBy != null && it.createdBy != ownUid }.forEach { memory ->
            // An author no longer bound to the space is still "the other one"
            val authorRole = space.roleOf(memory.createdBy) ?: space.roleOf(ownUid)?.swapped() ?: PartnerRole.PARTNER_2
            notifier.notifyPartnerMoment(memory.id, space.nameOf(authorRole), memory.title)
        }
    }
}
