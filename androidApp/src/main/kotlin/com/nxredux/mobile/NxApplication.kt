package com.nxredux.mobile

import android.app.Application
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.tools.CheatsRoot
import com.swordfish.libretrodroid.NxLog
import java.io.File

/**
 * Applies Settings → Developer → Debug log when the process starts, whichever activity starts it (a game
 * launched from another launcher starts at GameActivity), moves cheats an earlier version kept in private
 * storage into the home folder, and clears an earlier run's in-app update download.
 */
class NxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Off the main thread, as both touch storage: drops an earlier run's update download (its .part, or an APK the
        // installer already took or the user cancelled), then starts moving older private cheats into the home folder.
        Thread({
            File(cacheDir, "update").deleteRecursively()
            CheatsRoot.of(this)
        }, "cheats-root").start()
        // Off is both flags' default: only On needs setting, so a launcher-only start never loads the native library.
        if (!AndroidAppSettings(this).debugLog) return
        try {
            NxLog.enabled = true
        } catch (e: UnsatisfiedLinkError) {
            // The Kotlin flag is already set (the setter sets it first); a game launch hits the same load failure.
            NxLog.e("NXAPP", "native log flag not set", e)
        }
    }
}
