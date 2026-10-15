package com.aistudio.couplebubble.qxztrw.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.data.local.displayRoleFlow
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipCalendar
import com.aistudio.couplebubble.qxztrw.domain.model.momentMilestones
import com.aistudio.couplebubble.qxztrw.domain.repository.CoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.session.MilestoneReminder
import com.aistudio.couplebubble.qxztrw.domain.session.PartnerMomentWatcher
import com.aistudio.couplebubble.qxztrw.domain.session.PartnerRoleSync
import com.aistudio.couplebubble.qxztrw.domain.session.SessionRestorer
import com.aistudio.couplebubble.qxztrw.domain.session.SpaceContent
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.widget.WidgetPublisher
import com.aistudio.couplebubble.qxztrw.widget.widgetSnapshotOf
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Decides which screen the app shows and runs the background work of a session: restoring it, keeping the
 * stored session in step with the space, partner and milestone notifications, widgets and partner roles.
 */
class AppViewModel(
    private val repository: CoupleRepository,
    private val sessionStore: SessionStore,
    private val restorer: SessionRestorer,
    private val googleAuth: GoogleAuthController,
    private val spaceContent: SpaceContent,
    momentNotifier: MomentNotifier,
    widgetPublisher: WidgetPublisher,
    private val clock: Clock = Clock.systemDefaultZone(),
    started: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val partnerMoments = PartnerMomentWatcher(momentNotifier, sessionStore)
    private val milestoneReminder = MilestoneReminder(momentNotifier, sessionStore)
    private val roleSync = PartnerRoleSync(repository, sessionStore)
    private var silentRestoreAttempted = false

    val route: StateFlow<AppRoute> = combine(
        repository.currentSpace,
        restorer.isRestored,
        sessionStore.onboardingCompletedFlow
    ) { space, isRestored, onboardingCompleted ->
        when {
            space != null && space.isActive -> AppRoute.PAIRED
            !isRestored && space == null -> AppRoute.LOADING
            !onboardingCompleted -> AppRoute.ONBOARDING
            else -> AppRoute.UNPAIRED
        }
    }.stateIn(viewModelScope, started, AppRoute.LOADING)

    fun completeOnboarding() {
        viewModelScope.launch {
            sessionStore.saveOnboardingCompleted(true)
        }
    }

    init {
        viewModelScope.launch { restorer.restore() }

        // The stored session follows the space: kept while active, cleared once the space was disconnected
        viewModelScope.launch {
            repository.currentSpace.filterNotNull().collect { space ->
                if (space.isActive) {
                    sessionStore.saveActiveCoupleId(space.id)
                    sessionStore.saveOnboardingCompleted(true)
                } else {
                    sessionStore.clearSession()
                }
            }
        }

        // Once restored, no space means the couple is gone (partner disconnected or access lost): clear widget and session
        viewModelScope.launch {
            combine(repository.currentSpace, restorer.isRestored) { space, restored -> space == null && restored }
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    widgetPublisher.publish(null)
                    sessionStore.clearSession()
                }
        }

        viewModelScope.launch {
            combine(repository.currentSpace, repository.currentUserProfile, sessionStore.displayRoleFlow) { space, profile, role ->
                Triple(space, profile?.uid, role)
            }.collect { (space, uid, role) -> roleSync.sync(space, uid, role) }
        }

        viewModelScope.launch {
            spaceContent.spaceMemories.collect { update ->
                val space = repository.currentSpace.value
                when {
                    update == null -> partnerMoments.reset()
                    space?.id == update.first ->
                        partnerMoments.onMemories(space, update.second, repository.currentUserProfile.value?.uid)
                }
            }
        }

        val counterPreferences = sessionStore.counterPreferencesFlow
        val momentMilestones = spaceContent.memories.map { it.momentMilestones() }.distinctUntilChanged()
        viewModelScope.launch {
            // A null space only means "still loading"; leaving the space clears the widgets explicitly
            combine(
                repository.currentSpace.filterNotNull(),
                counterPreferences,
                spaceContent.customMilestones,
                momentMilestones,
                spaceContent.reunion,
                ::widgetSnapshotOf
            )
                .distinctUntilChanged()
                .collect { widgetPublisher.publish(it) }
        }
        viewModelScope.launch {
            combine(repository.currentSpace, counterPreferences, spaceContent.customMilestones, momentMilestones) { space, counter, custom, moments ->
                space?.takeIf { it.isActive }?.let {
                    it.id to RelationshipCalendar.of(it, counter, custom, moments).metrics(LocalDate.now(clock))
                }
            }.collect { update -> update?.let { (spaceId, metrics) -> milestoneReminder.onMetrics(spaceId, metrics) } }
        }
    }

    /**
     * After a reinstall the anonymous UID is gone, but a Google account that already used the app still
     * points to the space via `/users/{uid}`. Once per app start, an unpaired device without a Google
     * login tries a picker-free sign-in with that account and returns to the space on its own.
     */
    fun tryRestoreWithGoogle(activityContext: Context) {
        if (silentRestoreAttempted) return
        silentRestoreAttempted = true
        viewModelScope.launch {
            restorer.isRestored.first { it }
            if (repository.currentSpace.value != null || repository.currentUserProfile.value?.email != null) return@launch
            googleAuth.signInSilently(activityContext)
        }
    }
}
