package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

/** A list's highlighted row is brought into view by the smallest scroll ([nxKeepInViewScroll]). */
class KeepInViewTest {
    // A 200 px viewport of 20 px rows: rows 0..9 show.
    private fun scroll(selected: Int, offset: Int?, lastVisible: Int = 9) =
        nxKeepInViewScroll(selected, offset, itemSize = 20, viewportStart = 0, viewportEnd = 200, lastVisible = lastVisible, rowHeight = 20)

    @Test fun aRowFullyInViewDoesNotScroll() {
        assertEquals(NxKeepInView.None, scroll(3, offset = 60))
    }

    @Test fun aPartlyVisibleRowScrollsByItsOverflow() {
        assertEquals(NxKeepInView.By(12), scroll(9, offset = 192))     // 12 px past the bottom
        assertEquals(NxKeepInView.By(-5), scroll(0, offset = -5))      // 5 px above the top
    }

    @Test fun anOffScreenRowJumpsToTheNearLine() {
        assertEquals(NxKeepInView.ToItem(15, -180), scroll(15, offset = null))                 // below: lands on the last line
        assertEquals(NxKeepInView.ToItem(2, 0), scroll(2, offset = null, lastVisible = 14))    // above: lands on the top line
    }
}
