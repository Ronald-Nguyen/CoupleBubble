package com.aistudio.couplebubble.qxztrw.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Body of an `AlertDialog` that scrolls instead of being cut off at large font scales or in landscape. */
@Composable
fun DialogScrollColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), content = content)
}
