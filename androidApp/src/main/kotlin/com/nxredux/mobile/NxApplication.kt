package com.nxredux.mobile

import android.app.Application
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.tools.CheatDbService
import com.nxredux.mobile.tools.CheatsRoot
import com.swordfish.libretrodroid.NxLog

/**
 * Applies Settings → Developer → Debug log when the process starts, whichever activity starts it (a game
 * launched from another launcher starts at GameActivity), and moves cheats an earlier version kept in private
 * storage into the home folder.
 */
class NxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Off the main thread: a database is thousands of files, copied across file systems.
        Thread({ runCatching { CheatsRoot.moveFromPrivate(this); CheatDbService.refreshIfStarted() } }, "cheats-move").start()
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
