package com.nxredux.mobile.netplay.usb

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Collections

class UsbLinkEngineTest {
    private class FakeTransport : UsbTransport {
        val written = ArrayList<ByteArray>()
        var closed = false
        override fun read(): UsbFrame.Frame? = null
        override fun write(frame: ByteArray): Boolean { written += frame; return true }
        override fun close() { closed = true }
    }

    private var clock = 0L
    private val events = ArrayList<UsbLinkEngine.Event>()
    private val v = byteArrayOf(UsbHello.VERSION.toByte())
    private fun ipv4(n: Int) = ByteArray(n).also { it[0] = 0x45 }

    private fun engine(side: UsbSide, t: FakeTransport) = UsbLinkEngine(side, t, { clock }) { events += it }

    @Test fun host_says_hello_links_and_relays_both_ways() {
        val t = FakeTransport()
        val e = engine(UsbSide.HOST, t)
        val tun = ByteArrayOutputStream()
        e.attachTun(tun)
        e.tick()
        assertArrayEquals(byteArrayOf(1, 0, UsbFrame.HELLO.toByte(), 2), t.written.single())
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO_ACK, v))
        assertEquals(listOf(UsbLinkEngine.Event.UP), events)
        assertTrue(e.up)

        e.onTunPacket(ipv4(20), 20)
        val data = t.written.last()
        assertEquals(UsbFrame.DATA, UsbFrame.decode(data, data.size)!!.type)
        assertEquals(20, UsbFrame.decode(data, data.size)!!.payload.size)

        e.onFrame(UsbFrame.Frame(UsbFrame.DATA, ipv4(28)))
        assertEquals(28, tun.size())
    }

    @Test fun nothing_is_relayed_before_the_link_is_up() {
        val t = FakeTransport()
        val e = engine(UsbSide.HOST, t)
        val tun = ByteArrayOutputStream()
        e.attachTun(tun)
        e.onTunPacket(ipv4(20), 20)
        e.onFrame(UsbFrame.Frame(UsbFrame.DATA, ipv4(28)))
        assertEquals(0, t.written.size)
        assertEquals(0, tun.size())
    }

    @Test fun non_ipv4_packets_are_dropped() {
        val t = FakeTransport()
        val e = engine(UsbSide.DEVICE, t)
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO, v))
        val before = t.written.size
        e.onTunPacket(ByteArray(40).also { it[0] = 0x60 }, 40)   // IPv6
        e.onTunPacket(ByteArray(0), 0)
        assertEquals(before, t.written.size)
    }

    @Test fun device_answers_hello_with_ack() {
        val t = FakeTransport()
        val e = engine(UsbSide.DEVICE, t)
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO, v))
        assertArrayEquals(byteArrayOf(1, 0, UsbFrame.HELLO_ACK.toByte(), 2), t.written.single())
        assertEquals(listOf(UsbLinkEngine.Event.UP), events)
    }

    @Test fun another_version_is_reported_once() {
        val t = FakeTransport()
        val e = engine(UsbSide.HOST, t)
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO_ACK, byteArrayOf(3)))
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO_ACK, byteArrayOf(3)))
        assertEquals(listOf(UsbLinkEngine.Event.VERSION), events)
    }

    @Test fun silence_then_detach_report_down() {
        val t = FakeTransport()
        val e = engine(UsbSide.HOST, t)
        clock = 10
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO_ACK, v))
        clock = 10 + UsbHello.DEAD_MS
        e.tick()
        assertEquals(listOf(UsbLinkEngine.Event.UP, UsbLinkEngine.Event.DOWN), events)
        e.onFrame(UsbFrame.Frame(UsbFrame.HELLO_ACK, v))
        e.onDetached()
        assertEquals(listOf(UsbLinkEngine.Event.UP, UsbLinkEngine.Event.DOWN, UsbLinkEngine.Event.UP, UsbLinkEngine.Event.DOWN), events)
    }

    @Test fun stop_closes_the_transport() {
        val t = FakeTransport()
        engine(UsbSide.HOST, t).stop()
        assertTrue(t.closed)
    }

    @Test fun a_dead_transport_ends_the_reader_with_down() {
        var reads = 0
        val t = object : UsbTransport {
            override fun read(): UsbFrame.Frame? {
                if (reads++ == 0) return UsbFrame.Frame(UsbFrame.HELLO_ACK, v)
                throw IOException("gone")
            }
            override fun write(frame: ByteArray): Boolean = true
            override fun close() {}
        }
        val seen = Collections.synchronizedList(ArrayList<UsbLinkEngine.Event>())
        val e = UsbLinkEngine(UsbSide.HOST, t, { clock }) { seen += it }
        e.start()
        val deadline = System.currentTimeMillis() + 2000
        while (seen.size < 2 && System.currentTimeMillis() < deadline) Thread.sleep(10)
        Thread.sleep(50)
        e.stop()
        assertEquals(listOf(UsbLinkEngine.Event.UP, UsbLinkEngine.Event.DOWN), seen.toList())
        assertEquals(2, reads)
    }
}
