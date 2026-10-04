package com.aistudio.couplebubble.qxztrw.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** User-facing text produced outside the UI: a string resource, or a raw message (e.g. from an exception). */
sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText

    fun asString(resources: Resources): String = when (this) {
        is Resource -> resources.getString(id, *args.toTypedArray())
        is Raw -> text
    }
}

@Composable
fun UiText.asString(): String = asString(LocalContext.current.resources)
