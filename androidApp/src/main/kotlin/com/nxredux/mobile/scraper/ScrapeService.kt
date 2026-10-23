package com.nxredux.mobile.scraper

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
import com.nxredux.mobile.NotificationIds
import com.swordfish.libretrodroid.NxLog

/** The notification's pure parts (unit-tested). */
internal object ScrapeNotificationText {
    const val TITLE = "Fetching artwork"
    private val COUNT = Regex("""(\d+)\s*/\s*(\d+)""")

    /** The (done, total) in the controller's progress text ("Fetching art 3/10"), or null. */
    fun progressOf(text: String?): Pair<Int, Int>? {
        val m = text?.let { COUNT.find(it) } ?: return null
        val done = m.groupValues[1].toIntOrNull() ?: return null; val total = m.groupValues[2].toIntOrNull() ?: return null
        return if (total <= 0) null else done.coerceIn(0, total) to total
    }

    fun body(text: String?): String = text?.takeIf { it.isNotBlank() } ?: "Starting…"
}

/**
 * The foreground service (type dataSync) that keeps an artwork fetch alive, with its progress notification (Pause and
 * Cancel act on [ArtworkService]'s controller). Driven only through [update]: the first active call starts it (a
 * fetch is always started by the user in the foreground), later ones update the notification, an inactive one stops it.
 * All state lives on the main thread.
 */
class ScrapeService : Service() {
    companion object {
        private const val TAG = "NxScrapeService"
        private const val CHANNEL = "artwork"
        private const val NOTIFICATION_ID = NotificationIds.SCRAPE
        private const val ACTION_PAUSE = "com.nxredux.mobile.scraper.PAUSE"
        private const val ACTION_CANCEL = "com.nxredux.mobile.scraper.CANCEL"

        private val main = Handler(Looper.getMainLooper())
        // Main thread only.
        private var wanted = false
        private var text: String? = null
        private var starting = false
        private var running: ScrapeService? = null

        /** The [ScrapeRunner] side: start / update on [active], stop otherwise. Any thread. */
        fun update(context: Context, active: Boolean, text: String?) {
            val app = context.applicationContext
            main.post { apply(app, active, text) }
        }

        private fun apply(app: Context, active: Boolean, progress: String?) {
            wanted = active; text = progress
            val svc = running
            when {
                svc != null && active -> svc.refresh()
                svc != null -> svc.stop()
                active && !starting -> {
                    starting = true
                    try {
                        ContextCompat.startForegroundService(app, Intent(app, ScrapeService::class.java))
                    } catch (e: Exception) {   // background start refused (API 31+): the job still runs, unprotected
                        starting = false
                        NxLog.w(TAG, "could not start the fetch service (${e::class.java.simpleName})")
                    }
                }
                // Inactive before onStartCommand ran: onStartCommand sees [wanted] false and stops at once.
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> ArtworkService.current()?.pause()
            ACTION_CANCEL -> ArtworkService.current()?.cancel()
            else -> {
                // The answer to startForegroundService: startForeground is owed even when the job already ended.
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
        // An action that arrived with no fetch to keep alive (the service was re-created by it): do not linger.
        if (!wanted && !starting) stopSelf(startId)
        return START_NOT_STICKY
    }

    /** API 35: a dataSync service's daily time limit ran out. Pause the job (it resumes from the app) and stop. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        ArtworkService.current()?.pause()
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
        val progress = ScrapeNotificationText.progressOf(text)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(ScrapeNotificationText.TITLE)
            .setContentText(ScrapeNotificationText.body(text))
            .setProgress(progress?.second ?: 0, progress?.first ?: 0, progress == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, flags))
            .addAction(0, "Pause", PendingIntent.getService(this, 1, Intent(this, ScrapeService::class.java).setAction(ACTION_PAUSE), flags))
            .addAction(0, "Cancel", PendingIntent.getService(this, 2, Intent(this, ScrapeService::class.java).setAction(ACTION_CANCEL), flags))
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Artwork", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress while artwork is fetched"
                setShowBadge(false)
            })
        }
    }
}
