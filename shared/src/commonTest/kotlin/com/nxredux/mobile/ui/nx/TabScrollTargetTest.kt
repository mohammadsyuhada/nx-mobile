package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TabScrollTargetTest {
    // A 300 px view over 500 px of tabs (max 200), a 40 px fade margin.
    @Test fun aTabToTheRightStopsTheMarginShortOfTheEdge() {
        assertEquals(150, tabScrollTarget(tx = 360, tw = 50, scroll = 0, view = 300, max = 200, margin = 40))   // 360 + 50 + 40 - 300
    }

    @Test fun aTabToTheLeftStopsTheMarginInFromTheEdge() {
        assertEquals(60, tabScrollTarget(tx = 100, tw = 50, scroll = 150, view = 300, max = 200, margin = 40))
    }

    @Test fun theEndsClampSoTheFirstAndLastTabsSitAtTheEdge() {
        assertEquals(200, tabScrollTarget(tx = 450, tw = 50, scroll = 0, view = 300, max = 200, margin = 40))
        assertEquals(0, tabScrollTarget(tx = 0, tw = 50, scroll = 150, view = 300, max = 200, margin = 40))
    }

    @Test fun aTabClearOfBothFadesStays() {
        assertNull(tabScrollTarget(tx = 100, tw = 50, scroll = 0, view = 300, max = 200, margin = 40))
        assertNull(tabScrollTarget(tx = 0, tw = 50, scroll = 0, view = 300, max = 200, margin = 40))   // already at the start: the clamp leaves it
    }
}
