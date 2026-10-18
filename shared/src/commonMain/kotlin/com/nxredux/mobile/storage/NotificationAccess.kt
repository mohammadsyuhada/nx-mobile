package com.nxredux.mobile.storage

/** The notification permission (Android 13+), asked on setup's Permissions page; null on a platform that needs none. */
interface NotificationAccess {
    fun granted(): Boolean
    /** Shows the platform's prompt; the answer is seen on the next resume ([com.nxredux.mobile.ui.AppState.onAllFilesAccessChanged]). */
    fun request()
}
