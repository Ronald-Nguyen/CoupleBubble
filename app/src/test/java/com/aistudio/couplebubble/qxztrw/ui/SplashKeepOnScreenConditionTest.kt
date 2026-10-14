package com.aistudio.couplebubble.qxztrw.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashKeepOnScreenConditionTest {

    @Test
    fun keepsOnScreenWhileSessionNotRestoredAndWithinMaxDuration() {
        var uptime = 1_000L
        var restored = false
        val condition = SplashKeepOnScreenCondition(
            isRestored = { restored },
            currentUptimeMillis = { uptime },
            maxDurationMillis = 3_000L
        )

        assertTrue(condition.shouldKeepOnScreen())

        uptime = 2_500L
        assertTrue(condition.shouldKeepOnScreen())
    }

    @Test
    fun dismissesImmediatelyWhenSessionIsRestored() {
        var uptime = 1_000L
        var restored = false
        val condition = SplashKeepOnScreenCondition(
            isRestored = { restored },
            currentUptimeMillis = { uptime },
            maxDurationMillis = 3_000L
        )

        assertTrue(condition.shouldKeepOnScreen())

        restored = true
        assertFalse(condition.shouldKeepOnScreen())
    }

    @Test
    fun dismissesWhenMaximumDurationIsReachedEvenIfNotRestored() {
        var uptime = 1_000L
        var restored = false
        val condition = SplashKeepOnScreenCondition(
            isRestored = { restored },
            currentUptimeMillis = { uptime },
            maxDurationMillis = 3_000L
        )

        assertTrue(condition.shouldKeepOnScreen())

        // Reaching exact timeout (1000 + 3000 = 4000)
        uptime = 4_000L
        assertFalse(condition.shouldKeepOnScreen())

        // Exceeding timeout
        uptime = 5_000L
        assertFalse(condition.shouldKeepOnScreen())
    }
}
