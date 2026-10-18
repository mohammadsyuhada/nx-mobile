package com.nxredux.mobile.runner

import com.nxredux.mobile.settings.GameRumble
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RumbleAmplitudeTest {
    @Test fun offAndZeroAreSilent() {
        assertEquals(0, rumbleAmplitude(GameRumble.OFF, 1f, 1f))
        for (level in GameRumble.entries) assertEquals(0, rumbleAmplitude(level, 0f, 0f))
        assertEquals(0, rumbleAmplitude(GameRumble.STRONG, -0.5f, 0f))
    }

    @Test fun theStrongerMotorWins() {
        assertEquals(rumbleAmplitude(GameRumble.MEDIUM, 0f, 0.5f), rumbleAmplitude(GameRumble.MEDIUM, 0.5f, 0.2f))
        assertEquals(rumbleAmplitude(GameRumble.MEDIUM, 0.7f, 0f), rumbleAmplitude(GameRumble.MEDIUM, 0.1f, 0.7f))
    }

    @Test fun anyNonzeroStrengthLandsInTheLevelsFeltRange() {
        // Light 45-80 %, Medium 60-100 %, Strong 80-100 % of 255.
        assertEquals(115, rumbleAmplitude(GameRumble.LIGHT, 0.0001f, 0f))
        assertEquals(204, rumbleAmplitude(GameRumble.LIGHT, 1f, 0f))
        assertEquals(153, rumbleAmplitude(GameRumble.MEDIUM, 0.0001f, 0f))
        assertEquals(255, rumbleAmplitude(GameRumble.MEDIUM, 1f, 0f))
        assertEquals(204, rumbleAmplitude(GameRumble.STRONG, 0.0001f, 0f))
        assertEquals(255, rumbleAmplitude(GameRumble.STRONG, 1f, 0f))
        assertEquals(204, rumbleAmplitude(GameRumble.MEDIUM, 0.5f, 0f))   // 0.6 + 0.5 * 0.4 = 0.8
        assertEquals(255, rumbleAmplitude(GameRumble.STRONG, 3f, 0f))     // clamped
    }

    @Test fun previewIsTheLevelsFloor() {
        assertEquals(0, rumblePreviewAmplitude(GameRumble.OFF))
        assertEquals(115, rumblePreviewAmplitude(GameRumble.LIGHT))
        assertEquals(153, rumblePreviewAmplitude(GameRumble.MEDIUM))
        assertEquals(204, rumblePreviewAmplitude(GameRumble.STRONG))
    }
}

class RumbleScheduleTest {
    private val run = { a: Int -> RumbleCommand.Run(a, RumbleSchedule.RUN_MS) }

    @Test fun startsAtOnceAndStopsAfterTheOffDebounce() {
        val s = RumbleSchedule()
        assertEquals(run(200), s.set(200, 0))
        assertNull(s.set(0, 100))                       // not yet: a gap shorter than the debounce keeps it on
        assertEquals(150L, s.nextDeadline())
        assertNull(s.tick(149))
        assertEquals(RumbleCommand.Stop, s.tick(150))
        assertNull(s.nextDeadline())
        assertNull(s.tick(10_000))
    }

    @Test fun aShortGapNeverStopsTheMotor() {
        val s = RumbleSchedule()
        s.set(200, 0)
        assertNull(s.set(0, 10))
        assertNull(s.set(200, 40))                      // back within 50 ms, same amplitude: nothing to re-issue
        assertNull(s.tick(60))                          // the pending off was dropped
        assertEquals(RumbleSchedule.RUN_MS - RumbleSchedule.REISSUE_MARGIN_MS, s.nextDeadline())
    }

    @Test fun smallAmplitudeChangesAreThrottled() {
        val s = RumbleSchedule()
        s.set(200, 0)
        assertNull(s.set(203, 10))
        assertNull(s.set(196, 20))
        assertEquals(run(205), s.set(205, 30))         // 5 from what was issued
        assertNull(s.set(209, 40))
        assertEquals(run(150), s.set(150, 50))
    }

    @Test fun aLongRumbleIsReissuedBeforeItExpires() {
        val s = RumbleSchedule()
        s.set(180, 0)
        val due = RumbleSchedule.RUN_MS - RumbleSchedule.REISSUE_MARGIN_MS
        assertNull(s.tick(due - 1))
        assertEquals(run(180), s.tick(due))
        assertEquals(due + due, s.nextDeadline())
    }

    @Test fun stopCancelsAtOnceOnlyWhenRunning() {
        val s = RumbleSchedule()
        assertNull(s.stop())
        s.set(100, 0)
        assertEquals(RumbleCommand.Stop, s.stop())
        assertNull(s.stop())
        assertNull(s.nextDeadline())
        assertEquals(run(100), s.set(100, 5))           // starts again from idle
    }

    @Test fun zeroWhileIdleDoesNothing() {
        val s = RumbleSchedule()
        assertNull(s.set(0, 0))
        assertNull(s.nextDeadline())
    }

}
