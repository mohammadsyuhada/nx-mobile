package com.nxredux.mobile.runner

import com.nxredux.mobile.ui.nx.NxButton

/**
 * L2/R2 as digital buttons from a pad's trigger axes (AXIS_LTRIGGER/AXIS_BRAKE, AXIS_RTRIGGER/AXIS_GAS, the larger of each
 * pair): a pull past [press] presses, falling back under [release] releases (the gap keeps a trigger resting near the line
 * from chattering). A trigger that has come as a key event once goes by its keys alone from then on ([onKey]), so a pad
 * that sends both a key and an axis for one pull presses once.
 *
 * Pure: the host feeds axes and keys and sends each change to the core (or its hotkeys).
 */
class TriggerAxes(private val press: Float = 0.5f, private val release: Float = 0.3f) {
    private val held = HashSet<NxButton>()       // L2/R2 the core has been given as pressed
    private val keysSeen = HashSet<NxButton>()   // L2/R2 that came as key events: their axes are ignored

    /** The trigger values of one motion event; the presses and releases they make, L2 before R2. */
    fun onAxes(left: Float, right: Float): List<Pair<NxButton, Boolean>> {
        val out = ArrayList<Pair<NxButton, Boolean>>(2)
        fun pull(button: NxButton, v: Float) {
            if (button in keysSeen) return
            val isHeld = button in held
            if (!isHeld && v > press) set(button, true)?.let(out::add)
            else if (isHeld && v < release) set(button, false)?.let(out::add)
        }
        pull(NxButton.L2, left)
        pull(NxButton.R2, right)
        return out
    }

    /**
     * L2/R2 as a key event; true when the core should see it (a pull its axis already pressed is not pressed again, a
     * stray second up is dropped). From the first key on, that trigger's axes are ignored.
     */
    fun onKey(button: NxButton, down: Boolean): Boolean {
        keysSeen += button
        return set(button, down) != null
    }

    /** The menu opened or the game paused: the host released everything in the core, so nothing is held here. */
    fun releaseAll() = held.clear()

    private fun set(button: NxButton, down: Boolean): Pair<NxButton, Boolean>? {
        if (down == button in held) return null
        if (down) held += button else held -= button
        return button to down
    }
}
