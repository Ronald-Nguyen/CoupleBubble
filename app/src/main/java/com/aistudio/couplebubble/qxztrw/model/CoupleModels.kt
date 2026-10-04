package com.aistudio.couplebubble.qxztrw.model

import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

data class MemoryMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateText: String,
    val iconType: String, // "FIRST_DATE", "TRIP", "HOME", "CELEBRATION"
)

data class Memory(
    val id: String = "",
    val title: String = "",
    val date: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    val note: String = "",
    val imageUrl: String? = null,
    val partnerAImageUrl: String? = null,
    val partnerBImageUrl: String? = null,
) {
    // Legacy `imageUrl` only counts for A on old single-photo moments; it used to mirror B and caused duplicates.
    val effectivePartnerAImage: String? get() = partnerAImageUrl ?: imageUrl.takeIf { partnerBImageUrl.isNullOrBlank() }
    val effectivePartnerBImage: String? get() = partnerBImageUrl
    val hasAnyImage: Boolean get() = !effectivePartnerAImage.isNullOrBlank() || !effectivePartnerBImage.isNullOrBlank()
    val imageCount: Int get() = (if (!effectivePartnerAImage.isNullOrBlank()) 1 else 0) + (if (!effectivePartnerBImage.isNullOrBlank()) 1 else 0)
}

data class UserProfile(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val coupleId: String? = null
)

data class CoupleSpace(
    val id: String = "space_prototype_1",
    val partnerAName: String = "Alex",
    val partnerBName: String = "Sam",
    val anniversaryYear: Int = 2025,
    val anniversaryMonth: Int = 6, // 1-based (June)
    val anniversaryDay: Int = 25,
    val anniversaryEpochMillis: Long = 1750800000000L,
    val isSetupComplete: Boolean = true,
    val isActive: Boolean = true,
    val userUids: List<String> = emptyList(),
    val partner1Id: String? = null,
    val partner2Id: String? = null,
    val partner1AvatarColor: Long = 0xFF1A6B99, // Mediterranean Blue
    val partner2AvatarColor: Long = 0xFFE26533, // Sunset Terracotta
    val partner1PhotoUrl: String? = null,
    val partner2PhotoUrl: String? = null,
    val partner1ColorHex: String = "#FF6B6B",
    val partner2ColorHex: String = "#4ECDC4",
    val memories: List<MemoryMilestone> = listOf(
        MemoryMilestone(
            id = "m1",
            title = "Erster Tag zusammen",
            subtitle = "Der schönste Spaziergang am See",
            dateText = "25. Juni 2025",
            iconType = "FIRST_DATE"
        ),
        MemoryMilestone(
            id = "m2",
            title = "Erste gemeinsame Reise",
            subtitle = "Magischer Sommerurlaub am Meer",
            dateText = "18. September 2025",
            iconType = "TRIP"
        ),
        MemoryMilestone(
            id = "m3",
            title = "Zusammengezogen",
            subtitle = "Unsere erste gemeinsame Wohnung",
            dateText = "12. Februar 2026",
            iconType = "HOME"
        ),
        MemoryMilestone(
            id = "m4",
            title = "1. Jahrestag gefeiert",
            subtitle = "Ein unvergesslicher Abend zu zweit",
            dateText = "25. Juni 2026",
            iconType = "CELEBRATION"
        )
    )
) {
    // Backwards-compatible aliases
    @get:Suppress("unused")
    val partner1Name: String get() = partnerAName
    @get:Suppress("unused")
    val partner2Name: String get() = partnerBName
    val partner1Initial: String get() = partnerAName.take(1).uppercase()
    val partner2Initial: String get() = partnerBName.take(1).uppercase()
    @get:Suppress("unused")
    val partnerAPhotoUrl: String? get() = partner1PhotoUrl
    @get:Suppress("unused")
    val partnerBPhotoUrl: String? get() = partner2PhotoUrl
    @get:Suppress("unused")
    val partnerAColorHex: String get() = partner1ColorHex
    @get:Suppress("unused")
    val partnerBColorHex: String get() = partner2ColorHex
}

/** How the "Zusammen seit" hero counter breaks down the time together; chosen per device. */
enum class CounterDisplayMode { DAYS, WEEKS, MONTHS, YEARS }

/** Milestone types in ascending priority: when two land on the same date, the later entry wins. */
enum class MilestoneKind { DAYS, WEEKS, MONTHS, YEARS, CUSTOM }

val DEFAULT_MILESTONE_KINDS: Set<MilestoneKind> = setOf(MilestoneKind.DAYS, MilestoneKind.YEARS, MilestoneKind.CUSTOM)

data class CounterPreferences(
    val displayMode: CounterDisplayMode = CounterDisplayMode.DAYS,
    val enabledMilestoneKinds: Set<MilestoneKind> = DEFAULT_MILESTONE_KINDS
)

/** A user-defined milestone shared by both partners (`spaces/{id}/milestones`). */
data class CustomMilestone(
    val id: String = "",
    val title: String = "",
    val date: LocalDate = LocalDate.now(ZoneId.systemDefault())
)

/** [value] is the day/week/month/year count; custom milestones carry their own [customTitle] instead. */
data class Milestone(
    val kind: MilestoneKind,
    val value: Int,
    val date: LocalDate,
    val customTitle: String? = null
)

data class RelationshipMetrics(
    val formattedStartDate: String,
    val totalDays: Long,
    val years: Int,
    val months: Int,
    val days: Int,
    val totalWeeks: Long,
    val weekRemainderDays: Int,
    val totalMonths: Long,
    val monthRemainderDays: Int,
    val nextMilestone: Milestone?,
    val daysUntilNextMilestone: Long,
    val progressToNextMilestone: Float
)

data class PairingCode(
    val code: String,
    val totalValidSeconds: Int = 900
)

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
