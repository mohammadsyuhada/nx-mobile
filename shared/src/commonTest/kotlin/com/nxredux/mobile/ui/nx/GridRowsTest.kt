package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Grid's row count: 2 on phones, 3–4 on tablet-class screens (the mockup's large screens). */
class GridRowsTest {
    // The mockup's body: the screen less the header and the hint bar (44 + 44).
    private fun body(h: Float) = h - 88f
    private fun grid(w: Float, h: Float, portrait: Boolean) = gridFor(w, h, w, body(h), portrait)

    @Test fun tabletClassIsAShortestSideOf600OrMore() {
        assertFalse(gridTabletClass(960f, 432f))
        assertFalse(gridTabletClass(475f, 379f))
        assertTrue(gridTabletClass(933f, 704f))
        assertTrue(gridTabletClass(704f, 933f))
        assertTrue(gridTabletClass(600f, 600f))
        assertFalse(gridTabletClass(599.9f, 1200f))
    }

    @Test fun rowsAreTwoOnPhonesAndAsManyAsFitClampedToThreeOrFourOnTablets() {
        assertEquals(2, gridRowsFor(tablet = false, bodyDp = 344f))
        assertEquals(2, gridRowsFor(tablet = false, bodyDp = 2000f))   // a tall phone body is still 2 rows
        assertEquals(3, gridRowsFor(tablet = true, bodyDp = 704f - 88f))
        assertEquals(4, gridRowsFor(tablet = true, bodyDp = 933f - 88f))
        assertEquals(3, gridRowsFor(tablet = true, bodyDp = 480f))     // clamped up to 3: the tile shrinks to about 148
        assertEquals(2, gridRowsFor(tablet = true, bodyDp = 300f))     // 3 rows would shrink it below 0.9 x 154: 2
        assertEquals(3, gridRowsFor(tablet = true, bodyDp = 3 * 138.6f + 2 * 14f + 8f + 0.1f))   // just 0.9 x 154
        assertEquals(2, gridRowsFor(tablet = true, bodyDp = 3 * 138.6f + 2 * 14f + 8f - 0.1f))
        assertEquals(4, gridRowsFor(tablet = true, bodyDp = 2000f))    // clamped down to 4
        assertEquals(4, gridRowsFor(tablet = true, bodyDp = 4 * 154f + 3 * 14f + 8f))   // exactly four fit
        assertEquals(3, gridRowsFor(tablet = true, bodyDp = 4 * 154f + 3 * 14f + 7f))
    }

    @Test fun theMockupScreensGetTheirRowsAndColumns() {
        val land = grid(960f, 432f, portrait = false)
        assertEquals(2, land.rows); assertEquals(6, land.columns)
        val port = grid(475f, 379f, portrait = true)
        assertEquals(2, port.rows); assertEquals(3, port.columns)
        val large = grid(933f, 704f, portrait = false)
        assertEquals(3, large.rows); assertEquals(5, large.columns)
        val lport = grid(704f, 933f, portrait = true)    // portrait, but a tablet: the phone landscape tile
        assertEquals(4, lport.rows); assertEquals(4, lport.columns)
        assertFalse(port.tallPortrait); assertFalse(lport.tallPortrait); assertFalse(land.tallPortrait)
        val tall = grid(360f, 511f, portrait = true)     // the Flip's tall portrait: two columns of 3 rows
        assertEquals(3, tall.rows); assertEquals(2, tall.columns)
    }

    @Test fun phonesKeepTheirExactGeometry() {
        assertEquals(gridGeometry(960f, gridSizingHeight(344f), portrait = false), gridFor(960f, 432f, 960f, 344f, portrait = false))
        assertEquals(gridGeometry(475f, gridSizingHeight(291f), portrait = true), gridFor(475f, 379f, 475f, 291f, portrait = true))
        assertEquals(2, gridGeometry(960f, 344f, portrait = false).rows)
    }

    @Test fun tabletTilesAreThePhoneLandscapeTileBetweenLandscapeGutters() {
        for (g in listOf(grid(933f, 704f, portrait = false), grid(704f, 933f, portrait = true))) {
            assertEquals(140f, g.tileW, 0.01f); assertEquals(154f, g.tileH, 0.01f)
            assertEquals(14f, g.gap); assertEquals(24f, g.gutter)
        }
    }

    @Test fun aSplitTabletWindowWithThePadFallsBackToThePhoneTwoRows() {
        // 636 × 800: the pad takes the lower part, a body of about 351 (3 rows would be about 105 dp tiles).
        val g = gridFor(636f, 800f, 636f, 351f, portrait = true)
        assertEquals(2, g.rows)
        assertEquals(gridGeometry(636f, gridSizingHeight(351f), portrait = true), g)   // exactly the phone geometry
        assertEquals(128f, g.tileH, 0.01f)                                             // at full size
        // A small freeform window (600 × 620, body 532 without the pad): 2 full rows, not 3 shrunk ones.
        val free = gridFor(600f, 620f, 600f, 350f, portrait = false)
        assertEquals(2, free.rows); assertEquals(154f, free.tileH, 0.01f)
    }

