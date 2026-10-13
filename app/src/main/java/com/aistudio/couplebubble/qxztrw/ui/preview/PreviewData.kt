package com.aistudio.couplebubble.qxztrw.ui.preview

import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlaceSource
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionStatus
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingUiState
import com.aistudio.couplebubble.qxztrw.ui.screens.home.ReunionUiState
import java.time.LocalDate

/** Realistic sample data for `@Preview`s, so previews don't each invent their own couple. */
internal object PreviewData {
    val space = CoupleSpace(
        id = "space_482913",
        partner1Name = "Mia",
        partner2Name = "Leo",
        anniversaryYear = 2023,
        anniversaryMonth = 6,
        anniversaryDay = 25,
        isSetupComplete = true,
        isActive = true,
        partner1Id = "uid_mia",
        partner2Id = "uid_leo",
        userUids = listOf("uid_mia", "uid_leo"),
        partner1ColorHex = "#E65D2E",
        partner2ColorHex = "#5B8DB8",
        pairingCode = "482913"
    )

    val profile = UserProfile(uid = "uid_mia", email = "mia@example.com", displayName = "Mia Berger", coupleId = space.id)

    val memory = Memory(
        id = "m1",
        title = "Erster Tag am See",
        date = LocalDate.of(2023, 7, 2),
        note = "Barfuß am Steg, bis die Sonne weg war.",
        partner2ImageUrl = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800",
        createdBy = "uid_leo"
    )

    val milestones = listOf(
        Milestone(MilestoneKind.YEARS, 2, LocalDate.of(2025, 6, 25)),
        Milestone(MilestoneKind.DAYS, 500, LocalDate.of(2024, 11, 6))
    )

    private const val PREVIEW_NOW = 1_760_000_000_000L

    private val reunionPlan = Reunion(
        startDate = LocalDate.of(2025, 10, 24),
        endDate = LocalDate.of(2025, 10, 26),
        plannedAtDate = LocalDate.of(2025, 9, 20),
        places = mapOf(
            "uid_mia" to ReunionPlace(sharesLocation = true, location = GeoPoint(50.9, 7.0), locatedAtMillis = PREVIEW_NOW),
            "uid_leo" to ReunionPlace(city = "Berlin, DE", cityLocation = GeoPoint(52.52, 13.40))
        )
    )

    /** Mia shares her location (Köln), Leo chose Berlin from the city list. */
    val reunion = ReunionUiState(
        reunion = reunionPlan,
        status = ReunionStatus.Upcoming(daysUntil = 12, durationDays = 3, progress = 0.65f),
        distanceKm = 476,
        ownPlace = reunionPlan.places["uid_mia"],
        partnerPlace = reunionPlan.places["uid_leo"],
        ownSource = ReunionPlaceSource.Location(PREVIEW_NOW),
        partnerSource = ReunionPlaceSource.City("Berlin, DE"),
        nowMillis = PREVIEW_NOW
    )

    val reunionTogether = reunion.copy(status = ReunionStatus.Together(day = 1, durationDays = 3))

    val pairing = PairingUiState(generatedCode = "482-913", countdownSeconds = 754, enteredCode = "4829")
}
