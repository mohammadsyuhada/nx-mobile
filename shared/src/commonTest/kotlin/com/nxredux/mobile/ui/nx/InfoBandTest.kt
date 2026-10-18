package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

class InfoBandTest {
    @Test fun theBandIsTheLinePlusFourEachSideAndReachesIntoTheBarsEmptyTop() {
        val band = nxInfoBandGeometry(lineDp = 18f, inkTopDp = 8.25f)
        assertEquals(26f, band.height)
        assertEquals(8.25f, band.overlap)
        assertEquals(17.75f, band.reserve)
    }

    @Test fun theOverlapNeverPassesTwelve() {
        assertEquals(12f, nxInfoBandGeometry(lineDp = 18f, inkTopDp = 20f).overlap)
    }

    @Test fun onTouchTheBandReachesIntoNothingAndReservesItsWholeHeight() {
        val band = nxInfoBandGeometry(lineDp = 18f, inkTopDp = 8.25f, touch = true)
        assertEquals(26f, band.height)
        assertEquals(0f, band.overlap)
        assertEquals(26f, band.reserve)
    }
}
