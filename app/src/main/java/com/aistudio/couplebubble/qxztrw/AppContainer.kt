package com.aistudio.couplebubble.qxztrw

import android.content.Context
import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.auth.GoogleAuthClient
import com.aistudio.couplebubble.qxztrw.data.AndroidLocalImageStore
import com.aistudio.couplebubble.qxztrw.data.CoupleSessionPreferences
import com.aistudio.couplebubble.qxztrw.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.repository.FirebaseNotesRepository
import com.aistudio.couplebubble.qxztrw.ui.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.NotesViewModel
import com.aistudio.couplebubble.qxztrw.ui.defaultNoteLabels

/** Manual dependency wiring: builds the ViewModels with their production collaborators. */
class AppContainer(private val appContext: Context) {

    fun createCoupleViewModel(): CoupleViewModel = CoupleViewModel(
        repository = FirebaseCoupleRepository(appContext),
        preferences = CoupleSessionPreferences(appContext),
        localImageStore = AndroidLocalImageStore(appContext),
        googleSignInClient = GoogleAuthClient(appContext)
    )

    fun createNotesViewModel(coupleViewModel: CoupleViewModel, resources: Resources): NotesViewModel = NotesViewModel(
        notesRepository = FirebaseNotesRepository(),
        spaceFlow = coupleViewModel.currentSpace,
        userProfileFlow = coupleViewModel.currentUserProfile,
        defaultLabels = defaultNoteLabels(resources)
    )
}
