package com.aistudio.couplebubble.qxztrw.data.demo

import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.UiText
import com.aistudio.couplebubble.qxztrw.data.fake.FakeLocalImageStore
import com.aistudio.couplebubble.qxztrw.domain.error.InvalidPairingCodeException
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MemoryPhotoChange
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.domain.model.roleOf
import com.aistudio.couplebubble.qxztrw.ui.screens.home.googleSignInErrorText
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoCoupleRepositoryTest {

    private val today = LocalDate.of(2026, 10, 29)

    private fun repository(scope: CoroutineScope) =
        DemoCoupleRepository(FakeLocalImageStore(), scope, today = { today }, partnerJoinDelayMillis = 1_000L)

    @Test
    fun anyCodeJoinsTheSeededSpaceAsPartner2() = runTest {
        val repo = repository(backgroundScope)

        val space = repo.connectWithCode("123-456").getOrThrow()

        assertEquals(DemoSeed.SPACE_ID, space.id)
        assertTrue(space.isActive && space.isSetupComplete)
        assertEquals(PartnerRole.PARTNER_2, space.roleOf(DemoSeed.USER_UID))
        assertTrue(repo.getMemories(space.id).first().isNotEmpty())
        assertEquals(DemoSeed.SPACE_ID, repo.currentUserProfile.value?.coupleId)
    }

    @Test
    fun malformedCodeIsRefused() = runTest {
        val repo = repository(backgroundScope)

        assertTrue(repo.connectWithCode("12345").exceptionOrNull() is InvalidPairingCodeException)
        assertNull(repo.currentSpace.value)
    }

    @Test
    fun partnerJoinsANewSpaceOnlyAfterTheCodeWasShared() = runTest {
        val repo = repository(backgroundScope)
        val code = repo.generateNewPairingCode().code

        advanceTimeBy(5_000L)
        assertNull(repo.currentSpace.value)

        repo.onPairingCodeShared(code)
        advanceTimeBy(1_001L)
        runCurrent()

        val space = requireNotNull(repo.currentSpace.value)
        assertEquals("space_${code.replace("-", "")}", space.id)
        assertFalse(space.isSetupComplete)
        assertEquals(PartnerRole.PARTNER_1, space.roleOf(DemoSeed.USER_UID))
        assertTrue(repo.getMemories(space.id).first().isEmpty())
    }

    @Test
    fun ownPhotoGoesIntoTheOwnSlotOnly() = runTest {
        val repo = repository(backgroundScope)
        val space = repo.connectWithCode("123456").getOrThrow()

        val saved = repo.addMemory(space.id, Memory(title = "Test", date = today), ownPhoto = MemoryPhotoChange.Replace(byteArrayOf(1))).getOrThrow()

        assertNull(saved.partner1ImageUrl)
        assertNotNull(saved.partner2ImageUrl)
        assertEquals(DemoSeed.USER_UID, saved.createdBy)
    }

    @Test
    fun restartRestoresOnlyTheSeededSpace() = runTest {
        val repo = repository(backgroundScope)

        assertTrue(repo.restoreSession(DemoSeed.SPACE_ID).isSuccess)
        assertTrue(repo.restoreSession("space_123456").isFailure)
    }

    @Test
    fun googleSignInExplainsTheDemoMode() {
        assertEquals(
            UiText.Resource(R.string.google_sign_in_demo_unavailable),
            googleSignInErrorText(GoogleSignInUnavailableInDemoException(), showDeveloperDetails = true)
        )
    }
}
