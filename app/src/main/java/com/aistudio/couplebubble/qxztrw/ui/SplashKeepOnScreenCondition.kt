package com.aistudio.couplebubble.qxztrw.ui

import android.os.SystemClock

/**
 * Keeps the splash screen visible while the session is being restored, replacing the startup
 * loading spinner. A maximum duration ensures the app never hangs indefinitely on the splash screen.
 */
class SplashKeepOnScreenCondition(
    private val isRestored: () -> Boolean,
    private val currentUptimeMillis: () -> Long = { SystemClock.uptimeMillis() },
    private val maxDurationMillis: Long = DEFAULT_MAX_SPLASH_DURATION_MILLIS
) {
    private val startTimeMillis = currentUptimeMillis()

    fun shouldKeepOnScreen(): Boolean {
        if (isRestored()) return false
        val elapsed = currentUptimeMillis() - startTimeMillis
        return elapsed < maxDurationMillis
    }

    companion object {
        const val DEFAULT_MAX_SPLASH_DURATION_MILLIS = 3_000L
    }
}
