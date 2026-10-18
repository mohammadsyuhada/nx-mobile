package com.nxredux.mobile.achievements

import com.nxredux.mobile.library.Names
import com.nxredux.mobile.runner.CoreRegistry
import com.nxredux.mobile.storage.GamePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A ROM's place in a console folder's `hashes.json`: the [consoleFolder] under `Roms/` and the [key] inside its file. */
data class RaFingerprintKey(val consoleFolder: String, val key: String)

/** How a game pointer becomes a [RaFingerprintKey], and which hashes may be stored. */
object RaFingerprintKeys {
    /**
     * A home ROM in `Roms/<Folder>/…`: the path below the folder. An extra-folder ROM: `.sources/<id>/<path in the extra>`
     * in its tag's folder ([tagFolder]) — a dot key nx-redux never looks up, so it can't hit a home ROM of the same name.
     * Null for a ROM directly in `Roms/`, in a dot folder, or with a `.`/`..` segment.
     */
    fun of(p: RaGamePointer, tagFolder: String?): RaFingerprintKey? {
        val dir = p.dir.split('/').filter { it.isNotEmpty() }
        val (folder, rest) = if (p.source == GamePath.HOME) {
            val own = RaBadgeFolders.ownFolder(p) ?: return null
            own to dir.drop(2)
        } else {
            (tagFolder ?: return null) to listOf(".sources", p.source) + dir
        }
        val segs = rest + p.file
        if (p.file.isEmpty() || segs.any { it.isEmpty() || it == "." || it == ".." }) return null
        return RaFingerprintKey(folder, segs.joinToString("/"))
    }

    /**
     * False for the hash of a raw archive outside [RaConsoles.ARCADE] (the one console whose raw archive hash nx-redux
     * stores); an unpacked ROM's hash is storable.
     */
    fun storable(tag: String, fileName: String, consoleId: Int, unpacked: Boolean): Boolean =
        unpacked || !Names.isArchive(fileName) || consoleId == RaConsoles.ARCADE || !CoreRegistry.loadsArchive(tag, fileName)
}

/** Reads the stamp [RaHasher.stamp] returns. */
object RaStamp {
    /** "size:millis" as (size, whole seconds); null when [stamp] is null or malformed. */
    fun parse(stamp: String?): Pair<Long, Long>? {
        val (s, m) = stamp?.split(':')?.takeIf { it.size == 2 } ?: return null
        val size = s.toLongOrNull() ?: return null
        val millis = m.toLongOrNull() ?: return null
        return size to millis / 1000
    }
}

/**
 * The per-folder `hashes.json` files in the home tree. [update] reads, changes and writes under one lock per folder
 * (a launch and a prefetch never lose each other's entry); a file is written only when it changed, through
 * `hashes.json.part` ([RaHomeTree.writeText]). A leftover part is read as the file only when a listing shows no main
 * file. A file (main or part) that is there but would not read, or a folder that would not list, is never written over:
 * the handheld writes these files too, and a rewrite from an empty read would drop its entries.
 */
class RaFingerprints(private val home: RaHomeTree) {
    private val locks = HashMap<String, Mutex>()
    private val locksLock = Mutex()

    /** [p]'s place in its console folder's file, or null when it has none (see [RaFingerprintKeys.of]). */
    suspend fun keyFor(p: RaGamePointer): RaFingerprintKey? =
        RaFingerprintKeys.of(p, if (p.source == GamePath.HOME) null else home.folderForTag(p.tag))

    /** The current content of [folder]'s `hashes.json` (empty when absent or unreadable: a lookup misses). */
    suspend fun snapshot(folder: String): RaFingerprintFile = load(folder) ?: RaFingerprintFile.parse(null)

    /** Applies [change] to [folder]'s file under its lock and writes it back only if the change altered it; an unreadable file is left alone. */
    suspend fun update(folder: String, change: (RaFingerprintFile) -> Unit) {
        val lock = locksLock.withLock { locks.getOrPut(folder) { Mutex() } }
        lock.withLock {
            val f = load(folder) ?: return@withLock
            change(f)
            if (f.changed) home.writeText(RaBadgeFolders.dataDir(folder), FILE, f.encode())   // a failure only costs a rehash
        }
    }

    // The file, else the part when no main file is listed, else empty; null when either may be there but was not read.
    private suspend fun load(folder: String): RaFingerprintFile? {
        val dir = RaBadgeFolders.dataDir(folder)
        val text = when (val main = home.read(dir, FILE)) {
            is RaRead.Text -> main.text
            RaRead.Failed -> return null
            RaRead.Absent -> when (val part = home.read(dir, "$FILE.part")) {
                is RaRead.Text -> part.text
                RaRead.Absent -> null
                RaRead.Failed -> return null
            }
        }
        return RaFingerprintFile.parse(text)
    }

    private companion object { const val FILE = "hashes.json" }
}
