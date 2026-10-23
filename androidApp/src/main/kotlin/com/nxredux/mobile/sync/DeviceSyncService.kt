package com.nxredux.mobile.sync

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nxredux.mobile.LaunchActivity
import com.nxredux.mobile.NotificationIds
import com.nxredux.mobile.runner.GameActivity
import com.nxredux.mobile.storage.AndroidSaveTransfer
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * The process's one Device Sync controller (a sync outlives the screen, as an art fetch does). Its scope runs on
 * [Dispatchers.Main.immediate]: button handling and the controller's [DeviceSyncPlatform.gameScreenAlive] check (which
 * reads GameActivity's main-thread state) run on the main thread; the platform moves its I/O to IO itself.
 */
object DeviceSyncs {
    private const val TAG = "NxSync"
    @Volatile private var controller: DeviceSyncController? = null
    /**
     * MainActivity's keep-screen-on for all its live instances, set in onCreate; called from any thread (the callee
     * posts to the UI thread).
     */
    @Volatile var screenOn: (Boolean) -> Unit = {}
    /**
     * Main thread only: some GameActivity holds the session (what LibretroRunner.gameScreenAlive reads; process state,
     * so no activity has to set it or let go of it).
     */
    @Volatile var gameScreenAlive: () -> Boolean = { GameActivity.gameScreenAlive }
    /** An Import (from any MainActivity's AndroidSaveTransfer) is writing saves; process state, like [gameScreenAlive]. */
    @Volatile var importing: () -> Boolean = { AndroidSaveTransfer.anyImporting }
    /** MainActivity's POST_NOTIFICATIONS request, asked once when the first sync starts (the sync runs either way). */
    @Volatile var onNeedNotificationPermission: (() -> Unit)? = null
    @Volatile private var askedPermission = false

    fun current(): DeviceSyncController? = controller

    fun get(context: Context): DeviceSyncController = controller ?: synchronized(this) {
        controller ?: run {
            val app = context.applicationContext
            val platform = AndroidDeviceSync(app, { gameScreenAlive() }, { importing() }) { active, text ->
                if (active && !askedPermission) onNeedNotificationPermission?.let { ask -> askedPermission = true; ask() }
                DeviceSyncService.update(app, active, text)
                screenOn(active)
            }
            // A throwing step must not take the process down (the controller turns expected failures into pages).
            val errors = CoroutineExceptionHandler { _, e -> NxLog.e(TAG, "device sync failed", e) }
            // `now` only measures intervals (HELLO/READY pacing, the server's wait): the monotonic clock.
            DeviceSyncController(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + errors), platform) { SystemClock.elapsedRealtime() }
        }.also { controller = it }
    }
}

/**
 * The foreground service (type dataSync) that keeps a Device Sync alive, with its notification (Cancel stops the sync
 * through [DeviceSyncs]). Driven only through [update]: the first active call starts it (a sync is always started by
 * the user in the foreground), later ones update the notification, an inactive one stops it. All state lives on the
 * main thread. The same structure as [com.nxredux.mobile.scraper.ScrapeService].
 */
class DeviceSyncService : Service() {
    companion object {
        private const val TAG = "NxSyncService"
        private const val CHANNEL = "device-sync"
        private const val NOTIFICATION_ID = NotificationIds.DEVICE_SYNC
        private const val ACTION_CANCEL = "com.nxredux.mobile.sync.CANCEL"
        private const val TITLE = "Device Sync"

        private val main = Handler(Looper.getMainLooper())
        // Main thread only.
        private var wanted = false
        private var text: String? = null
        private var starting = false
        private var running: DeviceSyncService? = null

        /** The controller side ([DeviceSyncPlatform.keepAlive]): start / update on [active], stop otherwise. Any thread. */
        fun update(context: Context, active: Boolean, text: String?) {
            val app = context.applicationContext
            main.post { apply(app, active, text) }
        }

        private fun apply(app: Context, active: Boolean, status: String?) {
            wanted = active; text = status
            val svc = running
            when {
                svc != null && active -> svc.refresh()
                svc != null -> svc.stop()
                active && !starting -> {
                    starting = true
                    try {
                        ContextCompat.startForegroundService(app, Intent(app, DeviceSyncService::class.java))
                    } catch (e: Exception) {   // background start refused (API 31+): the sync still runs, unprotected
                        starting = false
                        NxLog.w(TAG, "could not start the sync service (${e::class.java.simpleName})")
                    }
                }
                // Inactive before onStartCommand ran: onStartCommand sees [wanted] false and stops at once.
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> DeviceSyncs.current()?.cancel()
            else -> {
                // The answer to startForegroundService: startForeground is owed even when the sync already ended.
                // Only a service in the foreground is [running]: until then [apply] treats it as starting, so an
                // inactive update in between never stops it before startForeground (a crash on API 28+).
                starting = false
                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } catch (e: Exception) {
                    NxLog.w(TAG, "could not enter the foreground (${e::class.java.simpleName})")
                    if (running === this) running = null
                    stopSelf(); return START_NOT_STICKY
                }
                running = this   // also a start that reached an instance whose stop was still pending
                if (!wanted) stop()
                return START_NOT_STICKY
            }
        }
        // An action that arrived with no sync to keep alive (the service was re-created by it): do not linger.
        if (!wanted && !starting) stopSelf(startId)
        return START_NOT_STICKY
    }

    /** API 35: a dataSync service's daily time limit ran out. Stop the sync (as Cancel) and the service. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        DeviceSyncs.current()?.cancel()
        wanted = false
        stop()
    }

    override fun onDestroy() {
        if (running === this) running = null
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")   // checked through areNotificationsEnabled; a refusal is caught
    private fun refresh() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, build())
        } catch (e: SecurityException) {   // POST_NOTIFICATIONS not granted
        }
    }

    private fun stop() {
        if (running === this) running = null   // a later start builds (or re-commands) a service instead of updating this one
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun build(): Notification {
        ensureChannel()
        val open = Intent(this, LaunchActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(TITLE)
            .setContentText(text?.takeIf { it.isNotBlank() } ?: TITLE)
            .setProgress(0, 0, true)   // no file count across steps: indeterminate while syncing
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, flags))
            .addAction(0, "Cancel", PendingIntent.getService(this, 2, Intent(this, DeviceSyncService::class.java).setAction(ACTION_CANCEL), flags))
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Device Sync", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows a running Device Sync"
                setShowBadge(false)
            })
        }
    }
}
