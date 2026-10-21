package com.nxredux.mobile.tools

import android.content.Context
import com.nxredux.mobile.cheats.db.CheatDbManifest
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.storage.AndroidAllFilesAccess
import com.nxredux.mobile.storage.FileRefs
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.io.IOException

/**
 * Where `Cheats/<TAG>/` and the Cheat Database manifest live: the home folder when it is a file path with All files
 * access, so an uninstall keeps the download and the folders match the handheld's card; app-private storage otherwise
 * (a picker home has no path to read cheats by, and access can be turned off).
 */
object CheatsRoot {
    private const val TAG = "CheatsRoot"
    private const val CHEATS = "Cheats"

    fun of(context: Context): File = pick(AndroidAppSettings(context).treeRef, AndroidAllFilesAccess.isGranted(), NxStorage.root(context))

    /** [home] when it is a file ref, [granted] and an existing folder; [private] otherwise. */
    fun pick(home: ContentRef?, granted: Boolean, private: File): File =
        home?.takeIf { granted && FileRefs.isFile(it) }?.let { File(FileRefs.path(it)) }?.takeIf { it.isDirectory } ?: private

    /** Moves cheats an earlier version kept in private storage into the home folder, once the home can hold them. */
    fun moveFromPrivate(context: Context) {
        val private = NxStorage.root(context)
        val root = of(context)
        if (root == private) return
        val moved = moveFromPrivate(private, root)
        if (moved > 0) NxLog.i(TAG, "moved $moved cheat files into ${root.path}")
    }

    /**
     * Moves [private]'s `Cheats/` files and the manifest into [home], never over a file there: a private copy identical
     * to the home's is dropped, a different one stays where it was. The manifest goes last, so the database only reads as
     * installed in [home] once its files are there. Emptied private folders are removed. Returns the files moved.
     */
    fun moveFromPrivate(private: File, home: File): Int {
        if (private.canonicalFile == home.canonicalFile) return 0
        var moved = 0
        val cheats = File(private, CHEATS)
        cheats.walkTopDown().filter { it.isFile }.toList().forEach { f ->
            if (move(f, File(home, f.relativeTo(private).path))) moved++
        }
        val manifest = File(private, CheatDbManifest.PATH)
        if (manifest.isFile && move(manifest, File(home, CheatDbManifest.PATH))) moved++
        cheats.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }   // only empty ones go
        manifest.parentFile?.delete()
        return moved
    }

    /** True when [from] now lives at [to]. */
    private fun move(from: File, to: File): Boolean {
        if (to.exists()) {
            if (to.isFile && runCatching { to.readBytes().contentEquals(from.readBytes()) }.getOrDefault(false)) from.delete()
            return false
        }
        return try {
            to.parentFile?.mkdirs()
            // Private storage and the home are different file systems: rename fails, so copy, then delete.
            if (!from.renameTo(to)) {
                from.copyTo(to)
                from.delete()
            }
            true
        } catch (e: IOException) {
            NxLog.w(TAG, "could not move ${from.name}", e)
            to.delete()
            false
        }
    }
}
