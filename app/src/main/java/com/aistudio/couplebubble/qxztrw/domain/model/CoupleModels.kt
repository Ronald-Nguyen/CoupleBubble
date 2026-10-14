package com.aistudio.couplebubble.qxztrw.domain.model

import java.time.LocalDate
import java.time.ZoneId

data class Memory(
    val id: String = "",
    val title: String = "",
    val date: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    val note: String = "",
    val imageUrl: String? = null,
    val partner1ImageUrl: String? = null,
    val partner2ImageUrl: String? = null,
    /** UID of the partner who created the moment; null for moments saved before this field existed. */
    val createdBy: String? = null,
    /** Shared switch: the moment's yearly anniversaries count as milestones ([MilestoneKind.MOMENT]). */
    val countsAsMilestone: Boolean = false,
) {
    // Legacy `imageUrl` only counts for A on old single-photo moments; it used to mirror B and caused duplicates.
    val effectivePartner1Image: String? get() = partner1ImageUrl ?: imageUrl.takeIf { partner2ImageUrl.isNullOrBlank() }
    val effectivePartner2Image: String? get() = partner2ImageUrl
    val hasAnyImage: Boolean get() = !effectivePartner1Image.isNullOrBlank() || !effectivePartner2Image.isNullOrBlank()
    val imageCount: Int get() = (if (!effectivePartner1Image.isNullOrBlank()) 1 else 0) + (if (!effectivePartner2Image.isNullOrBlank()) 1 else 0)
}

data class UserProfile(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val coupleId: String? = null
)

data class CoupleSpace(
    val id: String = "space_prototype_1",
    val partner1Name: String = SpaceDefaults.PARTNER_1_NAME,
    val partner2Name: String = SpaceDefaults.PARTNER_2_NAME,
    val anniversaryYear: Int = SpaceDefaults.ANNIVERSARY_YEAR,
    val anniversaryMonth: Int = SpaceDefaults.ANNIVERSARY_MONTH, // 1-based
    val anniversaryDay: Int = SpaceDefaults.ANNIVERSARY_DAY,
    val anniversaryEpochMillis: Long = SpaceDefaults.ANNIVERSARY_EPOCH_MILLIS,
    val isSetupComplete: Boolean = true,
    val isActive: Boolean = true,
    val userUids: List<String> = emptyList(),
    val partner1Id: String? = null,
    val partner2Id: String? = null,
    val partner1AvatarColor: Long = 0xFF1A6B99, // Mediterranean Blue
    val partner2AvatarColor: Long = 0xFFE26533, // Sunset Terracotta
    val partner1PhotoUrl: String? = null,
    val partner2PhotoUrl: String? = null,
    val partner1ColorHex: String = SpaceDefaults.PARTNER_1_COLOR_HEX,
    val partner2ColorHex: String = SpaceDefaults.PARTNER_2_COLOR_HEX,
    /** Stored without hyphen (`482913`); empty for the demo space. */
    val pairingCode: String = ""
) {
    /** True while only one person has joined, so the dashboard keeps offering the code to share. */
    val isWaitingForPartner: Boolean
        get() = pairingCode.length == 6 && memberUids.size < 2

    val formattedPairingCode: String get() = "${pairingCode.take(3)}-${pairingCode.drop(3)}"

    val partner1Initial: String get() = partner1Name.take(1).uppercase()
    val partner2Initial: String get() = partner2Name.take(1).uppercase()
}

/** How the "Zusammen seit" hero counter breaks down the time together; chosen per device. */
enum class CounterDisplayMode { DAYS, WEEKS, MONTHS, YEARS }

/**
 * Milestone types in ascending priority: when two land on the same date, the later entry wins.
 * [MOMENT] is not a device switch; each moment is switched on by itself ([Memory.countsAsMilestone]).
 */
enum class MilestoneKind { DAYS, WEEKS, MONTHS, YEARS, MOMENT, CUSTOM }

val DEFAULT_MILESTONE_KINDS: Set<MilestoneKind> = setOf(MilestoneKind.DAYS, MilestoneKind.YEARS, MilestoneKind.CUSTOM)

data class CounterPreferences(
    val displayMode: CounterDisplayMode = CounterDisplayMode.DAYS,
    val enabledMilestoneKinds: Set<MilestoneKind> = DEFAULT_MILESTONE_KINDS
)

/** A moment switched on as a milestone; its yearly anniversaries (from year 1 on) are counted. */
data class MomentMilestone(
    val memoryId: String,
    val title: String,
    val date: LocalDate
) {
    /** All anniversaries from the first one up to and including [untilYear], paired with their year count. */
    fun anniversaries(untilYear: Int): List<Pair<Int, LocalDate>> =
        (1..(untilYear - date.year)).map { years -> years to date.plusYears(years.toLong()) }

    /** The next anniversary (today included). */
    fun nextAnniversary(today: LocalDate): LocalDate {
        val years = (today.year - date.year).coerceAtLeast(1).toLong()
        val candidate = date.plusYears(years)
        return if (candidate.isBefore(today)) date.plusYears(years + 1) else candidate
    }
}

/** The moments both partners switched on as milestones. */
fun List<Memory>.momentMilestones(): List<MomentMilestone> =
    filter { it.countsAsMilestone && it.title.isNotBlank() }.map { MomentMilestone(it.id, it.title, it.date) }

/** A user-defined milestone shared by both partners (`spaces/{id}/milestones`). */
data class CustomMilestone(
    val id: String = "",
    val title: String = "",
    val date: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    /** Repeats on the same day every year from [date] on (e.g. "Hochzeitstag"). */
    val recurringYearly: Boolean = false,
    /** Switched off milestones stay in the list but are never counted, notified or tagged. */
    val enabled: Boolean = true
) {
    /** The next date (today included) this milestone falls on; for one-off milestones simply [date]. */
    fun nextOccurrence(today: LocalDate): LocalDate {
        if (!recurringYearly || !date.isBefore(today)) return date
        val yearsPassed = (today.year - date.year).toLong()
        val candidate = date.plusYears(yearsPassed)
        return if (candidate.isBefore(today)) date.plusYears(yearsPassed + 1) else candidate
    }

    /** All dates this milestone falls on up to and including [untilYear]. */
    fun occurrences(untilYear: Int): List<LocalDate> =
        if (!recurringYearly) listOf(date)
        else (0..(untilYear - date.year).coerceAtLeast(0)).map { date.plusYears(it.toLong()) }
}

/** [value] is the day/week/month/year count (years since the moment for [MilestoneKind.MOMENT]); custom and moment milestones carry their own [customTitle]. */
data class Milestone(
    val kind: MilestoneKind,
    val value: Int,
    val date: LocalDate,
    val customTitle: String? = null
) {
    /** Stable id a moment stores to link itself to this milestone. */
    val key: String get() = "${kind.name}:$date"
}

data class RelationshipMetrics(
    val startDate: LocalDate,
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

/** What the current user does with their own photo slot of a moment; the partner's slot is never written. */
sealed interface MemoryPhotoChange {
    data object Keep : MemoryPhotoChange
    data object Remove : MemoryPhotoChange
    class Replace(val bytes: ByteArray) : MemoryPhotoChange
}
