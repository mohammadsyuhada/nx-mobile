package com.nxredux.mobile.runner

import org.junit.Assert.assertEquals
import org.junit.Test

class ThumbSizeTest {
    @Test fun aFrameThatFitsKeepsItsSize() {
        assertEquals(1280 to 720, SaveStates.thumbSize(1280, 720))
        assertEquals(640 to 480, SaveStates.thumbSize(640, 480))
    }

    @Test fun aWideFrameIsScaledToTheLongEdge() = assertEquals(1280 to 576, SaveStates.thumbSize(2400, 1080))

    @Test fun aTallFrameIsScaledByItsHeight() = assertEquals(763 to 1280, SaveStates.thumbSize(1080, 1812))

    @Test fun anExtremeStripNeverReachesZero() = assertEquals(1280 to 1, SaveStates.thumbSize(100_000, 2))
}
