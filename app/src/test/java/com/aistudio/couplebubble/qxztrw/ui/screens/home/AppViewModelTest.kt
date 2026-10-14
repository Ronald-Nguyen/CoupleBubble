package com.aistudio.couplebubble.qxztrw.ui.screens.home

import com.aistudio.couplebubble.qxztrw.data.fake.FakeSessionStore
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstStartWithoutAStoredSpaceGoesToOnboarding() = runTest {
        val app = CoupleTestApp(this)
        assertEquals(AppRoute.LOADING, app.route)

        runCurrent()

        assertEquals(AppRoute.ONBOARDING, app.route)
    }

    @Test
    fun completingOnboardingTransitionsToPairing() = runTest {
        val app = CoupleTestApp(this)
        runCurrent()
        assertEquals(AppRoute.ONBOARDING, app.route)

        app.app.completeOnboarding()
        runCurrent()

        assertEquals(AppRoute.UNPAIRED, app.route)
        assertTrue(app.sessionStore.onboardingCompletedFlow.value)
    }

    @Test
    fun completedOnboardingGoesDirectlyToPairing() = runTest {
        val store = FakeSessionStore(initialOnboardingCompleted = true)
        val app = CoupleTestApp(this, sessionStore = store)
        assertEquals(AppRoute.LOADING, app.route)

        runCurrent()

        assertEquals(AppRoute.UNPAIRED, app.route)
    }

    @Test
    fun storedSpaceIsRestoredWithoutShowingPairingOrOnboarding() = runTest {
        val store = FakeSessionStore(initialOnboardingCompleted = false).apply { saveActiveCoupleId("space_482913") }
        val app = CoupleTestApp(this, sessionStore = store)

        runCurrent()

        assertEquals(AppRoute.PAIRED, app.route)
        assertEquals("space_482913", app.dashboardState.space.id)
    }

    @Test
    fun aDisconnectedSpaceClearsTheStoredSession() = runTest {
        val repository = MockCoupleRepository()
        val app = CoupleTestApp(this, repository)
        runCurrent()
        repository.setStateForTesting(CoupleSpace(id = "space_1", isActive = true), null)
        runCurrent()
        assertEquals("space_1", app.sessionStore.activeCoupleIdFlow.value)

        repository.setStateForTesting(CoupleSpace(id = "space_1", isActive = false), null)
        runCurrent()

        assertNull(app.sessionStore.activeCoupleIdFlow.value)
        assertEquals(AppRoute.UNPAIRED, app.route)
    }

    @Test
    fun onlyMomentsThePartnerAddsLaterNotify() = runTest {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(
            CoupleSpace(id = "space_1", partner1Id = "uid_mia", partner2Id = "uid_leo", userUids = listOf("uid_mia", "uid_leo"), isActive = true),
            UserProfile(uid = "uid_mia")
        )
        val app = CoupleTestApp(this, repository)
        runCurrent()

        repository.addMemory("space_1", Memory(id = "new_leo", title = "Paris", date = LocalDate.of(2026, 1, 2), createdBy = "uid_leo"))
        repository.addMemory("space_1", Memory(id = "new_mia", title = "Eigenes", date = LocalDate.of(2026, 1, 3), createdBy = "uid_mia"))
        advanceTimeBy(500)
        runCurrent()

        assertEquals(listOf("new_leo"), app.notifier.moments)
    }

    @Test
    fun widgetsGetThePlannedReunionAndLoseItOnceDeleted() = runTest {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(
            CoupleSpace(id = "space_1", partner1Id = "uid_mia", partner2Id = "uid_leo", userUids = listOf("uid_mia", "uid_leo"), isActive = true),
            UserProfile(uid = "uid_mia")
        )
        val app = CoupleTestApp(this, repository)
        runCurrent()

        app.reunions.saveReunionDates("space_1", LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 8), LocalDate.of(2026, 10, 14))
        runCurrent()
        assertEquals(LocalDate.of(2026, 11, 6), app.widgets.published.last()?.reunion?.startDate)

        app.reunions.deleteReunion("space_1")
        runCurrent()
        assertNull(app.widgets.published.last()?.reunion)
    }

    @Test
    fun widgetsFollowTheSpaceAndAreClearedOnDisconnect() = runTest {
        val app = CoupleTestApp(this)
        runCurrent()

        app.pairing.onEvent(PairingEvent.OpenDemoSpace)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(CoupleSpace().partner1Name, app.widgets.published.last()?.partner1Name)

        app.dashboard.onEvent(DashboardEvent.ConfirmDisconnect)
        advanceTimeBy(500)
        runCurrent()
        assertNull(app.widgets.published.last())
        assertTrue(app.widgets.published.size >= 2)
    }

    @Test
    fun milestoneDueTodayNotifiesOnceOnTheDay() = runTest {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(
            CoupleSpace(id = "space_1", anniversaryYear = 2025, anniversaryMonth = 10, anniversaryDay = 13, isActive = true),
            null
        )
        val anniversary = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC)
        val app = CoupleTestApp(this, repository, clock = anniversary)
        runCurrent()

        assertEquals(listOf(MilestoneKind.YEARS), app.notifier.milestones.map { it.kind })
        assertEquals("space_1:2026-10-13", app.sessionStore.lastNotifiedMilestoneFlow.value)
    }

    @Test
    fun spaceSetupRejectsAFutureAnniversary() = runTest {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(CoupleSpace(id = "space_1", isActive = true), null)
        val today = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC)
        val app = CoupleTestApp(this, repository, clock = today)
        runCurrent()

        app.dashboard.onEvent(DashboardEvent.SaveSpaceSetup("Mia", "Leo", LocalDate.of(2026, 10, 14)))
        advanceTimeBy(500)
        runCurrent()
        assertEquals(CoupleSpace().partner1Name, app.dashboardState.space.partner1Name)

        app.dashboard.onEvent(DashboardEvent.SaveSpaceSetup("Mia", "Leo", LocalDate.of(2026, 10, 13)))
        advanceTimeBy(500)
        runCurrent()
        assertEquals("Mia", app.dashboardState.space.partner1Name)
    }
}
