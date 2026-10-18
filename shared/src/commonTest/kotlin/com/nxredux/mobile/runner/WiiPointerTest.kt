package com.nxredux.mobile.runner

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WiiPointerTest {
    @Test fun insideTheDeadZoneIsStill() = assertEquals(0f to 0f, WiiPointer.velocity(0.1f, -0.14f))

    @Test fun fullDeflectionIsOneWidthPerSecond() {
        val (vx, vy) = WiiPointer.velocity(1f, 0f)
        assertTrue(abs(vx - 1f) < 1e-4f && vy == 0f)
        assertTrue(abs(WiiPointer.velocity(0f, -1f).second + 1f) < 1e-4f)
    }

    @Test fun theDeadZoneIsRescaledSoSpeedStartsAtZero() {
        assertTrue(WiiPointer.velocity(0.16f, 0f).first < 0.02f)
    }
}
