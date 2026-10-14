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
}
