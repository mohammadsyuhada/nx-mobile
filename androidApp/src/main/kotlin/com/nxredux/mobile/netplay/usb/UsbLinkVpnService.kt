package com.nxredux.mobile.netplay.usb

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.swordfish.libretrodroid.NxLog

/**
 * The cable's IP end: a TUN that owns 10.99.0.0/30 only (all other traffic is untouched), MTU 1500, blocking.
 * The app's own sockets to the peer route through it, and packets written to it reach local listeners. The established
 * fd goes to [handoff] (the cable, same process); [onRevoked] hears Android taking the VPN away.
 */
class UsbLinkVpnService : VpnService() {
    private var pfd: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // stopSelf(startId), never a bare stopSelf(): a quick restart queues STOP then START, and a bare stop from STOP
        // would destroy the service after START's TUN is up (onDestroy closes it). Commands run in order on the main
        // thread, so STOP's closeTun only ever closes the TUN that came before it.
        if (intent?.action == ACTION_STOP) {
            closeTun()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val local = intent?.getStringExtra(EXTRA_LOCAL)
        if (local == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        closeTun()
        val fd = try {
            Builder()
                .setSession(SESSION)
                .addAddress(local, 30)
                .addRoute("10.99.0.0", 30)
                .setMtu(1500)
                .setBlocking(true)
                .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setMetered(false) }
                .establish()   // null: another app's always-on or lockdown VPN holds the slot
        } catch (e: Exception) {
            NxLog.w(TAG, "vpn establish failed", e)
            null
        }
        pfd = fd
        handoff?.invoke(fd)
        if (fd == null) stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        closeTun()
        onRevoked?.invoke()
        stopSelf()
    }

    override fun onDestroy() {
        closeTun()
        super.onDestroy()
    }

    private fun closeTun() {
        pfd?.let { runCatching { it.close() } }
        pfd = null
    }

    companion object {
        private const val TAG = "NxUsbLink"
        const val SESSION = "NX Redux USB link"
        private const val EXTRA_LOCAL = "local"
        private const val ACTION_STOP = "com.nxredux.mobile.USB_LINK_STOP"

        /** Hears the established fd (null when Android refused it); set by the cable before [start]. */
        @Volatile var handoff: ((ParcelFileDescriptor?) -> Unit)? = null
        @Volatile var onRevoked: (() -> Unit)? = null

        fun start(context: Context, localIp: String) {
            context.startService(Intent(context, UsbLinkVpnService::class.java).putExtra(EXTRA_LOCAL, localIp))
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, UsbLinkVpnService::class.java).setAction(ACTION_STOP)) }
        }
    }
}
