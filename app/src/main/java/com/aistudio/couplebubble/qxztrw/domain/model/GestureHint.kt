package com.aistudio.couplebubble.qxztrw.domain.model

/**
 * Gestures in the app that might not be immediately obvious, with one-time hints shown to explain them.
 */
enum class GestureHint {
    /** Long-pressing a note in the overview selects it (shown once >= 2 notes exist). */
    NOTE_LONG_PRESS,

    /** Swiping down in the fullscreen image viewer closes it (shown on first opening). */
    FULLSCREEN_PHOTO_SWIPE,

    /** Tapping the next milestone card/row opens milestone settings. */
    MILESTONE_TAP,

    /** Dragging the handle next to an open checklist item reorders the list. */
    CHECKLIST_DRAG
}
