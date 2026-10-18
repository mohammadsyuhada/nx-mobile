package com.nxredux.mobile.runner

import android.content.Context
import com.nxredux.mobile.storage.CacheEntry
import com.nxredux.mobile.storage.CacheSweep
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

/**
 * The ROM cache, `cacheDir/roms/`: one top-level entry per archive extraction (`<archive name>.<length>/<inner ROM>`,
 * [Archives]) or real-path copy (`<file name>`, GameActivity.copyToCache). An entry's mtime is its last use (touched on
 * creation and on every reuse), and [sweep] trims the cache by [CacheSweep]'s policy. Each entry has one lock
 * (unpack/copy and delete of one entry never overlap) and a hold count: a held entry (the running game's, one being
 * hashed) is never deleted. Both are per process, and the app runs in one.
 */
object RomCache {
    private const val TAG = "RomCache"
    /**
     * A `.part` older than this is a killed process's leftover. In this process an unpack in flight holds its entry's
     * lock (the sweep waits for it), so the age rule only guards a `.part` a dying process may have written moments ago.
     */
    private const val PART_MAX_AGE_MILLIS = 60L * 60 * 1000
    private const val PART = ".part"

    private val locks = ConcurrentHashMap<String, Any>()
    private val holds = ConcurrentHashMap<String, Int>()

    fun root(context: Context): File = File(context.cacheDir, "roms")

    /** The lock of top-level entry [name]; held while it is unpacked, copied, reused or deleted. */
    fun lockFor(name: String): Any = locks.getOrPut(name) { Any() }

    /** Marks entry [name] in use until the matching [release]; counted, so a game and a hash may hold one entry at once. */
    fun hold(name: String) { holds.merge(name, 1, Int::plus) }

    fun release(name: String) { holds.computeIfPresent(name) { _, n -> (n - 1).takeIf { it > 0 } } }

    private fun isHeld(name: String) = holds.containsKey(name)

    /** Records a use of top-level [entry] (and of [inner], the ROM inside an extraction): their mtimes become now. */
    fun touch(entry: File, inner: File? = null) {
        val now = System.currentTimeMillis()
        inner?.setLastModified(now)
        entry.setLastModified(now)
    }

    /**
     * Deletes what [CacheSweep] picks (unused for 30 days, then least recently used first while over 256 MiB) and `.part`
     * leftovers older than an hour. Never deletes a held entry, one with a younger `.part` (an unpack in flight), or
     * one used since it was listed. Blocking (it may wait on an unpack's lock): call off the main thread. Best effort:
     * each entry fails alone and nothing is thrown.
     */
    fun sweep(context: Context, now: Long = System.currentTimeMillis()) {
        runCatching {
            val root = root(context)
            val children = root.listFiles() ?: return
            val listed = children.mapNotNull { f -> runCatching { CacheEntry(f.name, sizeOf(f), f.lastModified()) }.getOrNull() }
            val unpacking = children.filter { f -> f.isDirectory && parts(f).any { now - it.lastModified() <= PART_MAX_AGE_MILLIS } }.map { it.name }
            val victims = CacheSweep.victims(listed, now, keep = holds.keys + unpacking)
            val usedAt = listed.associate { it.name to it.lastUsedMillis }
            var freed = 0L
            for (name in victims) runCatching {
                val entry = File(root, name)
                synchronized(lockFor(name)) {
                    // Held or reused while we waited for its lock: it is the running game's or was just unpacked again.
                    if (isHeld(name) || entry.lastModified() != usedAt[name]) return@synchronized
                    val bytes = sizeOf(entry)
                    if (deleteTree(entry)) freed += bytes else NxLog.w(TAG, "could not delete ${entry.path}")
                }
            }.onFailure { NxLog.w(TAG, "sweep of $name failed", it) }
            for (dir in root.listFiles().orEmpty()) if (dir.isDirectory && !isLink(dir)) runCatching { dropStaleParts(dir, now) }
                .onFailure { NxLog.w(TAG, "part sweep of ${dir.name} failed", it) }
            if (victims.isNotEmpty()) NxLog.i(TAG, "swept ${victims.size} of ${listed.size} entries, ${freed shr 20} MiB")
        }.onFailure { NxLog.w(TAG, "sweep failed", it) }
    }

    /** A stale `.part` is removed without bumping the entry's mtime (the removal is not a use). */
    private fun dropStaleParts(dir: File, now: Long) = synchronized(lockFor(dir.name)) {
        if (isHeld(dir.name)) return@synchronized
        val stale = parts(dir).filter { now - it.lastModified() > PART_MAX_AGE_MILLIS }
        if (stale.isEmpty()) return@synchronized
        val usedAt = dir.lastModified()
        stale.forEach { if (!it.delete()) NxLog.w(TAG, "could not delete ${it.path}") }
        dir.setLastModified(usedAt)
    }

    private fun parts(dir: File): List<File> = dir.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(PART) }

    /** Symlinks are not followed (an entry never holds one; if it did, what it points at is not the cache's). */
    private fun isLink(f: File) = Files.isSymbolicLink(f.toPath())

    private fun sizeOf(f: File): Long = when {
        isLink(f) -> 0L
        f.isDirectory -> f.listFiles().orEmpty().sumOf(::sizeOf)
        else -> f.length()
    }

    /** Deletes [f] and, if it is a real directory, its contents; a symlink is removed itself, never its target. */
    private fun deleteTree(f: File): Boolean {
        if (!isLink(f) && f.isDirectory) f.listFiles().orEmpty().forEach(::deleteTree)
        return f.delete() || !f.exists()
    }
}
