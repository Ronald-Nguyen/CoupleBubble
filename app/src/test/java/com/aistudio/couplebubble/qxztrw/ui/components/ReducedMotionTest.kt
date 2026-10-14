package com.aistudio.couplebubble.qxztrw.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducedMotionTest {
    @Test
    fun zeroScaleMeansReducedMotion() {
        assertTrue(isReducedMotion(0f))
    }

    @Test
    fun otherScalesKeepMotion() {
        assertFalse(isReducedMotion(1f))
        assertFalse(isReducedMotion(0.5f))
        assertFalse(isReducedMotion(2f))
    }
}
