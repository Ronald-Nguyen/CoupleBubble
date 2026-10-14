package com.aistudio.couplebubble.qxztrw.widget

import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetSnapshotStoreTest {

    private val now = 1_760_000_000_000L
    private val snapshot = WidgetSnapshot(
        partner1Name = "Mia",
        partner2Name = "Leo",
        anniversaryYear = 2021,
        anniversaryMonth = 5,
        anniversaryDay = 12,
        enabledMilestoneKinds = setOf(MilestoneKind.DAYS, MilestoneKind.YEARS),
        customMilestones = emptyList()
    )

    @Test
    fun reunionSurvivesTheRoundTripWithoutUids() {
        val reunion = Reunion(
            startDate = LocalDate.of(2026, 11, 6),
            endDate = LocalDate.of(2026, 11, 8),
            plannedAtDate = LocalDate.of(2026, 10, 14),
            places = mapOf(
                "uid_mia" to ReunionPlace(sharesLocation = true, location = GeoPoint(50.9, 7.0), locatedAtMillis = now),
                "uid_leo" to ReunionPlace(sharesLocation = true, location = GeoPoint(52.5, 13.4), locatedAtMillis = now)
            )
        )

        val decoded = AndroidWidgetPublisher.decode(AndroidWidgetPublisher.encode(snapshot.copy(reunion = reunion)))

        val stored = requireNotNull(decoded.reunion)
        assertEquals(reunion.startDate, stored.startDate)
        assertEquals(reunion.endDate, stored.endDate)
        assertEquals(reunion.plannedAtDate, stored.plannedAtDate)
        assertEquals(reunion.places.values.toSet(), stored.places.values.toSet())
        assertEquals(reunion.distanceKm(now), stored.distanceKm(now))
    }

    @Test
    fun placeWithoutLocationStaysEmpty() {
        val reunion = Reunion(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8), places = mapOf("uid_mia" to ReunionPlace()))

        val decoded = AndroidWidgetPublisher.decode(AndroidWidgetPublisher.encode(snapshot.copy(reunion = reunion)))

        assertEquals(listOf(ReunionPlace()), decoded.reunion?.places?.values?.toList())
    }

    @Test
    fun snapshotWithoutReunionDecodesToNone() {
        assertNull(AndroidWidgetPublisher.decode(AndroidWidgetPublisher.encode(snapshot)).reunion)
    }
}
