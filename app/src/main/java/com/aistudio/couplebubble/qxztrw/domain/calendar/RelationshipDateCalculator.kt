package com.aistudio.couplebubble.qxztrw.domain.calendar

import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.DEFAULT_MILESTONE_KINDS
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object RelationshipDateCalculator {

    private val DAY_MILESTONES = listOf(
        100, 200, 300, 500, 750, 1000, 1111, 1500, 2000, 2222, 2500, 3000, 3333,
        4444, 5000, 5555, 6666, 7777, 8888, 9999, 10000
    )
    private val WEEK_MILESTONES = listOf(10, 50, 100, 250, 500, 1000)

    fun calculate(
        year: Int,
        month: Int,
        day: Int,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        enabledKinds: Set<MilestoneKind> = DEFAULT_MILESTONE_KINDS,
        customMilestones: List<CustomMilestone> = emptyList(),
        momentMilestones: List<MomentMilestone> = emptyList()
    ): RelationshipMetrics {
        val startDate = LocalDate.of(year, month, day)
        val hasStarted = !today.isBefore(startDate)

        val totalDays = if (hasStarted) ChronoUnit.DAYS.between(startDate, today) else 0L
        val period = if (hasStarted) Period.between(startDate, today) else Period.ZERO
        val years = period.years.coerceAtLeast(0)
        val months = period.months.coerceAtLeast(0)
        val days = period.days.coerceAtLeast(0)
        val totalMonths = period.toTotalMonths().coerceAtLeast(0)

        val candidates = milestoneCandidates(startDate, today, enabledKinds, customMilestones, momentMilestones)
        val next = candidates.firstOrNull { !it.date.isBefore(today) }
        val previousDate = candidates.lastOrNull { it.date.isBefore(today) }?.date ?: startDate

        val daysUntilNext = next?.let { ChronoUnit.DAYS.between(today, it.date) } ?: 0L
        val progress = if (next == null) {
            0f
        } else {
            val span = ChronoUnit.DAYS.between(previousDate, next.date).toFloat()
            val elapsed = ChronoUnit.DAYS.between(previousDate, today).toFloat()
            if (span > 0f) (elapsed / span).coerceIn(0f, 1f) else 1f
        }

        return RelationshipMetrics(
            startDate = startDate,
            totalDays = totalDays,
            years = years,
            months = months,
            days = days,
            totalWeeks = totalDays / 7,
            weekRemainderDays = (totalDays % 7).toInt(),
            totalMonths = totalMonths,
            monthRemainderDays = days,
            nextMilestone = next,
            daysUntilNextMilestone = daysUntilNext,
            progressToNextMilestone = progress
        )
    }

    /**
     * Couples moments to the milestone they were explicitly linked to ([Memory.milestoneKey]), as long as that
     * milestone is still enabled. Moments without a link stay untagged.
     */
    fun milestonesForMemories(
        year: Int,
        month: Int,
        day: Int,
        memories: List<Memory>,
        enabledKinds: Set<MilestoneKind> = DEFAULT_MILESTONE_KINDS,
        customMilestones: List<CustomMilestone> = emptyList(),
        momentMilestones: List<MomentMilestone> = emptyList(),
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ): Map<String, Milestone> {
        val linked = memories.filter { it.milestoneKey != null }
        if (linked.isEmpty()) return emptyMap()
        val startDate = LocalDate.of(year, month, day)
        val reference = maxOf(today, linked.maxOf { it.date })
        val byKey = milestoneCandidates(startDate, reference, enabledKinds, customMilestones, momentMilestones).associateBy { it.key }
        return linked.mapNotNull { memory -> byKey[memory.milestoneKey]?.let { memory.id to it } }.toMap()
    }

    /** Enabled milestones reached up to [today], newest first: the choices for linking a moment. */
    fun reachedMilestones(
        year: Int,
        month: Int,
        day: Int,
        enabledKinds: Set<MilestoneKind> = DEFAULT_MILESTONE_KINDS,
        customMilestones: List<CustomMilestone> = emptyList(),
        momentMilestones: List<MomentMilestone> = emptyList(),
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ): List<Milestone> =
        milestoneCandidates(LocalDate.of(year, month, day), today, enabledKinds, customMilestones, momentMilestones)
            .filter { !it.date.isAfter(today) }
            .reversed()

    /**
     * All enabled milestones up to a little past [today], sorted by date.
     * Only one milestone per date survives: the one whose [MilestoneKind] ranks highest.
     * Moment anniversaries don't depend on [enabledKinds]; each moment carries its own switch.
     */
    internal fun milestoneCandidates(
        startDate: LocalDate,
        today: LocalDate,
        enabledKinds: Set<MilestoneKind>,
        customMilestones: List<CustomMilestone>,
        momentMilestones: List<MomentMilestone> = emptyList()
    ): List<Milestone> {
        val reference = if (today.isBefore(startDate)) startDate else today
        val elapsedDays = ChronoUnit.DAYS.between(startDate, reference)
        val elapsedMonths = Period.between(startDate, reference).toTotalMonths()
        val candidates = mutableListOf<Milestone>()

        if (MilestoneKind.DAYS in enabledKinds) {
            val thousands = (11..(elapsedDays / 1000 + 2).toInt()).map { it * 1000 }
            (DAY_MILESTONES + thousands).forEach { count ->
                candidates += Milestone(MilestoneKind.DAYS, count, startDate.plusDays(count.toLong()))
            }
        }
        if (MilestoneKind.WEEKS in enabledKinds) {
            WEEK_MILESTONES.forEach { count ->
                candidates += Milestone(MilestoneKind.WEEKS, count, startDate.plusWeeks(count.toLong()))
            }
        }
        if (MilestoneKind.MONTHS in enabledKinds) {
            // plusMonths clamps to the last day of shorter months; full years belong to the anniversaries
            (1..(elapsedMonths + 13).toInt()).filter { it % 12 != 0 }.forEach { count ->
                candidates += Milestone(MilestoneKind.MONTHS, count, startDate.plusMonths(count.toLong()))
            }
        }
        if (MilestoneKind.YEARS in enabledKinds) {
            (1..(elapsedMonths / 12 + 2).toInt()).forEach { count ->
                candidates += Milestone(MilestoneKind.YEARS, count, startDate.plusYears(count.toLong()))
            }
        }
        momentMilestones.forEach { moment ->
            moment.anniversaries(untilYear = reference.year + 2).forEach { (years, date) ->
                candidates += Milestone(MilestoneKind.MOMENT, years, date, moment.title)
            }
        }
        if (MilestoneKind.CUSTOM in enabledKinds) {
            customMilestones.filter { it.enabled }.forEach { custom ->
                custom.occurrences(untilYear = reference.year + 2).forEach { date ->
                    candidates += Milestone(MilestoneKind.CUSTOM, 0, date, custom.title)
                }
            }
        }

        return candidates
            .groupBy { it.date }
            .map { (_, sameDay) -> sameDay.maxBy { it.kind.ordinal } }
            .sortedBy { it.date }
    }
}
