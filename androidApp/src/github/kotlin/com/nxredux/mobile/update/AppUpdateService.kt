package com.nxredux.mobile.update

import android.content.Context
import androidx.core.app.NotificationCompat
import com.nxredux.mobile.KeepAlive
import com.nxredux.mobile.KeepAliveService
import com.nxredux.mobile.NotificationIds

/**
 * The foreground service (type dataSync) that keeps an update's download and its install wait alive, with its
 * notification (Cancel stops it through [AppUpdates]). Driven only through [update], as
 * [com.nxredux.mobile.sync.DeviceSyncService] is.
 */
class AppUpdateService : KeepAliveService<String>() {
    companion object {
        private const val TITLE = "NX Redux update"
        private val state = KeepAlive(AppUpdateService::class.java, "NxUpdateService", "update")

        /** The controller side ([UpdatePlatform.keepAlive]): start / update on [active], stop otherwise. Any thread. */
        fun update(context: Context, active: Boolean, text: String?) = state.update(context, active, text)
    }

    override val keepAlive get() = state
    override val notificationId = NotificationIds.APP_UPDATE
    override val channel = Channel("app-update", "App update", "Shows an app update download")
    override val actions = listOf(Action("Cancel", "com.nxredux.mobile.update.CANCEL", 1) { AppUpdates.current()?.cancel() })

    /** Stop the download or the wait (as Cancel) and the service. */
    override fun onTimedOut() { AppUpdates.current()?.cancel() }

    override fun NotificationCompat.Builder.content(progress: String?): NotificationCompat.Builder =
        setContentTitle(TITLE)
            .setContentText(progress?.takeIf { it.isNotBlank() } ?: TITLE)
            .setProgress(0, 0, true)   // the text carries the percent; a wait has none
}
