package com.nxredux.mobile.netplay.usb

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UsbHelloTest {
    private val v = byteArrayOf(UsbHello.VERSION.toByte())

    @Test fun host_handshake_and_keepalive() {
        val l = UsbHello(UsbSide.HOST)
        assertEquals(setOf(UsbAct.SEND_HELLO), l.tick(1000))    // the first hello goes out at once
        assertEquals(emptySet(), l.tick(1100))                   // < 250 ms
        assertEquals(setOf(UsbAct.SEND_HELLO), l.tick(1250))
        assertEquals(setOf(UsbAct.UP), l.onFrame(UsbFrame.HELLO_ACK, v, 1300))
        assertEquals(UsbHello.Phase.UP, l.phase)
        assertEquals(emptySet(), l.tick(1500))                   // 1 s once up
        assertEquals(setOf(UsbAct.SEND_HELLO), l.tick(2250))
        l.onFrame(UsbFrame.HELLO_ACK, v, 2260)
        assertTrue(UsbAct.SEND_HELLO in l.tick(5000))            // 2740 ms since rx: still up
        assertEquals(UsbHello.Phase.UP, l.phase)
    }

    @Test fun dead_after_silence_then_relinks() {
        val l = UsbHello(UsbSide.HOST)
        l.tick(0)
        l.onFrame(UsbFrame.HELLO_ACK, v, 10)
        assertEquals(setOf(UsbAct.DOWN), l.tick(10 + UsbHello.DEAD_MS))
        assertEquals(UsbHello.Phase.IDLE, l.phase)
        assertTrue(UsbAct.SEND_HELLO in l.tick(10 + UsbHello.DEAD_MS + 250))
        assertTrue(UsbAct.UP in l.onFrame(UsbFrame.HELLO_ACK, v, 4000))
    }

    @Test fun device_side_answers_and_never_sends_hellos() {
        val l = UsbHello(UsbSide.DEVICE)
        assertEquals(setOf(UsbAct.SEND_ACK, UsbAct.UP), l.onFrame(UsbFrame.HELLO, v, 100))
        assertEquals(emptySet(), l.tick(2000))
        assertEquals(setOf(UsbAct.SEND_ACK), l.onFrame(UsbFrame.HELLO, v, 1100))   // keepalive refreshes
        assertEquals(setOf(UsbAct.DOWN), l.tick(1100 + UsbHello.DEAD_MS))
    }

    @Test fun data_only_once_up() {
        val l = UsbHello(UsbSide.DEVICE)
        val pkt = ByteArray(20) { 0x45 }
        assertEquals(emptySet(), l.onFrame(UsbFrame.DATA, pkt, 5))
        l.onFrame(UsbFrame.HELLO, v, 10)
        assertEquals(setOf(UsbAct.DELIVER), l.onFrame(UsbFrame.DATA, pkt, 20))
    }

    @Test fun the_wrong_direction_is_ignored() {
        assertEquals(emptySet(), UsbHello(UsbSide.HOST).onFrame(UsbFrame.HELLO, v, 10))       // a host never acks
        assertEquals(emptySet(), UsbHello(UsbSide.DEVICE).onFrame(UsbFrame.HELLO_ACK, v, 10))
    }

    @Test fun version_mismatch_is_a_sticky_error() {
        val bad = byteArrayOf((UsbHello.VERSION + 1).toByte())
        val d = UsbHello(UsbSide.DEVICE)
        assertEquals(setOf(UsbAct.SEND_ACK, UsbAct.ERROR), d.onFrame(UsbFrame.HELLO, bad, 10))
        assertEquals(UsbHello.Phase.ERROR, d.phase)
        assertEquals(UsbHello.VERSION + 1, d.peerVersion)
        val h = UsbHello(UsbSide.HOST)
        assertEquals(setOf(UsbAct.ERROR), h.onFrame(UsbFrame.HELLO_ACK, bad, 10))
        assertEquals(emptySet(), h.tick(9999))                                      // terminal
        assertEquals(emptySet(), UsbHello(UsbSide.DEVICE).onFrame(UsbFrame.HELLO, ByteArray(0), 10))   // truncated: ignored
    }

    @Test fun detach_drops_a_link_that_was_up() {
        val l = UsbHello(UsbSide.HOST)
        l.onFrame(UsbFrame.HELLO_ACK, v, 10)
        assertEquals(setOf(UsbAct.DOWN), l.onDetached())
        assertEquals(emptySet(), l.onDetached())
    }

    @Test fun sio_counts_as_traffic_only_when_up_and_in_size() {
        val l = UsbHello(UsbSide.DEVICE)
        assertEquals(emptySet(), l.onFrame(UsbFrame.SIO, ByteArray(25), 5))
        l.onFrame(UsbFrame.HELLO, v, 10)
        assertEquals(setOf(UsbAct.SIO), l.onFrame(UsbFrame.SIO, ByteArray(25), 2000))
        assertEquals(emptySet(), l.onFrame(UsbFrame.SIO, ByteArray(0), 2020))
        assertEquals(emptySet(), l.onFrame(UsbFrame.SIO, ByteArray(UsbFrame.SIO_MAX + 1), 2030))
        assertEquals(emptySet(), l.tick(2000 + UsbHello.DEAD_MS - 1))   // the SIO at 2000 kept it alive
        assertEquals(emptySet(), UsbHello(UsbSide.DEVICE).onFrame(UsbFrame.CONFIRM, v, 10))
    }

    @Test fun addresses_follow_the_side() {
        assertEquals("10.99.0.1", UsbHello.localIp(UsbSide.HOST))
        assertEquals("10.99.0.2", UsbHello.peerIp(UsbSide.HOST))
        assertEquals("10.99.0.2", UsbHello.localIp(UsbSide.DEVICE))
        assertEquals("10.99.0.1", UsbHello.peerIp(UsbSide.DEVICE))
    }
}
