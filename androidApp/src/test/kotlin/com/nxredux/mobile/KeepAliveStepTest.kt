package com.nxredux.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class KeepAliveStepTest {
    @Test fun runningServiceRefreshesWhileWantedAndStopsAfter() {
        assertEquals(KeepAliveStep.REFRESH, KeepAliveStep.of(running = true, active = true, starting = false))
        assertEquals(KeepAliveStep.STOP, KeepAliveStep.of(running = true, active = false, starting = false))
    }

    @Test fun firstActiveUpdateStartsOnlyOnce() {
        assertEquals(KeepAliveStep.START, KeepAliveStep.of(running = false, active = true, starting = false))
        assertEquals(KeepAliveStep.NONE, KeepAliveStep.of(running = false, active = true, starting = true))   // a start in flight
    }

    // Inactive before onStartCommand ran: nothing here; onStartCommand reads `wanted` false and stops at once.
    @Test fun inactiveWithNoneRunningDoesNothing() {
        assertEquals(KeepAliveStep.NONE, KeepAliveStep.of(running = false, active = false, starting = true))
        assertEquals(KeepAliveStep.NONE, KeepAliveStep.of(running = false, active = false, starting = false))
    }
}
