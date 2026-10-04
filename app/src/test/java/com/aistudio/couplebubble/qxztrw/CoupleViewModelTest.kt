package com.aistudio.couplebubble.qxztrw

import com.aistudio.couplebubble.qxztrw.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.model.UserProfile
import com.aistudio.couplebubble.qxztrw.repository.SpaceFullException
import com.aistudio.couplebubble.qxztrw.repository.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.ui.CoupleEffect
import com.aistudio.couplebubble.qxztrw.ui.CoupleMainState
import com.aistudio.couplebubble.qxztrw.ui.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.MainTab
import com.aistudio.couplebubble.qxztrw.ui.PairingTab
import com.aistudio.couplebubble.qxztrw.ui.PhotoSlotKey
import com.aistudio.couplebubble.qxztrw.ui.PhotoSyncState
import com.aistudio.couplebubble.qxztrw.ui.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class CoupleViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectEffects(viewModel: CoupleViewModel): MutableList<CoupleEffect> {
        val effects = mutableListOf<CoupleEffect>()
        // Unconfined: receives each effect right when it is sent, even if only background work is left
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.collect { effects += it } }
        return effects
    }

    private fun createViewModel(): CoupleViewModel {
        return CoupleViewModel(
            repository = MockCoupleRepository(),
            started = SharingStarted.Eagerly
        )
    }

    @Test
    fun testInitialStateIsUnpaired() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        val state = viewModel.uiState.value

        assertTrue(state is CoupleMainState.Unpaired)
        val unpairedState = (state as CoupleMainState.Unpaired).state
        assertEquals(PairingTab.CREATE, unpairedState.selectedTab)
        assertFalse(unpairedState.isLoading)
        assertNull(unpairedState.errorMessage)
    }

    @Test
    fun testTabSelection() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onTabSelected(PairingTab.ENTER)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertEquals(PairingTab.ENTER, state.state.selectedTab)
    }

    @Test
    fun testEnteredCodeFormatting() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onEnteredCodeChanged("48a2-91 37")
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertEquals("482913", state.state.enteredCode)
    }

    @Test
    fun testShortCodeConnectShowsError() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onEnteredCodeChanged("123")
        viewModel.onConnectClicked()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertEquals(UiText.Resource(R.string.pairing_code_incomplete), state.state.errorMessage)
    }

    @Test
    fun testSuccessfulConnectTransitionsToPaired() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onEnteredCodeChanged("482913")
        viewModel.onConnectClicked()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CoupleMainState.Paired)
        val pairedState = (state as CoupleMainState.Paired).state
        assertEquals("Alex", pairedState.space.partner1Name)
        assertEquals("Sam", pairedState.space.partner2Name)
    }

    @Test
    fun testOpenDemoSpaceTransitionsToPaired() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CoupleMainState.Paired)
    }

    @Test
    fun testCounterDisplayModeAndMilestoneKindsReachUiState() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCounterDisplayModeSelected(CounterDisplayMode.WEEKS)
        viewModel.onMilestoneKindToggled(MilestoneKind.MONTHS, true)
        viewModel.onMilestoneKindToggled(MilestoneKind.DAYS, false)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals(CounterDisplayMode.WEEKS, state.counterPreferences.displayMode)
        assertTrue(MilestoneKind.MONTHS in state.counterPreferences.enabledMilestoneKinds)
        assertFalse(MilestoneKind.DAYS in state.counterPreferences.enabledMilestoneKinds)
        assertTrue(state.metrics.nextMilestone?.kind != MilestoneKind.DAYS)
    }

    @Test
    fun testAddAndDeleteCustomMilestone() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val date = LocalDate.now().plusDays(3)
        viewModel.setShowAddCustomMilestoneDialog(true)
        viewModel.onAddCustomMilestone("  Hochzeit  ", date)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertFalse(state.dialogs.showAddCustomMilestoneDialog)
        val saved = state.customMilestones.single()
        assertEquals("Hochzeit", saved.title)
        assertEquals("Hochzeit", state.metrics.nextMilestone?.customTitle)

        viewModel.onDeleteCustomMilestone(saved.id)
        testDispatcher.scheduler.advanceUntilIdle()

        state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(state.customMilestones.isEmpty())
    }

    @Test
    fun testDisconnectTransitionsBackToUnpaired() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CoupleMainState.Paired)

        viewModel.onConfirmDisconnect()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CoupleMainState.Unpaired)
    }

    @Test
    fun testMainTabSelectionReachesUiStateAndResetsAfterDisconnect() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(MainTab.US, (viewModel.uiState.value as CoupleMainState.Paired).state.dialogs.selectedTab)

        viewModel.onMainTabSelected(MainTab.NOTES)
        testDispatcher.scheduler.runCurrent()
        assertEquals(MainTab.NOTES, (viewModel.uiState.value as CoupleMainState.Paired).state.dialogs.selectedTab)

        viewModel.onConfirmDisconnect()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MainTab.US, (viewModel.uiState.value as CoupleMainState.Paired).state.dialogs.selectedTab)
    }

    @Test
    fun testCurrentSpaceIsSharedForNotes() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        assertNull(viewModel.currentSpace.value)

        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("demo_space", viewModel.currentSpace.value?.id)
    }

    @Test
    fun testUpdatePartnerNames() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUpdatePartnerNames("Max", "Mia")
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals("Max", pairedState.space.partner1Name)
        assertEquals("Mia", pairedState.space.partner2Name)
    }

    @Test
    fun testAddMemory() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddMemory(
            title = "Verlobung",
            date = LocalDate.of(2026, 5, 20),
            note = "Ein unvergesslicher Moment im Park"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(pairedState.memories.any { it.title == "Verlobung" })
    }

    @Test
    fun testPhotoSyncStateGoesFromUploadingToSyncedAndClears() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddMemory(
            title = "Hüttenwochenende",
            date = LocalDate.of(2026, 2, 14),
            note = "",
            image1Bytes = byteArrayOf(1, 2, 3)
        )
        testDispatcher.scheduler.runCurrent()

        val uploadingState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val memory = uploadingState.memories.first { it.title == "Hüttenwochenende" }
        val slotA = PhotoSlotKey(memory.id, isPartner1 = true)
        assertEquals(PhotoSyncState.UPLOADING, uploadingState.photoSyncStates[slotA])
        assertNull(uploadingState.photoSyncStates[PhotoSlotKey(memory.id, isPartner1 = false)])
        assertFalse(uploadingState.dialogs.showAddMemoryDialog)

        // Mock upload takes 100 ms and returns a Firebase Storage HTTPS URL
        testDispatcher.scheduler.advanceTimeBy(150)
        testDispatcher.scheduler.runCurrent()
        val syncedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals(PhotoSyncState.SYNCED, syncedState.photoSyncStates[slotA])
        assertTrue(syncedState.memories.single { it.id == memory.id }.partner1ImageUrl!!.startsWith("https://"))

        testDispatcher.scheduler.advanceTimeBy(1_600)
        testDispatcher.scheduler.runCurrent()
        val clearedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(clearedState.photoSyncStates.isEmpty())
    }

    @Test
    fun testPhotoSyncBadgeDropsWithoutCheckWhenUploadFails() = runTest {
        val failingRepo = object : MockCoupleRepository() {
            override suspend fun addMemory(
                coupleId: String,
                memory: Memory,
                image1Bytes: ByteArray?,
                image2Bytes: ByteArray?
            ): Result<Memory> = Result.failure(IllegalStateException("offline"))
        }
        val viewModel = CoupleViewModel(repository = failingRepo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddMemory(
            title = "Fehlversuch",
            date = LocalDate.of(2026, 3, 1),
            note = "",
            image2Bytes = byteArrayOf(4, 5, 6)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(state.photoSyncStates.isEmpty())
        assertFalse(state.memories.any { it.title == "Fehlversuch" })
    }

    @Test
    fun testUpdateMemory() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val existingMemory = pairedState.memories.first()

        val updatedMemory = existingMemory.copy(title = "Aktualisierter Titel", note = "Neue Notiz")
        viewModel.onUpdateMemory(updatedMemory)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(updatedState.memories.any { it.id == existingMemory.id && it.title == "Aktualisierter Titel" })
    }

    @Test
    fun testDeleteMemory() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val memoryToDelete = pairedState.memories.first()

        viewModel.onDeleteMemory(memoryToDelete.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertFalse(updatedState.memories.any { it.id == memoryToDelete.id })
    }

    @Test
    fun testMemoryDualPhotoProperties() {
        val legacyMemory = Memory(
            id = "leg1",
            title = "Legacy Moment",
            imageUrl = "https://example.com/legacy.jpg"
        )
        assertEquals("https://example.com/legacy.jpg", legacyMemory.effectivePartner1Image)
        assertNull(legacyMemory.effectivePartner2Image)
        assertTrue(legacyMemory.hasAnyImage)
        assertEquals(1, legacyMemory.imageCount)

        val dualMemory = Memory(
            id = "dual1",
            title = "Dual Moment",
            partner1ImageUrl = "https://example.com/a.jpg",
            partner2ImageUrl = "https://example.com/b.jpg"
        )
        assertEquals("https://example.com/a.jpg", dualMemory.effectivePartner1Image)
        assertEquals("https://example.com/b.jpg", dualMemory.effectivePartner2Image)
        assertTrue(dualMemory.hasAnyImage)
        assertEquals(2, dualMemory.imageCount)

        val emptyMemory = Memory(
            id = "empty1",
            title = "No photo"
        )
        assertFalse(emptyMemory.hasAnyImage)
        assertEquals(0, emptyMemory.imageCount)
    }

    @Test
    fun testAddMemoryWithDualPhotos() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddMemory(
            title = "Strandspaziergang",
            date = LocalDate.of(2026, 7, 10),
            note = "Beide haben ein Foto gemacht",
            image1Bytes = byteArrayOf(1, 2, 3),
            image2Bytes = byteArrayOf(4, 5, 6)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val memory = pairedState.memories.firstOrNull { it.title == "Strandspaziergang" }
        assertTrue(memory != null)
        assertTrue(memory?.effectivePartner1Image?.startsWith("https://") == true)
        assertTrue(memory?.effectivePartner2Image?.startsWith("https://") == true)
        assertFalse(memory?.effectivePartner1Image?.startsWith("file://") == true)
        assertFalse(memory?.effectivePartner2Image?.startsWith("file://") == true)
    }

    @Test
    fun testGoogleSignInUpdatesUserProfile() = runTest {
        val repo = MockCoupleRepository()
        val viewModel = CoupleViewModel(repository = repo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.runCurrent()

        repo.signInWithGoogleUser(
            uid = "google_user_123",
            email = "partner@example.com",
            displayName = "Partner Name"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertEquals("google_user_123", state.state.userProfile?.uid)
        assertEquals("partner@example.com", state.state.userProfile?.email)
        assertEquals("Partner Name", state.state.userProfile?.displayName)
    }

    @Test
    fun testGoogleSignOutClearsUserProfile() = runTest {
        val repo = MockCoupleRepository()
        val viewModel = CoupleViewModel(repository = repo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.runCurrent()

        repo.signInWithGoogleUser(
            uid = "google_user_123",
            email = "partner@example.com",
            displayName = "Partner Name"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSignOutGoogle()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertNull(state.state.userProfile)
    }

    @Test
    fun testClearGoogleAuthError() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.clearGoogleAuthError()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertNull(state.state.googleAuth.error)
    }

    @Test
    fun testUpdatePartnerColor() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUpdatePartnerColor(colorHex = "#8FA89B", isPartner1 = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals("#8FA89B", pairedState.space.partner1ColorHex)
    }

    @Test
    fun testSwapPartnerRoles() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val initialP1 = (viewModel.uiState.value as CoupleMainState.Paired).state.space.partner1Name
        val initialP2 = (viewModel.uiState.value as CoupleMainState.Paired).state.space.partner2Name

        val effects = collectEffects(viewModel)
        viewModel.swapPartnerRoles()
        testDispatcher.scheduler.advanceUntilIdle()

        val swappedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(CoupleEffect.PartnersSwapped in effects)
        assertEquals(initialP2, swappedState.space.partner1Name)
        assertEquals(initialP1, swappedState.space.partner2Name)
    }

    @Test
    fun testRoomFullErrorHandling() = runTest {
        val mockRepo = object : MockCoupleRepository() {
            override suspend fun connectWithCode(code: String): Result<com.aistudio.couplebubble.qxztrw.model.CoupleSpace> {
                return Result.failure(SpaceFullException())
            }
        }
        val viewModel = CoupleViewModel(
            repository = mockRepo,
            started = SharingStarted.Eagerly
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.onTabSelected(PairingTab.ENTER)
        viewModel.onEnteredCodeChanged("482913")
        viewModel.onConnectClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = (viewModel.uiState.value as CoupleMainState.Unpaired).state
        assertTrue(state.showSpaceFullDialog)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun testRoomFullDialogRecheckCodeClearsInput() = runTest {
        val mockRepo = object : MockCoupleRepository() {
            override suspend fun connectWithCode(code: String): Result<com.aistudio.couplebubble.qxztrw.model.CoupleSpace> =
                Result.failure(SpaceFullException())
        }
        val viewModel = CoupleViewModel(repository = mockRepo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.runCurrent()

        viewModel.onTabSelected(PairingTab.ENTER)
        viewModel.onEnteredCodeChanged("482913")
        viewModel.onConnectClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onRecheckCodeAfterSpaceFull()
        testDispatcher.scheduler.runCurrent()

        val state = (viewModel.uiState.value as CoupleMainState.Unpaired).state
        assertFalse(state.showSpaceFullDialog)
        assertEquals("", state.enteredCode)
        assertEquals(PairingTab.ENTER, state.selectedTab)
    }

    @Test
    fun testRoomFullDialogCreateOwnSpaceSwitchesToCreateTab() = runTest {
        val mockRepo = object : MockCoupleRepository() {
            override suspend fun connectWithCode(code: String): Result<com.aistudio.couplebubble.qxztrw.model.CoupleSpace> =
                Result.failure(SpaceFullException())
        }
        val viewModel = CoupleViewModel(repository = mockRepo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.runCurrent()

        viewModel.onTabSelected(PairingTab.ENTER)
        viewModel.onEnteredCodeChanged("482913")
        viewModel.onConnectClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onCreateOwnSpaceAfterSpaceFull()
        testDispatcher.scheduler.runCurrent()

        val state = (viewModel.uiState.value as CoupleMainState.Unpaired).state
        assertFalse(state.showSpaceFullDialog)
        assertEquals("", state.enteredCode)
        assertEquals(PairingTab.CREATE, state.selectedTab)
    }

    @Test
    fun testUploadProfilePhotoWithBytes() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5)
        val effects = collectEffects(viewModel)

        viewModel.onUploadProfilePhoto(isPartner1 = true, imageBytes = dummyBytes)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(CoupleEffect.Toast(UiText.Resource(R.string.profile_photo_saved))), effects)

        val state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertFalse(state.dialogs.isUploadingProfilePhoto)
        assertTrue(state.space.partner1PhotoUrl?.contains("partner1.jpg") == true)
    }

    @Test
    fun testCustomColorHexUpdate() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val customHex = "#8A2BE2" // Custom Violet
        viewModel.onUpdatePartnerColor(colorHex = customHex, isPartner1 = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals(customHex, state.space.partner1ColorHex)
    }

    @Test
    fun testUpdateMemoryWithoutNewBytesPreservesExistingPhotos() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        // m1 has both partner1ImageUrl and partner2ImageUrl
        val existingMemory = pairedState.memories.first { it.id == "m1" }
        assertTrue(existingMemory.effectivePartner1Image != null)
        assertTrue(existingMemory.effectivePartner2Image != null)

        val initialPhotoA = existingMemory.effectivePartner1Image
        val initialPhotoB = existingMemory.effectivePartner2Image

        // Update only title and note, without passing new image bytes
        val updated = existingMemory.copy(title = "Neuer Titel für Moment 1", note = "Neue Notiz")
        viewModel.onUpdateMemory(updated, image1Bytes = null, image2Bytes = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val memoryAfter = updatedState.memories.first { it.id == "m1" }

        assertEquals("Neuer Titel für Moment 1", memoryAfter.title)
        assertEquals(initialPhotoA, memoryAfter.effectivePartner1Image)
        assertEquals(initialPhotoB, memoryAfter.effectivePartner2Image)
        assertTrue(memoryAfter.hasAnyImage)
        assertEquals(2, memoryAfter.imageCount)
    }

    @Test
    fun testSwapPartnerRolesSwapsMemoryPhotos() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val initialState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val m1Before = initialState.memories.first { it.id == "m1" }
        val origPhotoA = m1Before.partner1ImageUrl
        val origPhotoB = m1Before.partner2ImageUrl

        val effects = collectEffects(viewModel)
        viewModel.swapPartnerRoles()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(CoupleEffect.PartnersSwapped in effects)
        val swappedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val m1After = swappedState.memories.first { it.id == "m1" }

        // After swap, photo slots are swapped so Partner A's photo is now B and vice-versa
        assertEquals(origPhotoB, m1After.partner1ImageUrl)
        assertEquals(origPhotoA, m1After.partner2ImageUrl)
    }

    @Test
    fun testLocalFirstImageCachingWithPreferences() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val sampleBytesA = byteArrayOf(10, 20, 30)
        val sampleBytesB = byteArrayOf(40, 50, 60)

        // 1. Verify LocalImageStorage saves immediately to local file system
        val savedUrlA = com.aistudio.couplebubble.qxztrw.data.LocalImageStorage.saveImage(context, "test_local_id", "a", sampleBytesA)
        val savedUrlB = com.aistudio.couplebubble.qxztrw.data.LocalImageStorage.saveImage(context, "test_local_id", "b", sampleBytesB)

        assertTrue(savedUrlA != null && savedUrlA.startsWith("file://"))
        assertTrue(savedUrlB != null && savedUrlB.startsWith("file://"))

        val fileA = java.io.File(savedUrlA!!.removePrefix("file://"))
        val fileB = java.io.File(savedUrlB!!.removePrefix("file://"))
        assertTrue(fileA.exists())
        assertTrue(fileB.exists())
        org.junit.Assert.assertArrayEquals(sampleBytesA, fileA.readBytes())
        org.junit.Assert.assertArrayEquals(sampleBytesB, fileB.readBytes())

        // 2. Verify Memory data model keeps and exposes both local paths
        val memory = Memory(
            id = "test_local_id",
            title = "Strandpicknick",
            date = LocalDate.of(2026, 8, 15),
            note = "Sonne und Meer",
            imageUrl = savedUrlA,
            partner1ImageUrl = savedUrlA,
            partner2ImageUrl = savedUrlB
        )

        assertEquals(savedUrlA, memory.effectivePartner1Image)
        assertEquals(savedUrlB, memory.effectivePartner2Image)
        assertTrue(memory.hasAnyImage)
        assertEquals(2, memory.imageCount)

        // 3. Verify saving in repository preserves the local photos
        val repo = MockCoupleRepository()
        val result = repo.addMemory("demo_space", memory, null, null)
        assertTrue(result.isSuccess)
        val saved = result.getOrNull()!!
        assertEquals(savedUrlA, saved.effectivePartner1Image)
        assertEquals(savedUrlB, saved.effectivePartner2Image)
    }

    @Test
    fun testFullscreenZoomDoubleTapAndPanLogic() {
        var scale = 1f
        var offset = androidx.compose.ui.geometry.Offset.Zero

        // Simulate double tap from 1f -> should toggle to 2.5f
        scale = if (scale > 1f) 1f else 2.5f
        offset = androidx.compose.ui.geometry.Offset.Zero
        assertEquals(2.5f, scale)
        assertEquals(androidx.compose.ui.geometry.Offset.Zero, offset)

        // Simulate second double tap from 2.5f -> should toggle back to 1f
        scale = if (scale > 1f) 1f else 2.5f
        offset = androidx.compose.ui.geometry.Offset.Zero
        assertEquals(1f, scale)
        assertEquals(androidx.compose.ui.geometry.Offset.Zero, offset)

        // Simulate pinch zoom
        val zoomFactor = 1.8f
        val newScale = (scale * zoomFactor).coerceIn(1f, 4f)
        scale = newScale
        assertEquals(1.8f, scale)

        // Pan when zoomed in
        val pan = androidx.compose.ui.geometry.Offset(20f, 30f)
        offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
        assertEquals(20f, offset.x)
        assertEquals(30f, offset.y)

        // Zoom out below 1f -> coerced to 1f and offset reset
        scale = (scale * 0.2f).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset else androidx.compose.ui.geometry.Offset.Zero
        assertEquals(1f, scale)
        assertEquals(androidx.compose.ui.geometry.Offset.Zero, offset)
    }

    @Test
    fun testMemoryBackwardCompatibilitySinglePhoto() {
        val memoryLegacy = Memory(
            id = "legacy_test",
            title = "Altes Foto",
            imageUrl = "https://example.com/single.jpg",
            partner1ImageUrl = null,
            partner2ImageUrl = null
        )
        // Verify effectivePartner1Image resolves legacy imageUrl
        assertEquals("https://example.com/single.jpg", memoryLegacy.effectivePartner1Image)
        assertNull(memoryLegacy.effectivePartner2Image)
        assertTrue(memoryLegacy.hasAnyImage)
        assertEquals(1, memoryLegacy.imageCount)
    }

    @Test
    fun testProfilePhotoSync_UpdatesPartner1AndPartner2Photos() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val sampleBytes1 = byteArrayOf(1, 2, 3, 4)
        val sampleBytes2 = byteArrayOf(5, 6, 7, 8)

        // Partner 1 uploads
        viewModel.onUploadProfilePhoto(isPartner1 = true, imageBytes = sampleBytes1)
        testDispatcher.scheduler.advanceUntilIdle()

        var pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(pairedState.space.partner1PhotoUrl?.contains("partner1") == true)

        // Partner 2 uploads
        viewModel.onUploadProfilePhoto(isPartner1 = false, imageBytes = sampleBytes2)
        testDispatcher.scheduler.advanceUntilIdle()

        pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(pairedState.space.partner1PhotoUrl?.contains("partner1") == true)
        assertTrue(pairedState.space.partner2PhotoUrl?.contains("partner2") == true)
    }

    @Test
    fun testMomentPhotoEdit_Partner1PreservesPartner2Photo() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val initialMemory = pairedState.memories.first { it.id == "m1" }
        assertEquals("https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800", initialMemory.partner2ImageUrl)

        // Partner 1 edits their own photo (Slot A) and updates title
        val updatedByPartner1 = initialMemory.copy(
            title = "Neuer Titel von Partner 1",
            partner1ImageUrl = "https://example.com/partner1_new.jpg",
            partner2ImageUrl = initialMemory.partner2ImageUrl // Partner 2's photo preserved!
        )

        viewModel.onUpdateMemory(updatedByPartner1)
        testDispatcher.scheduler.advanceUntilIdle()

        val refreshedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val updatedMemory = refreshedState.memories.first { it.id == "m1" }
        assertEquals("Neuer Titel von Partner 1", updatedMemory.title)
        assertEquals("https://example.com/partner1_new.jpg", updatedMemory.partner1ImageUrl)
        // Partner 2's photo is untouched and completely preserved!
        assertEquals("https://images.unsplash.com/photo-1518199266791-5375a83190b7?w=800", updatedMemory.partner2ImageUrl)
    }

    @Test
    fun testMomentPhotoEdit_Partner2PreservesPartner1Photo() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val initialMemory = pairedState.memories.first { it.id == "m1" }
        val originalPartnerAPhoto = initialMemory.partner1ImageUrl

        // Partner 2 adds their photo or updates note
        val updatedByPartner2 = initialMemory.copy(
            note = "Aktualisierte Notiz von Partner 2",
            partner1ImageUrl = originalPartnerAPhoto, // Partner 1's photo preserved!
            partner2ImageUrl = "https://example.com/partner2_new.jpg"
        )

        viewModel.onUpdateMemory(updatedByPartner2)
        testDispatcher.scheduler.advanceUntilIdle()

        val refreshedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val updatedMemory = refreshedState.memories.first { it.id == "m1" }
        assertEquals("Aktualisierte Notiz von Partner 2", updatedMemory.note)
        assertEquals(originalPartnerAPhoto, updatedMemory.partner1ImageUrl)
        assertEquals("https://example.com/partner2_new.jpg", updatedMemory.partner2ImageUrl)
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

    @Test
    fun testPartner2PhotoIsNotDuplicatedIntoSlotA() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddMemory("Nur Leos Foto", LocalDate.of(2026, 5, 1), "", image1Bytes = null, image2Bytes = byteArrayOf(1, 2, 3))
        testDispatcher.scheduler.advanceUntilIdle()

        val memory = (viewModel.uiState.value as CoupleMainState.Paired).state.memories.first { it.title == "Nur Leos Foto" }
        assertNull(memory.effectivePartner1Image)
        assertTrue(memory.effectivePartner2Image != null)
        assertEquals(1, memory.imageCount)
    }

    @Test
    fun testSwapFlipsRoleThroughPartnerIdsAndMovesProfilePhotos() = runTest {
        val repo = MockCoupleRepository()
        repo.setStateForTesting(pairedSpace("uid_mia", "uid_leo"), UserProfile(uid = "uid_mia"))
        val viewModel = CoupleViewModel(repository = repo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue((viewModel.uiState.value as CoupleMainState.Paired).state.isCurrentUserPartner1)

        val effects = collectEffects(viewModel)
        viewModel.swapPartnerRoles()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertTrue(CoupleEffect.PartnersSwapped in effects)
        assertFalse(state.isCurrentUserPartner1)
        assertEquals("uid_leo", state.space.partner1Id)
        assertEquals("uid_mia", state.space.partner2Id)
        assertNull(state.space.partner1PhotoUrl)
        assertEquals("https://example.com/mia.jpg", state.space.partner2PhotoUrl)
    }

    @Test
    fun testSwapWithoutConnectedPartnerFailsAndChangesNothing() = runTest {
        val repo = MockCoupleRepository()
        repo.setStateForTesting(pairedSpace("uid_mia", null), UserProfile(uid = "uid_mia"))
        val viewModel = CoupleViewModel(repository = repo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.advanceUntilIdle()

        val effects = collectEffects(viewModel)
        viewModel.swapPartnerRoles()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals(listOf(CoupleEffect.Snackbar(UiText.Resource(R.string.swap_partners_partner_missing))), effects)
        assertEquals("Mia", state.space.partner1Name)
        assertTrue(state.isCurrentUserPartner1)
    }

    @Test
    fun testRoleUsesPartnerIdsOverUidOrder() = runTest {
        val repo = MockCoupleRepository()
        // userUids order disagrees with the explicit IDs (e.g. after an arrayUnion append)
        repo.setStateForTesting(
            pairedSpace("uid_mia", "uid_leo").copy(userUids = listOf("uid_leo", "uid_mia")),
            UserProfile(uid = "uid_mia"),
        )
        val viewModel = CoupleViewModel(repository = repo, started = SharingStarted.Eagerly)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue((viewModel.uiState.value as CoupleMainState.Paired).state.isCurrentUserPartner1)
    }
}
