package com.nxredux.mobile.sync

import com.nxredux.mobile.netplay.NetplayAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Device Sync's UDP socket on one link's network ([socket] already bound and, on a device, bound to the Wi-Fi or cable
 * `Network`). Text is ISO-8859-1 so the handheld's NUL + `v2` trailer survives byte for byte. [onClose]: release the
 * Wi-Fi multicast lock (run once, however often [close] is called). [port] is the peer's port: [SyncWire.UDP_PORT]
 * except in tests.
 */
class AndroidSyncSocket(
    private val socket: DatagramSocket,
    private val broadcastTo: InetAddress,
    private val port: Int = SyncWire.UDP_PORT,
    private val onClose: () -> Unit = {},
) : SyncSocket {
    private val closed = AtomicBoolean(false)

    override suspend fun send(text: String, ip: String) = withContext(Dispatchers.IO) {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip), port))
    }

    override suspend fun broadcast(text: String) = withContext(Dispatchers.IO) {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        socket.send(DatagramPacket(bytes, bytes.size, broadcastTo, port))
    }

    override suspend fun receive(timeoutMs: Long): SyncDatagram? = withContext(Dispatchers.IO) {
        val buf = ByteArray(512)
        val packet = DatagramPacket(buf, buf.size)
        socket.soTimeout = timeoutMs.coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
        try { socket.receive(packet) } catch (_: SocketTimeoutException) { return@withContext null }
        // A non-IPv4 source can't be a peer: dropped like silence (the controller's poll just goes round again).
        val ip = NetplayAddress.ipv4(packet.address?.address) ?: return@withContext null
        SyncDatagram(String(packet.data, 0, packet.length, Charsets.ISO_8859_1), ip)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        socket.close()
        onClose()
    }
}
