package com.nxredux.mobile.update

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nxredux.mobile.NotificationIds

/**
 * The wait notice's Cancel ([AndroidUpdatePlatform.showWaiting]): drops the downloaded update and offers it again, as the
 * download notification's Cancel does. Not exported; the notice's PendingIntent names it explicitly.
 */
class UpdateWaitCancelReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "com.nxredux.mobile.update.CANCEL_WAIT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val updater = AppUpdates.current()
        if (updater != null) updater.cancel()
        // No updater: the process that posted the notice is gone, and its wait with it.
        else context.getSystemService(NotificationManager::class.java)?.cancel(NotificationIds.APP_UPDATE_WAIT)
    }
}
