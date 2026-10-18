package com.nxredux.mobile.sync

import java.io.File
import kotlin.math.abs

/** Whether a folder keeps a file's modified time: ROM sync (`-t`) is only worth running where it does. */
object SyncFileTimes {
    private const val TEMP = ".nx-sync-time.tmp"
    private const val DAY_MS = 86_400_000L
    private const val TOLERANCE_MS = 2_000L

    /** Sets a temp file's time in [dir] a day before [now], reads it again: true when it stuck (±2 s). Any failure is false. */
    fun check(dir: File, now: Long = System.currentTimeMillis()): Boolean {
        val file = File(dir, TEMP)
        return try {
            file.writeText("")
            val target = now - DAY_MS
            file.setLastModified(target) && abs(file.lastModified() - target) <= TOLERANCE_MS
        } catch (e: Exception) {
            false
        } finally {
            runCatching { file.delete() }
        }
    }
}
