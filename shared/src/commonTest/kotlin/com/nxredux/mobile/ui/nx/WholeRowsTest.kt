package com.nxredux.mobile.ui.nx

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.roundToInt

class WholeRowsTest {
    @Test fun gutterPixelRoundingDoesNotLoseARow() {
        // Samsung cover display, 5.8 cm pad: 11 rows of 27.879 dp; the two 13.939 dp gutters lay out as 37 px each at
        // 2.625x, leaving a 731 px (278.476 dp) slot for 10 rows of 73.18 px. It must still hold 10 rows.
        assertEquals(10, nxWholeRows(slot = 278.4762.dp, row = 27.878786.dp, pxSlack = 2f / 2.625f))
    }

    @Test fun exactFitKeepsItsRows() {
        assertEquals(10, nxWholeRows(slot = 260.dp, row = 26.dp, pxSlack = 2f / 2.625f))
    }

    @Test fun aPartialRowIsDropped() {
        assertEquals(9, nxWholeRows(slot = 250.dp, row = 26.dp, pxSlack = 2f / 2.625f))
    }

    @Test fun neverLessThanOne() {
        assertEquals(1, nxWholeRows(slot = 5.dp, row = 26.dp, pxSlack = 2f))
    }

    @Test fun aBandListFillsItsSlotWithWholeRows() {
        val (n, row) = nxListFit(300, 1f)
        assertEquals(9, n)   // floor(300 / 32.5)
        assertEquals(33f, row.value)   // floor(300 / 9) whole px
    }

    @Test fun bandRowsAreWholePixelsThatNeverOverrunTheSlot() {
        // At 2.625x a fractional row (736 px / 10 = 73.6 px) rounds up per item and 10 overrun the slot; floored, they fit.
        for (px in 700..760) {
            val (n, row) = nxListFit(px, 2.625f)
            val rowPx = (row.value * 2.625f).roundToInt()
            assertEquals(px / n, rowPx)
            assertTrue(n * rowPx <= px && px - n * rowPx < n)
            assertEquals(n, nxWholeRows((px / 2.625f).dp, row, pxSlack = 1f / 2.625f))   // NxList shows all n
        }
    }

    // Appearance → Text size Small: a band list counts its rows by a smaller pitch (0.75 of it), so more, shorter rows fit.
    @Test fun a_smaller_pitch_fits_more_rows() {
        val (large, largeRow) = nxListFit(1000, 1f)
        val (small, smallRow) = nxListFit(1000, 1f, NxListPitch * 0.75f)
        assertEquals((1000 / NxListPitch.value).toInt(), large)
        assertTrue(small > large); assertTrue(smallRow < largeRow)
    }
}
