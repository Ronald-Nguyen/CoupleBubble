package com.aistudio.couplebubble.qxztrw.core.di

import android.content.Context
import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleAuthClient
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleSignInClient
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
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseSpaceRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.repository.NotesRepository
import com.aistudio.couplebubble.qxztrw.notifications.AndroidMomentNotifier
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.ui.defaultNoteLabels
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesViewModel
import com.aistudio.couplebubble.qxztrw.widget.AndroidWidgetPublisher
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher

/** Manual dependency graph: one instance of every collaborator per process, plus the ViewModel factories. */
interface AppContainer {
    val dispatchers: AppDispatchers
    val coupleRepository: CoupleRepository
    val notesRepository: NotesRepository
    val sessionStore: SessionStore
    val localImageStore: LocalImageStore
    val googleSignInClient: GoogleSignInClient
    val momentNotifier: MomentNotifier
    val widgetPublisher: WidgetPublisher

    fun createCoupleViewModel(): CoupleViewModel = CoupleViewModel(
        repository = coupleRepository,
        preferences = sessionStore,
        localImageStore = localImageStore,
        googleSignInClient = googleSignInClient,
        momentNotifier = momentNotifier,
        widgetPublisher = widgetPublisher
    )

    fun createNotesViewModel(coupleViewModel: CoupleViewModel, resources: Resources): NotesViewModel = NotesViewModel(
        notesRepository = notesRepository,
        spaceFlow = coupleViewModel.currentSpace,
        userProfileFlow = coupleViewModel.currentUserProfile,
        defaultLabels = defaultNoteLabels(resources)
    )
}

/** Production wiring; created lazily by `CoupleBubbleApplication`. */
class DefaultAppContainer(private val appContext: Context) : AppContainer {
    override val dispatchers = AppDispatchers()
    override val localImageStore: LocalImageStore by lazy { AndroidLocalImageStore(appContext, dispatchers) }
    private val firebase by lazy { FirebaseProvider() }
    private val imageUploader by lazy { RemoteImageUploader(firebase) }
    private val spaceRepository by lazy { FirebaseSpaceRepository(firebase, imageUploader, localImageStore, dispatchers) }
    override val coupleRepository: CoupleRepository by lazy {
        FirebaseCoupleRepository(
            spaces = spaceRepository,
            memories = FirebaseMemoryRepository(firebase, imageUploader, spaceRepository),
            milestones = FirebaseMilestoneRepository(firebase)
        )
    }
    override val notesRepository: NotesRepository by lazy { FirebaseNotesRepository(firebase) }
    override val sessionStore: SessionStore by lazy { DataStoreSessionStore(appContext) }
    override val googleSignInClient: GoogleSignInClient by lazy { GoogleAuthClient(appContext) }
    override val momentNotifier: MomentNotifier by lazy { AndroidMomentNotifier(appContext) }
    override val widgetPublisher: WidgetPublisher by lazy { AndroidWidgetPublisher(appContext) }
}
