package com.nxredux.mobile.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A read that must finish before the next screen can show (the RetroAchievements games list, a game's achievements,
 * Game Time's first page). [visible] turns on only after [delayMs], so a fast load never flickers the overlay; it
 * turns off when the block ends, fails or is cancelled. While [active], B cancels ([cancel]) and other input waits;
 * a load started with `cancellable = false` (the library rescan: see AppState) ignores B too. [message] is the
 * overlay's line for the load showing.
 */
internal class ScreenLoader(private val scope: CoroutineScope, private val delayMs: Long = 150) {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()
    private val _message = MutableStateFlow(LOADING)
    val message: StateFlow<String> = _message.asStateFlow()
    private var job: Job? = null
    private var onCancel: (() -> Unit)? = null
    private var cancellable = true
    private var generation = 0   // the latest load; a replaced one must not hide its successor's overlay

    val active: Boolean get() = job?.isActive == true

    /** Replaces any load in flight (without its [onCancel]); [block] sets the new screen. [onCancel] runs only on a B cancel. */
    fun start(onCancel: () -> Unit = {}, message: String = LOADING, cancellable: Boolean = true, block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        val id = ++generation
        this.onCancel = onCancel
        this.cancellable = cancellable
        _message.value = message
        // The body may run to its end inside launch (an immediate dispatcher), so it never reads [job] itself.
        job = scope.launch {
            val show = launch { delay(delayMs); if (generation == id) _visible.value = true }
            try {
                block()
            } finally {
                show.cancel()
                if (generation == id) _visible.value = false
            }
        }
    }

    /**
     * B while loading: abandons the load and hides the overlay. True when a load was in flight, also one that is not
     * [cancellable]: B is then swallowed and the load runs on.
     */
    fun cancel(): Boolean {
        if (!active) return false
        if (!cancellable) return true
        generation++
        job?.cancel()
        job = null
        _visible.value = false
        onCancel?.also { onCancel = null }?.invoke()
        return true
    }

    companion object {
        const val LOADING = "Loading…"
    }
}
