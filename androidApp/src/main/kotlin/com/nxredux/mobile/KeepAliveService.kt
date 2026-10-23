package com.nxredux.mobile

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
import com.swordfish.libretrodroid.NxLog

/** What an update does to a keep-alive service (pure, unit-tested): see [KeepAlive.update]. */
internal enum class KeepAliveStep {
    /** In the foreground and still wanted: the notification shows the new progress. */
    REFRESH,
    /** In the foreground, no longer wanted: it leaves the foreground and stops. */
    STOP,
    /** Wanted, none running and none starting: startForegroundService. */
    START,
    /** Not wanted and none running, or a start already in flight (its onStartCommand reads [KeepAlive.wanted]). */
    NONE;

    companion object {
        fun of(running: Boolean, active: Boolean, starting: Boolean): KeepAliveStep = when {
            running && active -> REFRESH
            running -> STOP
            active && !starting -> START
            // Inactive before onStartCommand ran: onStartCommand sees [wanted] false and stops at once.
            else -> NONE
        }
    }
}

/**
 * One keep-alive service's process state, held in its class's companion (Android builds the instances; the state
 * outlives them): whether a job wants the service, the job's last progress, a start in flight and the instance in the
 * foreground. Driven only through [update]. All state lives on the main thread. [what]: the job in a log line ("fetch").
 */
class KeepAlive<P>(private val service: Class<out KeepAliveService<P>>, internal val tag: String, private val what: String) {
    private val main = Handler(Looper.getMainLooper())
    // Main thread only.
    internal var wanted = false
    internal var progress: P? = null
    internal var starting = false
    internal var running: KeepAliveService<P>? = null

    /** A job wants the service (its last [update] was active). Main thread only. */
    val active: Boolean get() = wanted

    /** The job's side: start / update on [active] (the notification shows [progress]), stop otherwise. Any thread. */
    fun update(context: Context, active: Boolean, progress: P?) {
        val app = context.applicationContext
        main.post { apply(app, active, progress) }
    }

    private fun apply(app: Context, active: Boolean, p: P?) {
        wanted = active; progress = p
        val svc = running
        when (KeepAliveStep.of(svc != null, active, starting)) {
            KeepAliveStep.REFRESH -> svc?.refresh()
            KeepAliveStep.STOP -> svc?.stop()
            KeepAliveStep.START -> {
                starting = true
                try {
                    ContextCompat.startForegroundService(app, Intent(app, service))
                } catch (e: Exception) {   // background start refused (API 31+): the job still runs, unprotected
                    starting = false
                    NxLog.w(tag, "could not start the $what service (${e::class.java.simpleName})")
                }
            }
            KeepAliveStep.NONE -> {}
        }
    }
}

/**
 * The foreground service (type dataSync) that keeps a user-started job alive off screen, with its progress
 * notification (opens the app; its [actions] act on the job's controller). The first active [KeepAlive.update] starts
 * it (a job is always started by the user in the foreground), later ones update the notification, an inactive one
 * stops it. Each job's service gives only what differs: its state, notification id, channel, actions, timeout and the
 * notification's title, text and progress.
 */
abstract class KeepAliveService<P> : Service() {
    /** The notification channel: its id, name and description in the system's settings. */
    protected class Channel(val id: String, val name: String, val description: String)

    /** A notification button: [label], the intent [action] it sends here, its PendingIntent [requestCode] and what it does. */
    protected class Action(val label: String, val action: String, val requestCode: Int, val run: () -> Unit)

    /** The companion's state. */
    protected abstract val keepAlive: KeepAlive<P>
    /** From [NotificationIds]. */
    protected abstract val notificationId: Int
    protected abstract val channel: Channel
    /** The notification's buttons, in order. */
    protected abstract val actions: List<Action>

    /** API 35: a dataSync service's daily time limit ran out. Pause or cancel the job; the service then stops. */
    protected abstract fun onTimedOut()

    /** The notification's title, text and progress for the job's last [progress]. */
    protected abstract fun NotificationCompat.Builder.content(progress: P?): NotificationCompat.Builder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val state = keepAlive
        val action = actions.firstOrNull { it.action == intent?.action }
        if (action != null) {
            action.run()
            // An action that arrived with no job to keep alive (the service was re-created by it): do not linger.
            if (!state.wanted && !state.starting) stopSelf(startId)
            return START_NOT_STICKY
        }
        // The answer to startForegroundService: startForeground is owed even when the job already ended. Only a
        // service in the foreground is [KeepAlive.running]: until then an update treats it as starting, so an
        // inactive update in between never stops it before startForeground (a crash on API 28+).
        state.starting = false
        try {
            ServiceCompat.startForeground(this, notificationId, build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } catch (e: Exception) {
            NxLog.w(state.tag, "could not enter the foreground (${e::class.java.simpleName})")
            if (state.running === this) state.running = null
            stopSelf(); return START_NOT_STICKY
        }
        state.running = this   // also a start that reached an instance whose stop was still pending
        if (!state.wanted) stop()
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        onTimedOut()
        keepAlive.wanted = false
        stop()
    }

    override fun onDestroy() {
        if (keepAlive.running === this) keepAlive.running = null
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")   // checked through areNotificationsEnabled; a refusal is caught
    internal fun refresh() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(this).notify(notificationId, build())
        } catch (e: SecurityException) {   // POST_NOTIFICATIONS not granted
        }
    }

    internal fun stop() {
        if (keepAlive.running === this) keepAlive.running = null   // a later start builds (or re-commands) a service instead of updating this one
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun build(): Notification {
        ensureChannel()
        val open = Intent(this, LaunchActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val builder = NotificationCompat.Builder(this, channel.id)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .content(keepAlive.progress)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, flags))
        for (a in actions) {
            builder.addAction(0, a.label, PendingIntent.getService(this, a.requestCode, Intent(this, javaClass).setAction(a.action), flags))
        }
        return builder.build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(channel.id) == null) {
            nm.createNotificationChannel(NotificationChannel(channel.id, channel.name, NotificationManager.IMPORTANCE_LOW).apply {
                description = channel.description
                setShowBadge(false)
            })
        }
    }
}
