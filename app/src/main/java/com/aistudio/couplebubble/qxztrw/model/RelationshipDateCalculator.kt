package com.aistudio.couplebubble.qxztrw.model

import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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
        customMilestones: List<CustomMilestone> = emptyList()
    ): RelationshipMetrics {
        val startDate = LocalDate.of(year, month, day)
        val hasStarted = !today.isBefore(startDate)

        val totalDays = if (hasStarted) ChronoUnit.DAYS.between(startDate, today) else 0L
        val period = if (hasStarted) Period.between(startDate, today) else Period.ZERO
        val years = period.years.coerceAtLeast(0)
        val months = period.months.coerceAtLeast(0)
        val days = period.days.coerceAtLeast(0)
        val totalMonths = period.toTotalMonths().coerceAtLeast(0)

        val candidates = milestoneCandidates(startDate, today, enabledKinds, customMilestones)
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

        val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.GERMAN)
        val formattedDate = "$day. ${startDate.format(monthFormatter)} $year"

        return RelationshipMetrics(
            formattedStartDate = formattedDate,
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
     * All enabled milestones up to a little past [today], sorted by date.
     * Only one milestone per date survives: the one whose [MilestoneKind] ranks highest.
     */
    internal fun milestoneCandidates(
        startDate: LocalDate,
        today: LocalDate,
        enabledKinds: Set<MilestoneKind>,
        customMilestones: List<CustomMilestone>
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
        if (MilestoneKind.CUSTOM in enabledKinds) {
            customMilestones.forEach { custom ->
                candidates += Milestone(MilestoneKind.CUSTOM, 0, custom.date, custom.title)
            }
        }

        return candidates
            .groupBy { it.date }
            .map { (_, sameDay) -> sameDay.maxBy { it.kind.ordinal } }
            .sortedBy { it.date }
    }
}
