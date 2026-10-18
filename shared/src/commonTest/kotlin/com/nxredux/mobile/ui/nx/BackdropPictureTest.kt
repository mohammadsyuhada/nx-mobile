package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals

/** The Backdrop picture fades only to where the selection stops ([backdropPictureDelayMs]). */
class BackdropPictureTest {
    @Test fun aScreenshotReplacingAScreenshotWaitsForTheSelectionToSettle() {
        assertEquals(BACKDROP_PICTURE_SETTLE_MS, backdropPictureDelayMs("a.png", "b.png"))
    }

    @Test fun theFirstScreenshotAndGoingPlainAreImmediate() {
        assertEquals(0L, backdropPictureDelayMs(null, "a.png"))
        assertEquals(0L, backdropPictureDelayMs("a.png", null))
        assertEquals(0L, backdropPictureDelayMs("a.png", "a.png"))
    }
}
