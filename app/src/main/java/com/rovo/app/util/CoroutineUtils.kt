package com.rovo.app.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * Launches a coroutine that catches unhandled exceptions and logs them
 * instead of crashing the app via the default uncaught exception handler.
 */
fun CoroutineScope.launchCatching(
    tag: String = "Rovo",
    block: suspend CoroutineScope.() -> Unit
) {
    val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e(tag, "Unhandled exception in coroutine", throwable)
    }
    launch(handler, block = block)
}
