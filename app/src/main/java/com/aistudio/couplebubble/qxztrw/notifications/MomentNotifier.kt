package com.aistudio.couplebubble.qxztrw.notifications

import com.aistudio.couplebubble.qxztrw.domain.model.Milestone

/** Tells the user that the partner added a new moment or that a milestone is reached today. */
interface MomentNotifier {
    fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String)

    fun notifyMilestoneReached(milestone: Milestone) = Unit
}
