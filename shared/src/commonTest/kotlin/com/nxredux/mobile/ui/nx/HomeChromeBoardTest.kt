package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Home with Layouts → Page title hidden. The tab row's 44 dp goes to the body's top: the strip moves up into it and
 * the top section gains it, every tool column one more square. A hidden hint bar keeps its room (it never reaches the
 * frame), so only the tab row is tested here.
 */
class HomeChromeBoardTest {
    private fun near(e: Float, a: Float, what: String) = assertEquals(e, a, 0.05f, what)
    private fun tiles(b: HomeBoard, k: HomeTopKind) = b.top.filter { it.kind == k }.map { it.rect }
    private fun continueOf(b: HomeBoard) = tiles(b, HomeTopKind.CONTINUE).single()

    /** [base] with the tab row hidden (its 44 dp in the body) unless [title]. */
    private fun chrome(base: HomeFrame, title: Boolean) = base.copy(h = base.h + (if (title) 0f else 44f), titleHidden = !title)

    // The Fold cover (475 × 291 between the bars), the portrait strip 14 / 18: topY 2 + 2 · 18 + 2 + 8 = 48, the top
    // section capped at 235, 5 squares a column of (235 − 24) / 5 = 42.2. Title hidden: the body 335, the top 279 (the
    // cap + 44), 6 squares a column of (279 − 30) / 6 = 41.5; 12 slots past 6 tools.
    @Test fun foldCoverWithAndWithoutTheTabRow() {
        val cover = HomeFrame(475f, 291f, true)
        data class Case(val title: Boolean, val topH: Float, val side: Float, val rows: Int, val contH: Float)
        for (c in listOf(Case(true, 235f, 42.2f, 5, 235f), Case(false, 279f, 249f / 6, 6, 279f))) {
            val f = chrome(cover, c.title)
            for (tools in 1..14) {
                val b = homeBoard(f, 2, tools = tools, games = 4)
                val what = "title ${c.title} tools $tools"
                assertEquals(HomeToolMode.COLUMN, b.mode, what); near(48f, b.topY, "$what: topY")
                near(c.topH, b.topH, "$what: topH"); near(c.side, b.square, "$what: square")
                assertEquals(if (tools <= c.rows) c.rows else 2 * c.rows, b.slots, "$what: slots")
                assertEquals(if (tools > 2 * c.rows) tools - 2 * c.rows + 1 else 0, b.more, "$what: more")
                val sq = b.top.filter { it.kind != HomeTopKind.CONTINUE }
                for ((i, t) in sq.withIndex()) {
                    near(16f + 443f - c.side - (i / c.rows) * (c.side + 12f), t.rect.x, "$what: square $i x")
                    near(48f + (i % c.rows) * (c.side + 6f), t.rect.y, "$what: square $i y, top-aligned")
                }
                near(c.contH, continueOf(b).h, "$what: continue h"); near(48f + c.contH + 12f, b.tilesY, "$what: the rows under Continue")
                assertTrue(48f + c.contH + 8f <= f.h + 0.05f, "$what: Continue ends a margin above the body's bottom")
            }
        }
    }

    // The strip with no tab row: centred between the body's top (the screen's) and the top section, as it was between
    // the underline and the top section. The room below ends at Continue's ring (3 dp above topY) and the two lines are
    // homeStripPitch apart: (48 − 3 − pitch − inkBottom − capTop) / 2 = 3.13 dp on the cover (the portrait strip
    // 14 / 18), one landscape line (28 − 3 − inkBottom − capTop) / 2 = 1.88.
    @Test fun theStripCentresUnderTheScreenTopWithNoTabRow() {
        val cover = HomeFrame(475f, 291f, true)
        val hidden = homeBoard(chrome(cover, title = false), 2, tools = 3, games = 0)
        near(3.13f, hidden.stripY, "stripY")
        val land = HomeFrame(800f, 340f, false, 800f / 428f)
        near(1.88f, homeBoard(chrome(land, title = false), 1, tools = 3, games = 4).stripY, "landscape stripY")
    }

