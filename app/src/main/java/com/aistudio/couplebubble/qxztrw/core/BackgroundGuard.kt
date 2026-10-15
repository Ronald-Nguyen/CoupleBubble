package com.aistudio.couplebubble.qxztrw.core

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

/** A failing background job ends only itself and is logged; without it any uncaught exception kills the app. */
val backgroundGuard = CoroutineExceptionHandler { _, error ->
    runCatching { Log.e("CoupleBubble", "Background work failed", error) }
}

/** `viewModelScope` with [backgroundGuard], also for `stateIn`, whose upstream failures would otherwise crash. */
val ViewModel.guardedScope: CoroutineScope get() = viewModelScope + backgroundGuard
