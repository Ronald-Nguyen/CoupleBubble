package com.aistudio.couplebubble.qxztrw.domain.session

import com.aistudio.couplebubble.qxztrw.data.fake.FakeSessionStore
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipCalendar
import com.aistudio.couplebubble.qxztrw.domain.model.CoupleSpace
import com.aistudio.couplebubble.qxztrw.domain.model.Memory
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import com.aistudio.couplebubble.qxztrw.notifications.MomentNotifier
import com.aistudio.couplebubble.qxztrw.ui.screens.home.MemoryPhotoUploadTracker
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PhotoSlotKey
import com.aistudio.couplebubble.qxztrw.ui.screens.home.PhotoSyncState
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCoordinatorsTest {

    private class RecordingNotifier : MomentNotifier {
        val moments = mutableListOf<Pair<String, String>>()
        val milestones = mutableListOf<Milestone>()

        override fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String) {
            moments += partnerName to title
        }

        override fun notifyMilestoneReached(milestone: Milestone) {
            milestones += milestone
        }
    }

    private val space = CoupleSpace(
        id = "space_1",
        partner1Name = "Mia",
        partner2Name = "Leo",
        partner1Id = "mia",
        partner2Id = "leo",
        isActive = true
    )

    @Test
    fun onlyMomentsThePartnerAddsLaterNotify() = runTest {
        val notifier = RecordingNotifier()
        val watcher = PartnerMomentWatcher(notifier)
        val old = Memory(id = "m1", title = "Alt", createdBy = "leo")

        watcher.onMemories(space, listOf(old), ownUid = "mia")
        watcher.onMemories(
            space,
            listOf(
                old,
                Memory(id = "m2", title = "Paris", createdBy = "leo"),
                Memory(id = "m3", title = "Eigenes", createdBy = "mia"),
                Memory(id = "m4", title = "Ohne Autor")
            ),
            ownUid = "mia"
        )

        assertEquals(listOf("Leo" to "Paris"), notifier.moments)
    }

    @Test
    fun suppressesPartnerMomentsWhenDisabledInSessionStore() = runTest {
        val notifier = RecordingNotifier()
        val sessionStore = FakeSessionStore().apply {
            savePartnerMomentsNotificationEnabled(false)
        }
        val watcher = PartnerMomentWatcher(notifier, sessionStore)
        val old = Memory(id = "m1", title = "Alt", createdBy = "leo")

        watcher.onMemories(space, listOf(old), ownUid = "mia")
        watcher.onMemories(
            space,
            listOf(
                old,
                Memory(id = "m2", title = "Paris", createdBy = "leo")
            ),
            ownUid = "mia"
        )

        assertTrue(notifier.moments.isEmpty())
    }

    @Test
    fun resetMakesTheNextListABaselineAgain() = runTest {
        val notifier = RecordingNotifier()
        val watcher = PartnerMomentWatcher(notifier)
        watcher.onMemories(space, emptyList(), ownUid = "mia")

        watcher.reset()
        watcher.onMemories(space, listOf(Memory(id = "m1", title = "Paris", createdBy = "leo")), ownUid = "mia")

        assertTrue(notifier.moments.isEmpty())
    }

    @Test
    fun milestoneDueTodayNotifiesOnce() = runTest {
        val notifier = RecordingNotifier()
        val reminder = MilestoneReminder(notifier, sessionStore = null)
        val today = LocalDate.of(2026, 10, 13)
        val calendar = RelationshipCalendar(2025, 10, 13, setOf(MilestoneKind.YEARS), emptyList())

        repeat(2) { reminder.onMetrics(space.id, calendar.metrics(today)) }
        reminder.onMetrics(space.id, calendar.metrics(today.minusDays(1)))

        assertEquals(listOf(MilestoneKind.YEARS), notifier.milestones.map { it.kind })
    }

    @Test
    fun suppressesMilestoneWhenDisabledInSessionStore() = runTest {
        val notifier = RecordingNotifier()
        val sessionStore = FakeSessionStore().apply {
            saveMilestonesNotificationEnabled(false)
        }
        val reminder = MilestoneReminder(notifier, sessionStore)
        val today = LocalDate.of(2026, 10, 13)
        val calendar = RelationshipCalendar(2025, 10, 13, setOf(MilestoneKind.YEARS), emptyList())

        reminder.onMetrics(space.id, calendar.metrics(today))

        assertTrue(notifier.milestones.isEmpty())
    }

    @Test
    fun momentAnniversaryDueTodayNotifies() = runTest {
        val notifier = RecordingNotifier()
        val reminder = MilestoneReminder(notifier, sessionStore = null)
        val today = LocalDate.of(2026, 10, 13)
        val moments = listOf(MomentMilestone("m1", "Erster Urlaub", LocalDate.of(2025, 10, 13)))
        val calendar = RelationshipCalendar(2025, 1, 1, emptySet(), emptyList(), moments)

        reminder.onMetrics(space.id, calendar.metrics(today))

        assertEquals(listOf("Erster Urlaub"), notifier.milestones.map { it.customTitle })
    }

    @Test
    fun uploadTrackerShowsTheLocalCopyUntilTheUploadReturns() = runTest {
        val tracker = MemoryPhotoUploadTracker(syncedBadgeMillis = 1_000)
        val local = Memory(id = "m1", title = "Paris", partner1ImageUrl = "file:///local.jpg")
        val upload = CompletableDeferred<Result<Memory>>()
        val slot = PhotoSlotKey("m1", isPartner1 = true)

        val job = launch { tracker.track(local, PartnerRole.PARTNER_1) { upload.await() } }
        runCurrent()
        assertEquals(local, tracker.pendingMemories.value["m1"])
        assertEquals(PhotoSyncState.UPLOADING, tracker.syncStates.value[slot])

        upload.complete(Result.success(local.copy(partner1ImageUrl = "https://firebasestorage.googleapis.com/x.jpg")))
        runCurrent()
        assertTrue(tracker.pendingMemories.value.isEmpty())
        assertEquals(PhotoSyncState.SYNCED, tracker.syncStates.value[slot])

        advanceUntilIdle()
        job.join()
        assertTrue(tracker.syncStates.value.isEmpty())
    }

    @Test
    fun base64FallbackDropsTheBadgeWithoutCheckMark() = runTest {
        val tracker = MemoryPhotoUploadTracker()
        val local = Memory(id = "m1", partner2ImageUrl = "file:///local.jpg")

        tracker.track(local, PartnerRole.PARTNER_2) { Result.success(local.copy(partner2ImageUrl = "data:image/jpeg;base64,AAAA")) }

        assertTrue(tracker.syncStates.value.isEmpty())
        assertTrue(tracker.pendingMemories.value.isEmpty())
    }
}
