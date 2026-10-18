package com.nxredux.mobile.runner

import android.content.Context
import com.swordfish.libretrodroid.NxLog
import java.io.File

/** The app-private stand-in for the handheld's SD card root. */
object NxStorage {
    private const val TAG = "NxStorage"
    private val TEMP = Regex(""".+\.[0-9a-f]{1,8}\.tmp""")

    fun root(context: Context): File = File(context.filesDir, "sd").apply { mkdirs() }

    /**
     * Deletes temp files that [writeAtomically] left behind when the process was killed mid-write. Only
     * files older than a minute are removed, so a write still in flight keeps its temp file.
     */
    fun sweepTemps(context: Context) = sweepTemps(root(context))

    internal fun sweepTemps(root: File) {
        val cutoff = System.currentTimeMillis() - 60_000
        root.walkTopDown()
            .filter { it.isFile && it.lastModified() < cutoff && TEMP.matches(it.name) }
            .forEach { if (!it.delete()) NxLog.w(TAG, "could not delete ${it.path}") }
    }
}