    @Test fun aTabletBodyTooShortForThreeFullRowsShrinksTheTileKeepingItsShape() {
        val g = gridFor(704f, 933f, 704f, 480f, portrait = true)   // the pad takes the lower screen
        assertEquals(3, g.rows)
        assertEquals((480f - 8f - 2 * 14f) / 3, g.tileH, 0.01f)
        assertEquals(140f * g.tileH / 154f, g.tileW, 0.01f)
        assertTrue(3 * g.tileH + 2 * g.gap + 2 * GRID_RING_ROOM <= 480f + 0.01f)
    }

    @Test fun rowsAreCentredWithRingRoom() {
        assertEquals((616f - (3 * 154f + 2 * 14f)) / 2, gridRowsTop(616f, 154f, 14f, rows = 3), 0.01f)
        assertEquals((845f - (4 * 154f + 3 * 14f)) / 2, gridRowsTop(845f, 154f, 14f, rows = 4), 0.01f)
        assertEquals(GRID_RING_ROOM, gridRowsTop(300f, 154f, 14f, rows = 3), 0.01f)   // overflowing: never above the ring room
        assertEquals(gridRowsTop(358f, 128f, 14f), gridRowsTop(358f, 128f, 14f, rows = 2), 0.01f)
    }

    @Test fun stillWhenTheItemsFitRowsByColumns() {
        assertEquals(GridMode.STILL, gridModeFor(count = 15, columns = 5, rows = 3))
        assertEquals(GridMode.SLIDING, gridModeFor(count = 16, columns = 5, rows = 3))
        assertEquals(GridMode.STILL, gridModeFor(count = 16, columns = 4, rows = 4))
        assertEquals(GridMode.SLIDING, gridModeFor(count = 17, columns = 4, rows = 4))
        assertEquals(GridMode.SLIDING, gridModeFor(count = 11, columns = 5))           // phones: 2 rows
    }

    @Test fun stillThreeRowsStepRowOrderAndWrapInTheColumn() {
        fun step(i: Int, b: NxButton) = gridStep(i, 13, 5, b, rows = 3)   // rows of 5, 5, 3
        assertEquals(5, step(4, NxButton.RIGHT))       // on along the row order
        assertEquals(3, step(4, NxButton.LEFT))
        assertNull(step(0, NxButton.LEFT))             // past the first: the caller switches tab
        assertNull(step(12, NxButton.RIGHT))           // past the last
        assertEquals(7, step(2, NxButton.DOWN))
        assertEquals(12, step(7, NxButton.DOWN))
        assertEquals(2, step(12, NxButton.DOWN))       // wraps bottom -> top, same column
        assertEquals(12, step(2, NxButton.UP))         // wraps top -> bottom
        assertEquals(12, step(9, NxButton.DOWN))       // below is empty: the last tile
        assertEquals(12, step(4, NxButton.UP))         // wraps onto an empty spot: the last tile
        assertEquals(4, step(9, NxButton.UP))
    }

    @Test fun stillFourRowsWrapAndFillEmptySpots() {
        fun step(i: Int, b: NxButton) = gridStep(i, 14, 4, b, rows = 4)   // rows of 4, 4, 4, 2
        assertEquals(13, step(1, NxButton.UP))         // wraps to the bottom row, same column
        assertEquals(13, step(2, NxButton.UP))         // that spot is empty: the last tile
        assertEquals(1, step(13, NxButton.DOWN))       // wraps to the top
        assertEquals(13, step(10, NxButton.DOWN))      // below is empty: the last tile
        assertEquals(8, step(4, NxButton.DOWN))
        assertEquals(0, step(4, NxButton.UP))
    }

    @Test fun slidingThreeRowsStepColumnsAndWrapWithinTheColumn() {
        fun step(i: Int, b: NxButton) = gridStep(i, 16, 5, b, rows = 3)   // columns of 3; the last holds 15 only
        assertEquals(3, step(0, NxButton.RIGHT))
        assertEquals(4, step(1, NxButton.RIGHT))       // same row
        assertEquals(15, step(13, NxButton.RIGHT))     // into the one-tile last column
        assertEquals(15, step(14, NxButton.RIGHT))
        assertNull(step(15, NxButton.RIGHT))           // past the last column
        assertNull(step(2, NxButton.LEFT))             // past the first column
        assertEquals(1, step(4, NxButton.LEFT))
        assertEquals(1, step(0, NxButton.DOWN))
        assertEquals(2, step(1, NxButton.DOWN))
        assertEquals(0, step(2, NxButton.DOWN))        // wraps within the column
        assertEquals(2, step(0, NxButton.UP))
        assertEquals(15, step(15, NxButton.DOWN))      // a one-tile column: nothing to wrap to
        assertEquals(15, step(15, NxButton.UP))
    }

