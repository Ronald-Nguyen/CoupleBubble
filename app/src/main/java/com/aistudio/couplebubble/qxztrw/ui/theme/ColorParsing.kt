package com.aistudio.couplebubble.qxztrw.ui.theme

import androidx.compose.ui.graphics.Color
import com.aistudio.couplebubble.qxztrw.model.SpaceDefaults

/** Parses a stored accent color; invalid or missing values fall back to [fallbackHex], then to Terracotta. */
fun parseColorHexToCompose(hex: String?, fallbackHex: String = SpaceDefaults.PARTNER_1_COLOR_HEX): Color {
    val targetHex = if (!hex.isNullOrBlank()) hex.trim() else fallbackHex
    return try {
        Color(android.graphics.Color.parseColor(targetHex))
    } catch (_: Exception) {
        try {
            Color(android.graphics.Color.parseColor(fallbackHex))
        } catch (_: Exception) {
            Color(0xFFE65D2E)
        }
    }
}

fun parseColorHexToLong(hex: String?, fallback: Long): Long {
    if (hex.isNullOrBlank()) return fallback
    return try {
        android.graphics.Color.parseColor(hex.trim()).toLong() and 0xFFFFFFFFL
    } catch (_: Exception) {
        fallback
    }
}
