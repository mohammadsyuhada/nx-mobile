package com.nxredux.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopRateModeTest {
    // A Retroid Pocket Nova-like panel (960p at 60 / 90 / 120) plus a lower resolution that must never be picked.
    private val p60 = ModeSpec(1, 1280, 960, 60f)
    private val p90 = ModeSpec(2, 1280, 960, 90f)
    private val p120 = ModeSpec(3, 1280, 960, 120f)
    private val low144 = ModeSpec(4, 640, 480, 144f)
    private val modes = listOf(p60, low144, p90, p120)

    @Test fun picksTheTopRateAtTheCurrentResolution() = assertEquals(3, topRateModeId(modes, p60))

    @Test fun neverSwitchesResolutionForAHigherRate() = assertEquals(3, topRateModeId(modes, p90))

    @Test fun theCurrentModeWhenItIsAlreadyTheTop() = assertEquals(3, topRateModeId(modes, p120))

    @Test fun keepsTheCurrentModeOnATie() {
        val twin = ModeSpec(5, 1280, 960, 120f)
        assertEquals(5, topRateModeId(listOf(p60, p120, twin), twin))
    }

    @Test fun theOtherResolutionGroupUsesItsOwnModes() = assertEquals(4, topRateModeId(modes, low144))

    @Test fun aSingleModeIsItself() = assertEquals(1, topRateModeId(listOf(p60), p60))

    @Test fun nothingAtTheCurrentResolutionIsNull() = assertNull(topRateModeId(listOf(low144), p60))
}
