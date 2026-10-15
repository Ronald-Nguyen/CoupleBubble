package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.data.local.SessionStore
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceUnavailableException
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.locksOut
import com.aistudio.couplebubble.qxztrw.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Brings the device back into its space once per process: first through the user anchor `/users/{uid}`, then
 * through the space id stored on the device. [isRestored] stays `false` until that check is done, so the app
 * shows a loading screen instead of flashing the pairing screen. A stored space the device has lost access to
 * (Firebase login gone, or a new UID facing a full space) is left instead of shown from the offline cache,
 * where every write would be refused silently; [lostAccess] then tells the pairing screen why.
 */
class SessionRestorer(
    private val repository: SpaceRepository,
    private val sessionStore: SessionStore,
    private val spaceLoadTimeoutMillis: Long = 10_000L
) {
    private val _isRestored = MutableStateFlow(false)
    val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()

    /** `true` once the check left a stored space because this device can no longer read or write it. */
    var lostAccess: Boolean = false
        private set

    /** Called once the pairing screen showed the reason, so a new screen in this process does not repeat it. */
    fun acknowledgeLostAccess() {
        lostAccess = false
    }

    private var started = false

    /** Runs the check; later calls (e.g. a recreated ViewModel) return right away. */
    suspend fun restore() {
        if (started) return
        started = true

        val uid = repository.ensureSignedIn()
        if (uid != null) {
            val space = repository.restoreSessionForUser(uid).getOrNull()
            if (space != null && !leaveIfLockedOut(space, uid)) {
                sessionStore.saveActiveCoupleId(space.id)
                sessionStore.saveOnboardingCompleted(true)
                _isRestored.value = true
                return
            }
        }

        val activeCoupleId = sessionStore.activeCoupleIdFlow.firstOrNull()
        if (!activeCoupleId.isNullOrBlank()) {
            sessionStore.saveOnboardingCompleted(true)
            val result = repository.restoreSession(activeCoupleId)
            result.getOrNull()?.let { leaveIfLockedOut(it, uid) }
            if (result.exceptionOrNull() is SpaceUnavailableException) {
                // Offline without cache: keep the session and the loading screen until the listener delivers
                withTimeoutOrNull(spaceLoadTimeoutMillis) {
                    repository.currentSpace.first { it != null }
                }
            } else if (result.isFailure) {
                sessionStore.clearSession()
            }
        }
        _isRestored.value = true
    }

    private suspend fun leaveIfLockedOut(space: CoupleSpace, uid: String?): Boolean {
        if (!space.locksOut(uid)) return false
        repository.disconnect()
        sessionStore.clearSession()
        lostAccess = true
        return true
    }
}
