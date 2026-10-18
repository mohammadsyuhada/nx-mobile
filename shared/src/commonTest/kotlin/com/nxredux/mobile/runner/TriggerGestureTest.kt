package com.nxredux.mobile.runner

import com.nxredux.mobile.runner.TriggerGesture.State
import kotlin.test.Test
import kotlin.test.assertEquals

class TriggerGestureTest {
    @Test fun quickTapLatchesSoft() {
        val g = TriggerGesture()
        assertEquals(State.OFF, g.down(0))
        assertEquals(State.SOFT, g.up(100))
    }

    @Test fun secondTapReleasesTheLatch() {
        val g = TriggerGesture()
        g.down(0); g.up(100)
        g.down(1000)
        assertEquals(State.OFF, g.up(1100))
    }

    @Test fun holdBecomesFullAtHoldMs() {
        val g = TriggerGesture()
        g.down(0)
        assertEquals(State.OFF, g.tick(299))
        assertEquals(State.FULL, g.tick(300))
    }

    @Test fun releaseAfterHoldWithoutLatchGoesOff() {
        val g = TriggerGesture()
        g.down(0); g.tick(300)
        assertEquals(State.OFF, g.up(500))
    }

    @Test fun releaseAfterHoldWhileLatchedReturnsToSoft() {
        val g = TriggerGesture()
        g.down(0); g.up(50)
        g.down(1000)
        assertEquals(State.FULL, g.tick(1300))
        assertEquals(State.SOFT, g.up(1500))
    }

    @Test fun downWhileLatchedStaysSoftUntilHold() {
        val g = TriggerGesture()
        g.down(0); g.up(50)
        assertEquals(State.SOFT, g.down(1000))
        assertEquals(State.SOFT, g.tick(1299))
        assertEquals(State.FULL, g.tick(1300))
    }

    @Test fun clearGivesOffAndDropsTheLatch() {
        val g = TriggerGesture()
        g.down(0); g.up(50)
        assertEquals(State.OFF, g.clear())
        g.down(100)
        assertEquals(State.SOFT, g.up(150))
    }

    @Test fun tickWithoutDownChangesNothing() {
        val g = TriggerGesture()
        assertEquals(State.OFF, g.tick(10_000))
    }
}
