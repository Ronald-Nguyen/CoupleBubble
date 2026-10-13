package com.aistudio.couplebubble.qxztrw.widget

import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone

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
    val customMilestones: List<CustomMilestone>,
    val momentMilestones: List<MomentMilestone> = emptyList()
)

/** Hands the active space to the homescreen widgets; `null` shows the "not paired" state. */
interface WidgetPublisher {
    suspend fun publish(snapshot: WidgetSnapshot?)
}

/** What the widgets show for [space]; `null` ("not paired") once the space was left. */
fun widgetSnapshotOf(
    space: CoupleSpace,
    counter: CounterPreferences,
    customMilestones: List<CustomMilestone>,
    momentMilestones: List<MomentMilestone>
): WidgetSnapshot? =
    space.takeIf { it.isActive }?.let {
        WidgetSnapshot(
            partner1Name = it.partner1Name,
            partner2Name = it.partner2Name,
            anniversaryYear = it.anniversaryYear,
            anniversaryMonth = it.anniversaryMonth,
            anniversaryDay = it.anniversaryDay,
            enabledMilestoneKinds = counter.enabledMilestoneKinds,
            customMilestones = customMilestones,
            momentMilestones = momentMilestones
        )
    }
