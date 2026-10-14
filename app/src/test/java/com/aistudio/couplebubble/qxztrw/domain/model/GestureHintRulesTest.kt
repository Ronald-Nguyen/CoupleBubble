package com.aistudio.couplebubble.qxztrw.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureHintRulesTest {

    @Test
    fun noteLongPressHintShownOnlyWhenAtLeastTwoNotesAndNotInSelection() {
        val emptyDismissed = emptySet<GestureHint>()

        // Less than 2 notes -> false
        assertFalse(
            GestureHintRules.shouldShowNoteLongPress(
                dismissedHints = emptyDismissed,
                noteCount = 1,
                isSelecting = false
            )
        )

        // 2 notes in overview -> true
        assertTrue(
            GestureHintRules.shouldShowNoteLongPress(
                dismissedHints = emptyDismissed,
                noteCount = 2,
                isSelecting = false
            )
        )

        // Already selecting -> false
        assertFalse(
            GestureHintRules.shouldShowNoteLongPress(
                dismissedHints = emptyDismissed,
                noteCount = 2,
                isSelecting = true
            )
        )

        // Search open -> false
        assertFalse(
            GestureHintRules.shouldShowNoteLongPress(
                dismissedHints = emptyDismissed,
                noteCount = 2,
                isSelecting = false,
                isSearchOpen = true
            )
        )

        // Already dismissed -> false
        assertFalse(
            GestureHintRules.shouldShowNoteLongPress(
                dismissedHints = setOf(GestureHint.NOTE_LONG_PRESS),
                noteCount = 3,
                isSelecting = false
            )
        )
    }

    @Test
    fun fullscreenSwipeHintShownOnlyWhenFullscreenOpenAndNotDismissed() {
        val emptyDismissed = emptySet<GestureHint>()

        assertTrue(
            GestureHintRules.shouldShowFullscreenSwipe(
                dismissedHints = emptyDismissed,
                isFullscreenOpen = true
            )
        )

        assertFalse(
            GestureHintRules.shouldShowFullscreenSwipe(
                dismissedHints = emptyDismissed,
                isFullscreenOpen = false
            )
        )

        assertFalse(
            GestureHintRules.shouldShowFullscreenSwipe(
                dismissedHints = setOf(GestureHint.FULLSCREEN_PHOTO_SWIPE),
                isFullscreenOpen = true
            )
        )
    }

    @Test
    fun milestoneTapHintShownOnlyWhenMilestonePresentAndFullscreenClosed() {
        val emptyDismissed = emptySet<GestureHint>()

        // Milestone present and fullscreen closed -> true
        assertTrue(
            GestureHintRules.shouldShowMilestoneTap(
                dismissedHints = emptyDismissed,
                hasMilestone = true,
                isFullscreenOpen = false
            )
        )

        // No milestone -> false
        assertFalse(
            GestureHintRules.shouldShowMilestoneTap(
                dismissedHints = emptyDismissed,
                hasMilestone = false,
                isFullscreenOpen = false
            )
        )

        // Fullscreen open over dashboard -> false (mutual exclusivity)
        assertFalse(
            GestureHintRules.shouldShowMilestoneTap(
                dismissedHints = emptyDismissed,
                hasMilestone = true,
                isFullscreenOpen = true
            )
        )

        // Search open -> false
        assertFalse(
            GestureHintRules.shouldShowMilestoneTap(
                dismissedHints = emptyDismissed,
                hasMilestone = true,
                isFullscreenOpen = false,
                isSearchOpen = true
            )
        )

        // Already dismissed -> false
        assertFalse(
            GestureHintRules.shouldShowMilestoneTap(
                dismissedHints = setOf(GestureHint.MILESTONE_TAP),
                hasMilestone = true,
                isFullscreenOpen = false
            )
        )
    }

    @Test
    fun checklistDragHintShownOnlyInOpenChecklistWithAtLeastTwoOpenItems() {
        val emptyDismissed = emptySet<GestureHint>()

        // 2 open items in open checklist -> true
        assertTrue(
            GestureHintRules.shouldShowChecklistDrag(
                dismissedHints = emptyDismissed,
                isChecklist = true,
                openItemCount = 2
            )
        )

        // Only 1 item in checklist -> false
        assertFalse(
            GestureHintRules.shouldShowChecklistDrag(
                dismissedHints = emptyDismissed,
                isChecklist = true,
                openItemCount = 1
            )
        )

        // Text note (not checklist) -> false
        assertFalse(
            GestureHintRules.shouldShowChecklistDrag(
                dismissedHints = emptyDismissed,
                isChecklist = false,
                openItemCount = 3
            )
        )

        // Already dismissed -> false
        assertFalse(
            GestureHintRules.shouldShowChecklistDrag(
                dismissedHints = setOf(GestureHint.CHECKLIST_DRAG),
                isChecklist = true,
                openItemCount = 3
            )
        )
    }

    @Test
    fun resolveActiveDashboardHintEnforcesNeverMoreThanOneHint() {
        // When both fullscreen swipe and milestone tap could be eligible, fullscreen takes precedence
        val hintWhenFullscreen = GestureHintRules.resolveActiveDashboardHint(
            dismissedHints = emptySet(),
            hasMilestone = true,
            isFullscreenOpen = true
        )
        assertEquals(GestureHint.FULLSCREEN_PHOTO_SWIPE, hintWhenFullscreen)

        // When fullscreen closed, milestone tap is returned
        val hintWhenNormal = GestureHintRules.resolveActiveDashboardHint(
            dismissedHints = emptySet(),
            hasMilestone = true,
            isFullscreenOpen = false
        )
        assertEquals(GestureHint.MILESTONE_TAP, hintWhenNormal)

        // When milestone tap is dismissed, returns null
        val hintWhenDismissed = GestureHintRules.resolveActiveDashboardHint(
            dismissedHints = setOf(GestureHint.MILESTONE_TAP),
            hasMilestone = true,
            isFullscreenOpen = false
        )
        assertNull(hintWhenDismissed)
    }

    @Test
    fun resolveActiveNotesHintEnforcesNeverMoreThanOneHint() {
        // When editing a checklist note with 2 items and overview also had >= 2 notes
        val hintInEditor = GestureHintRules.resolveActiveNotesHint(
            dismissedHints = emptySet(),
            noteCount = 5,
            isOpenNote = true,
            isChecklist = true,
            openItemCount = 2,
            isSelecting = false
        )
        assertEquals(GestureHint.CHECKLIST_DRAG, hintInEditor)

        // When in overview
        val hintInOverview = GestureHintRules.resolveActiveNotesHint(
            dismissedHints = emptySet(),
            noteCount = 5,
            isOpenNote = false,
            isChecklist = false,
            openItemCount = 0,
            isSelecting = false
        )
        assertEquals(GestureHint.NOTE_LONG_PRESS, hintInOverview)

        // When selecting notes in overview
        val hintWhileSelecting = GestureHintRules.resolveActiveNotesHint(
            dismissedHints = emptySet(),
            noteCount = 5,
            isOpenNote = false,
            isChecklist = false,
            openItemCount = 0,
            isSelecting = true
        )
        assertNull(hintWhileSelecting)
    }
}
