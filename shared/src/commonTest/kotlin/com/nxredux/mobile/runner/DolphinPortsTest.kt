package com.nxredux.mobile.runner

import kotlin.test.Test
import kotlin.test.assertEquals

class DolphinPortsTest {
    @Test
    fun onlyPlayerOneIsConnectedUntilASlotIsClaimed() {
        assertEquals(mapOf(0 to 513, 1 to 0, 2 to 0, 3 to 0), dolphinPorts(513, emptySet(), 4))
    }

    @Test
    fun claimedSlotsGetTheDevice() {
        assertEquals(mapOf(0 to 513, 1 to 513, 2 to 0, 3 to 513), dolphinPorts(513, setOf(1, 3), 4))
    }

    @Test
    fun portZeroIsAlwaysConnectedAndOutOfRangeClaimsAreIgnored() {
        assertEquals(mapOf(0 to 1, 1 to 0), dolphinPorts(1, setOf(5), 2))
        assertEquals(mapOf(0 to 1), dolphinPorts(1, emptySet(), 0))
    }
}
