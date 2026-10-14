package com.aistudio.couplebubble.qxztrw.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSizesTest {

    @Test
    fun everyResponsiveSizeMapsToItsLayout() {
        assertEquals(WidgetLayout.COMPACT, widgetLayout(DpSize(48.dp, 48.dp)))
        assertEquals(WidgetLayout.COMPACT, widgetLayout(DpSize(48.dp, 110.dp)))
        assertEquals(WidgetLayout.ROW, widgetLayout(DpSize(110.dp, 48.dp)))
        assertEquals(WidgetLayout.ROW, widgetLayout(DpSize(250.dp, 48.dp)))
        assertEquals(WidgetLayout.REGULAR, widgetLayout(DpSize(110.dp, 110.dp)))
        assertEquals(WidgetLayout.REGULAR, widgetLayout(DpSize(180.dp, 110.dp)))
        assertEquals(WidgetLayout.TALL, widgetLayout(DpSize(180.dp, 170.dp)))
    }

    @Test
    fun responsiveSizesCoverEveryLayout() {
        assertEquals(WidgetLayout.entries.toSet(), WidgetResponsiveSizes.sizes.map(::widgetLayout).toSet())
    }

    @Test
    fun longCompactHeroesShrink() {
        assertEquals(22f, heroSize(WidgetLayout.COMPACT, "412").value, 0f)
        assertEquals(16f, heroSize(WidgetLayout.COMPACT, "Heute").value, 0f)
    }
}
