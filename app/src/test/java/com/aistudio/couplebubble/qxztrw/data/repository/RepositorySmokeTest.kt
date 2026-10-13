package com.aistudio.couplebubble.qxztrw.data.repository

import androidx.test.core.app.ApplicationProvider
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.data.local.AndroidLocalImageStore
import com.aistudio.couplebubble.qxztrw.data.remote.FirebaseProvider
import com.aistudio.couplebubble.qxztrw.data.remote.RemoteImageUploader
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.error.SpaceUnavailableException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositorySmokeTest {

    @Test
    fun testRelationshipDateCalculator() {
        val testNow = LocalDate.of(2026, 9, 25)

        val metrics = RelationshipDateCalculator.calculate(
            year = 2025,
            month = 6,
            day = 25,
            today = testNow
        )

        assertEquals(1, metrics.years)
        assertEquals(3, metrics.months)
        assertEquals(0, metrics.days)
        assertTrue(metrics.totalDays > 450)
        assertEquals(MilestoneKind.DAYS, metrics.nextMilestone?.kind)
        assertEquals(500, metrics.nextMilestone?.value)
        assertTrue(metrics.progressToNextMilestone > 0f)
    }

    @Test
    fun testMockCoupleRepositoryPairingAndNames() = runBlocking {
        val repo = MockCoupleRepository()

        val invalidResult = repo.connectWithCode("123")
        assertTrue(invalidResult.isFailure)
        assertTrue(repo.connectWithCode("BLU-789").isFailure)

        val validResult = repo.connectWithCode("482-913")
        assertTrue(validResult.isSuccess)
        val space = validResult.getOrNull()
        assertNotNull(space)
        assertEquals("Alex", space?.partner1Name)
        assertEquals("Sam", space?.partner2Name)

        repo.updatePartnerNames(space!!.id, "Anna", "Ben")
        assertEquals("Anna", repo.currentSpace.value?.partner1Name)
        assertEquals("Ben", repo.currentSpace.value?.partner2Name)
    }

    @Test
    fun testMemoriesFlowAndAdd() = runBlocking {
        val repo = MockCoupleRepository()
        val initialMemories = repo.getMemories("space1").first()
        assertTrue(initialMemories.isNotEmpty())

        val newMemory = Memory(
            title = "Test Meilenstein",
            date = LocalDate.of(2026, 1, 1),
            note = "Eine Test-Notiz"
        )
        val addResult = repo.addMemory("space1", newMemory)
        assertTrue(addResult.isSuccess)

        val updated = repo.getMemories("space1").first()
        assertTrue(updated.any { it.title == "Test Meilenstein" })
    }

    @Test
    fun testFirebaseSpaceRepositoryPairing() = runBlocking {
        val repo = firebaseSpaceRepository()

        val invalidResult = repo.connectWithCode("123")
        assertTrue(invalidResult.isFailure)

        // Without Firebase there is no space to join, and no placeholder space is invented
        val offlineResult = repo.connectWithCode("482-913")
        assertTrue(offlineResult.exceptionOrNull() is SpaceUnavailableException)
        assertNull(repo.currentSpace.value)
    }

    @Test
    fun restoreSessionWithoutFirebaseKeepsSessionInsteadOfPlaceholder() = runBlocking {
        val repo = firebaseSpaceRepository()

        val result = repo.restoreSession("space_482913")

        assertTrue(result.exceptionOrNull() is SpaceUnavailableException)
        assertNull(repo.currentSpace.value)
    }

    private fun firebaseSpaceRepository(): FirebaseSpaceRepository {
        val firebase = FirebaseProvider()
        return FirebaseSpaceRepository(
            firebase = firebase,
            uploader = RemoteImageUploader(firebase),
            localImageStore = AndroidLocalImageStore(ApplicationProvider.getApplicationContext())
        )
    }
}
