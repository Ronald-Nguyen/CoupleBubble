package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlaceSource
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionRules
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionStatus
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleTestApp
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ReunionViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val today = LocalDate.now()
    private val spaceId = "space_test"

    private fun TestScope.settle() {
        advanceTimeBy(3_000)
        runCurrent()
    }

    private fun TestScope.pairedApp(uid: String = "uid_mia"): CoupleTestApp {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(
            CoupleSpace(
                id = spaceId,
                partner1Name = "Mia",
                partner2Name = "Leo",
                userUids = listOf("uid_mia", "uid_leo"),
                partner1Id = "uid_mia",
                partner2Id = "uid_leo"
            ),
            UserProfile(uid = uid)
        )
        return CoupleTestApp(this, repository).also { settle() }
    }

    @Test
    fun savingWithLocationStoresItRoundedUnderTheOwnUid() = runTest {
        val app = pairedApp()
        app.dashboard.onEvent(DashboardEvent.ShowReunionDialog(true))
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(10), today.plusDays(12), shareLocation = true))
        settle()

        val stored = app.reunions.reunions.value.getValue(spaceId)
        val own = stored.places.getValue("uid_mia")
        assertTrue(own.sharesLocation)
        assertEquals(GeoPoint(50.9, 7.0), own.location)
        assertEquals(today, stored.plannedAtDate)

        val state = app.dashboardState
        assertEquals(DashboardDialog.None, state.dialogs.dialog)
        assertEquals(10, (state.reunion?.status as ReunionStatus.Upcoming).daysUntil)
        assertTrue(state.reunion?.ownSource is ReunionPlaceSource.Location)
        assertEquals(ReunionPlaceSource.None, state.reunion?.partnerSource)
        assertNull(state.reunion?.distanceKm)
    }

    @Test
    fun withoutPermissionNothingIsStored() = runTest {
        val app = pairedApp()
        app.location.granted = false
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(3), today.plusDays(4), shareLocation = true))
        settle()

        assertNull(app.reunions.reunions.value.getValue(spaceId).places["uid_mia"])
        assertEquals(0, app.location.requests)
    }

    @Test
    fun locationTurnedOffOnTheDeviceKeepsSharingOnWithAHint() = runTest {
        val app = pairedApp()
        val effects = app.effects
        app.location.location = null
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(3), today.plusDays(4), shareLocation = true))
        settle()

        // Sharing stays on, so the next resume asks again; without a fix there is no distance
        val own = app.reunions.reunions.value.getValue(spaceId).places.getValue("uid_mia")
        assertTrue(own.sharesLocation)
        assertNull(own.location)
        assertTrue(effects.any { it is CoupleEffect.Snackbar })
    }

    @Test
    fun refusedSaveShowsAHintAndAsksForNoLocation() = runTest {
        val app = pairedApp()
        val effects = app.effects
        app.reunions.failWrites = true
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(3), today.plusDays(4), shareLocation = true))
        settle()

        assertEquals(listOf(CoupleEffect.Snackbar(UiText.Resource(R.string.reunion_save_failed))), effects)
        assertEquals(0, app.location.requests)
        assertNull(app.dashboardState.reunion)
    }

    @Test
    fun distanceCombinesBothLocations() = runTest {
        val app = pairedApp()
        app.reunions.saveReunionDates(spaceId, today.plusDays(5), today.plusDays(6), today)
        app.reunions.saveOwnPlace(spaceId, "uid_leo", ReunionPlace(sharesLocation = true, location = GeoPoint(52.5, 13.4), locatedAtMillis = System.currentTimeMillis()))
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(5), today.plusDays(6), shareLocation = true))
        settle()

        val reunion = requireNotNull(app.dashboardState.reunion)
        assertTrue(reunion.partnerSource is ReunionPlaceSource.Location)
        assertTrue("was ${reunion.distanceKm}", reunion.distanceKm in 460..490)
    }

    @Test
    fun refreshOnlyAsksAgainWhenTheLocationIsOlderThanAnHour() = runTest {
        val app = pairedApp()
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(5), today.plusDays(6), shareLocation = true))
        settle()
        assertEquals(1, app.location.requests)

        app.dashboard.onEvent(DashboardEvent.RefreshReunionLocation)
        settle()
        assertEquals(1, app.location.requests)

        val old = System.currentTimeMillis() - ReunionRules.LOCATION_REFRESH_MILLIS - 1
        val own = app.reunions.reunions.value.getValue(spaceId).places.getValue("uid_mia")
        app.reunions.saveOwnPlace(spaceId, "uid_mia", own.copy(locatedAtMillis = old))
        settle()
        app.dashboard.onEvent(DashboardEvent.RefreshReunionLocation)
        settle()
        assertEquals(2, app.location.requests)
        assertTrue(app.reunions.reunions.value.getValue(spaceId).places.getValue("uid_mia").locatedAtMillis!! > old)
    }

    @Test
    fun aReunionThatIsOverIsRemoved() = runTest {
        val app = pairedApp()
        app.reunions.reunions.value = mapOf(spaceId to Reunion(today.minusDays(4), today.minusDays(2)))
        settle()

        assertNull(app.dashboardState.reunion)
        assertFalse(spaceId in app.reunions.reunions.value)
    }

    @Test
    fun leavingTheSpaceRemovesTheOwnPlace() = runTest {
        val app = pairedApp()
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(5), today.plusDays(6), shareLocation = true))
        settle()

        app.dashboard.onEvent(DashboardEvent.ConfirmDisconnect)
        settle()

        assertNull(app.reunions.reunions.value.getValue(spaceId).places["uid_mia"])
    }

    @Test
    fun endBeforeStartIsRefused() = runTest {
        val app = pairedApp()
        app.dashboard.onEvent(DashboardEvent.ShowReunionDialog(true))
        app.dashboard.onEvent(DashboardEvent.SaveReunion(today.plusDays(5), today.plusDays(2), shareLocation = false))
        settle()

        assertTrue(app.reunions.reunions.value.isEmpty())
        assertEquals(DashboardDialog.Reunion, app.dashboardState.dialogs.dialog)
    }
}
