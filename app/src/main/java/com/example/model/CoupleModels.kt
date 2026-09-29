package com.example.model

import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

data class MemoryMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateText: String,
    val iconType: String // "FIRST_DATE", "TRIP", "HOME", "CELEBRATION"
)

data class CoupleSpace(
    val id: String = "space_prototype_1",
    val partner1Name: String = "Alex",
    val partner2Name: String = "Sam",
    val anniversaryYear: Int = 2025,
    val anniversaryMonth: Int = 6, // 1-based (June)
    val anniversaryDay: Int = 25,
    val partner1AvatarColor: Long = 0xFF1A6B99, // Mediterranean Blue
    val partner2AvatarColor: Long = 0xFFE26533, // Sunset Terracotta
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
    val partner1Initial: String get() = partner1Name.take(1).uppercase()
    val partner2Initial: String get() = partner2Name.take(1).uppercase()
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
        nowCal: Calendar = Calendar.getInstance()
    ): RelationshipMetrics {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCal = Calendar.getInstance().apply {
            timeInMillis = nowCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = todayCal.timeInMillis - startCal.timeInMillis
        val totalDays = (diffMillis / (24 * 60 * 60 * 1000L)).coerceAtLeast(0L)

        // Exact breakdown in Years, Months, Days
        var years = todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        var months = todayCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        var days = todayCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            val tempCal = (todayCal.clone() as Calendar).apply {
                add(Calendar.MONTH, -1)
            }
            days += tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            months--
        }

        if (months < 0) {
            months += 12
            years--
        }

        years = years.coerceAtLeast(0)
        months = months.coerceAtLeast(0)
        days = days.coerceAtLeast(0)

        // Next Anniversary
        val nextMilestoneYears = years + 1
        val lastAnniversaryCal = (startCal.clone() as Calendar).apply {
            add(Calendar.YEAR, years)
        }
        val nextAnniversaryCal = (startCal.clone() as Calendar).apply {
            add(Calendar.YEAR, nextMilestoneYears)
        }

        val millisUntilNext = (nextAnniversaryCal.timeInMillis - todayCal.timeInMillis).coerceAtLeast(0L)
        val daysUntilNext = (millisUntilNext / (24 * 60 * 60 * 1000L))

        val cycleTotalMillis = (nextAnniversaryCal.timeInMillis - lastAnniversaryCal.timeInMillis).toFloat()
        val cycleElapsedMillis = (todayCal.timeInMillis - lastAnniversaryCal.timeInMillis).toFloat()
        val progress = if (cycleTotalMillis > 0f) {
            (cycleElapsedMillis / cycleTotalMillis).coerceIn(0f, 1f)
        } else 0f

        val monthName = DateFormatSymbols.getInstance(Locale.GERMAN).months.getOrNull(month - 1) ?: "$month"
        val formattedDate = "$day. $monthName $year"

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
