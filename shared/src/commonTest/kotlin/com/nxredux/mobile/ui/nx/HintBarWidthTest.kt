package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

/** The hint bar's own width: the space it is given, or its hints' when asked unbounded (an intrinsic width). */
class HintBarWidthTest {
    @Test fun aBoundedBarTakesItsWidth() {
        assertEquals(1000, hintBarWidth(bounded = true, maxWidth = 1000, hints = 420))
    }

    // NxEmptyState sizes its bar to the hints (IntrinsicSize.Max): unbounded, the bar is its hints' width, never Int.MAX.
    @Test fun anUnboundedBarIsItsHintsWidth() {
        assertEquals(420, hintBarWidth(bounded = false, maxWidth = Int.MAX_VALUE, hints = 420))
    }
}
