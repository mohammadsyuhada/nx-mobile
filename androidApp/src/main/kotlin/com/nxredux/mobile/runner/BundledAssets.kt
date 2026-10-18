package com.nxredux.mobile.runner

import android.content.Context
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** Bundled asset trees the app owns outright: replaced whole whenever the APK changes, never merged with user files. */
object BundledAssets {
    private const val TAG = "BundledAssets"
    private const val STAMP = ".bundled"

    // One lock per tree: the overlay copy (~10 MB) must not hold up the shader copy a game may be waiting on.
    private val locks = ConcurrentHashMap<String, Any>()

    /**
     * Makes `sd/<dir>` an exact copy of `assets/<assetDir>` if the installed APK changed since the last sync.
     * The fresh tree is built beside the live one and swapped in by renames, so the live tree is never
     * half-written; on any failure the current tree stays and the next start retries. Blocking I/O: call off
     * the main thread. Locked per [dir], so concurrent callers of one tree (MainActivity and GameActivity) never
     * build it twice at once, while different trees sync in parallel. Returns true when a refresh ran and
     * swapped in a new tree.
     */
    fun sync(context: Context, assetDir: String, dir: String): Boolean = synchronized(locks.getOrPut(dir) { Any() }) {
        val sd = NxStorage.root(context)
        val target = File(sd, dir)
        val fresh = File(sd, "$dir.new")
        val old = File(sd, "$dir.old")
        runCatching {
            // lastUpdateTime changes on every install, dev builds included, unlike versionCode.
            val current = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
            val stamp = File(target, STAMP)
            if (stamp.isFile && stamp.readText() == current) {
                // Strays from a run killed mid-sync or mid-swap.
                if (fresh.exists()) fresh.deleteRecursively()
                if (old.exists()) old.deleteRecursively()
                return false
            }
            fresh.deleteRecursively()   // a leftover from a killed run
            val assets = context.assets
            fun copy(rel: String) {
                val path = if (rel.isEmpty()) assetDir else "$assetDir/$rel"
                val children = assets.list(path).orEmpty()
                if (children.isEmpty() && rel.isNotEmpty()) {   // a file
                    assets.open(path).use { File(fresh, rel).writeAtomically(it.readBytes()) }
                    return
                }
                children.forEach { copy(if (rel.isEmpty()) it else "$rel/$it") }
            }
            copy("")
            // Last: a tree without its stamp is never trusted, so a partial copy can't pass the fast path.
            File(fresh, STAMP).writeAtomically(current.toByteArray())
            swap(target, fresh)
            NxLog.i(TAG, "synced $assetDir -> $dir ($current)")
        }.onFailure {
            NxLog.w(TAG, "could not sync $assetDir; keeping the current $dir, will retry next start", it)
            fresh.deleteRecursively()
        }.isSuccess
    }

    /**
     * Replaces [target] with [fresh] by renames: the only mutation of the live tree. Files already open in the
     * old tree stay readable until closed. Throws with [target] left in place (or restored) when a rename fails.
     */
    internal fun swap(target: File, fresh: File) {
        val old = File(target.parentFile, "${target.name}.old")
        old.deleteRecursively()   // a leftover from a killed run; renameTo won't replace a non-empty directory
        val hadTarget = target.exists()
        if (hadTarget && !target.renameTo(old)) throw IOException("rename failed: $target -> $old")
        if (!fresh.renameTo(target)) {
            if (hadTarget) old.renameTo(target)
            throw IOException("rename failed: $fresh -> $target")
        }
        old.deleteRecursively()
    }
}
