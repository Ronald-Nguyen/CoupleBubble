package com.aistudio.couplebubble.qxztrw.domain.calendar

import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.momentMilestones
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun recurringCustomMilestoneRepeatsEveryYear() {
        val wedding = CustomMilestone(id = "c1", title = "Hochzeitstag", date = LocalDate.of(2024, 10, 1), recurringYearly = true)
        val metrics = RelationshipDateCalculator.calculate(
            2025, 6, 25,
            today = today,
            enabledKinds = setOf(MilestoneKind.CUSTOM),
            customMilestones = listOf(wedding)
        )

        assertEquals(Milestone(MilestoneKind.CUSTOM, 0, LocalDate.of(2026, 10, 1), "Hochzeitstag"), metrics.nextMilestone)
        assertEquals(LocalDate.of(2026, 10, 1), wedding.nextOccurrence(today))
        assertEquals(LocalDate.of(2024, 10, 1), wedding.copy(recurringYearly = false).nextOccurrence(today))
    }

    @Test
    fun momentAnniversaryBecomesTheNextMilestone() {
        val vacation = MomentMilestone("m1", "Erster Urlaub", LocalDate.of(2024, 10, 1))
        val metrics = RelationshipDateCalculator.calculate(
            2025, 6, 25,
            today = today,
            enabledKinds = emptySet(),
            momentMilestones = listOf(vacation)
        )

        assertEquals(Milestone(MilestoneKind.MOMENT, 2, LocalDate.of(2026, 10, 1), "Erster Urlaub"), metrics.nextMilestone)
        assertEquals(LocalDate.of(2026, 10, 1), vacation.nextAnniversary(today))
    }

    @Test
    fun theMomentItselfIsNoMilestone() {
        val fresh = MomentMilestone("m1", "Eingezogen", LocalDate.of(2026, 9, 1))
        val candidates = RelationshipDateCalculator.milestoneCandidates(
            LocalDate.of(2025, 6, 25), today, emptySet(), emptyList(), listOf(fresh)
        )

        assertEquals(listOf(LocalDate.of(2027, 9, 1), LocalDate.of(2028, 9, 1)), candidates.map { it.date })
        assertEquals(LocalDate.of(2027, 9, 1), fresh.nextAnniversary(today))
    }

    @Test
    fun onlySwitchedOnMomentsCount() {
        val memories = listOf(
            Memory(id = "on", title = "Erster Urlaub", date = LocalDate.of(2024, 10, 1), countsAsMilestone = true),
            Memory(id = "off", title = "Kino", date = LocalDate.of(2024, 10, 5)),
            Memory(id = "untitled", date = LocalDate.of(2024, 10, 9), countsAsMilestone = true)
        )

        assertEquals(listOf("on"), memories.momentMilestones().map { it.memoryId })
    }

    @Test
    fun momentRanksBetweenYearsAndCustom() {
        val onAnniversary = MomentMilestone("m1", "Erster Urlaub", LocalDate.of(2025, 6, 25))
        val start = LocalDate.of(2024, 6, 25)
        val sameDay = LocalDate.of(2026, 6, 25)

        val withYears = RelationshipDateCalculator.milestoneCandidates(
            start, today, setOf(MilestoneKind.YEARS), emptyList(), listOf(onAnniversary)
        )
        assertEquals(MilestoneKind.MOMENT, withYears.first { it.date == sameDay }.kind)

        val withCustom = RelationshipDateCalculator.milestoneCandidates(
            start, today, setOf(MilestoneKind.CUSTOM), listOf(CustomMilestone(id = "c1", title = "Hochzeit", date = sameDay)),
            listOf(onAnniversary)
        )
        assertEquals(MilestoneKind.CUSTOM, withCustom.first { it.date == sameDay }.kind)
    }
}
