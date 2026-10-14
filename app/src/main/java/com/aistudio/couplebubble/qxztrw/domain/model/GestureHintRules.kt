package com.aistudio.couplebubble.qxztrw.domain.model

/**
 * Pure evaluation rules for gesture hint visibility, ensuring that hints appear only when their
 * prerequisites are met and that never more than one hint is shown at the same time.
 */
object GestureHintRules {

    fun shouldShowNoteLongPress(
        dismissedHints: Set<GestureHint>,
        noteCount: Int,
        isSelecting: Boolean,
        isSearchOpen: Boolean = false
    ): Boolean =
        GestureHint.NOTE_LONG_PRESS !in dismissedHints &&
            noteCount >= 2 &&
            !isSelecting &&
            !isSearchOpen

    fun shouldShowFullscreenSwipe(
        dismissedHints: Set<GestureHint>,
        isFullscreenOpen: Boolean
    ): Boolean =
        GestureHint.FULLSCREEN_PHOTO_SWIPE !in dismissedHints &&
            isFullscreenOpen

    fun shouldShowMilestoneTap(
        dismissedHints: Set<GestureHint>,
        hasMilestone: Boolean,
        isFullscreenOpen: Boolean = false,
        isSearchOpen: Boolean = false
    ): Boolean =
        GestureHint.MILESTONE_TAP !in dismissedHints &&
            hasMilestone &&
            !isFullscreenOpen &&
            !isSearchOpen

    fun shouldShowChecklistDrag(
        dismissedHints: Set<GestureHint>,
        isChecklist: Boolean,
        openItemCount: Int
    ): Boolean =
        GestureHint.CHECKLIST_DRAG !in dismissedHints &&
            isChecklist &&
            openItemCount >= 2

    /**
     * Resolves the single active hint for the Dashboard ("Uns" tab) and full-screen image viewer.
     * Fullscreen swipe takes precedence over milestone tap so that opening a photo never leaves two
     * hints active simultaneously.
     */
    fun resolveActiveDashboardHint(
        dismissedHints: Set<GestureHint>,
        hasMilestone: Boolean,
        isFullscreenOpen: Boolean,
        isSearchOpen: Boolean = false
    ): GestureHint? = when {
        shouldShowFullscreenSwipe(dismissedHints, isFullscreenOpen) -> GestureHint.FULLSCREEN_PHOTO_SWIPE
        shouldShowMilestoneTap(dismissedHints, hasMilestone, isFullscreenOpen, isSearchOpen) -> GestureHint.MILESTONE_TAP
        else -> null
    }

    /**
     * Resolves the single active hint for the Notes tab.
     * Checklist drag takes precedence inside an open checklist note, while note long-press
     * is shown in the overview grid.
     */
    fun resolveActiveNotesHint(
        dismissedHints: Set<GestureHint>,
        noteCount: Int,
        isOpenNote: Boolean,
        isChecklist: Boolean,
        openItemCount: Int,
        isSelecting: Boolean,
        isSearchOpen: Boolean = false
    ): GestureHint? = when {
        isOpenNote && shouldShowChecklistDrag(dismissedHints, isChecklist, openItemCount) -> GestureHint.CHECKLIST_DRAG
        !isOpenNote && shouldShowNoteLongPress(dismissedHints, noteCount, isSelecting, isSearchOpen) -> GestureHint.NOTE_LONG_PRESS
        else -> null
    }
}
