package com.nxredux.mobile.runner

import com.nxredux.mobile.ui.nx.NxButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TriggerAxesTest {
    private val l2Down = listOf(NxButton.L2 to true)
    private val l2Up = listOf(NxButton.L2 to false)

    @Test fun a_pull_past_the_threshold_presses_and_a_let_go_below_the_release_releases() {
        val t = TriggerAxes()
        assertEquals(emptyList(), t.onAxes(0.4f, 0f))
        assertEquals(l2Down, t.onAxes(0.6f, 0f))
        assertEquals(emptyList(), t.onAxes(1f, 0f))
        assertEquals(emptyList(), t.onAxes(0.4f, 0f))   // inside the hysteresis: still held
        assertEquals(l2Up, t.onAxes(0.2f, 0f))
        assertEquals(emptyList(), t.onAxes(0f, 0f))
    }

    @Test fun both_triggers_change_in_one_event() {
        val t = TriggerAxes()
        assertEquals(listOf(NxButton.L2 to true, NxButton.R2 to true), t.onAxes(1f, 1f))
        assertEquals(listOf(NxButton.R2 to false), t.onAxes(1f, 0f))
    }

    @Test fun a_trigger_that_came_as_a_key_ignores_its_axis_from_then_on() {
        val t = TriggerAxes()
        assertTrue(t.onKey(NxButton.L2, down = true))
        assertEquals(emptyList(), t.onAxes(1f, 0f))
        assertEquals(emptyList(), t.onAxes(0f, 0f))      // the axis does not release what the key holds
        assertTrue(t.onKey(NxButton.L2, down = false))
        assertEquals(emptyList(), t.onAxes(1f, 0f))
        assertEquals(listOf(NxButton.R2 to true), t.onAxes(0f, 1f))   // the other trigger still goes by its axis
    }

    @Test fun a_key_for_a_pull_its_axis_already_pressed_is_not_pressed_again() {
        val t = TriggerAxes()
        assertEquals(l2Down, t.onAxes(1f, 0f))
        assertFalse(t.onKey(NxButton.L2, down = true))   // already down in the core
        assertEquals(emptyList(), t.onAxes(0f, 0f))      // the key owns it now
        assertTrue(t.onKey(NxButton.L2, down = false))
        assertFalse(t.onKey(NxButton.L2, down = false))  // a stray second up
    }

    @Test fun release_all_forgets_what_is_held_so_a_pull_kept_through_the_menu_presses_again() {
        val t = TriggerAxes()
        t.onAxes(1f, 0f)
        t.releaseAll()
        assertEquals(l2Down, t.onAxes(1f, 0f))
    }
}
