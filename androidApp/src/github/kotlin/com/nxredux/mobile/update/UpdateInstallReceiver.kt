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

        /**
         * A committed session's answer, whether STARTED was reported, and the confirm screen's intent once asked for (kept for
         * [AndroidUpdatePlatform.resumeInstall]). Main thread only (onReceive runs there).
         */
        private class Waiting(
            val clear: () -> Boolean,
            val onResult: (UpdateInstallResult) -> Unit,
            var started: Boolean = false,
            var confirm: Intent? = null,
        )
        private val waiting = HashMap<Int, Waiting>()

        /**
         * Routes [sessionId]'s statuses to [onResult]; called on the main thread before the commit. [clear]: whether the
         * confirm screen may open now (nothing started meanwhile holds the install).
         */
        fun expect(sessionId: Int, clear: () -> Boolean, onResult: (UpdateInstallResult) -> Unit) { waiting[sessionId] = Waiting(clear, onResult) }

        /** The commit itself failed, or the session was abandoned: no status will be passed on. */
        fun forget(sessionId: Int) { waiting.remove(sessionId) }

        /** Whether [sessionId] still waits on a status. */
        fun waits(sessionId: Int): Boolean = sessionId in waiting

        /** The confirm screen [sessionId]'s status asked for; null before that status came. */
        fun confirm(sessionId: Int): Intent? = waiting[sessionId]?.confirm
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
                w.confirm = Intent(confirm)
                // A game started while the APK was copied in: keep the confirm screen for the next free main menu.
                if (!w.clear()) { started(w); return }
                try {
                    // The install is only committed while an activity is started (busyReason waits on NxApplication.foreground),
                    // so a visible app may launch this. Leaving the app in the moment between commit and status can still block it;
                    // the next main menu reopens it then ([AndroidUpdatePlatform.resumeInstall]), as it does after Home on the screen.
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    finish(id, w, UpdateInstallResult.FAILED, "confirm screen refused (${e::class.java.simpleName})")
                    return
                }
                started(w)
            }
            PackageInstaller.STATUS_SUCCESS -> waiting.remove(id)   // the replaced app restarts; nothing to show
            PackageInstaller.STATUS_FAILURE_ABORTED -> finish(id, w, UpdateInstallResult.CANCELLED, null)
            else -> finish(id, w, UpdateInstallResult.FAILED, "status $status: ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}")
        }
    }

    private fun started(w: Waiting) {
        if (!w.started) { w.started = true; w.onResult(UpdateInstallResult.STARTED) }
    }

    private fun finish(id: Int, w: Waiting, result: UpdateInstallResult, why: String?) {
        waiting.remove(id)
        if (why != null) NxLog.w(TAG, "install failed: $why")
        w.onResult(result)
    }
}
