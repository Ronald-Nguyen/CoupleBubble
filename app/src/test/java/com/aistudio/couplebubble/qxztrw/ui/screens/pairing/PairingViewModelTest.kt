package com.aistudio.couplebubble.qxztrw.ui.screens.pairing

import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceFullException
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppRoute
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleTestApp
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FullSpaceRepository : MockCoupleRepository() {
        override suspend fun connectWithCode(code: String): Result<CoupleSpace> = Result.failure(SpaceFullException())
    }

    private fun TestScope.startedApp(repository: MockCoupleRepository = MockCoupleRepository()) =
        CoupleTestApp(this, repository).also { runCurrent() }

    private fun TestScope.enterCodeIntoFullSpace(app: CoupleTestApp) {
        app.pairing.onEvent(PairingEvent.SelectTab(PairingTab.ENTER))
        app.pairing.onEvent(PairingEvent.EnteredCodeChanged("482913"))
        app.pairing.onEvent(PairingEvent.Connect)
        advanceTimeBy(500)
        runCurrent()
    }

    @Test
    fun startsUnpairedOnTheCreateTab() = runTest {
        val app = startedApp()

        assertEquals(AppRoute.UNPAIRED, app.route)
        assertEquals(PairingTab.CREATE, app.pairingState.selectedTab)
        assertFalse(app.pairingState.isLoading)
        assertNull(app.pairingState.errorMessage)
    }

    @Test
    fun tabSelection() = runTest {
        val app = startedApp()

        app.pairing.onEvent(PairingEvent.SelectTab(PairingTab.ENTER))
        runCurrent()

        assertEquals(PairingTab.ENTER, app.pairingState.selectedTab)
    }

    @Test
    fun enteredCodeKeepsSixDigits() = runTest {
        val app = startedApp()

        app.pairing.onEvent(PairingEvent.EnteredCodeChanged("48a2-91 37"))
        runCurrent()

        assertEquals("482913", app.pairingState.enteredCode)
    }

    @Test
    fun unpairedDeviceGetsItsOwnRandomCode() = runTest {
        val generated = mutableListOf<String>()
        val repository = object : MockCoupleRepository() {
            override suspend fun generateNewPairingCode() = super.generateNewPairingCode().also { generated += it.code }
        }
        val app = startedApp(repository)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, generated.size)
        assertEquals(generated.single(), app.pairingState.generatedCode)
        assertTrue(app.pairingState.countdownSeconds > 0)
    }

    @Test
    fun leavingASpaceCreatesAFreshCode() = runTest {
        val app = startedApp()
        advanceTimeBy(500)
        val firstCode = app.pairingState.generatedCode
        app.pairing.onEvent(PairingEvent.OpenDemoSpace)
        advanceTimeBy(500)
        runCurrent()

        app.dashboard.onEvent(DashboardEvent.ConfirmDisconnect)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(AppRoute.UNPAIRED, app.route)
        assertTrue(app.pairingState.generatedCode.isNotEmpty())
        assertNotEquals(firstCode, app.pairingState.generatedCode)
    }

    @Test
    fun shortCodeShowsAnError() = runTest {
        val app = startedApp()

        app.pairing.onEvent(PairingEvent.EnteredCodeChanged("123"))
        app.pairing.onEvent(PairingEvent.Connect)
        runCurrent()

        assertEquals(UiText.Resource(R.string.pairing_code_incomplete), app.pairingState.errorMessage)
    }

    @Test
    fun successfulConnectOpensTheDashboard() = runTest {
        val app = startedApp()

        app.pairing.onEvent(PairingEvent.EnteredCodeChanged("482913"))
        app.pairing.onEvent(PairingEvent.Connect)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(AppRoute.PAIRED, app.route)
        assertEquals("Alex", app.dashboardState.space.partner1Name)
        assertEquals("Sam", app.dashboardState.space.partner2Name)
        assertEquals("2", app.sessionStore.partnerRoleFlow.value)
    }

    @Test
    fun demoSpaceOpensTheDashboard() = runTest {
        val app = startedApp()

        app.pairing.onEvent(PairingEvent.OpenDemoSpace)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(AppRoute.PAIRED, app.route)
    }

    @Test
    fun fullSpaceShowsItsDialogInsteadOfAnError() = runTest {
        val app = startedApp(FullSpaceRepository())

        enterCodeIntoFullSpace(app)

        assertTrue(app.pairingState.showSpaceFullDialog)
        assertNull(app.pairingState.errorMessage)
        assertFalse(app.pairingState.isLoading)
    }

    @Test
    fun fullSpaceDialogRecheckClearsTheInput() = runTest {
        val app = startedApp(FullSpaceRepository())
        enterCodeIntoFullSpace(app)

        app.pairing.onEvent(PairingEvent.RecheckCodeAfterSpaceFull)
        runCurrent()

        assertFalse(app.pairingState.showSpaceFullDialog)
        assertEquals("", app.pairingState.enteredCode)
        assertEquals(PairingTab.ENTER, app.pairingState.selectedTab)
    }

    @Test
    fun fullSpaceDialogCreateOwnSpaceSwitchesToCreateTab() = runTest {
        val app = startedApp(FullSpaceRepository())
        enterCodeIntoFullSpace(app)

        app.pairing.onEvent(PairingEvent.CreateOwnSpaceAfterSpaceFull)
        runCurrent()

        assertFalse(app.pairingState.showSpaceFullDialog)
        assertEquals("", app.pairingState.enteredCode)
        assertEquals(PairingTab.CREATE, app.pairingState.selectedTab)
    }

    @Test
    fun googleProfileReachesThePairingScreen() = runTest {
        val app = startedApp()

        app.repository.signInWithGoogleUser(uid = "google_user_123", email = "partner@example.com", displayName = "Partner Name")
        runCurrent()

        val profile = app.pairingState.userProfile
        assertEquals("google_user_123", profile?.uid)
        assertEquals("partner@example.com", profile?.email)
        assertEquals("Partner Name", profile?.displayName)
    }

    @Test
    fun googleSignOutClearsTheProfile() = runTest {
        val app = startedApp()
        app.repository.signInWithGoogleUser(uid = "google_user_123", email = "partner@example.com", displayName = "Partner Name")
        runCurrent()

        app.pairing.onEvent(PairingEvent.SignOutGoogle)
        runCurrent()

        assertNull(app.pairingState.userProfile)
        assertEquals(1, app.googleClient.signOutCount)
    }

    @Test
    fun clearingTheGoogleErrorLeavesNoError() = runTest {
        val app = startedApp()

        app.pairing.onEvent(PairingEvent.ClearGoogleAuthError)
        advanceUntilIdle()

        assertNull(app.pairingState.googleAuth.error)
    }
}
