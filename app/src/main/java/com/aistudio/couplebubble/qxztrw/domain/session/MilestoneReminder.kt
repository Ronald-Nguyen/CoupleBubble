package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import kotlinx.coroutines.flow.first

/**
 * Notifies once when the next milestone is due today. The key `"{spaceId}:{date}"` is remembered in memory and
 * in [sessionStore], so neither a recomposition nor an app restart notifies twice.
 */
class MilestoneReminder(
    private val notifier: MomentNotifier,
    private val sessionStore: SessionStore?
) {
    private var lastNotified: String? = null

    suspend fun onMetrics(spaceId: String, metrics: RelationshipMetrics) {
        val milestone = metrics.nextMilestone?.takeIf { metrics.daysUntilNextMilestone == 0L } ?: return
        val key = "$spaceId:${milestone.date}"
        if (key == lastNotified) return
        lastNotified = key
        if (sessionStore != null) {
            if (sessionStore.lastNotifiedMilestoneFlow.first() == key) return
            sessionStore.saveLastNotifiedMilestone(key)
        }
        notifier.notifyMilestoneReached(milestone)
    }
}
