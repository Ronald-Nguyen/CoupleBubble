package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = OceanBluePrimaryLight,
    onPrimary = OceanBlueOnPrimaryLight,
    primaryContainer = OceanBlueContainerLight,
    onPrimaryContainer = OceanBlueOnContainerLight,
    secondary = SunsetTerracottaLight,
    onSecondary = SunsetTerracottaOnLight,
    secondaryContainer = SunsetTerracottaContainerLight,
    onSecondaryContainer = SunsetTerracottaOnContainerLight,
    tertiary = GoldenSunsetLight,
    onTertiary = GoldenSunsetOnLight,
    tertiaryContainer = GoldenSunsetContainerLight,
    onTertiaryContainer = GoldenSunsetOnContainerLight,
    background = WarmPorcelainBackgroundLight,
    onBackground = OnWarmBackgroundLight,
    surface = WarmPorcelainSurfaceLight,
    onSurface = OnWarmSurfaceLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = OnWarmSurfaceVariantLight,
    outline = WarmOutlineLight,
    surfaceContainer = WarmCreamContainerLight,
    surfaceContainerHigh = WarmCreamContainerHighLight
)

private val DarkColorScheme = darkColorScheme(
    primary = OceanBluePrimaryDark,
    onPrimary = OceanBlueOnPrimaryDark,
    primaryContainer = OceanBlueContainerDark,
    onPrimaryContainer = OceanBlueOnContainerDark,
    secondary = SunsetTerracottaDark,
    onSecondary = SunsetTerracottaOnDark,
    secondaryContainer = SunsetTerracottaContainerDark,
    onSecondaryContainer = SunsetTerracottaOnContainerDark,
    tertiary = GoldenSunsetDark,
    onTertiary = GoldenSunsetOnDark,
    tertiaryContainer = GoldenSunsetContainerDark,
    onTertiaryContainer = GoldenSunsetOnContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark
)

val CoupleShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CoupleShapes,
        content = content
    )
}
