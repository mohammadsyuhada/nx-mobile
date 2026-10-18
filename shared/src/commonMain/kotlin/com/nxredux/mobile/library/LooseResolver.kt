package com.nxredux.mobile.library

import com.nxredux.mobile.runner.Console
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.CoreRegistry

/**
 * One candidate file in an extra tree. [relativeDir] is "" at the root, else "a/b". [tag] null = unassigned.
 * [lookupFolder]/[lookupDir] are where its art and map.txt name live: its own folder, or for a disc folder's sheet the
 * folder around it.
 */
data class LooseFile(
    val entry: Entry,
    val folder: ContentRef,
    val relativeDir: String,
    val tag: String?,
    val lookupFolder: ContentRef = folder,
    val lookupDir: String = relativeDir,
)

/**
 * Walks an extra tree and resolves every candidate file with [Resolution]. Hidden names, `.media` and files a sibling disc
 * sheet loads are skipped. A subfolder `D/` holding `D.m3u` (else `D.cue`, else `D.gdi`, any case; see
 * [DiscSheet.folderSheet]) that a core runs is NX Redux's disc folder: one game, its sheet, with art and name from the
 * folder around it; nothing else in `D/` is listed.
 */
class LooseResolver internal constructor(
    private val source: RomsSource,
    private val reads: ScanReads,
    private val defaultTag: (Console) -> String,
    /** A disc image's console by its contents ([DiscProbes]), asked last, for the disc extensions nothing else placed. */
    private val probe: (suspend (Entry, List<Entry>) -> String?)? = null,
) {
    constructor(source: RomsSource, sidecars: Sidecars, defaultTag: (Console) -> String) : this(source, ScanReads(source, sidecars, null), defaultTag)

    /** [skipTop]: lowercase names of folders at [tree]'s root that are not walked (a home's `Roms`, `Bios` ...). */
    suspend fun scan(tree: ContentRef, sourceId: String, skipTop: Set<String> = emptySet()): List<LooseFile> {
        val out = ArrayList<LooseFile>()
        walk(tree, sourceId, "", listOf(source.displayName(tree)), out, skipTop)
        return out
    }

    /** [dir] is listed through [reads] once per scan; its sidecar comes from that listing. */
    private suspend fun walk(
        dir: ContentRef, sourceId: String, relativeDir: String, ancestors: List<String>, out: MutableList<LooseFile>, skip: Set<String> = emptySet(),
    ) {
        val children = reads.list(dir)
        val sidecar = reads.sidecars.read(sourceId, relativeDir, children)
        val folder = Resolution.FolderHints.of(ancestors, defaultTag)
        // A bin/cue or m3u set shows once, as its sheet.
        val hidden = DiscSheet.hiddenIn(children.filter { !it.isDirectory && !Names.isHidden(it.name) }) { source.readHead(it.ref, DiscSheet.HEAD_BYTES) }
        for (c in children) {
            if (Names.isHidden(c.name)) continue
            if (c.isDirectory) {
                if (c.name.lowercase() in skip) continue   // only the root's: deeper walks get none
                val childDir = if (relativeDir.isEmpty()) c.name else "$relativeDir/${c.name}"
                val childAncestors = listOf(c.name) + ancestors
                val disc = discFolder(c, reads.list(c.ref), sourceId, childDir, childAncestors, dir, relativeDir)
                if (disc != null) out += disc else walk(c.ref, sourceId, childDir, childAncestors, out)
                continue
            }
            if (c.name.lowercase() in hidden) continue
            val candidate = Resolution.isCandidate(c.name)
            // png, o, lmp, exe: a core runs them, but only a folder or a sidecar says which (a stray .png is not a game).
            if (!candidate && Names.extensionOf(c.name) !in CoreRegistry.coreExtensions) continue
            // Only a zip is peeked (its first local header names the entry), and only when the folder and sidecar place
            // nothing; a 7z is placed by folder or sidecar alone.
            val tag = Resolution.resolve(c.name, sidecar[c.name], folder, defaultTag) { source.readHead(c.ref, ZipPeek.HEAD_BYTES)?.let(ZipPeek::firstEntryName) }
                ?: probed(c, children)
            if (tag == null && !candidate) continue
            out += LooseFile(c, dir, relativeDir, tag)
        }
    }

    /** [file] (listed in [siblings]) placed by [probe], for a disc extension only. */
    private suspend fun probed(file: Entry, siblings: List<Entry>): String? {
        val p = probe ?: return null
        if (Names.extensionOf(file.name) !in DISC_EXTENSIONS) return null
        return p(file, siblings)?.let { Resolution.acceptsConsole(it, file.name, defaultTag) }
    }

    /**
     * [dir] (listed as [children]) as a disc folder: its [DiscSheet.folderSheet] resolved as a file in [dir] would
     * be, listed from [dir] with [parent]'s lookups. Null when it holds neither sheet or nothing runs it, so it is walked as
     * any folder.
     */
    private suspend fun discFolder(
        dir: Entry, children: List<Entry>, sourceId: String, relativeDir: String, ancestors: List<String>, parent: ContentRef, parentDir: String,
    ): LooseFile? {
        val sheet = DiscSheet.folderSheet(dir.name, children) ?: return null
        val sidecar = reads.sidecars.read(sourceId, relativeDir, children)
        val tag = Resolution.resolve(sheet.name, sidecar[sheet.name], ancestors, null, defaultTag) ?: probed(sheet, children) ?: return null
        return LooseFile(sheet, dir.ref, relativeDir, tag, lookupFolder = parent, lookupDir = parentDir)
    }
}

/** What a disc probe reads: disc images and the sheets that name them. */
private val DISC_EXTENSIONS = setOf("iso", "bin", "img", "cue", "gdi", "chd", "m3u")

/**
 * A scan's disc probes, each file's answer reused from [previous] while its size and modified time (and a sheet's data
 * file's) are unchanged; every readable answer this scan used is in [used], for the next index. An answer from a failed
 * read (or a sheet whose data file is missing) is not kept, so the next scan asks again.
 */
internal class DiscProbes(private val probe: com.nxredux.mobile.library.disc.DiscProbe, previous: List<IndexedProbe>) {
    private val before = previous.associateBy { it.ref }
    val used = ArrayList<IndexedProbe>()

    suspend fun consoleOf(file: Entry, siblings: List<Entry>): String? {
        val known = before[file.ref.value]?.takeIf { p ->
            p.size == file.sizeBytes && p.modified == file.lastModified && dataUnchanged(p, siblings)
        }
        if (known != null) { used += known; return known.console }
        val r = probe.probe(file, siblings)
        if (r.read) used += IndexedProbe(file.ref.value, file.sizeBytes, file.lastModified, r.console, r.data?.ref?.value, r.data?.sizeBytes, r.data?.lastModified)
        return r.console
    }

    /** A sheet's data file is still there as it was probed; true for a probe with none. */
    private fun dataUnchanged(p: IndexedProbe, siblings: List<Entry>): Boolean {
        val ref = p.data ?: return true
        return siblings.any { it.ref.value == ref && it.sizeBytes == p.dataSize && it.lastModified == p.dataModified }
    }
}
