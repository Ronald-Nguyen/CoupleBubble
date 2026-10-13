package com.aistudio.couplebubble.qxztrw.core.di

import android.content.Context
import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.data.auth.GoogleAuthClient
import com.aistudio.couplebubble.qxztrw.data.local.AndroidLocalImageStore
import com.aistudio.couplebubble.qxztrw.data.local.CoupleSessionPreferences
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.data.repository.FirebaseNotesRepository
import com.aistudio.couplebubble.qxztrw.notifications.AndroidMomentNotifier
import com.aistudio.couplebubble.qxztrw.ui.defaultNoteLabels
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.screens.notes.NotesViewModel
import com.aistudio.couplebubble.qxztrw.widget.AndroidWidgetPublisher

/** Manual dependency wiring: builds the ViewModels with their production collaborators. */
class AppContainer(private val appContext: Context) {

    fun createCoupleViewModel(): CoupleViewModel = CoupleViewModel(
        repository = FirebaseCoupleRepository(appContext),
        preferences = CoupleSessionPreferences(appContext),
        localImageStore = AndroidLocalImageStore(appContext),
        googleSignInClient = GoogleAuthClient(appContext),
        momentNotifier = AndroidMomentNotifier(appContext),
        widgetPublisher = AndroidWidgetPublisher(appContext)
    )

    fun createNotesViewModel(coupleViewModel: CoupleViewModel, resources: Resources): NotesViewModel = NotesViewModel(
        notesRepository = FirebaseNotesRepository(),
        spaceFlow = coupleViewModel.currentSpace,
        userProfileFlow = coupleViewModel.currentUserProfile,
        defaultLabels = defaultNoteLabels(resources)
    )
}
