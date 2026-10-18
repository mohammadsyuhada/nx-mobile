package com.nxredux.mobile.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ToolAppsGridTest {
    // As many tiles of at least the minimum width as fit between the gutters, never fewer than one.
    @Test fun columnsFitTheWidth() {
        assertEquals(4, toolAppsColumns(412f))    // a phone in portrait
        assertEquals(1, toolAppsColumns(60f))     // narrower than a tile: still one
        assertEquals(5, toolAppsColumns(520f))
        assertEquals(9, toolAppsColumns(870f))    // a phone in landscape, full screen
    }

    @Test fun tilesShareTheRowExactly() {
        val w = toolAppsTileWidth(412f, 4)
        assertEquals(412f, 4 * w + 3 * TOOL_APPS_GAP + 2 * TOOL_APPS_GUTTER, 0.001f)
    }

    // Rows 100 px tall, 10 apart; the viewport's whole rows end at 400; row 2 is laid out first, at -30 (partly above).
    @Test fun scrollShowsTheSelectedRowJustWhole() {
        assertEquals(0f, toolAppsScrollDelta(3, 2, -30f, 100f, 10f, 400f))             // row 3 at 80..180: whole
        assertEquals(-30f, toolAppsScrollDelta(2, 2, -30f, 100f, 10f, 400f))           // row 2 cut at the top: to 0
        assertEquals(0f, toolAppsScrollDelta(5, 2, -30f, 100f, 10f, 400f))             // row 5 at 300..400: just whole
        assertEquals(110f, toolAppsScrollDelta(6, 2, -30f, 100f, 10f, 400f))           // row 6 at 410..510, below: its bottom to 400
        assertEquals(-250f, toolAppsScrollDelta(0, 2, -30f, 100f, 10f, 400f))          // a row above, not laid out: its top to 0
    }
}
