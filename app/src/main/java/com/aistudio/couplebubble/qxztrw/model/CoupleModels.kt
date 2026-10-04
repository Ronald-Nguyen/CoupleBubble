package com.aistudio.couplebubble.qxztrw.model

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
    val partner2ColorHex: String = SpaceDefaults.PARTNER_2_COLOR_HEX
) {
    val partner1Initial: String get() = partner1Name.take(1).uppercase()
    val partner2Initial: String get() = partner2Name.take(1).uppercase()
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
