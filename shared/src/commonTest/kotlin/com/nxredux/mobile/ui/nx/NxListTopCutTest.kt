package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class NxListTopCutTest {
    // The icons' box: x 900..1248, y 0..132 (px); the list's right edge at 1248.
    @Test fun aRowStartingAboveTheIconsBottomStopsAtTheirLeft() {
        assertEquals(348f, nxRowTopCutPx(rowTop = 0f, cutLeft = 900f, cutBottom = 132f, listRight = 1248f))
        assertEquals(348f, nxRowTopCutPx(rowTop = 100f, cutLeft = 900f, cutBottom = 132f, listRight = 1248f))   // the second row, partly beside them
    }

    @Test fun aRowBelowTheIconsKeepsItsWidth() {
        assertEquals(0f, nxRowTopCutPx(rowTop = 132f, cutLeft = 900f, cutBottom = 132f, listRight = 1248f))
        assertEquals(0f, nxRowTopCutPx(rowTop = 400f, cutLeft = 900f, cutBottom = 132f, listRight = 1248f))
    }

    @Test fun aRowScrolledAboveTheListTopIsCutToo() {
        assertEquals(348f, nxRowTopCutPx(rowTop = -40f, cutLeft = 900f, cutBottom = 132f, listRight = 1248f))
    }

    @Test fun noIconsMeasuredYetCutsNothing() {
        assertEquals(0f, nxRowTopCutPx(rowTop = 0f, cutLeft = Float.NaN, cutBottom = Float.NaN, listRight = 1248f))
    }

    @Test fun rowTopFollowsTheScroll() {
        // List top at 50 px, rows 98 px, the first visible row 3 scrolled 20 px off the top: row 4 sits at 50 + 98 - 20.
        assertEquals(128f, nxRowTopPx(index = 4, listTop = 50f, firstVisible = 3, firstOffset = 20, pitch = 98))
    }
}
