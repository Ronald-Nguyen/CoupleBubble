package com.aistudio.couplebubble.qxztrw.data.demo

import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.DefaultNoteLabelIds
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.NoteItem
import com.aistudio.couplebubble.qxztrw.domain.model.NoteType
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.SharedNote
import java.time.LocalDate

/**
 * Sample content of the demo mode: a long-distance couple, "Mila" (simulated partner, Partner 1) and "Noah"
 * (the person holding the phone, Partner 2). Like user data it is content, not UI text, so it is not in
 * `strings.xml`. Dates are relative to [today], so counters and milestones always look current.
 */
internal object DemoSeed {
    const val USER_UID = "demo_user"
    const val PARTNER_UID = "demo_partner"
    const val SPACE_ID = "demo_space_seeded"
    const val PAIRING_CODE = "482913"

    private const val PHOTO_LAKE = "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?w=800"
    private const val PHOTO_HANDS = "https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800"
    const val PHOTO_SEA = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"

    /** Hamburg, rounded like every stored place; the user's own place starts empty. */
    private val partnerLocation = GeoPoint(53.6, 10.0)

    private const val HOUR_MILLIS = 60 * 60 * 1000L
    private const val DAY_MILLIS = 24 * HOUR_MILLIS

    fun space(today: LocalDate): CoupleSpace {
        val anniversary = anniversary(today)
        return CoupleSpace(
            id = SPACE_ID,
            partner1Name = "Mila",
            partner2Name = "Noah",
            anniversaryYear = anniversary.year,
            anniversaryMonth = anniversary.monthValue,
            anniversaryDay = anniversary.dayOfMonth,
            isSetupComplete = true,
            isActive = true,
            userUids = listOf(PARTNER_UID, USER_UID),
            partner1Id = PARTNER_UID,
            partner2Id = USER_UID,
            pairingCode = PAIRING_CODE
        )
    }

    /** A space created from the user's own code; the partner joins it, and both still have to set it up. */
    fun joinedSpace(code: String): CoupleSpace = CoupleSpace(
        id = "space_$code",
        isSetupComplete = false,
        isActive = true,
        userUids = listOf(USER_UID, PARTNER_UID),
        partner1Id = USER_UID,
        partner2Id = PARTNER_UID,
        pairingCode = code
    )

    private fun anniversary(today: LocalDate): LocalDate = today.minusYears(2).minusMonths(4).minusDays(9)

    fun memories(today: LocalDate): List<Memory> {
        val start = anniversary(today)
        return listOf(
            Memory(
                id = "demo_m1",
                title = "Unser erstes Date",
                date = start,
                note = "Pasta, viel zu lange geredet und den letzten Bus verpasst.",
                partner1ImageUrl = PHOTO_LAKE,
                partner2ImageUrl = PHOTO_HANDS,
                createdBy = PARTNER_UID,
                countsAsMilestone = true
            ),
            Memory(
                id = "demo_m2",
                title = "Wochenende an der Ostsee",
                date = start.plusMonths(5),
                note = "Barfuß am Strand, obwohl es eigentlich viel zu kalt war.",
                partner1ImageUrl = PHOTO_SEA,
                createdBy = PARTNER_UID
            ),
            Memory(
                id = "demo_m3",
                title = "Mila startet in Hamburg",
                date = start.plusMonths(14),
                note = "Erster Tag im neuen Job. Abends drei Stunden telefoniert.",
                createdBy = USER_UID
            ),
            Memory(
                id = "demo_m4",
                title = "Konzert im Stadtpark",
                date = start.plusMonths(20),
                partner2ImageUrl = PHOTO_HANDS,
                createdBy = USER_UID
            ),
            Memory(
                id = "demo_m5",
                title = "Sonntagsfrühstück per Video",
                date = today.minusDays(12),
                note = "Beide mit Croissants, nur 300 km auseinander.",
                createdBy = USER_UID
            )
        ).sortedByDescending { it.date }
    }

