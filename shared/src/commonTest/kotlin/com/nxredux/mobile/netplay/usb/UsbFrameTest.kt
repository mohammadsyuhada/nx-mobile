package com.nxredux.mobile.netplay.usb

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UsbFrameTest {
    @Test fun every_length_round_trips_and_ends_short() {
        val payload = ByteArray(UsbFrame.PAYLOAD_MAX) { (it * 7).toByte() }
        for (len in 0..1600) {
            val out = assertNotNull(UsbFrame.encode(UsbFrame.DATA, payload, 0, len))
            assertTrue(out.size >= len + UsbFrame.HDR)
            assertTrue(out.size % 64 != 0, "len $len framed to ${out.size}")
            val f = assertNotNull(UsbFrame.decode(out, out.size))
            assertEquals(UsbFrame.DATA, f.type)
            assertContentEquals(payload.copyOf(len), f.payload)
        }
    }

    @Test fun padding_boundaries() {
        val p = ByteArray(1024)
        assertEquals(65, UsbFrame.encode(UsbFrame.DATA, p, 0, 61)!!.size)    // 61 + 3 = 64 -> 65
        assertEquals(513, UsbFrame.encode(UsbFrame.DATA, p, 0, 509)!!.size)  // 509 + 3 = 512 -> 513
        assertEquals(65, UsbFrame.encode(UsbFrame.DATA, p, 0, 62)!!.size)    // 62 + 3 = 65, no pad
        assertEquals(0, UsbFrame.encode(UsbFrame.DATA, p, 0, 61)!![64].toInt())
    }

    @Test fun rejects() {
        val buf = ByteArray(UsbFrame.MAX)
        assertNull(UsbFrame.encode(UsbFrame.DATA, buf, 0, UsbFrame.PAYLOAD_MAX + 1))
        assertNull(UsbFrame.encode(UsbFrame.DATA, buf, 0, -1))
        assertNull(UsbFrame.decode(buf, 2))                                    // shorter than the header
        buf[0] = 10; buf[1] = 0; buf[2] = UsbFrame.DATA.toByte()
        assertNull(UsbFrame.decode(buf, 8))                                    // claims 10, carries 5
        buf[0] = 1; buf[1] = 0
        assertNull(UsbFrame.decode(buf, 7))                                    // claims 1, 3 trailing bytes
        buf[0] = 0; buf[1] = 0; buf[2] = 0
        assertNull(UsbFrame.decode(buf, 3))                                    // unknown type 0
        buf[2] = 6
        assertNull(UsbFrame.decode(buf, 3))                                    // unknown type 6
    }

    @Test fun control_frames() {
        val out = assertNotNull(UsbFrame.encode(UsbFrame.HELLO, byteArrayOf(2)))
        assertContentEquals(byteArrayOf(1, 0, 2, 2), out)
        val f = assertNotNull(UsbFrame.decode(out, out.size))
        assertEquals(UsbFrame.HELLO, f.type)
        assertContentEquals(byteArrayOf(2), f.payload)
        val confirm = UsbFrame.encode(UsbFrame.CONFIRM, byteArrayOf(1))!!
        assertEquals(UsbFrame.CONFIRM, UsbFrame.decode(confirm, confirm.size)!!.type)
        assertTrue(UsbFrame.encode(UsbFrame.SIO, ByteArray(UsbFrame.SIO_MAX))!!.size <= 512)
    }

    @Test fun stream_reassembles_frames_split_byte_by_byte() {
        val frames = listOf(
            UsbFrame.encode(UsbFrame.HELLO_ACK, byteArrayOf(2))!!,
            UsbFrame.encode(UsbFrame.DATA, ByteArray(61) { it.toByte() })!!,   // padded to 65
            UsbFrame.encode(UsbFrame.DATA)!!,                                  // empty payload
            UsbFrame.encode(UsbFrame.DATA, ByteArray(1500) { 0x45 })!!,
        )
        val all = frames.reduce { a, b -> a + b }
        val s = UsbFrameStream()
        val got = ArrayList<UsbFrame.Frame>()
        for (b in all) got += s.feed(byteArrayOf(b))
        assertEquals(listOf(UsbFrame.HELLO_ACK, UsbFrame.DATA, UsbFrame.DATA, UsbFrame.DATA), got.map { it.type })
        assertContentEquals(ByteArray(61) { it.toByte() }, got[1].payload)
        assertEquals(0, got[2].payload.size)
        assertEquals(1500, got[3].payload.size)
        assertFalse(s.broken)
    }

    @Test fun stream_returns_several_frames_from_one_read_and_keeps_the_tail() {
        val a = UsbFrame.encode(UsbFrame.DATA, byteArrayOf(1, 2, 3))!!
        val b = UsbFrame.encode(UsbFrame.DATA, byteArrayOf(4, 5))!!
        val s = UsbFrameStream()
        val joined = a + b.copyOf(2)
        assertEquals(1, s.feed(joined).size)                  // a, then b's first 2 bytes wait
        val rest = s.feed(b.copyOfRange(2, b.size))
        assertContentEquals(byteArrayOf(4, 5), rest.single().payload)
    }

    @Test fun stream_with_a_garbled_header_is_broken() {
        val s = UsbFrameStream()
        assertTrue(s.feed(byteArrayOf(0xFF.toByte(), 0x7F, 1)).isEmpty())   // length 32767
        assertTrue(s.broken)
        assertTrue(s.feed(UsbFrame.encode(UsbFrame.DATA, byteArrayOf(1))!!).isEmpty())   // stays broken
        val t = UsbFrameStream()
        assertTrue(t.feed(byteArrayOf(1, 0, 9, 0)).isEmpty())                  // type 9
        assertTrue(t.broken)
    }
}
