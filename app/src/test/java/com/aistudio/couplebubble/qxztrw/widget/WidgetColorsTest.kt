package com.aistudio.couplebubble.qxztrw.widget

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetColorsTest {

    @Test
    fun textRoles_reachAaContrastInBothModes() {
        WidgetColors.textRoles.forEach { role ->
            val day = contrastRatio(role.day, WidgetColors.Surface.day)
            val night = contrastRatio(role.night, WidgetColors.Surface.night)
            assertTrue("day contrast $day for $role", day >= 4.5)
            assertTrue("night contrast $night for $role", night >= 4.5)
        }
    }

    @Test
    fun progressBar_reachesNonTextContrastAgainstTrackAndSurface() {
        val p = WidgetColors.Progress
        assertTrue(contrastRatio(p.day, WidgetColors.ProgressTrack.day) >= 3.0)
        assertTrue(contrastRatio(p.night, WidgetColors.ProgressTrack.night) >= 3.0)
        assertTrue(contrastRatio(p.day, WidgetColors.Surface.day) >= 3.0)
        assertTrue(contrastRatio(p.night, WidgetColors.Surface.night) >= 3.0)
    }

    @Test
    fun designTokens_areUsed() {
        assertEquals(Color(0xFF0F2137), WidgetColors.Hero.day)
        assertEquals(Color(0xFF0F2137), WidgetColors.Surface.night)
        assertEquals(Color(0xFFE65D2E), WidgetColors.Progress.day)
        assertEquals(Color(0xFFFF8A65), WidgetColors.AccentText.night)
    }

    @Test
    fun contrastRatio_blackOnWhiteIs21() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
    }

    @Test
    fun progressPercent_roundsAndClamps() {
        assertEquals(43, progressPercent(0.426f))
        assertEquals(100, progressPercent(1.4f))
        assertEquals(0, progressPercent(-0.1f))
    }
}
