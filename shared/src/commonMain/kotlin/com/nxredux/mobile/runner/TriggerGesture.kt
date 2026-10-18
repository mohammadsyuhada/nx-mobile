package com.nxredux.mobile.runner

/**
 * One GameCube analog trigger on the on-screen pad, which is a button, not a slider. A quick tap toggles a latched soft
 * press (the light squeeze that reads half-way: games with a trigger-click threshold stay unpressed); holding for
 * [holdMs] or more is a full press for as long as the finger stays down, then it falls back to the latch. Pure: the
 * caller passes the time, and posts a [tick] for down + [holdMs] so the full press lands while the finger is still held.
 */
class TriggerGesture(val holdMs: Long = 300) {
    /** What the trigger sends: nothing, half way (analog only), or all the way (analog and the digital key). */
    enum class State { OFF, SOFT, FULL }

    private var latched = false
    private var downAt: Long? = null
    private var full = false

    private fun state(): State = when {
        full -> State.FULL
        latched -> State.SOFT
        else -> State.OFF
    }

    fun down(nowMs: Long): State {
        if (downAt == null) {
            downAt = nowMs
            full = false
        }
        return state()
    }

    fun tick(nowMs: Long): State {
        val at = downAt
        if (at != null && nowMs - at >= holdMs) full = true
        return state()
    }

    fun up(nowMs: Long): State {
        val at = downAt ?: return state()
        // A hold whose tick never arrived is still a hold: it must not flip the latch.
        if (!full && nowMs - at < holdMs) latched = !latched
        downAt = null
        full = false
        return state()
    }

    fun clear(): State {
        latched = false
        downAt = null
        full = false
        return State.OFF
    }
}
