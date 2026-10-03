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
    val effectivePartnerAImage: String? get() = partnerAImageUrl ?: imageUrl
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

data class RelationshipMetrics(
    val formattedStartDate: String,
    val totalDays: Long,
    val years: Int,
    val months: Int,
    val days: Int,
    val nextAnniversaryTitle: String,
    val daysUntilNextAnniversary: Long,
    val progressToNextAnniversary: Float
)

data class PairingCode(
    val code: String,
    val totalValidSeconds: Int = 900
)

object RelationshipDateCalculator {

    fun calculate(
        year: Int,
        month: Int,
        day: Int,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault())
    ): RelationshipMetrics {
        val startDate = LocalDate.of(year, month, day)

        val totalDays = if (today.isAfter(startDate) || today.isEqual(startDate)) {
            ChronoUnit.DAYS.between(startDate, today)
        } else {
            0L
        }

        val period = if (today.isAfter(startDate) || today.isEqual(startDate)) {
            Period.between(startDate, today)
        } else {
            Period.ZERO
        }
        val years = period.years.coerceAtLeast(0)
        val months = period.months.coerceAtLeast(0)
        val days = period.days.coerceAtLeast(0)

        val nextMilestoneYears = years + 1
        val lastAnniversary = startDate.plusYears(years.toLong())
        val nextAnniversary = startDate.plusYears(nextMilestoneYears.toLong())

        val daysUntilNext = if (nextAnniversary.isAfter(today)) {
            ChronoUnit.DAYS.between(today, nextAnniversary)
        } else {
            0L
        }

        val cycleDays = ChronoUnit.DAYS.between(lastAnniversary, nextAnniversary).toFloat()
        val elapsedDays = ChronoUnit.DAYS.between(lastAnniversary, today).toFloat()
        val progress = if (cycleDays > 0f) {
            (elapsedDays / cycleDays).coerceIn(0f, 1f)
        } else 0f

        val monthFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.GERMAN)
        val formattedDate = "$day. ${startDate.format(monthFormatter)} $year"
        val title = "$nextMilestoneYears. Jahrestag"

        return RelationshipMetrics(
            formattedStartDate = formattedDate,
            totalDays = totalDays,
            years = years,
            months = months,
            days = days,
            nextAnniversaryTitle = title,
            daysUntilNextAnniversary = daysUntilNext,
            progressToNextAnniversary = progress
        )
    }
}
