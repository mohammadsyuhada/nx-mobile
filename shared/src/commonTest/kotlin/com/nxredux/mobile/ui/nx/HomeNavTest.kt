package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeNavTest {
    private val land = homeShape(homeBoard(HomeFrame(960f, 344f, false, 960f / 432f), 1, tools = 5, games = 6))
    private val m = HomeMemory()

    // Landscape, 5 tools, 6 games: Continue 0, the 3 × 2 block 1 2 3 / 4 5, then the rows 6 7 8 / 9 10 11.
    @Test fun rightEntersTheBlockAtItsTopLeft() {
        assertEquals(1, homeStep(0, land, m, NxButton.RIGHT))   // a tie (1 and 4): the top one
        assertEquals(2, homeStep(1, land, m, NxButton.RIGHT))
        assertEquals(3, homeStep(2, land, m, NxButton.RIGHT))
        assertNull(homeStep(3, land, m, NxButton.RIGHT))        // the right edge: switch tab
        assertNull(homeStep(0, land, m, NxButton.LEFT))
    }
    @Test fun leftFromTheLeftColumnIsContinue() {
        assertEquals(0, homeStep(1, land, m, NxButton.LEFT))
        assertEquals(0, homeStep(4, land, m, NxButton.LEFT))
        assertEquals(1, homeStep(2, land, m, NxButton.LEFT))
        assertEquals(4, homeStep(5, land, m, NxButton.LEFT))
    }
    @Test fun comesBackToTheSquareItLeft() {
        val left = homeRemember(m, from = 4, to = 0, shape = land)   // LEFT from the bottom-left square to Continue
        assertEquals(4, homeStep(0, land, left, NxButton.RIGHT))
    }
    @Test fun squaresMoveInTheirBlock() {
        assertEquals(4, homeStep(1, land, m, NxButton.DOWN))
        assertEquals(5, homeStep(2, land, m, NxButton.DOWN))
        assertEquals(1, homeStep(4, land, m, NxButton.UP))
        assertEquals(8, homeStep(3, land, m, NxButton.DOWN))    // nothing under the last top square: the pin under it
    }
    @Test fun downLeavesForTheFirstPinUnderTheTile() {
        assertEquals(land.topCount + 0, homeStep(0, land, m, NxButton.DOWN))   // the wide Continue starts over the first pin
        assertEquals(8, homeStep(4, land, m, NxButton.DOWN))   // the squares sit over the last pin of the row
        assertEquals(8, homeStep(5, land, m, NxButton.DOWN))
    }
    @Test fun downTakesTheLeftPinAStraddlingTileOverlaps() {
        val s = HomeShape(listOf(HomeRect(80f, 0f, 60f, 60f)), listOf(HomeRect(0f, 70f, 100f, 50f), HomeRect(110f, 70f, 100f, 50f)))
        assertEquals(1, homeStep(0, s, m, NxButton.DOWN))   // over both pins (more of the right one): the left one
    }
    @Test fun downFromATileOverNoPinTakesTheNearestByCentre() {
        // Pins 0…100 and 110…210; the tile 300…340 overlaps neither, its centre (320) nearest pin 1's (160).
        val s = HomeShape(listOf(HomeRect(300f, 0f, 40f, 60f)), listOf(HomeRect(0f, 70f, 100f, 50f), HomeRect(110f, 70f, 100f, 50f)))
        assertEquals(2, homeStep(0, s, m, NxButton.DOWN))
    }
    @Test fun upFromTheTopIsTheTabRow() {
        assertTrue(homeAtTop(0, land, m)); assertTrue(homeAtTop(1, land, m)); assertTrue(homeAtTop(3, land, m))
    }
    @Test fun pinsMoveInRows() {
        assertEquals(7, homeStep(6, land, m, NxButton.RIGHT))
        assertNull(homeStep(8, land, m, NxButton.RIGHT))
        assertEquals(9, homeStep(6, land, m, NxButton.DOWN))
        assertEquals(10, homeStep(10, land, m, NxButton.DOWN))   // the last row: stays
        assertEquals(1, homeStep(6, land, HomeMemory(lastTop = 1), NxButton.UP))   // to the top tile last on
    }
    @Test fun shorterLastRowTakesItsLast() {
        val s = homeShape(homeBoard(HomeFrame(475f, 291f, true), 2, tools = 0, games = 3))   // pins 1, 2 / 3
        assertEquals(3, homeStep(2, s, m, NxButton.DOWN))
    }
    // The Fold cover with 7 tools: Continue 0, the right column 1–5 (top to bottom), the left column 6 7,
    // then pins 8 9 / 10 11. RIGHT from Continue is the left column's top; the columns move by position; DOWN from the
    // right column's bottom and from the left column's last goes to the pin under it.
    @Test fun portraitColumnsMoveByPosition() {
        val s = homeShape(homeBoard(HomeFrame(475f, 291f, true), 2, tools = 7, games = 4))
        assertEquals(8, s.topCount)
        assertEquals(6, homeStep(0, s, m, NxButton.RIGHT))
        assertEquals(1, homeStep(6, s, m, NxButton.RIGHT)); assertEquals(6, homeStep(1, s, m, NxButton.LEFT))
        assertEquals(2, homeStep(1, s, m, NxButton.DOWN)); assertEquals(7, homeStep(2, s, m, NxButton.LEFT))
        assertEquals(7, homeStep(6, s, m, NxButton.DOWN)); assertEquals(0, homeStep(7, s, m, NxButton.LEFT))
        assertEquals(0, homeStep(3, s, m, NxButton.LEFT))   // the right column's row 3: no left square there, Continue
        assertEquals(9, homeStep(5, s, m, NxButton.DOWN)); assertEquals(9, homeStep(7, s, m, NxButton.DOWN))
        assertTrue(homeAtTop(1, s, m)); assertTrue(homeAtTop(6, s, m))
    }
    @Test fun clampKeepsTheSection() {
        val fewer = homeShape(homeBoard(HomeFrame(960f, 344f, false, 960f / 432f), 1, tools = 4, games = 6))   // 2 × 2: squares 1…4
        assertEquals(4, homeClamp(5, land, fewer))
        val noTools = homeShape(homeBoard(HomeFrame(960f, 344f, false, 960f / 432f), 1, tools = 0, games = 6))
        assertEquals(0, homeClamp(5, land, noTools))       // past the top section: its last tile
        assertEquals(noTools.topCount, homeClamp(6, land, noTools))   // the first pin stays the first pin
    }
}
