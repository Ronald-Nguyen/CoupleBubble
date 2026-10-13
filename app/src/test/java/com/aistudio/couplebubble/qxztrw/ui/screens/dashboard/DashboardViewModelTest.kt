package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.net.Uri
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.UserProfile
import com.aistudio.couplebubble.qxztrw.ui.screens.home.AppRoute
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.screens.home.CoupleTestApp
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardDialog
import com.aistudio.couplebubble.qxztrw.ui.screens.home.DashboardEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.MainTab
import com.aistudio.couplebubble.qxztrw.ui.screens.home.OwnPhotoEdit
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PairingEvent
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PhotoSlotKey
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PhotoSyncState
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val photo: Uri = Uri.parse("content://media/picked/1")

    /** Lets every pending coroutine run, but stops before the pairing countdown would tick for minutes. */
    private fun TestScope.settle(millis: Long = 3_000) {
        advanceTimeBy(millis)
        runCurrent()
    }

    private fun TestScope.demoApp(repository: MockCoupleRepository = MockCoupleRepository()): CoupleTestApp =
        CoupleTestApp(this, repository).also {
            runCurrent()
            it.pairing.onEvent(PairingEvent.OpenDemoSpace)
            settle()
        }

    private fun TestScope.pairedApp(partner1Id: String?, partner2Id: String?, uid: String): CoupleTestApp {
        val repository = MockCoupleRepository()
        repository.setStateForTesting(pairedSpace(partner1Id, partner2Id), UserProfile(uid = uid))
        return CoupleTestApp(this, repository).also { settle() }
    }

    private fun pairedSpace(partner1Id: String?, partner2Id: String?) = CoupleSpace(
        id = "space_test",
        partner1Name = "Mia",
        partner2Name = "Leo",
        userUids = listOfNotNull(partner1Id, partner2Id),
        partner1Id = partner1Id,
        partner2Id = partner2Id,
        partner1PhotoUrl = "https://example.com/mia.jpg",
        partner2PhotoUrl = null,
    )

    private fun addMemory(title: String, date: LocalDate, withPhoto: Boolean) = DashboardEvent.AddMemory(
        title = title,
        date = date,
        note = "",
        milestoneKey = null,
        ownImageUri = photo.takeIf { withPhoto }
    )

    private fun CoupleTestApp.memory(id: String): Memory = dashboardState.memories.first { it.id == id }

    @Test
    fun counterModeAndMilestoneKindsReachTheState() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(DashboardEvent.SelectCounterDisplayMode(CounterDisplayMode.WEEKS))
        app.dashboard.onEvent(DashboardEvent.ToggleMilestoneKind(MilestoneKind.MONTHS, true))
        app.dashboard.onEvent(DashboardEvent.ToggleMilestoneKind(MilestoneKind.DAYS, false))
        settle()

        val state = app.dashboardState
        assertEquals(CounterDisplayMode.WEEKS, state.counterPreferences.displayMode)
        assertTrue(MilestoneKind.MONTHS in state.counterPreferences.enabledMilestoneKinds)
        assertFalse(MilestoneKind.DAYS in state.counterPreferences.enabledMilestoneKinds)
        assertTrue(state.metrics.nextMilestone?.kind != MilestoneKind.DAYS)
        assertEquals(CounterDisplayMode.WEEKS, app.sessionStore.counterPreferencesFlow.value.displayMode)
    }

    @Test
    fun addCustomMilestone() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(true))
        app.dashboard.onEvent(DashboardEvent.AddCustomMilestone("  Hochzeit  ", LocalDate.now().plusDays(3)))
        settle()

        val state = app.dashboardState
        assertEquals(DashboardDialog.MilestoneSettings(addingCustom = false), state.dialogs.dialog)
        assertEquals("Hochzeit", state.customMilestones.single().title)
        assertEquals("Hochzeit", state.metrics.nextMilestone?.customTitle)
    }

    @Test
    fun disconnectReturnsToPairingAndClearsTheSession() = runTest {
        val app = demoApp()
        assertEquals(AppRoute.PAIRED, app.route)

        app.dashboard.onEvent(DashboardEvent.ConfirmDisconnect)
        settle()

        assertEquals(AppRoute.UNPAIRED, app.route)
        assertNull(app.dashboard.uiState.value)
        assertNull(app.sessionStore.activeCoupleIdFlow.value)
        assertNull(app.widgets.published.last())
    }

    @Test
    fun mainTabResetsAfterDisconnect() = runTest {
        val app = demoApp()
        assertEquals(MainTab.US, app.dashboardState.dialogs.selectedTab)

        app.dashboard.onEvent(DashboardEvent.SelectMainTab(MainTab.NOTES))
        runCurrent()
        assertEquals(MainTab.NOTES, app.dashboardState.dialogs.selectedTab)

        app.dashboard.onEvent(DashboardEvent.ConfirmDisconnect)
        settle()
        app.pairing.onEvent(PairingEvent.OpenDemoSpace)
        settle()

        assertEquals(MainTab.US, app.dashboardState.dialogs.selectedTab)
    }

    @Test
    fun onlyOneDialogIsOpenAtATime() = runTest {
        val app = demoApp()
        val memory = app.memory("m1")

        app.dashboard.onEvent(DashboardEvent.ShowEditNamesDialog(true))
        app.dashboard.onEvent(DashboardEvent.ShowDeleteMemoryDialog(memory))
        runCurrent()
        assertEquals(DashboardDialog.DeleteMemory(memory), app.dashboardState.dialogs.dialog)

        // Closing a dialog that isn't open leaves the open one alone
        app.dashboard.onEvent(DashboardEvent.ShowEditNamesDialog(false))
        runCurrent()
        assertEquals(DashboardDialog.DeleteMemory(memory), app.dashboardState.dialogs.dialog)

        app.dashboard.onEvent(DashboardEvent.ShowDeleteMemoryDialog(null))
        runCurrent()
        assertEquals(DashboardDialog.None, app.dashboardState.dialogs.dialog)
    }

    @Test
    fun customMilestoneDialogOpensOnTopOfTheSettings() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(DashboardEvent.SetMenuExpanded(true))
        app.dashboard.onEvent(DashboardEvent.ShowMilestoneSettingsDialog(true))
        app.dashboard.onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(true))
        runCurrent()
        assertEquals(DashboardDialog.MilestoneSettings(addingCustom = true), app.dashboardState.dialogs.dialog)
        assertFalse(app.dashboardState.dialogs.isMenuExpanded)

        app.dashboard.onEvent(DashboardEvent.ShowAddCustomMilestoneDialog(false))
        runCurrent()
        assertEquals(DashboardDialog.MilestoneSettings(addingCustom = false), app.dashboardState.dialogs.dialog)
    }

    @Test
    fun updatePartnerNames() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(DashboardEvent.UpdatePartnerNames("Max", "Mia"))
        settle()

        assertEquals("Max", app.dashboardState.space.partner1Name)
        assertEquals("Mia", app.dashboardState.space.partner2Name)
    }

    @Test
    fun addMemory() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(addMemory("Verlobung", LocalDate.of(2026, 5, 20), withPhoto = false))
        settle()

        assertTrue(app.dashboardState.memories.any { it.title == "Verlobung" })
    }

    @Test
    fun photoBadgeGoesFromUploadingToSyncedAndClears() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(addMemory("Hüttenwochenende", LocalDate.of(2026, 2, 14), withPhoto = true))
        runCurrent()

        val uploading = app.dashboardState
        val memory = uploading.memories.first { it.title == "Hüttenwochenende" }
        val slotA = PhotoSlotKey(memory.id, isPartner1 = true)
        assertEquals(PhotoSyncState.UPLOADING, uploading.photoSyncStates[slotA])
        assertNull(uploading.photoSyncStates[PhotoSlotKey(memory.id, isPartner1 = false)])
        assertTrue("The local-first copy shows right away", memory.partner1ImageUrl!!.startsWith("file://"))
        assertEquals(DashboardDialog.None, uploading.dialogs.dialog)

        // Mock upload takes 100 ms and returns a Firebase Storage HTTPS URL
        advanceTimeBy(150)
        runCurrent()
        assertEquals(PhotoSyncState.SYNCED, app.dashboardState.photoSyncStates[slotA])
        assertTrue(app.memory(memory.id).partner1ImageUrl!!.startsWith("https://"))

        advanceTimeBy(1_600)
        runCurrent()
        assertTrue(app.dashboardState.photoSyncStates.isEmpty())
    }

    @Test
    fun photoBadgeDropsWithoutCheckWhenTheUploadFails() = runTest {
        val failing = object : MockCoupleRepository() {
            override suspend fun addMemory(
                coupleId: String,
                memory: Memory,
                fallbackRole: PartnerRole?,
                ownPhoto: MemoryPhotoChange
            ): Result<Memory> = Result.failure(IllegalStateException("offline"))
        }
        val app = demoApp(failing)

        app.dashboard.onEvent(addMemory("Fehlversuch", LocalDate.of(2026, 3, 1), withPhoto = true))
        settle()

        assertTrue(app.dashboardState.photoSyncStates.isEmpty())
        assertFalse(app.dashboardState.memories.any { it.title == "Fehlversuch" })
    }

    @Test
    fun updateMemory() = runTest {
        val app = demoApp()
        val existing = app.dashboardState.memories.first()

        app.dashboard.onEvent(DashboardEvent.UpdateMemory(existing.copy(title = "Aktualisierter Titel", note = "Neue Notiz"), OwnPhotoEdit.Keep))
        settle()

        assertTrue(app.dashboardState.memories.any { it.id == existing.id && it.title == "Aktualisierter Titel" })
    }

    @Test
    fun deleteMemory() = runTest {
        val app = demoApp()
        val toDelete = app.dashboardState.memories.first()

        app.dashboard.onEvent(DashboardEvent.DeleteMemory(toDelete.id))
        settle()

        assertFalse(app.dashboardState.memories.any { it.id == toDelete.id })
    }

    @Test
    fun newMemoryStoresOnlyTheOwnPhoto() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(addMemory("Strandspaziergang", LocalDate.of(2026, 7, 10), withPhoto = true))
        settle()

        val memory = app.dashboardState.memories.first { it.title == "Strandspaziergang" }
        assertTrue(memory.effectivePartner1Image?.startsWith("https://") == true)
        assertNull(memory.effectivePartner2Image)
    }

    @Test
    fun updatePartnerColorAcceptsCustomHex() = runTest {
        val app = demoApp()

        app.dashboard.onEvent(DashboardEvent.UpdatePartnerColor(colorHex = "#8A2BE2", isPartner1 = true))
        settle()

        assertEquals("#8A2BE2", app.dashboardState.space.partner1ColorHex)
    }

    @Test
    fun swapPartnerRolesSwapsNamesAndMemoryPhotos() = runTest {
        val app = demoApp()
        val before = app.dashboardState
        val m1Before = app.memory("m1")
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.SwapPartnerRoles)
        settle()

        val after = app.dashboardState
        assertTrue(CoupleEffect.PartnersSwapped in effects)
        assertEquals(before.space.partner2Name, after.space.partner1Name)
        assertEquals(before.space.partner1Name, after.space.partner2Name)
        assertEquals(m1Before.partner2ImageUrl, app.memory("m1").partner1ImageUrl)
        assertEquals(m1Before.partner1ImageUrl, app.memory("m1").partner2ImageUrl)
    }

    @Test
    fun uploadProfilePhoto() = runTest {
        val app = demoApp()
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.UploadProfilePhoto(byteArrayOf(1, 2, 3, 4, 5)))
        settle()

        assertEquals(listOf(CoupleEffect.Toast(UiText.Resource(R.string.profile_photo_saved))), effects)
        assertFalse(app.dashboardState.dialogs.isUploadingProfilePhoto)
        assertTrue(app.dashboardState.space.partner1PhotoUrl?.contains("partner1.jpg") == true)
    }

    @Test
    fun textOnlyEditKeepsBothPhotos() = runTest {
        val app = demoApp()
        val existing = app.memory("m1")
        assertTrue(existing.effectivePartner1Image != null && existing.effectivePartner2Image != null)

        app.dashboard.onEvent(DashboardEvent.UpdateMemory(existing.copy(title = "Neuer Titel für Moment 1", note = "Neue Notiz"), OwnPhotoEdit.Keep))
        settle()

        val after = app.memory("m1")
        assertEquals("Neuer Titel für Moment 1", after.title)
        assertEquals(existing.effectivePartner1Image, after.effectivePartner1Image)
        assertEquals(existing.effectivePartner2Image, after.effectivePartner2Image)
        assertEquals(2, after.imageCount)
    }

    @Test
    fun galleryExportSavesEveryPhotoOnceAndConfirms() = runTest {
        val app = demoApp()
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.SaveToGallery(listOf("https://x/a.jpg", "https://x/a.jpg", " ", "https://x/b.jpg")))
        settle()

        assertEquals(listOf("https://x/a.jpg", "https://x/b.jpg"), app.imageStore.galleryExports)
        assertEquals(listOf(CoupleEffect.SavedToGallery(2)), effects)
        assertFalse(app.dashboardState.dialogs.isSavingToGallery)
    }

    // region Partner slots

    @Test
    fun eachPartnerOnlyWritesTheirOwnProfilePhoto() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_leo")

        app.dashboard.onEvent(DashboardEvent.UploadProfilePhoto(byteArrayOf(5, 6, 7, 8)))
        settle()

        val state = app.dashboardState
        assertEquals(PartnerRole.PARTNER_2, state.currentUserRole)
        assertEquals("https://example.com/mia.jpg", state.space.partner1PhotoUrl)
        assertTrue(state.space.partner2PhotoUrl?.contains("partner2") == true)
    }

    @Test
    fun profilePhotoFollowsTheUidWhenRolesSwapDuringUpload() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_mia")
        val repo = app.repository

        // The partner swaps roles on their device while Mia's upload is still running
        repo.beforeProfilePhotoWrite = {
            val space = repo.currentSpace.value!!
            repo.setStateForTesting(
                space.copy(
                    partner1Id = space.partner2Id,
                    partner2Id = space.partner1Id,
                    partner1PhotoUrl = space.partner2PhotoUrl,
                    partner2PhotoUrl = space.partner1PhotoUrl,
                    userUids = space.userUids.reversed()
                ),
                UserProfile(uid = "uid_mia")
            )
        }
        app.dashboard.onEvent(DashboardEvent.UploadProfilePhoto(byteArrayOf(1, 2, 3)))
        settle()

        val state = app.dashboardState
        assertEquals(PartnerRole.PARTNER_2, state.currentUserRole)
        assertNull(state.space.partner1PhotoUrl)
        assertTrue(state.space.partner2PhotoUrl?.contains("partner2") == true)
    }

    @Test
    fun unboundUserGetsLinkedToTheFreeSlot() = runTest {
        // Leo's UID changed (e.g. Google sign-in) and the old claim never reached the server
        val app = pairedApp("uid_mia", null, uid = "uid_leo_new")

        assertEquals(listOf("space_test" to false), app.repository.ensuredSlots)
        assertEquals("uid_leo_new", app.dashboardState.space.partner2Id)
        assertEquals(PartnerRole.PARTNER_2, app.dashboardState.currentUserRole)
    }

    @Test
    fun unknownRoleBlocksProfilePhotoInAFullSpace() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_stranger")
        assertNull(app.dashboardState.currentUserRole)
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.UploadProfilePhoto(byteArrayOf(1)))
        settle()

        assertEquals(listOf(CoupleEffect.Snackbar(UiText.Resource(R.string.profile_photo_slot_unknown))), effects)
        assertEquals("https://example.com/mia.jpg", app.dashboardState.space.partner1PhotoUrl)
        assertNull(app.dashboardState.space.partner2PhotoUrl)
        assertTrue(app.repository.ensuredSlots.isEmpty())
    }

    @Test
    fun swapIsRejectedWhileAProfilePhotoUploads() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_mia")
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.UploadProfilePhoto(byteArrayOf(1)))
        app.dashboard.onEvent(DashboardEvent.SwapPartnerRoles)
        settle()

        assertEquals(CoupleEffect.Snackbar(UiText.Resource(R.string.swap_partners_wait_for_upload)), effects.first())
        assertEquals("uid_mia", app.dashboardState.space.partner1Id)
        assertTrue(app.dashboardState.space.partner1PhotoUrl?.contains("partner1") == true)
    }

    @Test
    fun partner1PhotoEditKeepsThePartnersPhoto() = runTest {
        val app = demoApp()
        val initial = app.memory("m1")
        assertEquals("https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800", initial.partner2ImageUrl)

        // A stale partner 2 photo in the edited copy must not be written back
        val edited = initial.copy(title = "Neuer Titel von Partner 1", partner2ImageUrl = "https://example.com/stale.jpg")
        app.dashboard.onEvent(DashboardEvent.UpdateMemory(edited, OwnPhotoEdit.Replace(photo)))
        settle()

        val updated = app.memory("m1")
        assertEquals("Neuer Titel von Partner 1", updated.title)
        assertTrue(updated.partner1ImageUrl?.contains("m1_a") == true)
        assertEquals("https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800", updated.partner2ImageUrl)
    }

    @Test
    fun partner2RemovesOnlyTheirOwnPhoto() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_leo")
        val initial = app.memory("m1")

        app.dashboard.onEvent(DashboardEvent.UpdateMemory(initial.copy(note = "Aktualisierte Notiz von Partner 2"), OwnPhotoEdit.Remove))
        settle()

        val updated = app.memory("m1")
        assertEquals("Aktualisierte Notiz von Partner 2", updated.note)
        assertEquals(initial.partner1ImageUrl, updated.partner1ImageUrl)
        assertNull(updated.partner2ImageUrl)
    }

    @Test
    fun momentPhotoFollowsTheUidWhenRolesSwapBeforeSaving() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_mia")
        val memory = app.memory("m1")

        // The dialog was opened as partner 1, then the partner swapped roles
        app.dashboard.onEvent(DashboardEvent.SwapPartnerRoles)
        settle()
        val partnerPhotoAfterSwap = app.memory("m1").partner1ImageUrl

        app.dashboard.onEvent(DashboardEvent.UpdateMemory(memory, OwnPhotoEdit.Replace(photo)))
        settle()

        val saved = app.memory("m1")
        assertEquals(partnerPhotoAfterSwap, saved.partner1ImageUrl)
        assertTrue(saved.partner2ImageUrl?.contains("m1_b") == true)
    }

    @Test
    fun momentPhotoIsDroppedWithoutASlot() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_stranger")
        val memory = app.memory("m1")
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.UpdateMemory(memory.copy(title = "Nur Text"), OwnPhotoEdit.Replace(photo)))
        settle()

        val saved = app.memory("m1")
        assertEquals(listOf(CoupleEffect.Snackbar(UiText.Resource(R.string.memory_photo_slot_unknown))), effects)
        assertEquals("Nur Text", saved.title)
        assertEquals(memory.partner1ImageUrl, saved.partner1ImageUrl)
        assertEquals(memory.partner2ImageUrl, saved.partner2ImageUrl)
    }

    @Test
    fun partner2PhotoIsNotDuplicatedIntoSlot1() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_leo")

        app.dashboard.onEvent(addMemory("Nur Leos Foto", LocalDate.of(2026, 5, 1), withPhoto = true))
        settle()

        val memory = app.dashboardState.memories.first { it.title == "Nur Leos Foto" }
        assertNull(memory.effectivePartner1Image)
        assertTrue(memory.effectivePartner2Image != null)
        assertEquals(1, memory.imageCount)
    }

    @Test
    fun swapFlipsTheRoleThroughPartnerIdsAndMovesProfilePhotos() = runTest {
        val app = pairedApp("uid_mia", "uid_leo", uid = "uid_mia")
        assertTrue(app.dashboardState.isCurrentUserPartner1)
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.SwapPartnerRoles)
        settle()

        val state = app.dashboardState
        assertTrue(CoupleEffect.PartnersSwapped in effects)
        assertFalse(state.isCurrentUserPartner1)
        assertEquals(PartnerRole.PARTNER_2, state.currentUserRole)
        assertEquals("uid_leo", state.space.partner1Id)
        assertEquals("uid_mia", state.space.partner2Id)
        assertNull(state.space.partner1PhotoUrl)
        assertEquals("https://example.com/mia.jpg", state.space.partner2PhotoUrl)
        assertEquals("2", app.sessionStore.partnerRoleFlow.value)
    }

    @Test
    fun swapWithoutAConnectedPartnerFailsAndChangesNothing() = runTest {
        val app = pairedApp("uid_mia", null, uid = "uid_mia")
        val effects = app.effects

        app.dashboard.onEvent(DashboardEvent.SwapPartnerRoles)
        settle()

        assertEquals(listOf(CoupleEffect.Snackbar(UiText.Resource(R.string.swap_partners_partner_missing))), effects)
        assertEquals("Mia", app.dashboardState.space.partner1Name)
        assertTrue(app.dashboardState.isCurrentUserPartner1)
    }

    @Test
    fun roleUsesPartnerIdsOverUidOrder() = runTest {
        val repository = MockCoupleRepository()
        // userUids order disagrees with the explicit IDs (e.g. after an arrayUnion append)
        repository.setStateForTesting(
            pairedSpace("uid_mia", "uid_leo").copy(userUids = listOf("uid_leo", "uid_mia")),
            UserProfile(uid = "uid_mia")
        )
        val app = CoupleTestApp(this, repository)
        settle()

        assertTrue(app.dashboardState.isCurrentUserPartner1)
    }

    // endregion
}
