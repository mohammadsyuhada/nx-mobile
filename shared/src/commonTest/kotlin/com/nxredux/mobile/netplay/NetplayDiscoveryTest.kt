package com.nxredux.mobile.netplay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetplayDiscoveryTest {
    private val v2 = NetplayWire.encodeDiscovery("Tetris (World)")

    @Test fun v2_wizard_packet_is_a_host() {
        assertEquals(FoundHost("10.0.0.2", "Tetris (World)"), NetplayDiscovery.accept(v2, v2.size, "10.0.0.2", "10.0.0.3", emptySet()))
    }

    @Test fun own_ip_and_seen_ips_are_skipped() {
        assertNull(NetplayDiscovery.accept(v2, v2.size, "10.0.0.3", "10.0.0.3", emptySet()))
        assertNull(NetplayDiscovery.accept(v2, v2.size, "10.0.0.2", null, setOf("10.0.0.2")))
    }

    @Test fun other_versions_modes_and_sizes_are_dropped() {
        val v1 = NetplayWire.encodeDiscovery("Tetris", version = 1)
        assertNull(NetplayDiscovery.accept(v1, v1.size, "10.0.0.2", null, emptySet()))
        val mode = v2.copyOf().also { "netarch".encodeToByteArray().copyInto(it, 78); it[85] = 0 }
        assertNull(NetplayDiscovery.accept(mode, mode.size, "10.0.0.2", null, emptySet()))
        assertNull(NetplayDiscovery.accept(v2, 100, "10.0.0.2", null, emptySet()))
    }
}
