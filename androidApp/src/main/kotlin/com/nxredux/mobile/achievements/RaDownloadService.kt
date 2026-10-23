package com.nxredux.mobile.achievements

import android.content.Context
import androidx.core.app.NotificationCompat
import com.nxredux.mobile.KeepAlive
import com.nxredux.mobile.KeepAliveService
import com.nxredux.mobile.NotificationIds

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
 * ones update the notification, an inactive one stops it. The state machine is [KeepAliveService]'s.
 */
class RaDownloadService : KeepAliveService<RaState.Prefetching>() {
    companion object {
        private val state = KeepAlive(RaDownloadService::class.java, "NxRaDownloadService", "download")

        /** Start / update while [state] is a download, stop otherwise. Any thread. */
        fun update(context: Context, state: RaState.Prefetching?) = this.state.update(context, state != null, state)

        /** A download is keeping the service alive (the updater's install gate). Main thread only. */
        val active: Boolean get() = state.active
    }

    override val keepAlive get() = state
    override val notificationId = NotificationIds.RA_DOWNLOAD
    override val channel = Channel("achievements", "Achievements", "Progress while achievement data downloads")
    override val actions = listOf(
        Action("Pause", "com.nxredux.mobile.achievements.PAUSE", 2) { AchievementsService.current()?.pause() },
        Action("Cancel", "com.nxredux.mobile.achievements.CANCEL", 1) { AchievementsService.current()?.cancel() },
    )

    /** The download stops (it skips cached games when run again). */
    override fun onTimedOut() { AchievementsService.current()?.cancel() }

    override fun NotificationCompat.Builder.content(progress: RaState.Prefetching?): NotificationCompat.Builder {
        val counts = RaDownloadNotificationText.progressOf(progress)
        return setContentTitle(RaDownloadNotificationText.TITLE)
            .setContentText(RaDownloadNotificationText.body(progress))
            .setProgress(counts?.second ?: 0, counts?.first ?: 0, counts == null)
    }
}
