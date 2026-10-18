package com.nxredux.mobile.storage

import com.nxredux.mobile.library.ListingUnavailable
import com.nxredux.mobile.runner.ContentRef
import java.io.IOException
import java.io.InputStream

/**
 * A tree folder in the card layout (the home folder, or one the user picked; SAF or a file ref: [ImportTree]) as import
 * files: the files under its `Saves/` and `.userdata/shared/` ([SaveArchive.ROOTS]). A Move deletes each taken file
 * (through [SaveImporter]) and, in [finish], only the folders this run emptied, walking up to `Saves` and
 * `.userdata/shared` themselves but never to `.userdata` or [root] itself, as [DirImportFiles] does. A hidden folder's
 * [ListingUnavailable] fails the import.
 */
class TreeImportFiles(private val tree: ImportTree, private val root: ContentRef) : ImportFiles {
    // Each walked folder's parent, the roots' excluded: walking up stops after `Saves` or `.userdata/shared`.
    private val parentOf = HashMap<ContentRef, ContentRef>()
    private val emptiedCandidates = LinkedHashSet<ContentRef>()

    override fun entries(): Sequence<ImportEntry> = sequence {
        for (top in SaveArchive.ROOTS) {
            var dir: ContentRef? = root
            for (seg in top.split('/')) dir = dir?.let { tree.child(it, seg)?.takeIf { e -> e.isDirectory }?.ref }
            dir?.let { yieldAll(walk(it, top)) }
        }
    }

    private fun walk(dir: ContentRef, path: String): Sequence<ImportEntry> = sequence {
        for (e in tree.list(dir)) {
            val p = "$path/${e.name}"
            if (e.isDirectory) { parentOf[e.ref] = dir; yieldAll(walk(e.ref, p)); continue }
            if (MirrorRules.isTransient(e.name)) continue
            yield(object : ImportEntry {
                override val path = p
                override val mtime = e.lastModified?.takeIf { it > 0 }   // unknown (or 0): the import's time, never 1970
                override val size = e.size
                override fun open(): InputStream = tree.openRead(e.ref) ?: throw IOException("cannot read ${e.name}")
                override fun delete() = tree.delete(e.ref).also { if (it) emptiedCandidates.add(dir) }
            })
        }
    }

    /** SAF deletes a folder with its contents: each one is listed empty right before it is removed. */
    override fun finish(moved: Boolean, allTaken: Boolean): Boolean {
        if (!moved) return true
        for (start in emptiedCandidates) {
            var dir: ContentRef? = start
            while (true) {
                val d = dir ?: break
                if (d == root) break
                if (!runCatching { tree.list(d).isEmpty() }.getOrDefault(false) || !tree.delete(d)) break
                dir = parentOf[d]
            }
        }
        return true
    }
}
