package com.nxredux.mobile.netplay.usb

import com.swordfish.libretrodroid.NxLog
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

/**
 * The cable's relay, after nx-redux `workspace/all/usblink/usblink_link.c` (GPL-3.0): frames from
 * [transport] go through the [UsbHello] machine; DATA payloads go to the VPN's TUN ([attachTun]); IPv4 packets the VPN
 * reads go out as DATA frames while the link is up; HELLO / HELLO_ACK go out on the machine's word. [onEvent] hears the
 * link come up, a version mismatch (once, though the machine flags every mismatched HELLO or HELLO_ACK), and the link
 * going down. The pumps are plain methods so tests drive them without threads; [start] and [startTun] run them on their
 * own threads until [stop].
 */
class UsbLinkEngine(
    side: UsbSide,
    private val transport: UsbTransport,
    private val now: () -> Long,
    private val onEvent: (Event) -> Unit,
) {
    enum class Event { UP, VERSION, DOWN }

    private val hello = UsbHello(side)
    private val lock = Any()
    @Volatile private var tunOut: OutputStream? = null
    @Volatile private var running = false
    private var versionReported = false

    val up: Boolean get() = synchronized(lock) { hello.phase == UsbHello.Phase.UP }

    /** Where DATA payloads go (the VPN fd's writer); null drops them. */
    fun attachTun(out: OutputStream?) { tunOut = out }

    /** One frame from the cable. */
    fun onFrame(f: UsbFrame.Frame) {
        val acts = synchronized(lock) { hello.onFrame(f.type, f.payload, now()) }
        if (UsbAct.SEND_ACK in acts) sendControl(UsbFrame.HELLO_ACK)
        if (UsbAct.DELIVER in acts) tunOut?.let { o ->
            try {
                synchronized(o) { o.write(f.payload) }
            } catch (e: IOException) {
                NxLog.w(TAG, "tun write failed: ${e.message}")
            }
        }
        report(acts)
    }

    /** Time passing: HELLO on the host side's cadence, DOWN after 3 s of silence. */
    fun tick() {
        val acts = synchronized(lock) { hello.tick(now()) }
        if (UsbAct.SEND_HELLO in acts) sendControl(UsbFrame.HELLO)
        report(acts)
    }

    /** One packet the VPN read ([n] bytes of [buf]): IPv4 only, as one DATA frame, while the link is up. */
    fun onTunPacket(buf: ByteArray, n: Int) {
        if (n <= 0 || (buf[0].toInt() ushr 4) and 0xf != 4) return
        if (!up) return
        UsbFrame.encode(UsbFrame.DATA, buf, 0, n)?.let { transport.write(it) }
    }

    /** The cable is gone for good. */
    fun onDetached() = report(synchronized(lock) { hello.onDetached() })

    /** The cable reader and the [TICK_MS] ticker, each on its own thread, until [stop]. */
    fun start() {
        running = true
        thread(name = "nx-usb-rx", isDaemon = true) {
            while (running) {
                val f = try {
                    transport.read()
                } catch (e: IOException) {
                    if (running) onDetached()
                    break
                }
                if (f != null) onFrame(f)
            }
        }
        thread(name = "nx-usb-tick", isDaemon = true) {
            while (running) {
                tick()
                try { Thread.sleep(TICK_MS) } catch (_: InterruptedException) { break }
            }
        }
    }

    /**
     * The VPN reader on its own thread: one packet per read, until [stop] or the fd closes. [input] must not block for
     * long (Android's [PollingTunInput] returns 0 between polls) so the loop sees [stop].
     */
    fun startTun(input: InputStream) {
        thread(name = "nx-usb-tun", isDaemon = true) {
            val buf = ByteArray(TUN_READ_MAX)
            while (running) {
                val n = try { input.read(buf) } catch (e: IOException) { break }
                if (n < 0) break
                onTunPacket(buf, n)
            }
        }
    }

    /** Stops the threads and closes the transport (the VPN fd is the cable's to close). */
    fun stop() {
        running = false
        tunOut = null
        runCatching { transport.close() }
    }

    private fun sendControl(type: Int) {
        UsbFrame.encode(type, byteArrayOf(UsbHello.VERSION.toByte()))?.let { transport.write(it) }
    }

    private fun report(acts: Set<UsbAct>) {
        if (UsbAct.UP in acts) onEvent(Event.UP)
        if (UsbAct.ERROR in acts && firstVersionError()) onEvent(Event.VERSION)
        if (UsbAct.DOWN in acts) onEvent(Event.DOWN)
    }

    /** True only the first time: the machine's error is sticky, so one report is enough. */
    private fun firstVersionError(): Boolean = synchronized(lock) {
        if (versionReported) false else { versionReported = true; true }
    }

    private companion object {
        const val TAG = "NxUsbLink"
        const val TICK_MS = 50L
        const val TUN_READ_MAX = 32767
    }
}
