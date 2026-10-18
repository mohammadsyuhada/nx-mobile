package com.nxredux.mobile.achievements

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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nxredux.mobile.LaunchActivity
import com.swordfish.libretrodroid.NxLog

/** The notification's pure parts (unit-tested). */
internal object RaDownloadNotificationText {
    const val TITLE = "Downloading achievement data"

    /** (done, total) once the library is read, or null while the total is unknown. */
    fun progressOf(state: RaState.Prefetching?): Pair<Int, Int>? =
        state?.takeIf { it.total > 0 }?.let { it.done.coerceIn(0, it.total) to it.total }

    /** "3/40 · Sonic the Hedgehog", or "Starting…" before the total is known. */
    fun body(state: RaState.Prefetching?): String {
        val (done, total) = progressOf(state) ?: return "Starting…"
        return if (state?.title.isNullOrBlank()) "$done/$total" else "$done/$total · ${state?.title}"
    }
}

/**
 * The foreground service (type dataSync) that keeps RetroAchievements' "Download all game data" alive off screen, with
 * its progress notification (Pause and Cancel act on [AchievementsService]'s controller; a paused download has no service). Driven only through [update], as
 * ScrapeService is: the first active call starts it (a download is always started by the user in the foreground), later
 * ones update the notification, an inactive one stops it. All state lives on the main thread.
 */
class RaDownloadService : Service() {
    companion object {
        private const val TAG = "NxRaDownloadService"
        private const val CHANNEL = "achievements"
        private const val NOTIFICATION_ID = 4102
        private const val ACTION_CANCEL = "com.nxredux.mobile.achievements.CANCEL"
        private const val ACTION_PAUSE = "com.nxredux.mobile.achievements.PAUSE"

        private val main = Handler(Looper.getMainLooper())
        // Main thread only.
        private var wanted = false
        private var progress: RaState.Prefetching? = null
        private var starting = false
        private var running: RaDownloadService? = null

        /** Start / update while [state] is a download, stop otherwise. Any thread. */
        fun update(context: Context, state: RaState.Prefetching?) {
            val app = context.applicationContext
            main.post { apply(app, state) }
        }

        private fun apply(app: Context, state: RaState.Prefetching?) {
            val active = state != null
            wanted = active; progress = state
            val svc = running
            when {
                svc != null && active -> svc.refresh()
                svc != null -> svc.stop()
                active && !starting -> {
                    starting = true
                    try {
                        ContextCompat.startForegroundService(app, Intent(app, RaDownloadService::class.java))
                    } catch (e: Exception) {   // background start refused (API 31+): the download still runs, unprotected
                        starting = false
                        NxLog.w(TAG, "could not start the download service (${e::class.java.simpleName})")
                    }
                }
                // Inactive before onStartCommand ran: onStartCommand sees [wanted] false and stops at once.
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL || intent?.action == ACTION_PAUSE) {
            if (intent.action == ACTION_PAUSE) AchievementsService.current()?.pause() else AchievementsService.current()?.cancel()
            // An action that arrived with no download to keep alive (the service was re-created by it): do not linger.
            if (!wanted && !starting) stopSelf(startId)
            return START_NOT_STICKY
        }
        // The answer to startForegroundService: startForeground is owed even when the download already ended. Only a
        // service in the foreground is [running]: until then [apply] treats it as starting, so an inactive update in
        // between never stops it before startForeground (a crash on API 28+).
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

    /** API 35: a dataSync service's daily time limit ran out. The download stops (it skips cached games when run again). */
    override fun onTimeout(startId: Int, fgsType: Int) {
        AchievementsService.current()?.cancel()
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
        val counts = RaDownloadNotificationText.progressOf(progress)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(RaDownloadNotificationText.TITLE)
            .setContentText(RaDownloadNotificationText.body(progress))
            .setProgress(counts?.second ?: 0, counts?.first ?: 0, counts == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, flags))
            .addAction(0, "Pause", PendingIntent.getService(this, 2, Intent(this, RaDownloadService::class.java).setAction(ACTION_PAUSE), flags))
            .addAction(0, "Cancel", PendingIntent.getService(this, 1, Intent(this, RaDownloadService::class.java).setAction(ACTION_CANCEL), flags))
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Achievements", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress while achievement data downloads"
                setShowBadge(false)
            })
        }
    }
}
