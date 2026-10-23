package com.nxredux.mobile.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.swordfish.libretrodroid.NxLog

/**
 * PackageInstaller's status for an update session (the commit's PendingIntent targets this receiver explicitly; it is
 * not exported). Pending user action opens the system's confirm screen; backing out of it arrives as aborted, which is
 * a cancel; any other failure fails the update. Success needs no answer: the install replaces and restarts the app.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NxUpdate"
        const val ACTION = "com.nxredux.mobile.update.INSTALL_STATUS"

        /** A committed session's answer, and whether STARTED was reported. Main thread only (onReceive runs there). */
        private class Waiting(val onResult: (UpdateInstallResult) -> Unit, var started: Boolean = false)
        private val waiting = HashMap<Int, Waiting>()

        /** Routes [sessionId]'s statuses to [onResult]; called on the main thread before the commit. */
        fun expect(sessionId: Int, onResult: (UpdateInstallResult) -> Unit) { waiting[sessionId] = Waiting(onResult) }

        /** The commit itself failed: no status will come. */
        fun forget(sessionId: Int) { waiting.remove(sessionId) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val w = waiting[id]
        if (w == null) {   // a session from before this process (it died meanwhile): nothing waits on it
            NxLog.w(TAG, "status $status for unknown session $id")
            return
        }
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm == null) { finish(id, w, UpdateInstallResult.FAILED, "no confirm intent"); return }
                try {
                    // The install only starts while the main menu shows: the app is in the foreground, so the launch is allowed.
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    finish(id, w, UpdateInstallResult.FAILED, "confirm screen refused (${e::class.java.simpleName})")
                    return
                }
                if (!w.started) { w.started = true; w.onResult(UpdateInstallResult.STARTED) }
            }
            PackageInstaller.STATUS_SUCCESS -> waiting.remove(id)   // the replaced app restarts; nothing to show
            PackageInstaller.STATUS_FAILURE_ABORTED -> finish(id, w, UpdateInstallResult.CANCELLED, null)
            else -> finish(id, w, UpdateInstallResult.FAILED, "status $status: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }

    private fun finish(id: Int, w: Waiting, result: UpdateInstallResult, why: String?) {
        waiting.remove(id)
        if (why != null) NxLog.w(TAG, "install failed: $why")
        w.onResult(result)
    }
}
