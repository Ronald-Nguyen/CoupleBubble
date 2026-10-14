package com.aistudio.couplebubble.qxztrw.core.di

import android.content.Context
import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleAuthClient
import com.aistudio.couplebubble.qxztrw.data.location.AndroidLocationProvider
import com.aistudio.couplebubble.qxztrw.data.location.LocationProvider
import com.aistudio.couplebubble.qxztrw.data.network.AndroidConnectivityObserver
import com.aistudio.couplebubble.qxztrw.data.network.ConnectivityObserver
import com.aistudio.couplebubble.qxztrw.data.local.AndroidLocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.DataStoreSessionStore
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.RemoteImageUploader
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseMemoryRepository
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseMilestoneRepository
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseNotesRepository
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseReunionRepository
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseSpaceRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.NotesRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.ReunionRepository
import com.aistudio.couplebubble.qxztrw.domain.session.SessionRestorer
import com.aistudio.couplebubble.qxztrw.domain.session.SpaceContent
import com.aistudio.couplebubble.qxztrw.notifications.AndroidMomentNotifier
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.ui.defaultNoteLabels
import com.aistudio.couplebubble.qxztrw.ui.screens.dashboard.DashboardViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.home.GoogleAuthController
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.pairing.PairingViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.settings.SettingsViewModel
import com.aistudio.couplebubble.qxztrw.widget.AndroidWidgetPublisher
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph: one instance of every collaborator per process, plus the ViewModel factories.
 * Session restore, Google sign-in state and the shared Firestore listeners live here, so the ViewModels that
 * use them never depend on each other.
 */
interface AppContainer {
    val dispatchers: AppDispatchers
    val coupleRepository: CoupleRepository
    val notesRepository: NotesRepository
    val reunionRepository: ReunionRepository
    val locationProvider: LocationProvider
    val connectivityObserver: ConnectivityObserver
    val sessionStore: SessionStore
    val localImageStore: LocalImageStore
    val momentNotifier: MomentNotifier
    val widgetPublisher: WidgetPublisher
    val sessionRestorer: SessionRestorer
    val googleAuth: GoogleAuthController
    val spaceContent: SpaceContent

    /** Lives as long as the process; for work that must outlast a ViewModel. */
    val appScope: CoroutineScope

    fun createAppViewModel(): AppViewModel = AppViewModel(
        repository = coupleRepository,
        sessionStore = sessionStore,
        restorer = sessionRestorer,
        googleAuth = googleAuth,
        spaceContent = spaceContent,
        momentNotifier = momentNotifier,
        widgetPublisher = widgetPublisher
    )

    fun createPairingViewModel(): PairingViewModel = PairingViewModel(
        repository = coupleRepository,
        sessionStore = sessionStore,
        restorer = sessionRestorer,
        googleAuth = googleAuth
    )

    fun createDashboardViewModel(): DashboardViewModel = DashboardViewModel(
        repository = coupleRepository,
        sessionStore = sessionStore,
        localImageStore = localImageStore,
        googleAuth = googleAuth,
        widgetPublisher = widgetPublisher,
        spaceContent = spaceContent,
        reunionRepository = reunionRepository,
        locationProvider = locationProvider,
        connectivity = connectivityObserver,
        appScope = appScope
    )

    fun createNotesViewModel(resources: Resources): NotesViewModel = NotesViewModel(
        notesRepository = notesRepository,
        spaceFlow = coupleRepository.currentSpace,
        userProfileFlow = coupleRepository.currentUserProfile,
        defaultLabels = defaultNoteLabels(resources),
        sessionStore = sessionStore,
        appScope = appScope
    )

    fun createSettingsViewModel(): SettingsViewModel = SettingsViewModel(
        repository = coupleRepository,
        sessionStore = sessionStore,
        googleAuth = googleAuth,
        spaceContent = spaceContent,
        reunionRepository = reunionRepository,
        connectivity = connectivityObserver,
        widgetPublisher = widgetPublisher,
        appScope = appScope
    )
}

/** Production wiring; created lazily by `CoupleBubbleApplication`. */
class DefaultAppContainer(private val appContext: Context) : AppContainer {
    override val dispatchers = AppDispatchers()

    // Lives as long as the process; shared listeners stop on their own once nobody collects them
    override val appScope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private val firebase by lazy { FirebaseProvider() }
    private val imageUploader by lazy { RemoteImageUploader(firebase) }
    private val spaceRepository by lazy { FirebaseSpaceRepository(firebase, imageUploader, localImageStore, dispatchers) }

    override val localImageStore: LocalImageStore by lazy { AndroidLocalImageStore(appContext, dispatchers) }
    override val coupleRepository: CoupleRepository by lazy {
        FirebaseCoupleRepository(
            spaces = spaceRepository,
            memories = FirebaseMemoryRepository(firebase, imageUploader, spaceRepository),
            milestones = FirebaseMilestoneRepository(firebase)
        )
    }
    override val notesRepository: NotesRepository by lazy { FirebaseNotesRepository(firebase) }
    override val reunionRepository: ReunionRepository by lazy { FirebaseReunionRepository(firebase) }
    override val locationProvider: LocationProvider by lazy { AndroidLocationProvider(appContext) }
    override val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
    override val sessionStore: SessionStore by lazy { DataStoreSessionStore(appContext) }
    override val momentNotifier: MomentNotifier by lazy { AndroidMomentNotifier(appContext) }
    override val widgetPublisher: WidgetPublisher by lazy { AndroidWidgetPublisher(appContext) }
    override val sessionRestorer by lazy { SessionRestorer(coupleRepository, sessionStore) }
    override val googleAuth by lazy {
        GoogleAuthController(coupleRepository, GoogleAuthClient(appContext), sessionStore)
    }
    override val spaceContent by lazy { SpaceContent(coupleRepository, reunionRepository, appScope) }
}
