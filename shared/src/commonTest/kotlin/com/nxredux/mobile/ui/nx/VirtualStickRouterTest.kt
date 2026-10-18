package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VirtualStickRouterTest {
    private val analog = mutableListOf<Pair<Float, Float>>()
    private val menu = mutableListOf<PadEvent>()
    private val router = VirtualStickRouter({ x, y -> analog += x to y }, { menu += it })

    @Test fun menu_closed_forwards_the_stick_to_the_game() {
        router.onStick(0.3f, -0.8f, menuOpen = false)
        assertEquals(listOf(0.3f to -0.8f), analog)
        assertTrue(menu.isEmpty())
    }

    @Test fun menu_open_turns_the_stick_into_menu_directions() {
        router.onStick(0f, 0.9f, menuOpen = true)
        router.onStick(0f, 0f, menuOpen = true)
        assertEquals(listOf<PadEvent>(PadEvent.Down(NxButton.DOWN), PadEvent.Up(NxButton.DOWN)), menu)
        assertTrue(analog.isEmpty())
    }

    @Test fun inside_the_deadzone_nothing_moves() {
        router.onStick(0.3f, -0.4f, menuOpen = true)
        assertTrue(menu.isEmpty())
    }

    @Test fun toggle_releases_menu_directions_and_centres_the_game_stick() {
        router.onStick(-0.9f, 0f, menuOpen = true)
        menu.clear()
        router.onMenuToggled()
        assertEquals(listOf<PadEvent>(PadEvent.Up(NxButton.LEFT)), menu)
        assertEquals(listOf(0f to 0f), analog)
        // Held again after the toggle: a fresh Down, not a stale "already held".
        router.onStick(-0.9f, 0f, menuOpen = true)
        assertEquals(PadEvent.Down(NxButton.LEFT), menu.last())
    }
}
