package com.aistudio.couplebubble.qxztrw.domain.calendar

import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import java.time.LocalDate
import java.time.ZoneId

/** A couple's anniversary plus the milestones this device counts, so callers don't repeat the inputs. */
data class RelationshipCalendar(
    val year: Int,
    val month: Int,
    val day: Int,
    val enabledKinds: Set<MilestoneKind>,
    val customMilestones: List<CustomMilestone>
) {
    fun metrics(today: LocalDate = today()): RelationshipMetrics =
        RelationshipDateCalculator.calculate(year, month, day, today, enabledKinds, customMilestones)

    /** Memory id → the enabled milestone the moment is linked to. */
    fun memoryMilestones(memories: List<Memory>, today: LocalDate = today()): Map<String, Milestone> =
        RelationshipDateCalculator.milestonesForMemories(year, month, day, memories, enabledKinds, customMilestones, today)

    /** Enabled milestones up to today, newest first, as offered when linking a moment. */
    fun reachedMilestones(today: LocalDate = today()): List<Milestone> =
        RelationshipDateCalculator.reachedMilestones(year, month, day, enabledKinds, customMilestones, today)

    companion object {
        fun of(space: CoupleSpace, counter: CounterPreferences, customMilestones: List<CustomMilestone>) =
            RelationshipCalendar(
                year = space.anniversaryYear,
                month = space.anniversaryMonth,
                day = space.anniversaryDay,
                enabledKinds = counter.enabledMilestoneKinds,
                customMilestones = customMilestones
            )

        private fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    }
}