    // A taller portrait body (the Flip's inner display) stops its top section at the cap: 235, or 279 with the tab row hidden.
    @Test fun tallPortraitCapGrowsByTheTabRow() {
        val inner = HomeFrame(360f, 640f, true)
        val shown = homeBoard(inner, 2, tools = 7, games = 6)
        val hidden = homeBoard(chrome(inner, title = false), 2, tools = 7, games = 6)
        near(235f, shown.topH, "shown"); near(279f, hidden.topH, "hidden: the cap + 44")
        assertEquals(12, hidden.slots); near(249f / 6, hidden.square, "square")
    }

    // SPS3 (a 16:9 phone in landscape, 800 × 340): the top section takes the 44 dp: avail 173.33 → 217.33, 4 rows of
    // (217.33 − 3 · 14) / 4 = 43.83 (3 of 48.44 with the tab row), half gaps apart; 8 slots past 4 tools. The pin row keeps its shape
    // (113.67, from the bars-shown height) and gains the half gaps' saving.
    @Test fun sps3GainsARow() {
        val land = HomeFrame(800f, 340f, false, 800f / 428f)
        val shown = homeBoard(land, 1, tools = 7, games = 4)
        assertEquals(HomeToolMode.SPS3, shown.mode); assertEquals(6, shown.slots); near(48.44f, shown.square, "shown square")
        val b = homeBoard(chrome(land, title = false), 1, tools = 7, games = 4)
        assertEquals(HomeToolMode.SPS3, b.mode); assertEquals(8, b.slots); assertEquals(0, b.more)
        near(43.83f, b.square, "square"); near(4 * 43.83f + 3 * 7f, b.topH, "topH: the block")
        near(113.67f + (217.33f - (4 * 43.83f + 21f)), b.pinH, "pinH")
        val sq = tiles(b, HomeTopKind.TOOL)
        near(28f + 3 * (43.83f + 7f), sq[6].y, "the 7th square: the 4th row"); near(sq[0].x, sq[6].x, "its column")
        assertEquals(4, homeBoard(chrome(land, title = false), 1, tools = 4, games = 4).slots, "4 tools: one column")
    }

    // The Flip's wide face (804 × 261, 0–2 games: the top section the body): 225, 2 rows of 58 → 269, 3 rows of 58
    // (min(58, (269 − 14) / 3 = 85)); 9 slots.
    @Test fun flipWideWithAndWithoutTheTabRow() {
        val flip = HomeFrame(804f, 261f, false, 804f / 330f)
        data class Case(val title: Boolean, val topH: Float, val rows: Int, val contH: Float)
        for (c in listOf(Case(true, 225f, 2, 225f), Case(false, 269f, 3, 269f))) {
            val f = chrome(flip, c.title)
            val what = "title ${c.title}"
            val b = homeBoard(f, 1, tools = 12, games = 2)
            assertEquals(HomeToolMode.WIDE, b.mode, what); near(c.topH, b.topH, "$what: topH"); near(58f, b.square, "$what: square")
            assertEquals(3 * c.rows, b.slots, "$what: slots"); assertEquals(12 - 3 * c.rows + 1, b.more, "$what: more")
            near(c.contH, continueOf(b).h, "$what: continue h")
            val sq = b.top.filter { it.kind == HomeTopKind.TOOL || it.kind == HomeTopKind.MORE }.map { it.rect }
            assertEquals(3 * c.rows, sq.size, what)
            for ((i, r) in sq.withIndex()) near(28f + (i / 3) * (58f + 7f), r.y, "$what: square $i y")
            val g = tiles(b, HomeTopKind.GAME)
            near(28f, g[0].y, "$what: game 0 y"); near(28f + c.contH, g[1].y + g[1].h, "$what: the games end with Continue")
            near(g[0].h, g[1].h, "$what: the games alike")
        }
        // 3 or more games are row games: the top what the first row leaves (116.8 → 160.8), 3 rows of (160.8 − 14) / 3.
        val rowsShown = homeBoard(flip, 1, tools = 6, games = 4)
        val rowsHidden = homeBoard(chrome(flip, title = false), 1, tools = 6, games = 4)
        near(116.8f, rowsShown.topH, "row games, shown"); near(160.8f, rowsHidden.topH, "row games, hidden")
        near(146.8f / 3, rowsHidden.square, "row games square"); near(rowsShown.pinH, rowsHidden.pinH, "the pin row keeps its shape")
    }

