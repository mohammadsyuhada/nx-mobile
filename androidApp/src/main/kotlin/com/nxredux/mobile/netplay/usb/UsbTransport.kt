package com.nxredux.mobile.netplay.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/** One end of the cable's byte pipe, carrying whole usblink frames. [read] runs on one reader thread; [write] from any. */
interface UsbTransport : Closeable {
    /**
     * The next frame; null when nothing came for a while (or a frame that does not decode); throws IOException once the
     * pipe is gone.
     */
    fun read(): UsbFrame.Frame?
    /** Writes one whole frame; false when it did not go out (the link's 3 s rule notices a dead cable). */
    fun write(frame: ByteArray): Boolean
}

/**
 * This phone as the USB host: bulk transfers on a claimed interface (a handheld's nxlink, or a phone in accessory mode).
 * One IN transfer is one frame, as every frame ends in a short packet. bulkTransfer returns -1 for a timeout and for a
 * failure alike, so a dead cable shows as silence (the HELLO machine's 3 s rule). A detached device fails at once
 * instead of waiting out the timeout, so [FAST_FAIL_LIMIT] quick failures in a row end the reader ([FastFailCounter]).
 */
class DeviceTransport(
    private val connection: UsbDeviceConnection,
    private val iface: UsbInterface,
    private val input: UsbEndpoint,
    private val output: UsbEndpoint,
) : UsbTransport {
    @Volatile private var closed = false
    private val buf = ByteArray(UsbFrame.MAX)
    private val writeLock = Any()
    private val fastFails = FastFailCounter(FAST_FAIL_LIMIT, FAST_FAIL_MS)

    override fun read(): UsbFrame.Frame? {
        if (closed) throw IOException("closed")
        val start = SystemClock.elapsedRealtime()
        val n = connection.bulkTransfer(input, buf, buf.size, READ_TIMEOUT_MS)
        if (closed) throw IOException("closed")
        if (fastFails.gone(n, SystemClock.elapsedRealtime() - start)) throw IOException("usb device gone")
        return if (n <= 0) null else UsbFrame.decode(buf, n)
    }

    override fun write(frame: ByteArray): Boolean = synchronized(writeLock) {
        !closed && connection.bulkTransfer(output, frame, frame.size, WRITE_TIMEOUT_MS) == frame.size
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { connection.releaseInterface(iface) }
        runCatching { connection.close() }
    }

    private companion object {
        /** The handheld's host side reads with 200 ms and writes with 1000 ms (usblink_host.c). */
        const val READ_TIMEOUT_MS = 200
        const val WRITE_TIMEOUT_MS = 1000
        /** A -1 back sooner than this is a failure, not a timeout (usbfs answers ENODEV at once once detached). */
        const val FAST_FAIL_MS = 50L
        /** Fast failures in a row that mean the device is gone. */
        const val FAST_FAIL_LIMIT = 3
    }
}

/**
 * Tells a detached device from a quiet one by how fast bulkTransfer fails: [limit] results of -1 in a row, each back in
 * under [fastMs], mean the device is gone. Any real result, or a -1 that waited out the timeout, starts the count again.
 */
internal class FastFailCounter(private val limit: Int = 3, private val fastMs: Long = 50) {
    private var count = 0

    /** Counts one transfer ([result] bytes or -1, taking [tookMs]); true once the device looks gone. */
    fun gone(result: Int, tookMs: Long): Boolean {
        if (result == -1 && tookMs < fastMs) count++ else count = 0
        return count >= limit
    }
}

/**
 * This phone as an Android Open Accessory (phone ↔ phone): a byte stream, so frames are reassembled from their
 * length headers ([UsbFrameStream]); a stream that loses its place is a dead link. Accessory reads take a 16 KB buffer
 * (shorter reads fail on some devices).
 */
class AccessoryTransport(private val pfd: ParcelFileDescriptor) : UsbTransport {
    private val input = FileInputStream(pfd.fileDescriptor)
    private val output = FileOutputStream(pfd.fileDescriptor)
    private val stream = UsbFrameStream()
    private val ready = ArrayDeque<UsbFrame.Frame>()
    private val buf = ByteArray(16384)
    private val writeLock = Any()
    @Volatile private var closed = false

    override fun read(): UsbFrame.Frame? {
        ready.removeFirstOrNull()?.let { return it }
        if (closed) throw IOException("closed")
        val n = input.read(buf)
        if (n < 0) throw IOException("accessory closed")
        ready.addAll(stream.feed(buf, n))
        if (stream.broken) throw IOException("accessory stream lost its framing")
        return ready.removeFirstOrNull()
    }

    override fun write(frame: ByteArray): Boolean = synchronized(writeLock) {
        if (closed) return false
        try {
            output.write(frame)
            true
        } catch (e: IOException) {
            false
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { pfd.close() }
    }
}
