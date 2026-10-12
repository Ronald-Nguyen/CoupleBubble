package com.aistudio.couplebubble.qxztrw.widget

import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind

/**
 * Everything the homescreen widgets need to render. Only raw inputs are stored; the widgets
 * compute days and the next milestone at render time, so they stay right as days pass.
 */
data class WidgetSnapshot(
    val partner1Name: String,
    val partner2Name: String,
    val anniversaryYear: Int,
    val anniversaryMonth: Int,
    val anniversaryDay: Int,
    val enabledMilestoneKinds: Set<MilestoneKind>,
    val customMilestones: List<CustomMilestone>
)

/** Hands the active space to the homescreen widgets; `null` shows the "not paired" state. */
interface WidgetPublisher {
    suspend fun publish(snapshot: WidgetSnapshot?)
}
