package com.aistudio.couplebubble.qxztrw.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataStoreSessionStoreTest {

    private lateinit var context: Context
    private lateinit var sessionStore: DataStoreSessionStore

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.coupleDataStore.edit { it.clear() }
        sessionStore = DataStoreSessionStore(context)
    }

    @Test
    fun onboardingFlagDefaultsToFalse() = runTest {
        assertFalse(sessionStore.onboardingCompletedFlow.first())
    }

    @Test
    fun saveOnboardingCompletedUpdatesFlag() = runTest {
        sessionStore.saveOnboardingCompleted(true)
        assertTrue(sessionStore.onboardingCompletedFlow.first())
    }

    @Test
    fun clearSessionLeavesOnboardingCompletedFlagUntouched() = runTest {
        sessionStore.saveActiveCoupleId("couple_123")
        sessionStore.savePartnerRole(PartnerRole.PARTNER_1)
        sessionStore.saveOnboardingCompleted(true)

        assertEquals("couple_123", sessionStore.activeCoupleIdFlow.first())
        assertEquals("1", sessionStore.partnerRoleFlow.first())
        assertTrue(sessionStore.onboardingCompletedFlow.first())

        sessionStore.clearSession()

        assertNull(sessionStore.activeCoupleIdFlow.first())
        assertNull(sessionStore.partnerRoleFlow.first())
        assertTrue("Onboarding completed flag must survive clearSession", sessionStore.onboardingCompletedFlow.first())
    }

    @Test
    fun dismissedGestureHintsDefaultToEmpty() = runTest {
        assertTrue(sessionStore.dismissedGestureHintsFlow.first().isEmpty())
    }

    @Test
    fun dismissGestureHintPersistsEachHintIndividually() = runTest {
        sessionStore.dismissGestureHint(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.NOTE_LONG_PRESS)
        assertEquals(
            setOf(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.NOTE_LONG_PRESS),
            sessionStore.dismissedGestureHintsFlow.first()
        )

        sessionStore.dismissGestureHint(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.FULLSCREEN_PHOTO_SWIPE)
        sessionStore.dismissGestureHint(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.MILESTONE_TAP)
        sessionStore.dismissGestureHint(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.CHECKLIST_DRAG)

        val allHints = com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.entries.toSet()
        assertEquals(allHints, sessionStore.dismissedGestureHintsFlow.first())
    }

    @Test
    fun dismissedGestureHintsSurviveClearSession() = runTest {
        sessionStore.saveActiveCoupleId("couple_123")
        sessionStore.dismissGestureHint(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.NOTE_LONG_PRESS)
        sessionStore.dismissGestureHint(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.MILESTONE_TAP)

        sessionStore.clearSession()

        val dismissed = sessionStore.dismissedGestureHintsFlow.first()
        assertTrue(dismissed.contains(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.NOTE_LONG_PRESS))
        assertTrue(dismissed.contains(com.aistudio.couplebubble.qxztrw.domain.model.GestureHint.MILESTONE_TAP))
    }

    @Test
    fun themeModeDefaultsToSystemAndPersists() = runTest {
        assertEquals(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.SYSTEM, sessionStore.themeModeFlow.first())

        sessionStore.saveThemeMode(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.DARK)
        assertEquals(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.DARK, sessionStore.themeModeFlow.first())

        sessionStore.saveThemeMode(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.LIGHT)
        assertEquals(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.LIGHT, sessionStore.themeModeFlow.first())
    }

    @Test
    fun themeModeSurvivesClearSession() = runTest {
        sessionStore.saveActiveCoupleId("couple_123")
        sessionStore.saveThemeMode(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.DARK)

        sessionStore.clearSession()

        assertEquals(com.aistudio.couplebubble.qxztrw.domain.model.ThemeMode.DARK, sessionStore.themeModeFlow.first())
    }

    @Test
    fun notificationPreferencesDefaultToTrueAndPersist() = runTest {
        val initial = sessionStore.notificationPreferencesFlow.first()
        assertTrue(initial.partnerMomentsEnabled)
        assertTrue(initial.milestonesEnabled)

        sessionStore.savePartnerMomentsNotificationEnabled(false)
        val afterMoments = sessionStore.notificationPreferencesFlow.first()
        assertFalse(afterMoments.partnerMomentsEnabled)
        assertTrue(afterMoments.milestonesEnabled)

        sessionStore.saveMilestonesNotificationEnabled(false)
        val afterMilestones = sessionStore.notificationPreferencesFlow.first()
        assertFalse(afterMilestones.partnerMomentsEnabled)
        assertFalse(afterMilestones.milestonesEnabled)

        sessionStore.savePartnerMomentsNotificationEnabled(true)
        val afterRestoreMoments = sessionStore.notificationPreferencesFlow.first()
        assertTrue(afterRestoreMoments.partnerMomentsEnabled)
        assertFalse(afterRestoreMoments.milestonesEnabled)
    }

    @Test
    fun notificationPreferencesSurviveClearSession() = runTest {
        sessionStore.saveActiveCoupleId("couple_123")
        sessionStore.savePartnerMomentsNotificationEnabled(false)
        sessionStore.saveMilestonesNotificationEnabled(false)

        sessionStore.clearSession()

        val prefs = sessionStore.notificationPreferencesFlow.first()
        assertFalse(prefs.partnerMomentsEnabled)
        assertFalse(prefs.milestonesEnabled)
    }
}
