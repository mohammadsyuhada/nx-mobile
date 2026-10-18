package com.nxredux.mobile.runner

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class N3dsFrameTest {
    private fun near(a: Vec3, b: Vec3) = abs(a.x - b.x) < 1e-4f && abs(a.y - b.y) < 1e-4f && abs(a.z - b.z) < 1e-4f
    private fun assertNear(expected: Vec3, actual: Vec3) = assertTrue(near(expected, actual), "expected $expected, got $actual")

    @Test fun portraitHeldUprightGravityPointsDownTheConsole() {
        // the reaction reads up the phone's Y; the 3DS reports gravity, -Z (Azahar's own resting default)
        assertNear(Vec3(0f, 0f, -1f), N3dsFrame.accel(Vec3(0f, 1f, 0f), 0))
    }

    @Test fun flatOnATableGravityPointsIntoTheScreenInEveryRotation() {
        for (r in 0..3) assertNear(Vec3(0f, -1f, 0f), N3dsFrame.accel(Vec3(0f, 0f, 1f), r))
    }

    @Test fun axesFollowAzaharsAndroidFrontendPerRotation() {
        // ndk_motion.cpp TransformAxes: portrait (-x, z, y), then (y, z, x), (x, z, -y), (-y, z, -x)
        val v = Vec3(1f, 2f, 3f)
        assertNear(Vec3(-1f, 3f, 2f), N3dsFrame.gyro(v, 0))
        assertNear(Vec3(2f, 3f, 1f), N3dsFrame.gyro(v, 1))
        assertNear(Vec3(1f, 3f, -2f), N3dsFrame.gyro(v, 2))
        assertNear(Vec3(-2f, 3f, -1f), N3dsFrame.gyro(v, 3))
    }

    @Test fun accelIsTheNegatedTransform() {
        assertNear(Vec3(-2f, -3f, -1f), N3dsFrame.accel(Vec3(1f, 2f, 3f), 1))
    }
}
