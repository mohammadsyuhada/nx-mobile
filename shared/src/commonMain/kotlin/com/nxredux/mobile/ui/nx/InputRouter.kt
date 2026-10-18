package com.nxredux.mobile.ui.nx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Turns pad/keyboard events into presses; held directions auto-repeat through [onRepeat] (default: [onPress]). */
class InputRouter(
    private val scope: CoroutineScope,
    private val onPress: (NxButton) -> Unit,
    private val initialDelayMs: Long = 400,
    private val repeatMs: Long = 90,
    private val onRepeat: (NxButton) -> Unit = onPress,
) {
    private val held = HashMap<NxButton, Job?>()

    fun onEvent(event: PadEvent) {
        when (event) {
            is PadEvent.Down -> {
                // A Down for an already-held button (e.g. a stale hold whose Up was lost)
                // re-arms as a fresh press: drop the stale repeat job, then press again.
                held.remove(event.button)?.cancel()
                onPress(event.button)
                held[event.button] = if (event.button.isDirection) scope.launch {
                    delay(initialDelayMs)
                    while (true) { onRepeat(event.button); delay(repeatMs) }
                } else null
            }
            is PadEvent.Up -> held.remove(event.button)?.cancel()
        }
    }

    fun releaseAll() { held.values.forEach { it?.cancel() }; held.clear() }
}
