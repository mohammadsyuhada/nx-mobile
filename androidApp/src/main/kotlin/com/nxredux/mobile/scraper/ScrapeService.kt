package com.nxredux.mobile.scraper

import android.content.Context
import androidx.core.app.NotificationCompat
import com.nxredux.mobile.KeepAlive
import com.nxredux.mobile.KeepAliveService
import com.nxredux.mobile.NotificationIds

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
 * The state machine is [KeepAliveService]'s.
 */
class ScrapeService : KeepAliveService<String>() {
    companion object {
        private val state = KeepAlive(ScrapeService::class.java, "NxScrapeService", "fetch")

        /** The [ScrapeRunner] side: start / update on [active], stop otherwise. Any thread. */
        fun update(context: Context, active: Boolean, text: String?) = state.update(context, active, text)
    }

    override val keepAlive get() = state
    override val notificationId = NotificationIds.SCRAPE
    override val channel = Channel("artwork", "Artwork", "Progress while artwork is fetched")
    override val actions = listOf(
        Action("Pause", "com.nxredux.mobile.scraper.PAUSE", 1) { ArtworkService.current()?.pause() },
        Action("Cancel", "com.nxredux.mobile.scraper.CANCEL", 2) { ArtworkService.current()?.cancel() },
    )

    /** Pause the job (it resumes from the app) and stop. */
    override fun onTimedOut() { ArtworkService.current()?.pause() }

    override fun NotificationCompat.Builder.content(progress: String?): NotificationCompat.Builder {
        val counts = ScrapeNotificationText.progressOf(progress)
        return setContentTitle(ScrapeNotificationText.TITLE)
            .setContentText(ScrapeNotificationText.body(progress))
            .setProgress(counts?.second ?: 0, counts?.first ?: 0, counts == null)
    }
}
