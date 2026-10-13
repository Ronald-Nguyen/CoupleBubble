package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.data.fake.FakeSessionStore
import com.aistudio.couplebubble.qxztrw.data.fake.MockCoupleRepository
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.locksOut
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRestorerTest {

    private val fullSpace = CoupleSpace(id = "space_1", userUids = listOf("mia", "leo"), partner1Id = "mia", partner2Id = "leo")

    /** Signs in as [uid] and serves [stored] for every restore by id. */
    private class Repository(private val uid: String?, private val stored: CoupleSpace) : MockCoupleRepository() {
        override suspend fun ensureSignedIn(): String? = uid

        override suspend fun restoreSession(coupleId: String): Result<CoupleSpace> {
            setStateForTesting(stored, null)
            return Result.success(stored)
        }
    }

    private suspend fun restore(uid: String?, stored: CoupleSpace): Triple<SessionRestorer, Repository, FakeSessionStore> {
        val repository = Repository(uid, stored)
        val sessionStore = FakeSessionStore().apply { saveActiveCoupleId(stored.id) }
        val restorer = SessionRestorer(repository, sessionStore)
        restorer.restore()
        return Triple(restorer, repository, sessionStore)
    }

    @Test
    fun memberKeepsTheStoredSpace() = runTest {
        val (restorer, repository, sessionStore) = restore("mia", fullSpace)

        assertEquals(fullSpace.id, repository.currentSpace.value?.id)
        assertEquals(fullSpace.id, sessionStore.activeCoupleIdFlow.value)
        assertFalse(restorer.lostAccess)
        assertTrue(restorer.isRestored.value)
    }

    @Test
    fun withoutLoginTheStoredSpaceIsLeft() = runTest {
        val (restorer, repository, sessionStore) = restore(null, fullSpace)

        assertNull(repository.currentSpace.value)
        assertNull(sessionStore.activeCoupleIdFlow.value)
        assertTrue(restorer.lostAccess)
        assertTrue(restorer.isRestored.value)
    }

    @Test
    fun newUidCannotStayInAFullSpace() = runTest {
        val (restorer, repository, _) = restore("stranger", fullSpace)

        assertNull(repository.currentSpace.value)
        assertTrue(restorer.lostAccess)
    }

    @Test
    fun lockOutRules() {
        val waiting = CoupleSpace(id = "space_2", userUids = listOf("mia"), partner1Id = "mia")

        assertFalse(CoupleSpace(id = "demo").locksOut(null))
        assertTrue(waiting.locksOut(null))
        assertFalse(waiting.locksOut("leo"))
        assertFalse(fullSpace.locksOut("leo"))
        assertTrue(fullSpace.locksOut("stranger"))
    }
}
