package com.aistudio.couplebubble.qxztrw

import androidx.compose.ui.graphics.Color
import com.aistudio.couplebubble.qxztrw.ui.theme.ContrastNavy
import com.aistudio.couplebubble.qxztrw.ui.theme.getContrastingTextColor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContrastingTextColorTest {

    @Test
    fun lightProfileColors_useNavyText() {
        assertEquals(ContrastNavy, getContrastingTextColor(Color(0xFF4ECDC4)))
        assertEquals(ContrastNavy, getContrastingTextColor(Color(0xFFFFE66D)))
        assertEquals(ContrastNavy, getContrastingTextColor(Color.White))
    }

    @Test
    fun darkProfileColors_useWhiteText() {
        assertEquals(Color.White, getContrastingTextColor(Color(0xFF0F2137)))
        assertEquals(Color.White, getContrastingTextColor(Color(0xFF9A5865)))
        assertEquals(Color.White, getContrastingTextColor(Color(0xFFE65D2E)))
        assertEquals(Color.White, getContrastingTextColor(Color.Black))
    }
}
