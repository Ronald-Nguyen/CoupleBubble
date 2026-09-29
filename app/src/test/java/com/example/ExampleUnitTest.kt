package com.example

import com.example.model.RelationshipDateCalculator
import com.example.repository.MockCoupleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testRelationshipDateCalculator() {
        val testNow = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 0, 0, 0)
        }

        // 1 year and 3 months prior: June 25, 2025
        val metrics = RelationshipDateCalculator.calculate(
            year = 2025,
            month = 6,
            day = 25,
            nowCal = testNow
        )

        assertEquals(1, metrics.years)
        assertEquals(3, metrics.months)
        assertEquals(0, metrics.days)
        assertTrue(metrics.totalDays > 450)
        assertEquals("2. Jahrestag", metrics.nextAnniversaryTitle)
        assertTrue(metrics.progressToNextAnniversary > 0f)
    }

    @Test
    fun testMockCoupleRepositoryPairing() = runBlocking {
        val repo = MockCoupleRepository()

        val invalidResult = repo.connectWithCode("123")
        assertTrue(invalidResult.isFailure)

        val validResult = repo.connectWithCode("BLU-789")
        assertTrue(validResult.isSuccess)
        val space = validResult.getOrNull()
        assertNotNull(space)
        assertEquals("Alex", space?.partner1Name)
        assertEquals("Sam", space?.partner2Name)
    }
}
