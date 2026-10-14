package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True when the system asks for less motion (developer option / accessibility
 * "remove animations", i.e. `ANIMATOR_DURATION_SCALE == 0`). Endless animations
 * become static then; one-time spring transitions stay.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Maps the system animator duration scale to the reduced-motion flag. */
fun isReducedMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f
