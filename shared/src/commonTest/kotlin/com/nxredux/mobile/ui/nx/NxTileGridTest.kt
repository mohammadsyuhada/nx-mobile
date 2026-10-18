package com.nxredux.mobile.ui.nx

import com.nxredux.mobile.ui.InfoKind
import com.nxredux.mobile.ui.InfoLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NxTileGridTest {
    private val g = GridGeometry(tileW = 100f, tileH = 120f, gap = 10f, gutter = 24f, columns = 3)

    @Test fun stillTilesRunRowMajor() {
        assertEquals(Pair(0f, 0f), gridTileOffset(0, GridMode.STILL, g))
        assertEquals(Pair(220f, 0f), gridTileOffset(2, GridMode.STILL, g))
        assertEquals(Pair(0f, 130f), gridTileOffset(3, GridMode.STILL, g))    // the second row starts in column 0
        assertEquals(Pair(110f, 130f), gridTileOffset(4, GridMode.STILL, g))
    }

    @Test fun slidingTilesRunColumnMajorOverTwoRows() {
        assertEquals(Pair(0f, 0f), gridTileOffset(0, GridMode.SLIDING, g))
        assertEquals(Pair(0f, 130f), gridTileOffset(1, GridMode.SLIDING, g))
        assertEquals(Pair(110f, 0f), gridTileOffset(2, GridMode.SLIDING, g))
        assertEquals(Pair(330f, 130f), gridTileOffset(7, GridMode.SLIDING, g))
    }

    @Test fun degenerateLayoutsNeitherDrawNorReport() {
        assertTrue(gridDrawable(900f, 300f, gridGeometry(900f, 300f, portrait = false)))
        assertFalse(gridDrawable(0f, 300f, gridGeometry(0f, 300f, portrait = false)))
        assertFalse(gridDrawable(900f, 0f, gridGeometry(900f, 0f, portrait = false)))
        assertFalse(gridDrawable(900f, 2 * GRID_TILE_MIN_DP - 1, gridGeometry(900f, 2 * GRID_TILE_MIN_DP - 1, portrait = false)))
        assertFalse(gridDrawable(40f, 300f, gridGeometry(40f, 300f, portrait = true)))   // portrait tiles narrower than 0
        assertFalse(gridDrawable(900f, 140f, gridGeometry(900f, 140f, portrait = false, rows = 3)))   // tiles under the minimum
        assertTrue(gridDrawable(900f, 150f, gridGeometry(900f, 150f, portrait = false, rows = 3)))
    }

    @Test fun infoTextJoinsTheSegmentsWithTheTrophyBeforeTheAchievements() {
        val text = infoAnnotatedString(listOf(InfoLine(InfoKind.TIME, "Today - 1m"), InfoLine(InfoKind.ACHIEVEMENTS, "3 of 20")))
        assertEquals("Today - 1m · [trophy]3 of 20", text.text)
        val trophy = text.getStringAnnotations(0, text.length).single()
        assertEquals("Today - 1m · ".length, trophy.start)                 // the trophy leads the achievement segment
        assertEquals(TROPHY, trophy.item)
        assertEquals("", infoAnnotatedString(emptyList()).text)
    }

    // pitch 110, 20 tiles = columns 0..9, a 500 dp viewport
    @Test fun visibleColumnsAtTheStartKeepOneExtraOnTheRight() {
        assertEquals(0..5, gridVisibleColumns(24f, 20, g, 500f))          // columns 0..4 in view, 5 the extra
    }

    @Test fun visibleColumnsAtTheEndKeepOneExtraOnTheLeft() {
        assertEquals(4..9, gridVisibleColumns(-614f, 20, g, 500f))        // columns 5..9 in view (5 half-cut), 4 the extra
    }

    @Test fun visibleColumnsInTheMiddleAndAtExactEdges() {
        assertEquals(0..7, gridVisibleColumns(-200f, 20, g, 500f))        // 1 (x -90, cut) .. 6 (x 460, cut) in view, plus 0 and 7
        assertEquals(0..4, gridVisibleColumns(0f, 20, g, 440f))           // column 4 starts exactly at the width: not in view, the extra
        assertEquals(IntRange.EMPTY, gridVisibleColumns(0f, 0, g, 500f))
        assertEquals(0..0, gridVisibleColumns(24f, 1, g, 500f))
    }
}
