package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The wide face (the Flip, 20:9 phones in landscape): 58 dp squares in a top-aligned 3 × 2 block, 0–2 games inline in
 * one double-wide column (stacked when 2).
 */
class HomeWideBoardTest {
    private fun near(e: Float, a: Float, what: String) = assertEquals(e, a, 0.05f, what)
    private val flip = HomeFrame(804f, 261f, false, 804f / 330f)
    private fun tiles(b: HomeBoard, k: HomeTopKind) = b.top.filter { it.kind == k }.map { it.rect }

    // w 756, topY 2 + 18 + 8 = 28, topH 261 − 28 − 8 = 225; square min(58, (225 − 7) / 2) = 58; block 3 · 58 + 2 · 14 = 202
    // wide, 2 · 58 + 7 = 123 tall, from 756 − 202 = 554 (page 578), top-aligned. The games are one inline column
    // 1.16 · 225 = 261 wide, stacked (225 − 14) / 2 = 105.5 tall; Continue 554 − 14 − 275 = 265.
    @Test fun twoGamesStackInOneDoubleWideColumn() {
        val b = homeBoard(flip, 1, tools = 6, games = 2)
        assertEquals(HomeToolMode.WIDE, b.mode); assertTrue(b.wide)
        assertEquals(2, b.inlineGames); assertEquals(0, b.rowGames)
        near(28f, b.topY, "topY"); near(225f, b.topH, "topH"); near(58f, b.square, "square")
        assertEquals(listOf(HomeTopKind.CONTINUE, HomeTopKind.GAME, HomeTopKind.GAME) + List(6) { HomeTopKind.TOOL }, b.top.map { it.kind })
        val c = tiles(b, HomeTopKind.CONTINUE).single()
        near(24f, c.x, "continue x"); near(28f, c.y, "continue y"); near(265f, c.w, "continue w"); near(225f, c.h, "continue h")
        val g = tiles(b, HomeTopKind.GAME)
        near(28f, g[0].y, "game 0 y"); near(147.5f, g[1].y, "game 1 y")
        for (r in g) { near(303f, r.x, "game x"); near(261f, r.w, "game w"); near(105.5f, r.h, "game h") }
        val sq = tiles(b, HomeTopKind.TOOL)
        for ((i, r) in sq.withIndex()) {
            near(578f + (i % 3) * 72f, r.x, "square $i x"); near(if (i < 3) 28f else 93f, r.y, "square $i y")
            near(58f, r.w, "square w"); near(58f, r.h, "square h")
        }
        near(261f, b.pageH, "pageH")
    }
    // 1 game, 6 tools: one card 261 wide, the top's full height, at 24 + 265 + 14.
    @Test fun oneGameIsOneDoubleWideCard() {
        val b = homeBoard(flip, 1, tools = 6, games = 1)
        assertEquals(1, b.inlineGames)
        near(265f, tiles(b, HomeTopKind.CONTINUE).single().w, "continue w")
        val g = tiles(b, HomeTopKind.GAME).single()
        near(303f, g.x, "game x"); near(28f, g.y, "game y"); near(261f, g.w, "game w"); near(225f, g.h, "game h")
    }
    // 4 tools: 2 columns (2 · 58 + 14 = 130), from 756 − 130 = 626. Continue 626 − 14 − 275 = 337, the game at 24 + 351.
    @Test fun oneGameFourTools() {
        val b = homeBoard(flip, 1, tools = 4, games = 1)
        assertEquals(1, b.inlineGames)
        val sq = tiles(b, HomeTopKind.TOOL)
        near(130f, sq.maxOf { it.x + it.w } - sq.minOf { it.x }, "block w")
        near(337f, tiles(b, HomeTopKind.CONTINUE).single().w, "continue w")
        near(375f, tiles(b, HomeTopKind.GAME).single().x, "game x")
    }
    // No tools: the column sits flush right, Continue 756 − 14 − 261 = 481.
    @Test fun oneGameNoTools() {
        val b = homeBoard(flip, 1, tools = 0, games = 1)
        near(481f, tiles(b, HomeTopKind.CONTINUE).single().w, "continue w")
        val g = tiles(b, HomeTopKind.GAME).single()
        near(24f + 756f, g.x + g.w, "flush right")
    }
    // A window too narrow for the column: it never crosses a gap before the squares. w 252, topH 160 − 36 = 124, square 58, block 202 from 50: the column 50 − 28 = 22 wide (not 143.84), Continue 0.
    @Test fun aTinyWindowCapsTheColumn() {
        val b = homeBoard(HomeFrame(300f, 160f, false, 4f), 1, tools = 6, games = 2)
        assertEquals(HomeToolMode.WIDE, b.mode)
        near(0f, tiles(b, HomeTopKind.CONTINUE).single().w, "continue w")
        for (g in tiles(b, HomeTopKind.GAME)) { near(22f, g.w, "game w"); near(24f + 50f - 14f, g.x + g.w, "game right") }
    }
    @Test fun nothingPinnedContinueFills() {
        val b = homeBoard(flip, 1, tools = 0, games = 0)
        val c = tiles(b, HomeTopKind.CONTINUE).single()
        near(756f, c.w, "continue w"); near(225f, c.h, "continue h"); assertEquals(1, b.top.size)
    }
    @Test fun moreThanSixToolsShowFiveAndMore() {
        val b = homeBoard(flip, 1, tools = 9, games = 2)
        assertEquals(6, b.slots); assertEquals(4, b.more)
        assertEquals(5, tiles(b, HomeTopKind.TOOL).size); assertEquals(HomeTopKind.MORE, b.top.last().kind)
    }
    // 3 games: the rows (6 columns of 114.33, shape min(114.33, max(85.75, (261 − 33) · 0.4 = 91.2)) = 91.2, 3 a
    // row); topH = fill − shape = (261 − 28 − 14 − 3 − 8) − 91.2 = 116.8; square (116.8 − 7) / 2 = 54.9.
    @Test fun threeGamesStayInTheRows() {
        val b = homeBoard(flip, 1, tools = 6, games = 3)
        assertEquals(HomeToolMode.WIDE, b.mode); assertEquals(0, b.inlineGames); assertEquals(3, b.rowGames)
        near(116.8f, b.topH, "topH"); near(54.9f, b.square, "square")
        assertEquals(3, b.perRow); near(242.67f, b.pinW, "pinW"); near(91.2f, b.pinH, "pinH")
        near(28f + 116.8f + 14f, b.tilesY, "tilesY")
        assertEquals(0, tiles(b, HomeTopKind.GAME).size)
        // The block, centred in the top section: (116.8 − (2 · 54.9 + 7)) / 2 = 0.
        val sq = tiles(b, HomeTopKind.TOOL)
        near(28f, sq.first().y, "block top"); near(24f + 756f, sq.last().x + sq.last().w, "flush right")
        near(756f - (3 * 54.9f + 28f) - 14f, tiles(b, HomeTopKind.CONTINUE).single().w, "continue w")
    }
    // A 20:9 phone: topH 344 − 28 − 8 = 308. The column 1.16 · 308 = 357.28 wide, the games (308 − 14) / 2 = 147 tall;
    // the block top-aligned.
    @Test fun twentyByNinePhone() {
        val b = homeBoard(HomeFrame(960f, 344f, false, 960f / 432f), 1, tools = 6, games = 2)
        near(308f, b.topH, "topH")
        for (r in tiles(b, HomeTopKind.GAME)) { near(357.28f, r.w, "game w"); near(147f, r.h, "game h") }
        near(28f, tiles(b, HomeTopKind.TOOL).first().y, "block top")
    }
    // The cover and SPS3 faces never put a game in the top section (their numbers are HomeBoardTest's; the large face's
    // inline games are HomeLargeBoardTest's).
    @Test fun otherFacesAreUntouched() {
        val faces = listOf(
            HomeFrame(475f, 291f, true) to HomeToolMode.COLUMN, HomeFrame(800f, 340f, false, 800f / 428f) to HomeToolMode.SPS3,
        )
        for ((f, mode) in faces) for (games in 0..3) {
            val b = homeBoard(f, if (f.portrait) 2 else 1, tools = 4, games = games)
            assertEquals(mode, b.mode); assertEquals(0, b.inlineGames); assertEquals(games, b.rowGames)
            assertTrue(b.top.none { it.kind == HomeTopKind.GAME })
        }
        // The cover with 2 games: its pin row.
        val cover = homeBoard(HomeFrame(475f, 291f, true), 2, tools = 3, games = 2)
        near(215.5f, cover.pinW, "cover pinW"); assertEquals(2, cover.rowGames)
    }
    // Focus by the stacked cards. Continue 0, G1 1 (top), G2 2, squares 3 4 5 /
    // 6 7 8. RIGHT Continue → G1; DOWN G1 → G2, UP back; RIGHT G1 → the top square, LEFT from it back to G1; RIGHT G2 →
    // the second square row (its rows 93–151 overlap G2's 147.5–253); LEFT G2 → Continue; DOWN from Continue and G2 stays.
    @Test fun focusMovesThroughTheStackedGames() {
        val s = homeShape(homeBoard(flip, 1, tools = 6, games = 2))
        val m = HomeMemory()
        assertEquals(1, homeStep(0, s, m, NxButton.RIGHT))
        assertEquals(2, homeStep(1, s, m, NxButton.DOWN)); assertEquals(1, homeStep(2, s, m, NxButton.UP))
        assertEquals(3, homeStep(1, s, m, NxButton.RIGHT)); assertEquals(1, homeStep(3, s, m, NxButton.LEFT))
        assertEquals(6, homeStep(2, s, m, NxButton.RIGHT))
        assertEquals(0, homeStep(2, s, m, NxButton.LEFT)); assertEquals(0, homeStep(1, s, m, NxButton.LEFT))
        assertEquals(0, homeStep(0, s, m, NxButton.DOWN)); assertEquals(2, homeStep(2, s, m, NxButton.DOWN))
        assertTrue(homeAtTop(0, s, m)); assertTrue(homeAtTop(1, s, m))
    }
}
