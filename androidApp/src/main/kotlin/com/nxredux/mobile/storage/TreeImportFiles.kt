package com.nxredux.mobile.storage

import com.nxredux.mobile.library.ListingUnavailable
import com.nxredux.mobile.runner.ContentRef
import java.io.IOException
import java.io.InputStream

/**
 * A tree folder (one the user picked, SAF or a file ref: [ImportTree]) as import files: the files under each of [tops]
 * (`""` = all of [root]), paths relative to [root], parts and other transient files ([MirrorRules.isTransient]) left
 * out. A hidden folder's [ListingUnavailable] fails the import.
 */
class TreeImportFiles(
    private val tree: ImportTree,
    private val root: ContentRef,
    private val tops: List<String> = listOf(""),
) : ImportFiles {
    override fun entries(): Sequence<ImportEntry> = sequence {
        for (top in tops) {
            if (top.isEmpty()) { yieldAll(walk(root, "")); continue }
            var dir: ContentRef? = root
            for (seg in top.split('/')) dir = dir?.let { tree.child(it, seg)?.takeIf { e -> e.isDirectory }?.ref }
            dir?.let { yieldAll(walk(it, top)) }
        }
    }

    private fun walk(dir: ContentRef, path: String): Sequence<ImportEntry> = sequence {
        for (e in tree.list(dir)) {
            val p = if (path.isEmpty()) e.name else "$path/${e.name}"
            if (e.isDirectory) { yieldAll(walk(e.ref, p)); continue }
            if (MirrorRules.isTransient(e.name)) continue
            yield(object : ImportEntry {
                override val path = p
                override val mtime = e.lastModified?.takeIf { it > 0 }   // unknown (or 0): the import's time, never 1970
                override val size = e.size
                override fun open(): InputStream = tree.openRead(e.ref) ?: throw IOException("cannot read ${e.name}")
            })
        }
    }
}
