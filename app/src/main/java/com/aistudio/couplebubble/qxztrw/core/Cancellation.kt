package com.aistudio.couplebubble.qxztrw.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * Generic `catch (e: Exception)` blocks around suspend calls also catch [CancellationException]; rethrowing it
 * lets a cancelled coroutine stop instead of carrying on with fallback writes.
 */
fun Throwable.rethrowIfCancellation() {
    if (this is CancellationException) throw this
}
