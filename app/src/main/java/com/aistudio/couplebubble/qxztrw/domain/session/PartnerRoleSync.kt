package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.hasBoundMembers
import com.aistudio.couplebubble.qxztrw.domain.model.roleOf
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository

/**
 * Keeps the stored role equal to the slot the space binds this user to (on both devices, also after the partner
 * swapped roles), and binds a signed-in user who is missing from a space with a free slot.
 */
class PartnerRoleSync(
    private val repository: SpaceRepository,
    private val sessionStore: SessionStore?
) {
    /** Space and UID for which [SpaceRepository.ensurePartnerSlot] already ran, so it is tried once only. */
    private var slotClaimAttempt: Pair<String, String>? = null

    suspend fun sync(space: CoupleSpace?, uid: String?, storedRole: PartnerRole?) {
        val active = space?.takeIf { it.isActive } ?: return
        uid ?: return
        val boundRole = active.roleOf(uid)
        if (boundRole != null) {
            if (boundRole != storedRole) sessionStore?.savePartnerRole(boundRole)
            return
        }
        val memberCount = (listOfNotNull(active.partner1Id, active.partner2Id) + active.userUids).distinct().size
        if (active.hasBoundMembers && memberCount < 2 && slotClaimAttempt != active.id to uid) {
            slotClaimAttempt = active.id to uid
            repository.ensurePartnerSlot(active.id, preferPartner2 = storedRole == PartnerRole.PARTNER_2)
        }
    }
}
