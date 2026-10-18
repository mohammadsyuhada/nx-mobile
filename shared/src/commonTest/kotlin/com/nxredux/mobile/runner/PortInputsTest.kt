package com.nxredux.mobile.runner

import com.nxredux.mobile.ui.nx.NxButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortInputsTest {
    @Test fun select_then_start_on_port_zero_opens_the_menu() {
        val p = PortInputs(2)
        assertFalse(p.menuChord(0, NxButton.SELECT, down = true))
        assertTrue(p.menuChord(0, NxButton.START, down = true))
    }

    @Test fun the_chord_is_port_zero_only() {
        val p = PortInputs(2)
        assertFalse(p.menuChord(1, NxButton.SELECT, down = true))
        assertFalse(p.menuChord(1, NxButton.START, down = true))
    }

    @Test fun select_on_one_port_and_start_on_another_never_chord() {
        val p = PortInputs(2)
        p.menuChord(1, NxButton.SELECT, down = true)   // P2 holds SELECT
        assertFalse(p.menuChord(0, NxButton.START, down = true))
    }

    @Test fun a_released_select_breaks_the_chord() {
        val p = PortInputs(1)
        p.menuChord(0, NxButton.SELECT, down = true)
        p.menuChord(0, NxButton.SELECT, down = false)
        assertFalse(p.menuChord(0, NxButton.START, down = true))
    }

    @Test fun reset_releases_one_port_only() {
        val p = PortInputs(2)
        p[0].sent.down(7 to 96, 96); p[1].sent.down(8 to 96, 96)
        p[0].stickDpadX = 1; p[1].stickDpadX = -1
        p[1].selectHeld = true
        p[1].reset()
        assertEquals(96, p[0].sent.held(7 to 96))
        assertEquals(1, p[0].stickDpadX)
        assertNull(p[1].sent.held(8 to 96))
        assertEquals(0, p[1].stickDpadX)
        assertFalse(p[1].selectHeld)
    }

    @Test fun only_an_axis_past_half_claims_a_slot() {
        assertFalse(axesClaim(floatArrayOf(0f, 0.1f, -0.3f, 0.49f)))   // a pad's resting report on connect
        assertTrue(axesClaim(floatArrayOf(0f, -0.5f)))
        assertTrue(axesClaim(floatArrayOf(1f)))
    }
}
