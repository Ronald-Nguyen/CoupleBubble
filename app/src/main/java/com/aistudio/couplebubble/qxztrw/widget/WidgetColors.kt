package com.aistudio.couplebubble.qxztrw.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** One widget color role with its light (day) and dark (night) value. */
internal data class WidgetColor(val day: Color, val night: Color) {
    val provider: ColorProvider get() = ColorProvider(day = day, night = night)
}

/**
 * Day/night palette of the Glance widgets: Deep Navy and Terracotta from the app's design tokens. Light mode uses
 * the warm window background with navy text, dark mode the navy surface with light text. Text accents use a
 * deeper Terracotta in light mode so small text keeps 4.5:1; contrast is checked in `WidgetColorsTest`.
 */
internal object WidgetColors {
    val Surface = WidgetColor(day = Color(0xFFFBF8F4), night = Color(0xFF0F2137))
    val Hero = WidgetColor(day = Color(0xFF0F2137), night = Color(0xFFFFFFFF))
    val Label = WidgetColor(day = Color(0xFF0F2137), night = Color(0xFF97CBFF))
    val Muted = WidgetColor(day = Color(0xFF45505E), night = Color(0xFFCBD5E1))
    val AccentText = WidgetColor(day = Color(0xFFB8441C), night = Color(0xFFFF8A65))
    val Progress = WidgetColor(day = Color(0xFFB8441C), night = Color(0xFFFF8A65))
    val ProgressTrack = WidgetColor(day = Color(0xFFE3D8CC), night = Color(0xFF2A3D57))

    /** Text roles drawn on [Surface]; each must reach 4.5:1 in both modes. */
    val textRoles = listOf(Hero, Label, Muted, AccentText)
}

/** WCAG relative luminance of an opaque color. */
internal fun relativeLuminance(color: Color): Double {
    fun channel(c: Float): Double = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

/** WCAG contrast ratio between two opaque colors (1.0 to 21.0). */
internal fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}
