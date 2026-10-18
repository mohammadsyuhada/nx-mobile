package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeLargeBoardTest {
    private fun near(e: Float, a: Float, what: String) = assertEquals(e, a, 0.05f, what)
    private val fold = HomeFrame(933f, 572f, false, 933f / 660f)
    private fun tiles(b: HomeBoard, k: HomeTopKind) = b.top.filter { it.kind == k }
    // Shelf sizes as offered; the board draws at most 3 a shelf.
    private val three = listOf(HomeShelf.NEW to 6, HomeShelf.NOT_STARTED to 3, HomeShelf.PICK_UP to 4)

    @Test fun whoIsLarge() {
        assertTrue(homeIsLarge(fold))
        assertFalse(homeIsLarge(HomeFrame(704f, 801f, true)))                      // unfolded portrait: COLUMN
        assertFalse(homeIsLarge(HomeFrame(960f, 344f, false, 960f / 432f)))        // 20:9 phone landscape: WIDE
        assertFalse(homeIsLarge(HomeFrame(800f, 340f, false, 800f / 428f)))        // 16:9 phone landscape: under 840
        assertFalse(homeIsLarge(HomeFrame(411f, 700f, true)))                       // phone portrait
        assertFalse(homeIsLarge(HomeFrame(900f, 1000f, false)))                     // a tall window: halves
        assertFalse(homeIsLarge(HomeFrame(846f, 324f, false, 846f / 412f)))        // wide but short (a 21:9 landscape): under 480 tall
    }
    // w 885 (933 − 2 · 24); a full square (885 / 2 − 14 − 28) / 3 = 133.5 sets the top, 2 · 133.5 + 7 = 274 from topY 28.
    // The squares are half that, 66.75, 3 a column (rows 7 apart), top-aligned, flush right: 6 tools are 2 columns
    // (2 · 66.75 + 14 = 147.5) from 737.5; Continue 737.5 − 14 = 723.5.
    @Test fun topSectionIsContinueAndHalfSquaresThreeAColumn() {
        val b = homeBoard(fold, 1, tools = 6, games = 5, shelves = three)
        assertEquals(HomeToolMode.LARGE, b.mode); assertEquals(6, b.slots); assertEquals(0, b.more)
        near(28f, b.topY, "topY"); near(274f, b.topH, "topH"); near(66.75f, b.square, "square")
        val c = tiles(b, HomeTopKind.CONTINUE).single().rect
        near(24f, c.x, "continue x"); near(723.5f, c.w, "continue w"); near(274f, c.h, "continue h")
        val sq = tiles(b, HomeTopKind.TOOL).map { it.rect }
        assertEquals(6, sq.size)
        for ((i, r) in sq.withIndex()) {
            near(24f + 737.5f + (i % 2) * 80.75f, r.x, "square $i x"); near(28f + (i / 2) * 73.75f, r.y, "square $i y")
            near(66.75f, r.w, "square w"); near(66.75f, r.h, "square h")
        }
    }
    // 4 tools are 2 columns as 6 are; 3 one (Continue 885 − 66.75 − 14 = 804.25).
    @Test fun fewerToolsNarrowTheBlockAndWidenContinue() {
        val b = homeBoard(fold, 1, tools = 4, games = 0)
        near(723.5f, tiles(b, HomeTopKind.CONTINUE).single().rect.w, "continue w (2 columns)")
        near(804.25f, tiles(homeBoard(fold, 1, tools = 3, games = 0), HomeTopKind.CONTINUE).single().rect.w, "continue w (1 column)")
        val none = homeBoard(fold, 1, tools = 0, games = 0)
        near(885f, tiles(none, HomeTopKind.CONTINUE).single().rect.w, "continue w (no tools)")
        near(274f, none.topH, "topH holds")
    }
    @Test fun moreThanSixToolsShowFiveAndMore() {
        val b = homeBoard(fold, 1, tools = 9, games = 0)
        assertEquals(5, tiles(b, HomeTopKind.TOOL).size); assertEquals(1, tiles(b, HomeTopKind.MORE).size); assertEquals(4, b.more)
        near(28f + 2 * 73.75f, tiles(b, HomeTopKind.MORE).single().rect.y, "+N: the last square, third row")
    }
    // 1 pinned game sits inline, 1.16 · 274 = 317.84 wide, the top's full
    // height, at 24 + 737.5 − 14 − 317.84 = 429.66; Continue 391.66. No Pinned band: New's label takes its place at 316.
    @Test fun oneGameSitsInline() {
        val b = homeBoard(fold, 1, tools = 6, games = 1, shelves = listOf(HomeShelf.NEW to 6))
        assertEquals(1, b.inlineGames); assertEquals(0, b.rowGames)
        assertEquals(listOf(HomeTopKind.CONTINUE, HomeTopKind.GAME) + List(6) { HomeTopKind.TOOL }, b.top.map { it.kind })
        near(391.66f, tiles(b, HomeTopKind.CONTINUE).single().rect.w, "continue w")
        val g = tiles(b, HomeTopKind.GAME).single().rect
        near(429.66f, g.x, "game x"); near(28f, g.y, "game y"); near(317.84f, g.w, "game w"); near(274f, g.h, "game h")
        assertEquals(listOf(HomeLabel("New", 316f)), b.labels)
        near(340f, homeBoardTile(b, 0).y, "New's tiles")
    }
    // 2 games: that column, stacked, each (274 − 14) / 2 = 130 tall, the second at 28 + 144.
    @Test fun twoGamesStackInline() {
        val b = homeBoard(fold, 1, tools = 6, games = 2)
        assertEquals(2, b.inlineGames); assertEquals(0, b.rowGames); assertEquals(emptyList(), b.labels)
        val g = tiles(b, HomeTopKind.GAME).map { it.rect }
        near(28f, g[0].y, "game 0 y"); near(172f, g[1].y, "game 1 y")
        for (r in g) { near(429.66f, r.x, "game x"); near(317.84f, r.w, "game w"); near(130f, r.h, "game h") }
        near(28f + 274f + 8f, b.pageH, "pageH: nothing below the top")
    }
    // No tools: the column flush right (24 + 885 − 317.84 = 591.16), Continue 885 − 14 − 317.84 = 553.16.
    @Test fun oneGameNoTools() {
        val b = homeBoard(fold, 1, tools = 0, games = 1)
        near(553.16f, tiles(b, HomeTopKind.CONTINUE).single().rect.w, "continue w")
        near(591.16f, tiles(b, HomeTopKind.GAME).single().rect.x, "game x")
    }
    // 3 games or more: all in the pins' band, none inline, with no Pinned label.
    @Test fun threeGamesStayInThePinnedBand() {
        val b = homeBoard(fold, 1, tools = 6, games = 3)
        assertEquals(0, b.inlineGames); assertEquals(3, b.rowGames)
        assertEquals(0, tiles(b, HomeTopKind.GAME).size)
        assertEquals(emptyList(), b.labels)
        near(316f, b.tilesY, "tilesY")
    }
    // The pins unlabelled, a gap under the top at 28 + 274 + 14 = 316: 4 a row, 210.75 × 118.55.
    @Test fun pinsAreFourARowUnderTheTop() {
        val b = homeBoard(fold, 1, tools = 6, games = 5, shelves = three)
        assertEquals(4, b.perRow); near(210.75f, b.pinW, "pinW"); near(118.55f, b.pinH, "pinH")
        assertEquals("New", b.labels.first().text)
        near(316f, b.tilesY, "tilesY")
        val fifth = homeBoardTile(b, 4)
        near(24f, fifth.x, "second row x"); near(316f + 118.55f + 14f, fifth.y, "second row y")
    }
    // After two pin rows (ending 316 + 2 · 118.55 + 14 = 567.1): New's label at 581.1, its tiles at 605.1. Every shelf
    // holds at most 3 (New's 6 and Pick up's 4 draw 3 each), in the pins' grid: 4 a row at the pin size (210.75 × 118.55).
    @Test fun shelvesFollowInOrderEachOneRow() {
        val b = homeBoard(fold, 1, tools = 6, games = 5, shelves = three)
        assertEquals(listOf("New", "Not started", "Pick up again"), b.labels.map { it.text })
        near(581.1f, b.labels[0].y, "New label")
        assertEquals(listOf(3, 3, 3), b.shelfSizes); assertEquals(9, b.shelfTiles.size)
        val n0 = homeBoardTile(b, 5)
        near(24f, n0.x, "New 0 x"); near(605.1f, n0.y, "New 0 y"); near(210.75f, n0.w, "w"); near(118.55f, n0.h, "h")
        near(24f + 2 * (210.75f + 14f), homeBoardTile(b, 7).x, "New 2 x")
        near(605.1f + 118.55f + 14f + 24f, homeBoardTile(b, 8).y, "Not started 0 y")
        for (k in 0..2) {
            val t = homeBoardTile(b, 8 + k)
            near(24f + k * (210.75f + 14f), t.x, "Not started $k x"); near(210.75f, t.w, "Not started w"); near(118.55f, t.h, "Not started h")
        }
        near(210.75f, homeBoardTile(b, 11).w, "Pick up again: the same width")
        val last = homeBoardTile(b, 13)
        near(last.y + last.h + 3f + 8f, b.pageH, "pageH")
    }
    // More than 3 offered to a shelf (Not started, New) draw 3.
    @Test fun notStartedHoldsThree() {
        val b = homeBoard(fold, 1, tools = 6, games = 0, shelves = listOf(HomeShelf.NOT_STARTED to 6, HomeShelf.NEW to 6))
        assertEquals(listOf(3, 3), b.shelfSizes)
    }
    @Test fun noPinsNoPinnedLabel() {
        val b = homeBoard(fold, 1, tools = 6, games = 0, shelves = listOf(HomeShelf.NOT_STARTED to 6))
        assertEquals(listOf(HomeLabel("Not started", 316f)), b.labels)
        near(340f, homeBoardTile(b, 0).y, "first shelf tile y")
    }
    @Test fun nothingBelowTheTop() {
        val b = homeBoard(fold, 1, tools = 2, games = 0)
        assertEquals(emptyList(), b.labels); near(28f + 274f + 8f, b.pageH, "pageH")
    }
    // The wide face shows the shelves too, in the pins' grid: the pins' columns and tile size (the pins unlabelled).
    @Test fun theWideFaceShowsShelves() {
        val f = HomeFrame(960f, 344f, false, 960f / 432f)
        val b = homeBoard(f, 1, 5, 6, shelves = three)
        assertEquals(HomeToolMode.WIDE, b.mode)
        assertEquals(listOf("New", "Not started", "Pick up again"), b.labels.map { it.text })
        assertEquals(listOf(3, 3, 3), b.shelfSizes); assertEquals(List(3) { b.perRow }, b.shelfCols)
        for (r in b.shelfTiles) { near(b.pinW, r.w, "shelf tile w"); near(b.pinH, r.h, "shelf tile h") }
        assertEquals(homeBoard(f, 1, 5, 6), homeBoard(f, 1, 5, 6, shelves = listOf(HomeShelf.NEW to 0)).copy(shelfCols = emptyList()))
    }
    // The scroll keeps a band's label in view: a tile in a band's first row is handed up 24 dp taller (the 18 dp label and
    // its 6 dp gap); the pins, unlabelled, and phone pins are handed up as they are.
    @Test fun firstRowTilesScrollWithTheirLabel() {
        val b = homeBoard(fold, 1, tools = 6, games = 5, shelves = three)
        val p0 = homeBoardTile(b, 0)
        assertEquals(p0, homeScrollRect(b, 0))                                                   // the pins: no label
        assertEquals(homeBoardTile(b, 4), homeScrollRect(b, 4))
        val n0 = homeBoardTile(b, 5)
        assertEquals(HomeRect(n0.x, n0.y - 24f, n0.w, n0.h + 24f), homeScrollRect(b, 5))       // New: one row
        val phone = homeBoard(HomeFrame(960f, 344f, false, 960f / 432f), 1, 4, 6)
        assertEquals(homeBoardPin(phone, 0), homeScrollRect(phone, 0))
    }
    // The band labels' line grows with the text scale as the strip's does (18 · 1.5 = 27 dp), never under 18:
    // at 1.5 each shelf's tiles move down 9 more than the top does (the strip's own lines), and the scroll reaches 27 + 6 up.
    @Test fun labelLinesFollowTheTextScale() {
        val one = homeBoard(fold, 1, tools = 6, games = 5, shelves = three)
        val small = homeBoard(fold.copy(textScale = 0.85f), 1, tools = 6, games = 5, shelves = three)   // a smaller system font: as at 1
        assertEquals(one, small.copy(stripY = one.stripY))                                       // (but the strip's smaller text)
        near(18f, one.labelH, "label line at 1")
        val big = homeBoard(fold.copy(textScale = 1.5f), 1, tools = 6, games = 5, shelves = three)
        near(27f, big.labelH, "label line at 1.5")
        val top = big.topY - one.topY                                                            // the strip's taller lines
        near(one.tilesY + top, big.tilesY, "pin tiles")
        near(one.labels[0].y + top, big.labels[0].y, "New label")
        near(homeBoardTile(one, 5).y + top + 9f, homeBoardTile(big, 5).y, "New tiles")
        val n0 = homeBoardTile(big, 5)
        assertEquals(HomeRect(n0.x, n0.y - 33f, n0.w, n0.h + 33f), homeScrollRect(big, 5))
    }
}
