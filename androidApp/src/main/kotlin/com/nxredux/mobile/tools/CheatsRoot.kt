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

    private val moveLock = Any()
    /** The home this process already moved private cheats into (what is left there conflicts with the home's files). */
    @Volatile private var settled: String? = null
    @Volatile private var moving = false

    /**
     * The root to read and write cheats in. While cheats an earlier version kept privately still have to move to the home
     * folder, that is private storage, whose set is whole until the move's last step; the move starts in the background
     * the first time it is needed (at process start, or once All files access or a file-path home is set).
     */
    fun of(context: Context): File {
        val private = NxStorage.root(context)
        val home = pick(AndroidAppSettings(context).treeRef, AndroidAllFilesAccess.isGranted(), private)
        if (home == private) { settled = null; return home }   // access off: what is installed meanwhile moves later
        if (settled == home.path) return home
        if (!hasCheats(private)) { settled = home.path; return home }
        startMove(context.applicationContext)
        return private
    }

    /** [home] when it is a file ref, [granted] and an existing folder; [private] otherwise. */
    fun pick(home: ContentRef?, granted: Boolean, private: File): File =
        home?.takeIf { granted && FileRefs.isFile(it) }?.let { File(FileRefs.path(it)) }?.takeIf { it.isDirectory } ?: private

    /** Blocks until a running move is done, so an install never writes where the move is copying. Not on the main thread. */
    fun awaitMove() { synchronized(moveLock) {} }

    private fun startMove(app: Context) {
        synchronized(this) { if (moving) return; moving = true }
        Thread({
            try {
                synchronized(moveLock) {
                    val private = NxStorage.root(app)
                    val home = pick(AndroidAppSettings(app).treeRef, AndroidAllFilesAccess.isGranted(), private)
                    if (home != private) {
                        val moved = moveFromPrivate(private, home)
                        if (moved > 0) NxLog.i(TAG, "moved $moved cheat files into ${home.path}")
                        settled = home.path
                    }
                }
                CheatDbService.refreshIfStarted()
            } catch (e: Exception) {
                NxLog.w(TAG, "moving cheats into the home folder failed", e)
            } finally {
                moving = false
            }
        }, "cheats-move").start()
    }

    private fun hasCheats(private: File) = File(private, CHEATS).isDirectory || File(private, CheatDbManifest.PATH).isFile

    /**
     * Moves [private]'s `Cheats/` files and the manifest into [home], never over a file there: a private copy identical
     * to the home's is dropped, a different one stays where it was. Everything is copied before anything is deleted, the
     * manifest last, so either root holds a whole set throughout. Emptied private folders are removed. Returns the files
     * moved.
     */
    fun moveFromPrivate(private: File, home: File): Int {
        if (private.canonicalFile == home.canonicalFile) return 0
        val cheats = File(private, CHEATS)
        val manifest = File(private, CheatDbManifest.PATH)
        val files = cheats.walkTopDown().filter { it.isFile }.toList() + listOf(manifest).filter { it.isFile }
        var moved = 0
        val gone = files.filter { f ->
            when (copy(f, File(home, f.relativeTo(private).path))) {
                Copy.COPIED -> { moved++; true }
                Copy.SAME -> true
                Copy.KEPT -> false
            }
        }
        gone.forEach { it.delete() }
        cheats.walkBottomUp().filter { it.isDirectory }.forEach { it.delete() }   // only empty ones go
        manifest.parentFile?.delete()
        return moved
    }

    private enum class Copy { COPIED, SAME, KEPT }

    private fun copy(from: File, to: File): Copy {
        if (to.exists()) {
            // Lengths first: a home database from another device differs in most files, and reading both is the cost.
            val same = to.isFile && to.length() == from.length() &&
                runCatching { to.readBytes().contentEquals(from.readBytes()) }.getOrDefault(false)
            return if (same) Copy.SAME else Copy.KEPT
        }
        return try {
            to.parentFile?.mkdirs()
            from.copyTo(to)
            Copy.COPIED
        } catch (e: IOException) {
            NxLog.w(TAG, "could not move ${from.name}", e)
            to.delete()
            Copy.KEPT
        }
    }
}
