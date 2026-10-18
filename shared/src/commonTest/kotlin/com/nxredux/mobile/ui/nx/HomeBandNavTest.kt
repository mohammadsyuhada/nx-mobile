package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HomeBandNavTest {
    private val fold = HomeFrame(933f, 572f, false, 933f / 660f)
    private val m = HomeMemory()
    // 6 tools: Continue 0, squares 1 2 / 3 4 / 5 6. Pins 7..11 (rows 7 8 9 10 / 11). New 12..14, Not started 15..17, Pick up 18..20.
    // Every shelf holds at most 3, so New's 6 and Pick up's 4 draw 3 each, in the pins' grid (4 a row, the pins' tile size).
    private val s = homeShape(homeBoard(fold, 1, tools = 6, games = 5,
        shelves = listOf(HomeShelf.NEW to 6, HomeShelf.NOT_STARTED to 3, HomeShelf.PICK_UP to 4)))

    @Test fun bandsAreBuilt() {
        assertEquals(listOf(HomeBand(0, 5, 4), HomeBand(5, 3, 4), HomeBand(8, 3, 4), HomeBand(11, 3, 4)), s.bands)
        assertEquals(7 + 14, s.count)
    }
    @Test fun alongARowAndItsEdges() {
        assertEquals(13, homeStep(12, s, m, NxButton.RIGHT))
        assertNull(homeStep(14, s, m, NxButton.RIGHT))          // New's last: switch tab
        assertNull(homeStep(12, s, m, NxButton.LEFT))
        assertNull(homeStep(20, s, m, NxButton.RIGHT))          // Pick up's last (3 of 6)
        assertEquals(17, homeStep(16, s, m, NxButton.RIGHT))
        assertNull(homeStep(17, s, m, NxButton.RIGHT))          // Not started's last (3, one row)
        assertNull(homeStep(15, s, m, NxButton.LEFT))
    }
    @Test fun downFromPinsLastRowGoesToNewNearestInX() {
        assertEquals(12, homeStep(11, s, m, NxButton.DOWN))     // pin 4 (x 24) → New 0
        assertEquals(11, homeStep(8, s, m, NxButton.DOWN))      // the pins' own rule: the shorter last row's last pin
    }
    // Every shelf is in the pins' columns, so DOWN and UP between shelves keep the column.
    @Test fun downAndUpBetweenShelvesKeepTheColumn() {
        assertEquals(15, homeStep(12, s, m, NxButton.DOWN))     // New 0 → Not started 0
        assertEquals(16, homeStep(13, s, m, NxButton.DOWN))     // New 1 → Not started 1
        assertEquals(17, homeStep(14, s, m, NxButton.DOWN))     // New 2 → Not started 2
        assertEquals(12, homeStep(15, s, m, NxButton.UP))       // Not started 0 → New 0
        assertEquals(13, homeStep(16, s, m, NxButton.UP))
        assertEquals(14, homeStep(17, s, m, NxButton.UP))       // Not started 2 → New 2
        assertEquals(18, homeStep(15, s, m, NxButton.DOWN))     // Not started 0 → Pick up 0
        assertEquals(19, homeStep(16, s, m, NxButton.DOWN))     // Not started 1 → Pick up 1
        assertEquals(20, homeStep(17, s, m, NxButton.DOWN))     // Not started 2 → Pick up 2
        assertEquals(15, homeStep(18, s, m, NxButton.UP))       // Pick up 0 → Not started 0
        assertEquals(16, homeStep(19, s, m, NxButton.UP))       // Pick up 1 → Not started 1
        assertEquals(17, homeStep(20, s, m, NxButton.UP))       // Pick up 2 → Not started 2
        assertEquals(20, homeStep(20, s, m, NxButton.DOWN))     // the last band stays
    }
    @Test fun upFromNewGoesToThePinsLastRowNearestInX() {
        assertEquals(11, homeStep(12, s, m, NxButton.UP))
        assertEquals(11, homeStep(14, s, m, NxButton.UP))       // only one pin on that row
    }
    @Test fun upFromThePinsFirstRowIsTheTop() {
        assertEquals(0, homeStep(7, s, m, NxButton.UP))
    }
    @Test fun noPinsDownGoesToTheFirstShelf() {
        val t = homeShape(homeBoard(fold, 1, tools = 0, games = 0, shelves = listOf(HomeShelf.NOT_STARTED to 3)))
        assertEquals(1, homeStep(0, t, m, NxButton.DOWN))
        assertEquals(0, homeStep(1, t, m, NxButton.UP))
    }
    @Test fun clampIntoFewerBands() {
        val phone = homeShape(homeBoard(HomeFrame(960f, 344f, false, 960f / 432f), 1, tools = 6, games = 5))
        assertEquals(phone.count - 1, homeClamp(20, s, phone))  // a shelf tile on the large face → the phone's last pin
    }

    // The Fold with 1 pinned game: Continue 0, the inline game 1 (429.66 … 747.5), squares 2 3 / 4 5 / 6 7; New 8..10
    // (3 a shelf, in the pins' grid: 4 a row, 210.75 wide). RIGHT
    // Continue → the game → the top square; LEFT back; DOWN from the game to the first New tile under it (New 1,
    // 248.75 … 459.5), UP back to it.
    @Test fun foldOneGameInline() {
        val t = homeShape(homeBoard(fold, 1, tools = 6, games = 1, shelves = listOf(HomeShelf.NEW to 6)))
        assertEquals(8, t.topCount); assertEquals(listOf(HomeBand(0, 3, 4)), t.bands)
        assertEquals(1, homeStep(0, t, m, NxButton.RIGHT)); assertEquals(2, homeStep(1, t, m, NxButton.RIGHT))
        assertEquals(1, homeStep(2, t, m, NxButton.LEFT)); assertEquals(1, homeStep(4, t, m, NxButton.LEFT))
        assertEquals(0, homeStep(1, t, m, NxButton.LEFT))
        assertEquals(9, homeStep(1, t, m, NxButton.DOWN)); assertEquals(8, homeStep(0, t, m, NxButton.DOWN))
        assertEquals(1, homeStep(9, t, HomeMemory(lastTop = 1), NxButton.UP))
        assertEquals(1, homeStep(1, t, m, NxButton.UP))         // the tab row
    }
}
