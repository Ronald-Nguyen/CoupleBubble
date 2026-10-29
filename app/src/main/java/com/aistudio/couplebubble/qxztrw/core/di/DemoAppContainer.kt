package com.aistudio.couplebubble.qxztrw.core.di

import android.content.Context
import android.util.Log
import com.aistudio.couplebubble.qxztrw.data.demo.DemoCoupleRepository
import com.aistudio.couplebubble.qxztrw.data.demo.DemoGoogleSignInClient
import com.aistudio.couplebubble.qxztrw.data.demo.DemoNotesRepository
import com.aistudio.couplebubble.qxztrw.data.demo.DemoPartner
import com.aistudio.couplebubble.qxztrw.data.demo.DemoReunionRepository
import com.aistudio.couplebubble.qxztrw.data.local.AndroidLocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.DataStoreSessionStore
import com.aistudio.couplebubble.qxztrw.data.local.LocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.data.location.AndroidLocationProvider
import com.aistudio.couplebubble.qxztrw.data.location.LocationProvider
import com.aistudio.couplebubble.qxztrw.data.network.AndroidConnectivityObserver
import com.aistudio.couplebubble.qxztrw.data.network.ConnectivityObserver
import com.aistudio.couplebubble.qxztrw.domain.session.SessionRestorer
import com.aistudio.couplebubble.qxztrw.domain.session.SpaceContent
import com.aistudio.couplebubble.qxztrw.notifications.AndroidMomentNotifier
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.ui.screens.home.GoogleAuthController
import com.aistudio.couplebubble.qxztrw.widget.AndroidWidgetPublisher
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Wiring of the demo mode (`BuildConfig.DEMO_MODE`): space, moments, milestones, notes and reunion live in memory
 * with sample content and a simulated partner, so the app runs without `google-services.json`. Everything else
 * (DataStore, local photos, notifications, widgets, location, connectivity) is the real Android implementation.
 */
class DemoAppContainer(private val appContext: Context) : AppContainer {
    override val dispatchers = AppDispatchers()

    override val appScope = CoroutineScope(
        SupervisorJob() + dispatchers.main + CoroutineExceptionHandler { _, error ->
            Log.e("CoupleBubble", "Unhandled error in demo app scope", error)
        }
    )

    override val localImageStore: LocalImageStore by lazy { AndroidLocalImageStore(appContext, dispatchers) }
    private val demoCoupleRepository by lazy { DemoCoupleRepository(localImageStore, appScope) }
    private val demoNotesRepository by lazy { DemoNotesRepository() }

    override val coupleRepository get() = demoCoupleRepository
    override val notesRepository get() = demoNotesRepository
    override val reunionRepository by lazy { DemoReunionRepository() }
    override val locationProvider: LocationProvider by lazy { AndroidLocationProvider(appContext) }
    override val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
    override val sessionStore: SessionStore by lazy { DataStoreSessionStore(appContext) }
    override val momentNotifier: MomentNotifier by lazy { AndroidMomentNotifier(appContext) }
    override val widgetPublisher: WidgetPublisher by lazy { AndroidWidgetPublisher(appContext) }
    override val sessionRestorer by lazy { SessionRestorer(coupleRepository, sessionStore) }
    override val googleAuth by lazy { GoogleAuthController(coupleRepository, DemoGoogleSignInClient(), sessionStore) }
    override val spaceContent by lazy { SpaceContent(coupleRepository, reunionRepository, appScope) }

    init {
        appScope.launch { DemoPartner(demoCoupleRepository, demoNotesRepository).run() }
    }
}
