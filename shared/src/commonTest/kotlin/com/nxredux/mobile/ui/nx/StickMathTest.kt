package com.nxredux.mobile.ui.nx

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class StickMathTest {
    private fun near(a: Float, b: Float) = assertTrue(abs(a - b) < 1e-4f, "expected $b, got $a")

    @Test fun centreIsZero() {
        val (x, y) = normalizeStick(0f, 0f, 100f); near(x, 0f); near(y, 0f)
    }

    @Test fun fullRightIsOne() {
        val (x, y) = normalizeStick(100f, 0f, 100f); near(x, 1f); near(y, 0f)
    }

    @Test fun fullDownIsPositiveY() {
        val (_, y) = normalizeStick(0f, 100f, 100f); near(y, 1f)
    }

    @Test fun halfIsHalf() {
        val (x, _) = normalizeStick(50f, 0f, 100f); near(x, 0.5f)
    }

    @Test fun beyondRadiusClampsToUnitLength() {
        val (x, y) = normalizeStick(300f, 0f, 100f); near(x, 1f); near(y, 0f)
        val (dx, dy) = normalizeStick(100f, 100f, 100f) // 45°, past radius
        near(kotlin.math.sqrt(dx * dx + dy * dy), 1f)
    }

    @Test fun zeroRadiusIsZero() {
        val (x, y) = normalizeStick(10f, 10f, 0f); near(x, 0f); near(y, 0f)
    }

    @Test fun signsPreserved() {
        val (x, y) = normalizeStick(-50f, -50f, 100f); assertTrue(x < 0f); assertTrue(y < 0f)
    }
}
