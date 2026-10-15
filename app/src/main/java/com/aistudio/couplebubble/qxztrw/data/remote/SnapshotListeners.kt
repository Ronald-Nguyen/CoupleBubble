package com.aistudio.couplebubble.qxztrw.data.remote

import com.google.firebase.firestore.ListenerRegistration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LISTENER_RETRY_DELAY = 5.seconds

/**
 * Registers a snapshot listener through [register] and registers it again after a pause whenever it reports an
 * error: Firestore ends a listener for good after a fatal error, which would silently freeze the screen.
 * [register] receives the callback to invoke on an error. Returns the function that stops everything.
 */
internal fun ProducerScope<*>.keepListening(register: (onError: () -> Unit) -> ListenerRegistration): () -> Unit {
    var registration: ListenerRegistration? = null
    var retryJob: Job? = null

    fun listen() {
        registration = register {
            registration?.remove()
            retryJob = launch {
                delay(LISTENER_RETRY_DELAY)
                listen()
            }
        }
    }
    listen()

    return {
        retryJob?.cancel()
        registration?.remove()
    }
}
