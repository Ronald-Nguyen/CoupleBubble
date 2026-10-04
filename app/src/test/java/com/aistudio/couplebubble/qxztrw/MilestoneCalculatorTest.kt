package com.aistudio.couplebubble.qxztrw

import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.Milestone
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MilestoneCalculatorTest {

    private val today = LocalDate.of(2026, 9, 25)

    @Test
    fun defaultKindsPickNextDayMilestone() {
        val metrics = RelationshipDateCalculator.calculate(2025, 6, 25, today = today)

        assertEquals(Milestone(MilestoneKind.DAYS, 500, LocalDate.of(2026, 11, 7)), metrics.nextMilestone)
        assertEquals(43L, metrics.daysUntilNextMilestone)
        // Progress runs from the 1st anniversary (2026-06-25): 92 of 135 days
        assertEquals(92f / 135f, metrics.progressToNextMilestone, 0.001f)
    }

    @Test
    fun counterBreakdownsForWeeksAndMonths() {
        val metrics = RelationshipDateCalculator.calculate(2025, 6, 25, today = today)

        assertEquals(457L, metrics.totalDays)
        assertEquals(65L, metrics.totalWeeks)
        assertEquals(2, metrics.weekRemainderDays)
        assertEquals(15L, metrics.totalMonths)
        assertEquals(0, metrics.monthRemainderDays)
    }

    @Test
    fun monthiversaryOnTodayCountsAsNext() {
        val metrics = RelationshipDateCalculator.calculate(
            2025, 6, 25, today = today, enabledKinds = setOf(MilestoneKind.MONTHS)
        )

        assertEquals(Milestone(MilestoneKind.MONTHS, 15, today), metrics.nextMilestone)
        assertEquals(0L, metrics.daysUntilNextMilestone)
    }

    @Test
    fun monthiversaryAfterTodayIsTheFollowingMonth() {
        val metrics = RelationshipDateCalculator.calculate(
            2025, 6, 25, today = today.plusDays(1), enabledKinds = setOf(MilestoneKind.MONTHS)
        )

        assertEquals(Milestone(MilestoneKind.MONTHS, 16, LocalDate.of(2026, 10, 25)), metrics.nextMilestone)
    }

    @Test
    fun monthiversaryClampsToEndOfShortMonth() {
        val metrics = RelationshipDateCalculator.calculate(
            2024, 1, 31, today = LocalDate.of(2024, 2, 1), enabledKinds = setOf(MilestoneKind.MONTHS)
        )

        assertEquals(LocalDate.of(2024, 2, 29), metrics.nextMilestone?.date)
    }

    @Test
    fun fullYearsAreAnniversariesNotMonthiversaries() {
        val candidates = RelationshipDateCalculator.milestoneCandidates(
            startDate = LocalDate.of(2025, 6, 25),
            today = today,
            enabledKinds = setOf(MilestoneKind.MONTHS, MilestoneKind.YEARS),
            customMilestones = emptyList()
        )

        assertFalse(candidates.any { it.kind == MilestoneKind.MONTHS && it.value % 12 == 0 })
        assertEquals(MilestoneKind.YEARS, candidates.first { it.date == LocalDate.of(2026, 6, 25) }.kind)
    }

    @Test
    fun customMilestoneWinsOverSameDayMilestone() {
        val metrics = RelationshipDateCalculator.calculate(
            2025, 6, 25,
            today = today,
            customMilestones = listOf(CustomMilestone(id = "c1", title = "Hochzeit", date = LocalDate.of(2026, 11, 7)))
        )

        assertEquals(MilestoneKind.CUSTOM, metrics.nextMilestone?.kind)
        assertEquals("Hochzeit", metrics.nextMilestone?.customTitle)
    }

    @Test
    fun noEnabledKindsMeansNoMilestone() {
        val metrics = RelationshipDateCalculator.calculate(2025, 6, 25, today = today, enabledKinds = emptySet())

        assertNull(metrics.nextMilestone)
        assertEquals(0f, metrics.progressToNextMilestone)
    }

    @Test
    fun pastCustomMilestoneIsNeverNext() {
        val metrics = RelationshipDateCalculator.calculate(
            2025, 6, 25,
            today = today,
            enabledKinds = setOf(MilestoneKind.CUSTOM),
            customMilestones = listOf(CustomMilestone(id = "c1", title = "Umzug", date = LocalDate.of(2026, 1, 1)))
        )

        assertNull(metrics.nextMilestone)
    }
}
