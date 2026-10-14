package com.aistudio.couplebubble.qxztrw.ui.screens.settings

import com.aistudio.couplebubble.qxztrw.BuildConfig
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppRoute
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleTestApp
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.settle(millis: Long = 3_000) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun TestScope.pairedApp(partner1Id: String? = "uid_1", partner2Id: String? = "uid_2", uid: String = "uid_1"): CoupleTestApp {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(
            CoupleSpace(
                id = "space_test",
                partner1Name = "Mia",
                partner2Name = "Leo",
                userUids = listOfNotNull(partner1Id, partner2Id),
                partner1Id = partner1Id,
                partner2Id = partner2Id,
            ),
            UserProfile(uid = uid)
        )
        return CoupleTestApp(this, repository).also { settle() }
    }

    @Test
    fun opensAndClosesSettings() = runTest {
        val app = pairedApp()
        assertFalse(app.settingsState.isOpen)
        assertEquals(BuildConfig.VERSION_NAME, app.settingsState.appVersion)

        app.settings.onEvent(SettingsEvent.Open)
        settle()
        assertTrue(app.settingsState.isOpen)

        app.settings.onEvent(SettingsEvent.Close)
        settle()
        assertFalse(app.settingsState.isOpen)
        assertEquals(SettingsDialog.None, app.settingsState.dialog)
    }

    @Test
    fun showsAndSavesPartnerDetails() = runTest {
        val app = pairedApp()
        app.settings.onEvent(SettingsEvent.ShowEditNamesDialog(true))
        settle()
        assertEquals(SettingsDialog.EditNames, app.settingsState.dialog)

        app.settings.onEvent(
            SettingsEvent.SavePartnerDetails(
                partner1Name = "Mimi",
                partner2Name = "Leon",
                partner1ColorHex = "#E65D2E",
                partner2ColorHex = "#97CBFF"
            )
        )
        settle()

        assertEquals("Mimi", app.repository.currentSpace.value?.partner1Name)
        assertEquals("Leon", app.repository.currentSpace.value?.partner2Name)
        assertTrue(app.effects.any { it is CoupleEffect.Snackbar })
    }

    @Test
    fun swapsPartnerRoles() = runTest {
        val app = pairedApp()
        app.settings.onEvent(SettingsEvent.SwapPartnerRoles)
        settle()

        assertTrue(app.effects.contains(CoupleEffect.PartnersSwapped))
        assertEquals(1, app.settingsState.completedRoleSwaps)
    }

    @Test
    fun managesMilestoneSettings() = runTest {
        val app = pairedApp()
        app.settings.onEvent(SettingsEvent.ShowMilestoneSettingsDialog(true))
        settle()
        assertTrue(app.settingsState.dialog is SettingsDialog.MilestoneSettings)

        app.settings.onEvent(SettingsEvent.ToggleMilestoneKind(MilestoneKind.YEARS, false))
        settle()
        assertFalse(app.settingsState.counterPreferences.enabledMilestoneKinds.contains(MilestoneKind.YEARS))

        app.settings.onEvent(SettingsEvent.AddCustomMilestone("Urlaub", LocalDate.of(2026, 8, 1), true))
        settle()
        val custom = app.settingsState.customMilestones.firstOrNull { it.title == "Urlaub" }
        assertTrue(custom != null)

        app.settings.onEvent(SettingsEvent.DeleteCustomMilestone(custom!!.id))
        settle()
        assertFalse(app.settingsState.customMilestones.any { it.title == "Urlaub" })
    }

    @Test
    fun googleBackupDialogOpensAndCloses() = runTest {
        val app = pairedApp()
        app.settings.onEvent(SettingsEvent.ShowGoogleBackupDialog(true))
        settle()
        assertEquals(SettingsDialog.GoogleBackup, app.settingsState.dialog)

        app.settings.onEvent(SettingsEvent.ShowGoogleBackupDialog(false))
        settle()
        assertEquals(SettingsDialog.None, app.settingsState.dialog)
    }

    @Test
    fun disconnectDialogAndConfirmDisconnect() = runTest {
        val app = pairedApp()
        app.settings.onEvent(SettingsEvent.Open)
        app.settings.onEvent(SettingsEvent.ShowDisconnectDialog(true))
        settle()
        assertEquals(SettingsDialog.Disconnect, app.settingsState.dialog)

        app.settings.onEvent(SettingsEvent.ConfirmDisconnect)
        settle()

        assertFalse(app.settingsState.isOpen)
        assertEquals(AppRoute.UNPAIRED, app.route)
    }

    @Test
    fun privacySummaryDialogOpensAndCloses() = runTest {
        val app = pairedApp()
        app.settings.onEvent(SettingsEvent.ShowPrivacySummaryDialog(true))
        settle()
        assertEquals(SettingsDialog.PrivacySummary, app.settingsState.dialog)

        app.settings.onEvent(SettingsEvent.ShowPrivacySummaryDialog(false))
        settle()
        assertEquals(SettingsDialog.None, app.settingsState.dialog)
    }
}
