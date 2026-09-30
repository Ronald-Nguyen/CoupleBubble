package com.aistudio.couplebubble.qxztrw

import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.repository.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.ui.CoupleMainState
import com.aistudio.couplebubble.qxztrw.ui.CoupleViewModel
import com.aistudio.couplebubble.qxztrw.ui.PairingTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
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
import java.time.LocalDate

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
        viewModel.onEnteredCodeChanged("blu-789a")
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertEquals("BLU789", state.state.enteredCode)
    }

    @Test
    fun testShortCodeConnectShowsError() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onEnteredCodeChanged("123")
        viewModel.onConnectClicked()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value as CoupleMainState.Unpaired
        assertEquals("Bitte gib alle 6 Stellen deines Codes ein.", state.state.errorMessage)
    }

    @Test
    fun testSuccessfulConnectTransitionsToPaired() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onEnteredCodeChanged("BLU789")
        viewModel.onConnectClicked()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CoupleMainState.Paired)
        val pairedState = (state as CoupleMainState.Paired).state
        assertEquals("Alex", pairedState.space.partnerAName)
        assertEquals("Sam", pairedState.space.partnerBName)
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
    fun testUpdatePartnerNames() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()
        viewModel.onOpenDemoSpace()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onUpdatePartnerNames("Max", "Mia")
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        assertEquals("Max", pairedState.space.partnerAName)
        assertEquals("Mia", pairedState.space.partnerBName)
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
        assertEquals("https://example.com/legacy.jpg", legacyMemory.effectivePartnerAImage)
        assertNull(legacyMemory.effectivePartnerBImage)
        assertTrue(legacyMemory.hasAnyImage)
        assertEquals(1, legacyMemory.imageCount)

        val dualMemory = Memory(
            id = "dual1",
            title = "Dual Moment",
            partnerAImageUrl = "https://example.com/a.jpg",
            partnerBImageUrl = "https://example.com/b.jpg"
        )
        assertEquals("https://example.com/a.jpg", dualMemory.effectivePartnerAImage)
        assertEquals("https://example.com/b.jpg", dualMemory.effectivePartnerBImage)
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
            imageABytes = byteArrayOf(1, 2, 3),
            imageBBytes = byteArrayOf(4, 5, 6)
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val pairedState = (viewModel.uiState.value as CoupleMainState.Paired).state
        val memory = pairedState.memories.firstOrNull { it.title == "Strandspaziergang" }
        assertTrue(memory != null)
    }

    @Test
    fun testGoogleSignInUpdatesUserProfile() = runTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.onSignInWithGoogle(
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
        val viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        viewModel.onSignInWithGoogle(
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
}
