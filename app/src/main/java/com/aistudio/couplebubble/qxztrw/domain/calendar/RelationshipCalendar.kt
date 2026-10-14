package com.aistudio.couplebubble.qxztrw.domain.calendar

import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import java.time.LocalDate
import java.time.ZoneId

/** A couple's anniversary plus the milestones this device counts, so callers don't repeat the inputs. */
data class RelationshipCalendar(
    val year: Int,
    val month: Int,
    val day: Int,
    val enabledKinds: Set<MilestoneKind>,
    val customMilestones: List<CustomMilestone>,
    val momentMilestones: List<MomentMilestone> = emptyList()
) {
    fun metrics(today: LocalDate = today()): RelationshipMetrics =
        RelationshipDateCalculator.calculate(year, month, day, today, enabledKinds, customMilestones, momentMilestones)

    companion object {
        fun of(
            space: CoupleSpace,
            counter: CounterPreferences,
            customMilestones: List<CustomMilestone>,
            momentMilestones: List<MomentMilestone> = emptyList()
        ) =
            RelationshipCalendar(
                year = space.anniversaryYear,
                month = space.anniversaryMonth,
                day = space.anniversaryDay,
                enabledKinds = counter.enabledMilestoneKinds,
                customMilestones = customMilestones,
                momentMilestones = momentMilestones
            )

        private fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    }
}
