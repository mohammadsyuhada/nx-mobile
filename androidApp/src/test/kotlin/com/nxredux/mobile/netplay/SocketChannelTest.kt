package com.nxredux.mobile.netplay

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket

class SocketChannelTest {
    private fun pair(): Pair<SocketChannel, SocketChannel> {
        val server = ServerSocket(0)
        val client = Socket("127.0.0.1", server.localPort)
        val accepted = server.accept(); server.close()
        return SocketChannel(accepted) to SocketChannel(client)
    }

    @Test fun two_lines_in_one_segment_share_one_buffer() = runBlocking {
        val (a, b) = pair()
        // two lines sent in the same write: the reader must not lose the bytes buffered after the first '\n'
        a.writeLine("FILE nxsave.srm\nSYNC-DONE")
        assertEquals("FILE nxsave.srm", b.readLine(1000))
        assertEquals("SYNC-DONE", b.readLine(1000))
        a.close(); b.close()
    }

    @Test fun timeout_and_close_messages() = runBlocking {
        val (a, b) = pair()
        assertEquals("timeout", assertThrows(NetplayIoException::class.java) { runBlocking { b.readLine(100) } }.message)
        a.close()
        assertEquals("closed", assertThrows(NetplayIoException::class.java) { runBlocking { b.readLine(1000) } }.message)
        b.close()
    }

    @Test fun overlong_line_is_an_error() = runBlocking {
        val (a, b) = pair()
        a.writeLine("x".repeat(600))
        assertThrows(NetplayIoException::class.java) { runBlocking { b.readLine(1000) } }
        a.close(); b.close()
    }

    @Test fun remote_address_is_dotted_ipv4() {
        val (a, b) = pair()
        assertEquals("127.0.0.1", a.remoteAddress)
        assertEquals("127.0.0.1", b.remoteAddress)
        a.close(); b.close()
    }

    @Test fun ipv4_mapped_addresses_normalise_to_dotted_ipv4() {
        assertEquals("192.168.1.20", NetplayAddress.ipv4("192.168.1.20"))
        assertEquals("192.168.1.20", NetplayAddress.ipv4("::ffff:192.168.1.20"))
        assertEquals("192.168.1.20", NetplayAddress.ipv4("::FFFF:192.168.1.20"))
        assertEquals("192.168.1.20", NetplayAddress.ipv4("0:0:0:0:0:ffff:192.168.1.20"))
        assertEquals("192.168.1.20", NetplayAddress.ipv4("0:0:0:0:0:ffff:c0a8:114"))   // the hex form of the same
        assertEquals("10.0.0.2", NetplayAddress.ipv4("::ffff:10.0.0.2%wlan0"))
        val mapped = ByteArray(16).also { it[10] = -1; it[11] = -1; it[12] = 10; it[15] = 2 }
        assertEquals("10.0.0.2", NetplayAddress.ipv4(mapped))
        assertEquals("10.0.0.2", NetplayAddress.ipv4(byteArrayOf(10, 0, 0, 2)))
    }

    @Test fun anything_but_ipv4_is_refused() {
        for (bad in listOf("", "fe80::1", "::1", "2001:db8::ffff:10.0.0.2", "::ffff:", "10.0.0", "10.0.0.256", "a.b.c.d", "1.2.3.4.5", " ", "localhost"))
            assertNull(bad, NetplayAddress.ipv4(bad))
        assertNull(NetplayAddress.ipv4(null as String?))
        assertNull(NetplayAddress.ipv4(ByteArray(16).also { it[15] = 1 }))   // ::1
        assertNull(NetplayAddress.ipv4(null as ByteArray?))
    }
}
