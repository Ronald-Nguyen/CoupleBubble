package com.aistudio.couplebubble.qxztrw

import com.aistudio.couplebubble.qxztrw.model.Memory
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.repository.FirebaseCoupleRepository
import com.aistudio.couplebubble.qxztrw.repository.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.repository.SpaceUnavailableException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {

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
        assertEquals("Alex", space?.partnerAName)
        assertEquals("Sam", space?.partnerBName)

        repo.updatePartnerNames(space!!.id, "Anna", "Ben")
        assertEquals("Anna", repo.currentSpace.value?.partnerAName)
        assertEquals("Ben", repo.currentSpace.value?.partnerBName)
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
    fun testFirebaseCoupleRepositoryPairing() = runBlocking {
        val repo = FirebaseCoupleRepository()

        val invalidResult = repo.connectWithCode("123")
        assertTrue(invalidResult.isFailure)

        // Without Firebase there is no space to join, and no placeholder space is invented
        val offlineResult = repo.connectWithCode("482-913")
        assertTrue(offlineResult.exceptionOrNull() is SpaceUnavailableException)
        assertNull(repo.currentSpace.value)
    }

    @Test
    fun restoreSessionWithoutFirebaseKeepsSessionInsteadOfPlaceholder() = runBlocking {
        val repo = FirebaseCoupleRepository()

        val result = repo.restoreSession("space_482913")

        assertTrue(result.exceptionOrNull() is SpaceUnavailableException)
        assertNull(repo.currentSpace.value)
    }
}
