package com.nxredux.mobile.netplay.usb

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import java.io.FileDescriptor
import java.io.IOException
import java.io.InputStream

/**
 * The VPN's TUN as [UsbLinkEngine.startTun]'s input, never blocking for longer than [POLL_MS]: a read polls the fd and
 * returns 0 on a timeout, so the engine's reader rechecks its stop between polls (a read blocked on the TUN may not wake
 * when another thread closes the fd). A closed fd (EBADF, or POLLNVAL / POLLHUP / POLLERR) ends the stream.
 */
class PollingTunInput(private val fd: FileDescriptor) : InputStream() {
    override fun read(): Int {
        val one = ByteArray(1)
        while (true) {
            val n = read(one, 0, 1)
            if (n != 0) return if (n < 0) -1 else one[0].toInt() and 0xff
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val p = StructPollfd().apply {
            this.fd = this@PollingTunInput.fd
            events = OsConstants.POLLIN.toShort()
        }
        try {
            if (Os.poll(arrayOf(p), POLL_MS) == 0) return 0
            val r = p.revents.toInt()
            if (r and OsConstants.POLLIN != 0) return Os.read(fd, b, off, len)
            if (r and (OsConstants.POLLNVAL or OsConstants.POLLHUP or OsConstants.POLLERR) != 0) return -1
            return 0
        } catch (e: ErrnoException) {
            if (e.errno == OsConstants.EINTR || e.errno == OsConstants.EAGAIN) return 0
            throw IOException(e)
        }
    }

    private companion object {
        const val POLL_MS = 250
    }
}
