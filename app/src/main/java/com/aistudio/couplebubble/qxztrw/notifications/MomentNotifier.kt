package com.aistudio.couplebubble.qxztrw.notifications

/** Tells the user that the partner added a new moment. */
interface MomentNotifier {
    fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String)
}
