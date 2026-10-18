package com.nxredux.mobile.ui.nx

import androidx.compose.runtime.mutableFloatStateOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A sliding Grid's swipe only scrolls: the selection holds through the drag and the release; a d-pad press brings it back. */
class GridScrollTest {
    // 140 × 154 tiles, pitch 154, 800 wide: column c rests at 115 − 154 c (the anchor), held within the far end and 24.
    private fun g(rows: Int) = GridGeometry(tileW = 140f, tileH = 154f, gap = 14f, gutter = 24f, columns = 5, rows = rows)
    private val w = 800f
    private fun snap(start: Float, drag: Float, velocity: Float = 0f, count: Int, rows: Int) =
        gridScrollSnap(start, drag, velocity, count, g(rows), w)

    @Test fun twoRowsSnapToTheNearestColumnRest() {
        val start = 115f - 5 * 154f                                          // column 5 on the anchor
        assertEquals(start - 154f, snap(start, -154f, count = 40, rows = 2), 0.01f)   // a pitch: the next column
        assertEquals(start, snap(start, -60f, count = 40, rows = 2), 0.01f)           // under half a pitch: back
        assertEquals(start - 2 * 154f, snap(start, -154f, -800f, count = 40, rows = 2), 0.01f)   // 200 dp of momentum more
        assertEquals(-2290f, snap(start, -10000f, count = 40, rows = 2), 0.01f)       // the far end: the last column on the gutter
        assertEquals(24f, snap(start, 10000f, count = 40, rows = 2), 0.01f)           // the start: column 0 on the gutter
    }

    @Test fun theStartEndIsARestOfItsOwn() {
        assertEquals(24f, snap(24f, -10f, count = 40, rows = 2), 0.01f)       // nearer the start than column 1's rest
        assertEquals(-39f, snap(24f, -40f, count = 40, rows = 2), 0.01f)      // nearer column 1's (115 − 154)
        assertEquals(24f, snap(24f, 500f, count = 40, rows = 2), 0.01f)       // past the start: held there
    }

    @Test fun threeRowsHoldTheFarEnd() {
        // 16 tiles in 3 rows: 6 columns, content 910, the far end at 800 − 24 − 910 = −134.
        assertEquals(-134f, snap(24f, -300f, count = 16, rows = 3), 0.01f)
        assertEquals(-39f, snap(24f, -100f, count = 16, rows = 3), 0.01f)     // column 1's rest, nearer than the end
        assertEquals(-134f, snap(-134f, -500f, -2000f, count = 16, rows = 3), 0.01f)
    }

    @Test fun fourRowsCarryMomentum() {
        // 40 tiles in 4 rows: 10 columns, the far end at −750.
        assertEquals(-501f, snap(115f - 2 * 154f, -50f, -1000f, count = 40, rows = 4), 0.01f)   // 300 dp: column 4's rest
        assertEquals(-750f, snap(-501f, -300f, count = 40, rows = 4), 0.01f)
    }

    @Test fun aGridThatDoesNotOverflowStaysOnTheGutter() {
        assertEquals(24f, snap(24f, -300f, -2000f, count = 8, rows = 2), 0.01f)   // 4 columns fit in 800
    }

    @Test fun theSelectionHoldsAcrossADragAndItsRelease() {
        val g = g(3)
        val count = 30
        val sel = 13
        val live = mutableFloatStateOf(Float.NaN)
        val d = SwipeDrag(live)
        d.begin(resting = gridShiftTarget(null, 0, sel, count, g, w))
        val lo = gridSlideX(count - 1, count, g, w); val hi = gridSlideX(0, count, g, w)
        repeat(40) { d.move(-17f, toValue = { it }, lo = lo, hi = hi, give = 77f) }
        var scroll: GridScroll? = null
        d.release(-600f) { start, drag, v -> scroll = GridScroll(press = 0, rest = gridScrollSnap(start, drag, v, count, g, w)) }
        val s = scroll!!
        assertEquals(-750f, s.rest, 0.01f)                                // 680 dp dragged + 150 flung: the far end
        assertEquals(s.rest, gridShiftTarget(s, 0, sel, count, g, w))     // the row stays where it was scrolled to
        assertEquals(true, gridTileLit(sel, sel))                         // the selection (unchanged) stays lit
    }

    @Test fun aTapAfterAScrollKeepsTheOffset() {
        val g = g(2)
        val scrolled = GridScroll(press = 3, rest = -2290f)
        // A tap selects 34 (in view there): no d-pad press, so the row stays exactly where the swipe left it.
        val tapped = gridScrollOnTap(scrolled, press = 3, shown = gridShiftTarget(scrolled, 3, 10, 40, g, w))
        assertEquals(-2290f, gridShiftTarget(tapped, 3, 34, 40, g, w))
        // A tap with no scroll doesn't move the row either: it stays at the old selection's rest.
        val still = gridScrollOnTap(null, press = 3, shown = gridShiftTarget(null, 3, 10, 40, g, w))
        assertEquals(gridSlideX(10, 40, g, w), gridShiftTarget(still, 3, 12, 40, g, w))
    }

    @Test fun aDpadPressAfterATapStillEasesBack() {
        val g = g(2)
        val tapped = gridScrollOnTap(GridScroll(press = 3, rest = -2290f), press = 3, shown = -2290f)
        assertEquals(gridSlideX(36, 40, g, w), gridShiftTarget(tapped, 4, 36, 40, g, w))   // the press: 34 -> 36, anchored as usual
        assertNull(gridScrollAfter(tapped, 4))
    }

    @Test fun aDpadPressThatMovesNothingStillBringsTheSelectionBack() {
        val g = g(2)
        val scrolled = GridScroll(press = 3, rest = -2290f)
        assertEquals(-2290f, gridShiftTarget(scrolled, 3, 10, 40, g, w))                   // no press yet: stays scrolled
        assertEquals(gridSlideX(10, 40, g, w), gridShiftTarget(scrolled, 4, 10, 40, g, w))   // a press, same selection: back
        assertEquals(gridSlideX(10, 40, g, w), gridShiftTarget(null, 3, 10, 40, g, w))       // not scrolled: the selection's
        assertEquals(scrolled, gridScrollAfter(scrolled, 3))
    }

    @Test fun aMoveChangesOnlyTheTwoTilesLitState() {
        assertEquals(120, GRID_LIT_MS)
        val before = (0 until 30).map { gridTileLit(it, litIndex = 3) }
        val after = (0 until 30).map { gridTileLit(it, litIndex = 4) }
        assertEquals(listOf(3, 4), (0 until 30).filter { before[it] != after[it] })
    }

    @Test fun aTallGridSnapsBetweenItsEnds() {
        val g = gridFor(360f, 511f, 360f, 423f, portrait = true)
        val count = 15                                                      // 5 columns
        val lo = gridSlideX(count - 1, count, g, 360f)
        val hi = gridSlideX(0, count, g, 360f)
        for (drag in listOf(-5000f, -400f, -150f, -50f, 0f, 50f, 150f, 400f, 5000f)) {
            val rest = gridScrollSnap(hi, drag, 0f, count, g, 360f)
            assertTrue(rest >= lo - 0.01f && rest <= hi + 0.01f, "rest $rest")
        }
        assertEquals(hi, gridScrollSnap(hi, 5000f, 0f, count, g, 360f), 0.01f)
        assertEquals(lo, gridScrollSnap(hi, -5000f, 0f, count, g, 360f), 0.01f)
    }
}
