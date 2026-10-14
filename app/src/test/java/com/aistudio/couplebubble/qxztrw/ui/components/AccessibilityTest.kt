package com.aistudio.couplebubble.qxztrw.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityTest {

    @Test
    fun spokenDigitsSpellsEachDigitAndDropsTheHyphen() {
        assertEquals("4, 8, 2, 9, 1, 3", spokenDigits("482-913"))
        assertEquals("", spokenDigits(""))
    }

    @Test
    fun minutesLeftRoundsUpAndNeverGoesNegative() {
        assertEquals(13, minutesLeft(754))
        assertEquals(1, minutesLeft(1))
        assertEquals(1, minutesLeft(60))
        assertEquals(0, minutesLeft(0))
        assertEquals(0, minutesLeft(-5))
    }
}
