package com.nxredux.mobile.launcher

import androidx.compose.ui.graphics.ImageBitmap

/** An app another launcher would list. [id] is "package/activity". [game]: the platform says it is a game (its category or flag, or an OUYA game activity). */
data class InstalledApp(val id: String, val label: String, val game: Boolean = false)

/** The platform's launcher side: this app as the home screen, and other apps to start from Tools. */
interface AppLauncher {
    /** Every launchable app except this one, sorted by label. Runs off the main thread. */
    suspend fun installedApps(): List<InstalledApp>
    /** [id]'s current label; null when it is no longer installed or launchable. */
    fun label(id: String): String?
    /** [id]'s launcher icon as a full-colour picture; null when it has none or is gone. Runs off the main thread. */
    suspend fun icon(id: String): ImageBitmap?
    /** Starts [id]; false when it cannot be started. */
    fun launch(id: String): Boolean
    /** This app is the default home app right now. */
    fun isHome(): Boolean
    /** On: offer this app as a home app and ask the system to make it the default. Off: stop offering it. */
    fun setHome(on: Boolean)
}