    fun customMilestones(today: LocalDate): List<CustomMilestone> = listOf(
        CustomMilestone(id = "demo_cm1", title = "Hochzeit von Lea & Tim", date = today.plusDays(40))
    )

    fun reunion(today: LocalDate, nowMillis: Long): Reunion = Reunion(
        startDate = today.plusDays(9),
        endDate = today.plusDays(12),
        plannedAtDate = today.minusDays(20),
        places = mapOf(PARTNER_UID to ReunionPlace(sharesLocation = true, location = partnerLocation, locatedAtMillis = nowMillis))
    )

    fun notes(nowMillis: Long): List<SharedNote> = listOf(
        SharedNote(
            id = "demo_n1",
            title = "Packliste fürs Wiedersehen",
            type = NoteType.CHECKLIST,
            pinned = true,
            items = items(USER_UID, "Ladekabel" to true, "Zugtickets aufs Handy laden" to false, "Regenjacke" to false, "Das Buch, das ich dir leihen wollte" to false),
            createdBy = USER_UID,
            updatedBy = USER_UID,
            createdAt = nowMillis - 6 * DAY_MILLIS,
            updatedAt = nowMillis - 3 * DAY_MILLIS
        ),
        SharedNote(
            id = "demo_n2",
            title = "Wochenendeinkauf",
            type = NoteType.CHECKLIST,
            labelId = DefaultNoteLabelIds.SHOPPING,
            items = items(PARTNER_UID, "Hafermilch" to false, "Zitronen" to false, "Basilikum" to false, "Kaffeebohnen" to true),
            createdBy = PARTNER_UID,
            updatedBy = PARTNER_UID,
            createdAt = nowMillis - 2 * DAY_MILLIS,
            updatedAt = nowMillis - 2 * HOUR_MILLIS
        ),
        SharedNote(
            id = "demo_n3",
            title = "Filmabend-Ideen",
            body = "Before Sunrise\nPast Lives\nIrgendwas von Ghibli",
            labelId = DefaultNoteLabelIds.IDEAS,
            createdBy = PARTNER_UID,
            updatedBy = PARTNER_UID,
            createdAt = nowMillis - 4 * DAY_MILLIS,
            updatedAt = nowMillis - DAY_MILLIS
        ),
        SharedNote(
            id = "demo_n4",
            title = "Irgendwann zusammen",
            type = NoteType.CHECKLIST,
            labelId = DefaultNoteLabelIds.BUCKET_LIST,
            items = items(USER_UID, "Nordlichter sehen" to false, "Eine Woche ohne Handy" to false, "Tanzen lernen" to false),
            createdBy = USER_UID,
            updatedBy = USER_UID,
            createdAt = nowMillis - 30 * DAY_MILLIS,
            updatedAt = nowMillis - 5 * DAY_MILLIS
        )
    )

    /** What the simulated partner adds while the app is open, so notifications and unread badges show up. */
    fun partnerMoment(today: LocalDate): Memory = Memory(
        title = "Sonnenuntergang am Hafen",
        date = today,
        note = "Hab an dich gedacht.",
        createdBy = PARTNER_UID
    )

    fun partnerNote(nowMillis: Long): SharedNote = SharedNote(
        id = "demo_n_partner_${nowMillis}",
        title = "Für Samstag",
        type = NoteType.CHECKLIST,
        items = items(PARTNER_UID, "Picknickdecke" to false, "Erdbeeren" to false),
        createdBy = PARTNER_UID,
        updatedBy = PARTNER_UID,
        createdAt = nowMillis,
        updatedAt = nowMillis
    )

    private fun items(author: String, vararg entries: Pair<String, Boolean>): List<NoteItem> =
        entries.mapIndexed { index, (text, checked) ->
            NoteItem(id = "item_${text.hashCode()}", text = text, checked = checked, createdBy = author, position = index.toLong())
        }
}