    @Test fun slidingFourRowsWrapWithinAShortLastColumn() {
        fun step(i: Int, b: NxButton) = gridStep(i, 18, 4, b, rows = 4)   // columns of 4; the last holds 16, 17
        assertEquals(17, step(16, NxButton.DOWN))
        assertEquals(16, step(17, NxButton.DOWN))      // wraps within the two it has
        assertEquals(17, step(16, NxButton.UP))
        assertEquals(3, step(0, NxButton.UP))
        assertEquals(17, step(13, NxButton.RIGHT))     // the last column, same row
        assertEquals(17, step(15, NxButton.RIGHT))     // an empty spot: the last tile
        assertEquals(12, step(16, NxButton.LEFT))
    }

    private val g3 = GridGeometry(tileW = 100f, tileH = 120f, gap = 10f, gutter = 24f, columns = 5, rows = 3)

    @Test fun slidingTilesRunColumnMajorOverNRows() {
        assertEquals(Pair(0f, 0f), gridTileOffset(0, GridMode.SLIDING, g3))
        assertEquals(Pair(0f, 260f), gridTileOffset(2, GridMode.SLIDING, g3))
        assertEquals(Pair(110f, 0f), gridTileOffset(3, GridMode.SLIDING, g3))
        assertEquals(Pair(220f, 130f), gridTileOffset(7, GridMode.SLIDING, g3))
    }

    @Test fun slidingColumnsCountByN() {
        assertEquals(0..3, gridVisibleColumns(0f, 10, g3, 1000f))       // 10 tiles in 3 rows: 4 columns
        val content = 4 * 110f - 10f
        assertEquals(400f - 24f - content, gridSlideX(9, 10, g3, 400f), 0.01f)   // the last column on the far gutter
        assertEquals(Pair(true, false), gridEdges(400f - 24f - content, 10, g3, 400f))
    }

    // The Flip's tall portrait: two columns of three rows filling the width; scrolled, the selection centres.
    @Test fun theFlipsTallPortraitIsThreeRowsOfTwoColumns() {
        val g = grid(360f, 511f, portrait = true)        // body 423
        assertEquals(3, g.rows); assertEquals(2, g.columns); assertTrue(g.tallPortrait)
        assertEquals((360f - 32f - 14f) / 2, g.tileW, 0.01f)             // 157: the two fill the width between the gutters
        assertEquals(117.3f, g.tileH, 0.01f)
        assertEquals((360f - g.tileW) / 2, g.anchorX!!, 0.01f)            // a scrolled selection centred
        val rowsH = 3 * g.tileH + 2 * 14f
        assertEquals((423f - rowsH) / 2, gridRowsTop(423f, g.tileH, g.gap, 3), 0.01f)   // centred in the body
    }

    // A wider phone's tall portrait starts with three columns: as many as fit at 110 dp, 2..3.
    @Test fun aWiderTallPortraitHasThreeColumns() {
        assertEquals(2, gridTallColumns(360f, 16f))
        assertEquals(3, gridTallColumns(411f, 16f))
        assertEquals(3, gridTallColumns(600f, 16f))
        assertEquals(3, grid(475f, 700f, portrait = true).columns)
    }

    @Test fun aTallTileFitsThreeRowsAtTheThreshold() {
        val g = gridFor(360f, 600f, 360f, 381.6f + 0.01f, portrait = true)   // three rows at 0.9 x 128 just fit
        assertEquals(3, g.rows)
        assertTrue(3 * g.tileH + 2 * g.gap <= gridSizingHeight(381.61f) + 0.01f)
        assertEquals(115.2f, g.tileH, 0.02f)
    }

    @Test fun tallNeedsThreeRowsAtNineTenthsOfTheTile() {
        assertTrue(gridTallPortrait(3 * 115.2f + 28f))
        assertFalse(gridTallPortrait(373.5f))
        assertFalse(gridFor(360f, 900f, 360f, 800f, portrait = false).tallPortrait)    // landscape never
        assertFalse(gridFor(704f, 933f, 704f, 845f, portrait = true).tallPortrait)     // a tablet's portrait never
        assertFalse(gridFor(475f, 379f, 475f, 291f, portrait = true).tallPortrait)     // the Fold cover: 3 columns x 2 rows
        assertTrue(gridFor(360f, 600f, 360f, 381.6f + 0.01f, portrait = true).tallPortrait)
        assertFalse(gridFor(360f, 600f, 360f, 381.5f, portrait = true).tallPortrait)
    }
}
