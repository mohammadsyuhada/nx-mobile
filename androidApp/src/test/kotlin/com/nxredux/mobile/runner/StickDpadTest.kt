package com.nxredux.mobile.runner

import org.junit.Assert.assertEquals
import org.junit.Test

class StickDpadTest {
    @Test fun aSmallTiltIsNoDirection() = assertEquals(0, stickDpadStep(0.3f, 0))

    @Test fun pastHalfwayPresses() {
        assertEquals(1, stickDpadStep(0.5f, 0))
        assertEquals(-1, stickDpadStep(-0.8f, 0))
    }

    @Test fun aHeldDirectionStaysUntilUnderTheReleaseLine() {
        assertEquals(1, stickDpadStep(0.45f, 1))
        assertEquals(0, stickDpadStep(0.35f, 1))
        assertEquals(-1, stickDpadStep(-0.45f, -1))
        assertEquals(0, stickDpadStep(0.45f, 0))   // not held: the press line applies
    }

    @Test fun straightAcrossFlips() = assertEquals(-1, stickDpadStep(-0.9f, 1))

    @Test fun aPressedHatWinsOverTheStick() {
        assertEquals(-1, hatOrStick(-1f, 1))
        assertEquals(1, hatOrStick(1f, 0))
    }

    @Test fun aCentredHatLeavesTheStick() {
        assertEquals(1, hatOrStick(0f, 1))
        assertEquals(0, hatOrStick(0f, 0))
    }
}
