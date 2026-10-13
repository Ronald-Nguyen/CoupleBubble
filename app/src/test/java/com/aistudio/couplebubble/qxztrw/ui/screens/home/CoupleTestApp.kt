package com.aistudio.couplebubble.qxztrw.ui.screens.home

import com.aistudio.couplebubble.qxztrw.data.fake.FakeGoogleSignInClient
import com.aistudio.couplebubble.qxztrw.data.fake.FakeLocalImageStore
import com.aistudio.couplebubble.qxztrw.data.fake.FakeLocationProvider
import com.aistudio.couplebubble.qxztrw.data.fake.FakeSessionStore
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.data.fake.MockReunionRepository
import com.aistudio.couplebubble.qxztrw.data.fake.RecordingMomentNotifier
import com.aistudio.couplebubble.qxztrw.data.fake.RecordingWidgetPublisher
import com.aistudio.couplebubble.qxztrw.domain.session.SessionRestorer
import com.aistudio.couplebubble.qxztrw.domain.session.SpaceContent
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingViewModel
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** The three couple ViewModels wired like `DefaultAppContainer` does, on fakes and the test's scheduler. */
@OptIn(ExperimentalCoroutinesApi::class)
class CoupleTestApp(
    testScope: TestScope,
    val repository: MockCoupleRepository = MockCoupleRepository(),
    val sessionStore: FakeSessionStore = FakeSessionStore(),
    clock: Clock = Clock.systemDefaultZone()
) {
    val imageStore = FakeLocalImageStore()
    val notifier = RecordingMomentNotifier()
    val widgets = RecordingWidgetPublisher()
    val googleClient = FakeGoogleSignInClient()
    val reunions = MockReunionRepository()
    val location = FakeLocationProvider()

    private val restorer = SessionRestorer(repository, sessionStore)
    private val googleAuth = GoogleAuthController(repository, googleClient, sessionStore)
    private val spaceContent = SpaceContent(repository, testScope.backgroundScope, SharingStarted.Eagerly)

    val app = AppViewModel(
        repository, sessionStore, restorer, googleAuth, spaceContent, notifier, widgets, clock, SharingStarted.Eagerly
    )
    val pairing = PairingViewModel(repository, sessionStore, restorer, googleAuth, SharingStarted.Eagerly)
    val dashboard = DashboardViewModel(
        repository, sessionStore, imageStore, googleAuth, widgets, spaceContent, reunions, location, clock,
        SharingStarted.Eagerly
    )

    val route: AppRoute get() = app.route.value
    val pairingState: PairingUiState get() = pairing.uiState.value
    val dashboardState: DashboardUiState get() = requireNotNull(dashboard.uiState.value) { "Not paired" }

    /** Effects of both screens in the order they were sent, received the moment they are sent. */
    val effects: List<CoupleEffect> by lazy {
        val received = mutableListOf<CoupleEffect>()
        val unconfined = UnconfinedTestDispatcher(testScope.testScheduler)
        testScope.backgroundScope.launch(unconfined) { pairing.effects.collect { received += it } }
        testScope.backgroundScope.launch(unconfined) { dashboard.effects.collect { received += it } }
        received
    }
}
