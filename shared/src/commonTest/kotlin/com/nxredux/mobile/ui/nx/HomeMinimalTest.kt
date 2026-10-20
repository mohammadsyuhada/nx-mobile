package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeMinimalTest {
    // A pinned tool wears a Tool tag; the games keep List Home's tags.
    @Test fun pinned_tools_wear_a_tool_tag() {
        val rows = homeMinimalRows(listOf(com.nxredux.mobile.ui.MainRow.PickAGame, com.nxredux.mobile.ui.MainRow.PinnedTool(com.nxredux.mobile.ui.ToolRow(com.nxredux.mobile.ui.ToolKind.SETTINGS, "Settings", null))))
        assertEquals(listOf(null, "Tool"), rows.map { it.tag })
        assertEquals("Settings", rows[1].label)
    }

    // Below the selection 0.5 for the next name, then down with the screen to nothing at its bottom edge;
    // above it fainter, in steps: 0.3, 0.22, then 0.2.
    @Test fun the_fade_steps_down_and_holds_to_the_edges() {
        assertEquals(1f, minimalAlpha(0f))
        assertEquals(.5f, minimalAlpha(1f), 1e-6f)
        assertEquals(.75f, minimalAlpha(.5f), 1e-6f)
        assertEquals(.25f, minimalAlpha(3f, depth = .5f), 1e-6f)   // halfway down
        assertEquals(0f, minimalAlpha(9f, depth = 1f), 1e-6f)
        assertEquals(0f, minimalAlpha(20f, depth = 4f), 1e-6f)   // past the bottom edge: held
        assertEquals(.3f, minimalAlpha(-1f), 1e-6f)
        assertEquals(.22f, minimalAlpha(-2f), 1e-6f)
        assertEquals(MENU_FAR_ALPHA, minimalAlpha(-9f), 1e-6f)
        assertEquals(.65f, minimalAlpha(-.5f), 1e-6f)
    }
}
