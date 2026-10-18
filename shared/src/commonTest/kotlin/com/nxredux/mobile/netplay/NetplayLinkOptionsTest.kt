package com.nxredux.mobile.netplay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetplayLinkOptionsTest {
    private fun session(role: NetplayRole, core: String, peer: String = "192.168.1.7", usb: Boolean = false) =
        NetplaySession(role, peer, core, core, overUsb = usb)

    @Test fun gpsp_serial_follows_the_handhelds_rewrite() {
        assertEquals("auto_cable", NetplayLinkOptions.gpspSerial("auto", overUsb = true))
        assertEquals("auto", NetplayLinkOptions.gpspSerial("auto", overUsb = false))
        assertEquals("auto", NetplayLinkOptions.gpspSerial("lockstep", overUsb = false))
        assertEquals("lockstep", NetplayLinkOptions.gpspSerial("lockstep", overUsb = true))
        assertEquals("rfu", NetplayLinkOptions.gpspSerial("rfu", overUsb = true))
        assertEquals("mul_poke", NetplayLinkOptions.gpspSerial("mul_poke", overUsb = false))
    }

    @Test fun ip_digits_pad_each_octet_to_three() {
        assertEquals("192168001007", NetplayLinkOptions.ipDigits("192.168.1.7"))
        assertEquals("010099000001", NetplayLinkOptions.ipDigits("10.99.0.1"))
        assertNull(NetplayLinkOptions.ipDigits("192.168.1"))
        assertNull(NetplayLinkOptions.ipDigits("192.168.1.256"))
        assertNull(NetplayLinkOptions.ipDigits("a.b.c.d"))
        assertNull(NetplayLinkOptions.ipDigits("1..2.3"))
        assertNull(NetplayLinkOptions.ipDigits("1.2.3.+4"))
    }

    @Test fun a_gb_host_serves_and_a_gb_joiner_dials_the_host() {
        assertEquals(
            mapOf("gambatte_gb_link_network_port" to "56400", "gambatte_gb_link_mode" to "Network Server"),
            NetplayLinkOptions.forSession(session(NetplayRole.HOST, "gambatte"), null),
        )
        val joiner = NetplayLinkOptions.forSession(session(NetplayRole.CLIENT, "gambatte", peer = "192.168.1.7"), null)
        assertEquals("Network Client", joiner["gambatte_gb_link_mode"])
        assertEquals("56400", joiner["gambatte_gb_link_network_port"])
        assertEquals("192168001007".map { it.toString() }, (1..12).map { joiner["gambatte_gb_link_network_server_ip_$it"] })
        assertEquals(14, joiner.size)
    }

    @Test fun gpsp_runs_the_agreed_mode_for_the_transport_and_mgba_needs_none() {
        assertEquals(mapOf("gpsp_serial" to "auto_cable"), NetplayLinkOptions.forSession(session(NetplayRole.HOST, "gpsp", usb = true), "auto"))
        assertEquals(mapOf("gpsp_serial" to "auto"), NetplayLinkOptions.forSession(session(NetplayRole.CLIENT, "gpsp"), "lockstep"))
        assertEquals(mapOf("gpsp_serial" to "auto"), NetplayLinkOptions.forSession(session(NetplayRole.CLIENT, "gpsp"), null))
        assertEquals(emptyMap<String, String>(), NetplayLinkOptions.forSession(session(NetplayRole.HOST, "mgba", usb = true), ""))
        assertEquals(emptyMap<String, String>(), NetplayLinkOptions.forSession(session(NetplayRole.HOST, "snes9x"), null))
    }

    @Test fun ready_modes() {
        assertEquals("rfu", NetplayLinkOptions.hostMode("gpsp", "rfu"))
        assertEquals("auto", NetplayLinkOptions.hostMode("gpsp", null))
        assertEquals("", NetplayLinkOptions.hostMode("mgba", null))
        assertEquals("lockstep", NetplayLinkOptions.joinerMode("gpsp", "auto", "lockstep"))
        assertEquals("rfu", NetplayLinkOptions.joinerMode("gpsp", "rfu", ""))   // the host sent none: keep this device's
        assertEquals("auto", NetplayLinkOptions.joinerMode("gpsp", null, ""))
        assertEquals("", NetplayLinkOptions.joinerMode("mgba", null, ""))
    }

    @Test fun link_sessions_play_on_real_saves() {
        assertFalse(session(NetplayRole.CLIENT, "gpsp").onCopy)
        assertFalse(NetplaySession(NetplayRole.CLIENT, "1.2.3.4", "gambatte", "gambatte", emptySaves = true).onCopy)
        assertFalse(NetplaySession(NetplayRole.HOST, "1.2.3.4", "mgba", "mgba").onCopy)
        assertTrue(NetplaySession(NetplayRole.CLIENT, "1.2.3.4", "snes9x", "snes9x").onCopy)   // lockstep: unchanged
    }
}
