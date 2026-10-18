package com.nxredux.mobile.runner

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class WiimoteFrameTest {
    private fun near(a: Vec3, b: Vec3) = abs(a.x - b.x) < 1e-4f && abs(a.y - b.y) < 1e-4f && abs(a.z - b.z) < 1e-4f
    private fun assertNear(expected: Vec3, actual: Vec3) = assertTrue(near(expected, actual), "expected $expected, got $actual")

    @Test fun displayFollowsTheScreenRotation() {
        val v = Vec3(1f, 2f, 3f)
        assertNear(Vec3(1f, 2f, 3f), WiimoteFrame.display(v, 0))
        assertNear(Vec3(2f, -1f, 3f), WiimoteFrame.display(v, 1))
        assertNear(Vec3(-1f, -2f, 3f), WiimoteFrame.display(v, 2))
        assertNear(Vec3(-2f, 1f, 3f), WiimoteFrame.display(v, 3))
    }

    @Test fun heldFacingYouAnUprightRemoteIsLevel() {
        // gravity reaction straight up the screen: the remote's +Z (up) reads 1 g
        assertNear(Vec3(0f, 0f, 1f), WiimoteFrame.forPad(Vec3(0f, 1f, 0f), WiiPad.UPRIGHT))
        assertNear(Vec3(0f, 0f, 1f), WiimoteFrame.forPad(Vec3(0f, 1f, 0f), WiiPad.NUNCHUK))
    }

    @Test fun sidewaysIsTheDisplayFrameForDolphinsOwnRotation() {
        assertNear(Vec3(0f, 1f, 0f), WiimoteFrame.forPad(Vec3(0f, 1f, 0f), WiiPad.SIDEWAYS))
    }

    @Test fun levelTakesTheRestingTiltAway() {
        val s = sqrt(0.5f)
        val tipped = Vec3(0f, s, s)   // phone tipped back 45°
        val level = WiimoteFrame.Level.capture(tipped)
        assertNear(Vec3(0f, 1f, 0f), level.apply(tipped))
        assertNear(Vec3(0f, 1f, 0f), WiimoteFrame.Level.NONE.apply(Vec3(0f, 1f, 0f)))
    }

    @Test fun levelIsARotationSoItKeepsLengths() {
        val level = WiimoteFrame.Level.capture(Vec3(0.3f, 0.8f, 0.52f))
        val v = level.apply(Vec3(0.2f, -0.4f, 0.9f))
        val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
        assertTrue(abs(len - sqrt(0.04f + 0.16f + 0.81f)) < 1e-4f)
    }

    @Test fun capturingGravityThatPointsAwayStillGivesAFiniteRotation() {
        val level = WiimoteFrame.Level.capture(Vec3(0f, -1f, 0f))   // phone upside down: the 180° case
        assertNear(Vec3(0f, 1f, 0f), level.apply(Vec3(0f, -1f, 0f)))
    }
}
