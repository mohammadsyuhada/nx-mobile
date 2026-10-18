package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class AxisDirectionsTest {
    private val events = ArrayList<PadEvent>()
    private val axes = AxisDirections { events += it }

    @Test fun an_axis_steps_past_half() {
        assertEquals(0, AxisDirections.step(0f))
        assertEquals(0, AxisDirections.step(0.5f))
        assertEquals(0, AxisDirections.step(-0.5f))
        assertEquals(1, AxisDirections.step(0.51f))
        assertEquals(-1, AxisDirections.step(-0.9f))
    }

    @Test fun the_hat_wins_over_the_stick() {
        assertEquals(-1, AxisDirections.combine(hat = -1f, stick = 1f))
        assertEquals(1, AxisDirections.combine(hat = 0f, stick = 0.8f))
        assertEquals(0, AxisDirections.combine(hat = 0.2f, stick = -0.3f))
    }

    @Test fun the_left_stick_presses_and_releases_directions() {
        axes.update(hatX = 0f, hatY = 0f, stickX = 0f, stickY = 0.9f)
        assertEquals(listOf<PadEvent>(PadEvent.Down(NxButton.DOWN)), events)
        axes.update(0f, 0f, 0f, 0.95f)                     // still down: no new event, the router repeats it
        assertEquals(1, events.size)
        axes.update(0f, 0f, 0f, 0.1f)
        assertEquals(PadEvent.Up(NxButton.DOWN), events.last())
        axes.update(0f, 0f, -0.7f, 0f)
        assertEquals(PadEvent.Down(NxButton.LEFT), events.last())
        axes.update(0f, 0f, 0.7f, 0f)                      // straight across: left up, then right down
        assertEquals(listOf(PadEvent.Up(NxButton.LEFT), PadEvent.Down(NxButton.RIGHT)), events.takeLast(2))
    }

    @Test fun the_hat_presses_directions() {
        axes.update(hatX = 0f, hatY = -1f, stickX = 0f, stickY = 0f)
        assertEquals(listOf<PadEvent>(PadEvent.Down(NxButton.UP)), events)
    }

    @Test fun release_lets_go_of_what_is_held() {
        axes.update(0f, 0f, 0.9f, -0.9f)
        events.clear()
        axes.release()
        assertEquals(setOf<PadEvent>(PadEvent.Up(NxButton.UP), PadEvent.Up(NxButton.RIGHT)), events.toSet())
        events.clear()
        axes.release()
        assertEquals(emptyList(), events)
    }

    @Test fun reset_forgets_without_events() {
        axes.update(0f, 0f, 0f, 0.9f)
        events.clear()
        axes.reset()
        assertEquals(emptyList(), events)
        axes.update(0f, 0f, 0f, 0.9f)
        assertEquals(listOf<PadEvent>(PadEvent.Down(NxButton.DOWN)), events)
    }
}
