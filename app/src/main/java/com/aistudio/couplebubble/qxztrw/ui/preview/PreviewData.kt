package com.aistudio.couplebubble.qxztrw.ui.preview

import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingUiState
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

    val pairing = PairingUiState(generatedCode = "482-913", countdownSeconds = 754, enteredCode = "4829")
}
