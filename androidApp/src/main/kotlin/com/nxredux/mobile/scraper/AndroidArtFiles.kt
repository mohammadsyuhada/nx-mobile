package com.nxredux.mobile.scraper

import android.content.Context
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.writeAtomically
import com.swordfish.libretrodroid.NxLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The Android games console's screenshots at `filesDir/android-art/<package>.png` (the package is the app id before
 * `/`), app-private: never the home tree, which holds only ROM data. A stored one's ref is a `file://` uri (Coil's model)
 * with the file's version, `?v=<lastModified>`: a replaced screenshot gets a new ref, so Coil's memory cache (keyed by
 * the uri) loads it afresh and the console, compared by refs, is rebuilt. Coil opens the uri's path, without the query.
 */
class AndroidArtFiles(private val dir: File) : AndroidArtStore {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, DIR))

    companion object {
        const val DIR = "android-art"
        private const val TAG = "AndroidArtFiles"
        private val PACKAGE = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*")
    }

    /** [appId]'s file; null for an id whose package is not a package name (nothing outside [dir] is ever named). */
    private fun file(appId: String): File? = appId.substringBefore('/').takeIf { PACKAGE.matches(it) }?.let { File(dir, "$it.png") }

    override fun screenshot(appId: String): ContentRef? =
        file(appId)?.takeIf { it.isFile }?.let { ContentRef("file://${it.absolutePath}?v=${it.lastModified()}") }

    override suspend fun writeScreenshot(appId: String, png: ByteArray): ContentRef? = withContext(Dispatchers.IO) {
        val f = file(appId) ?: return@withContext null
        runCatching { f.writeAtomically(png) }
            .onFailure { NxLog.w(TAG, "write failed (${it::class.java.simpleName})") }
            .getOrNull() ?: return@withContext null
        screenshot(appId)
    }

    /** Deletes every file in [dir] (a temp file left by an interrupted write too); counts only the `.png` screenshots deleted. */
    override suspend fun deleteAll(): Int = withContext(Dispatchers.IO) {
        val files = dir.listFiles().orEmpty().filter { it.isFile }
        files.count { f ->
            val gone = runCatching { f.delete() }.getOrDefault(false)
            if (!gone) NxLog.w(TAG, "delete failed")
            gone && f.name.endsWith(".png")
        }
    }
}
