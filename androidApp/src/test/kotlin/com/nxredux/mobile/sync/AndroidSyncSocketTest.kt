package com.nxredux.mobile.sync

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class AndroidSyncSocketTest {
    private fun bound() = DatagramSocket(null).apply { reuseAddress = true; bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0)) }

    @Test fun send_and_receive_keep_a_nul_trailer_and_report_the_source() = runBlocking {
        val a = bound(); val b = bound()
        val sa = AndroidSyncSocket(a, broadcastTo = InetAddress.getByName("127.0.0.1"), port = b.localPort)
        val sb = AndroidSyncSocket(b, broadcastTo = InetAddress.getByName("127.0.0.1"), port = a.localPort)
        sa.send("HELLO_TRIMUI_SYNC:127.0.0.1:0\u0000v2", "127.0.0.1")
        val got = sb.receive(2_000)!!
        assertEquals("HELLO_TRIMUI_SYNC:127.0.0.1:0\u0000v2", got.text)
        assertEquals("127.0.0.1", got.sourceIp)
        sb.broadcast("TRIMUI_SYNC_READY")
        assertEquals("TRIMUI_SYNC_READY", sa.receive(2_000)!!.text)
        assertNull(sa.receive(100))
        sa.close(); sb.close()
    }

    @Test fun close_is_idempotent_and_runs_on_close_once() {
        var released = 0
        val s = AndroidSyncSocket(bound(), broadcastTo = InetAddress.getByName("127.0.0.1"), onClose = { released++ })
        s.close(); s.close()
        assertEquals(1, released)
    }
}