    // The unfolded Fold (933 × 572, the large face): topH 2 · 133.5 + 7 = 274, squares 66.75, 3 a column → 318 and 4 a
    // column (4 · 66.75 + 3 · 7 = 288 fits); 8 slots.
    @Test fun foldUnfoldedWithAndWithoutTheTabRow() {
        val fold = HomeFrame(933f, 572f, false, 933f / 660f)
        data class Case(val title: Boolean, val topH: Float, val rows: Int, val contH: Float)
        val shown = homeBoard(fold, 1, tools = 9, games = 6, shelves = listOf(HomeShelf.NEW to 6))
        for (c in listOf(Case(true, 274f, 3, 274f), Case(false, 318f, 4, 318f))) {
            val f = chrome(fold, c.title)
            val what = "title ${c.title}"
            val b = homeBoard(f, 1, tools = 9, games = 6, shelves = listOf(HomeShelf.NEW to 6))
            assertEquals(HomeToolMode.LARGE, b.mode, what); near(c.topH, b.topH, "$what: topH"); near(66.75f, b.square, "$what: square")
            assertEquals(2 * c.rows, b.slots, "$what: slots"); assertEquals(9 - 2 * c.rows + 1, b.more, "$what: more")
            near(c.contH, continueOf(b).h, "$what: continue h")
            val sq = b.top.filter { it.kind == HomeTopKind.TOOL || it.kind == HomeTopKind.MORE }.map { it.rect }
            for ((i, r) in sq.withIndex()) near(28f + (i / 2) * (66.75f + 7f), r.y, "$what: square $i y")
            assertTrue(sq.all { it.y + it.h <= 28f + c.topH + 0.05f }, "$what: the squares inside the top")
            val drop = c.topH - 274f
            near(shown.tilesY + drop, b.tilesY, "$what: the bands follow"); near(shown.labels[0].y + drop, b.labels[0].y, "$what: the labels")
            near(shown.shelfTiles[0].y + drop, b.shelfTiles[0].y, "$what: the shelves"); near(shown.pageH + drop, b.pageH, "$what: pageH")
        }
    }

    // A tall narrow window without the pad (UNDER) keeps its 5 squares and takes the 44 dp: the cap 273.2 + 44, Continue 208 → 252.
    @Test fun underTakesTheHeightOnly() {
        val tall = HomeFrame(360f, 423f, false)
        val b = homeBoard(chrome(tall, title = false), 2, tools = 7, games = 4)
        assertEquals(HomeToolMode.UNDER, b.mode); assertEquals(5, b.slots); near(51.2f, b.square, "square")
        near(317.2f, b.topH, "topH"); near(252f, continueOf(b).h, "continue h")
    }

    // The face is read from the body as it would be with the tab row: its freed room never changes it.
    @Test fun theFreedRoomNeverChangesTheFace() {
        for (base in listOf(
            HomeFrame(960f, 344f, false), HomeFrame(933f, 572f, false), HomeFrame(800f, 340f, false), HomeFrame(400f, 440f, false),
            HomeFrame(846f, 450f, false, 846f / 538f), HomeFrame(475f, 291f, true),
        )) {
            val mode = homeBoard(base, 1, tools = 3, games = 2).mode
            for (t in listOf(true, false)) assertEquals(mode, homeBoard(chrome(base, t), 1, tools = 3, games = 2).mode, "$base title $t")
        }
    }
}
