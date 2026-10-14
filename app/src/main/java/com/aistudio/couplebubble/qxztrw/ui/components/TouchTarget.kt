package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Minimum touch target for every clickable element (Material accessibility guideline). */
val MinTouchTarget: Dp = 48.dp

/**
 * Circular 48 dp touch target around a smaller visual of [visualSize]: the click area covers
 * [MinTouchTarget] while the content drawn after this modifier keeps its smaller size.
 */
fun Modifier.circularTouchTarget(
    visualSize: Dp,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier {
    val inset = ((MinTouchTarget - visualSize) / 2).coerceAtLeast(0.dp)
    return this
        .size(maxOf(MinTouchTarget, visualSize))
        .clip(CircleShape)
        .clickable(role = role, onClickLabel = onClickLabel, onClick = onClick)
        .padding(inset)
}
