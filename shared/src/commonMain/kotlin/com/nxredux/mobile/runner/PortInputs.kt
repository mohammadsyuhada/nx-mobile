package com.nxredux.mobile.runner

import com.nxredux.mobile.ui.nx.NxButton
import kotlin.math.abs

/**
 * One controller port's live input: what its held keys were sent as ([sent], keyed by (device id, keycode)), its L2/R2
 * trigger axes, its left stick folded into the d-pad on a core without a stick, and SELECT/START for the menu chord.
 * Per port, so two players never mix: P2's SELECT never completes P1's START.
 */
class PortInput {
    val triggers = TriggerAxes()
    val sent = SentKeys<Pair<Int, Int>>()
    var stickDpadX = 0
    var stickDpadY = 0
    var selectHeld = false
    var startHeld = false

    /** Forgets everything held (the host sends the core the releases). */
    fun reset() {
        triggers.releaseAll()
        sent.clear()
        stickDpadX = 0
        stickDpadY = 0
        selectHeld = false
        startHeld = false
    }
}

/** The [count] ports' [PortInput]s. P1 owns the hotkeys: only port 0's SELECT + START opens the menu ([menuChord]). */
class PortInputs(val count: Int) {
    private val ports = Array(count) { PortInput() }

    operator fun get(port: Int): PortInput = ports[port]

    /** Tracks SELECT/START on [port]; true when this press completes SELECT + START on port 0. Other ports never chord. */
    fun menuChord(port: Int, button: NxButton, down: Boolean): Boolean {
        if (port != HOTKEY_PORT) return false
        val p = ports[port]
        when (button) {
            NxButton.SELECT -> p.selectHeld = down
            NxButton.START -> p.startHeld = down
            else -> return false
        }
        return down && p.selectHeld && p.startHeld
    }

    companion object {
        const val HOTKEY_PORT = 0
    }
}

/** A motion event's axes claim a player slot only when one is pushed past half: a pad's resting report on connect does not. */
fun axesClaim(values: FloatArray): Boolean = values.any { abs(it) >= 0.5f }
