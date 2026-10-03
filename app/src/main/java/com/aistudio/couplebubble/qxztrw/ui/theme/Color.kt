package com.aistudio.couplebubble.qxztrw.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// Mediterranean Ocean Blue + Warm Sunset Terracotta
val OceanBluePrimaryLight = Color(0xFF1A6B99)
val OceanBlueOnPrimaryLight = Color(0xFFFFFFFF)
val OceanBlueContainerLight = Color(0xFFCCE6FF)
val OceanBlueOnContainerLight = Color(0xFF001E31)

val SunsetTerracottaLight = Color(0xFFE26533)
val SunsetTerracottaOnLight = Color(0xFFFFFFFF)
val SunsetTerracottaContainerLight = Color(0xFFFFDBCF)
val SunsetTerracottaOnContainerLight = Color(0xFF390C00)

val GoldenSunsetLight = Color(0xFFFA8A55)
val GoldenSunsetOnLight = Color(0xFFFFFFFF)
val GoldenSunsetContainerLight = Color(0xFFFFE4DA)
val GoldenSunsetOnContainerLight = Color(0xFF3B1002)

// Warm Porcelain & Cream Surfaces (Light)
val WarmPorcelainBackgroundLight = Color(0xFFFBF8F4)
val OnWarmBackgroundLight = Color(0xFF1D1B19)
val WarmPorcelainSurfaceLight = Color(0xFFFFFFFF)
val OnWarmSurfaceLight = Color(0xFF1D1B19)
val WarmCreamContainerLight = Color(0xFFF5EFE6)
val WarmCreamContainerHighLight = Color(0xFFEBE4D8)
val WarmSurfaceVariantLight = Color(0xFFE6DFD4)
val OnWarmSurfaceVariantLight = Color(0xFF4C4740)
val WarmOutlineLight = Color(0xFF7E776E)

// Dark Theme Tokens (Deep Oceanic Night + Luminous Terracotta Glow)
val OceanBluePrimaryDark = Color(0xFF8BCEFF)
val OceanBlueOnPrimaryDark = Color(0xFF003453)
val OceanBlueContainerDark = Color(0xFF004C74)
val OceanBlueOnContainerDark = Color(0xFFCCE6FF)

val SunsetTerracottaDark = Color(0xFFFFB59C)
val SunsetTerracottaOnDark = Color(0xFF5D1900)
val SunsetTerracottaContainerDark = Color(0xFF812802)
val SunsetTerracottaOnContainerDark = Color(0xFFFFDBCF)

val GoldenSunsetDark = Color(0xFFFFB690)
val GoldenSunsetOnDark = Color(0xFF5A1B00)
val GoldenSunsetContainerDark = Color(0xFF7E2B08)
val GoldenSunsetOnContainerDark = Color(0xFFFFE4DA)

val BackgroundDark = Color(0xFF0F151B)
val OnBackgroundDark = Color(0xFFE4E2DF)
val SurfaceDark = Color(0xFF141C24)
val OnSurfaceDark = Color(0xFFE4E2DF)
val SurfaceContainerDark = Color(0xFF1B232D)
val SurfaceContainerHighDark = Color(0xFF232D3A)
val SurfaceVariantDark = Color(0xFF2B3645)
val OnSurfaceVariantDark = Color(0xFFC6C2BC)
val OutlineDark = Color(0xFF8F8B83)

// Specialty Accents
val SoftHeartPink = Color(0xFFFF627E)
val AmberGlow = Color(0xFFFFB03A)
val SoftAzureAccent = Color(0xFF53A8DE)

// Readable foreground on user-selected profile colors
val ContrastNavy = Color(0xFF0F172A)

fun getContrastingTextColor(backgroundColor: Color): Color {
    val colorInt: Int = backgroundColor.toArgb()
    return if (ColorUtils.calculateLuminance(colorInt) > 0.45) ContrastNavy else Color.White
}
