package com.nxredux.mobile.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nxredux.mobile.BuildConfig
import com.nxredux.mobile.LaunchActivity
import com.nxredux.mobile.NotificationIds
import com.swordfish.libretrodroid.NxLog

/**
 * `MY_PACKAGE_REPLACED`: an install replaced this app, which Android killed and does not reopen. Tries to reopen it
 * (Android usually blocks an activity start from the background) and posts a one-shot notice that opens it, cleared
 * when the app opens ([AppUpdates.onAppOpened]). Runs in the new version's process, so [BuildConfig] is the new one.
 */
class AppUpdatedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NxUpdate"

        fun cancelNotice(context: Context) = NotificationManagerCompat.from(context).cancel(NotificationIds.APP_UPDATED)

        private fun launchIntent(context: Context) =
            Intent(context, LaunchActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        try {
            context.startActivity(launchIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            NxLog.w(TAG, "reopen after update refused (${e::class.java.simpleName})")
        }
        postNotice(context)
    }

    private fun postNotice(context: Context) {
        // API 33+ needs the runtime grant; without it the notice is skipped, not asked for.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(AppUpdateService.CHANNEL_ID) == null) {
            // Same settings as the update service's own channel (KeepAliveService.ensureChannel), which may never have run.
            nm.createNotificationChannel(
                NotificationChannel(AppUpdateService.CHANNEL_ID, AppUpdateService.CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                    description = AppUpdateService.CHANNEL_DESCRIPTION
                    setShowBadge(false)
                },
            )
        }
        val open = PendingIntent.getActivity(context, 0, launchIntent(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notice = NotificationCompat.Builder(context, AppUpdateService.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("NX Redux updated to ${BuildConfig.VERSION_NAME}")
            .setContentText("Tap to open")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(NotificationIds.APP_UPDATED, notice)
        } catch (e: SecurityException) {   // the grant was revoked between the check and the post
            NxLog.w(TAG, "updated notice refused")
        }
    }
}
