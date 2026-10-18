package com.nxredux.mobile.ui.nx

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DpadHatTest {
    @Test fun centreByDefault() {
        val h = DpadHat(); assertEquals(0, h.x); assertEquals(0, h.y)
    }

    @Test fun upIsNegativeY() {
        val h = DpadHat(); h.press(NxButton.UP); assertEquals(0, h.x); assertEquals(-1, h.y)
    }

    @Test fun downIsPositiveY() {
        val h = DpadHat(); h.press(NxButton.DOWN); assertEquals(1, h.y)
    }

    @Test fun leftRightCancel() {
        val h = DpadHat(); h.press(NxButton.LEFT); h.press(NxButton.RIGHT); assertEquals(0, h.x)
    }

    @Test fun diagonalCombines() {
        val h = DpadHat(); h.press(NxButton.UP); h.press(NxButton.RIGHT)
        assertEquals(1, h.x); assertEquals(-1, h.y)
    }

    @Test fun releaseRestoresCentre() {
        val h = DpadHat(); h.press(NxButton.LEFT); h.release(NxButton.LEFT); assertEquals(0, h.x)
    }

    @Test fun nonDirectionIgnored() {
        val h = DpadHat(); assertFalse(h.press(NxButton.A)); assertEquals(0, h.x); assertEquals(0, h.y)
    }

    @Test fun redundantReleaseIsFalse() {
        val h = DpadHat(); assertFalse(h.release(NxButton.UP))
        h.press(NxButton.UP); assertTrue(h.release(NxButton.UP)); assertFalse(h.release(NxButton.UP))
    }
}
