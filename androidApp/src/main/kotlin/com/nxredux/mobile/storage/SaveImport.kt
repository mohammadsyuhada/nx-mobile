package com.nxredux.mobile.storage

import com.nxredux.mobile.runner.CoreRegistry
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

/** One file an import offers: its card-root-relative [path] (unchecked), modified time and size. */
interface ImportEntry {
    val path: String
    /** Null when the source has none: the copy then gets the import's time and never replaces a private file. */
    val mtime: Long?
    /** Null when unknown; when known, a copy of another length fails (the source is kept). */
    val size: Long?
    fun open(): InputStream
    /** Deletes the source file (Move); false when it could not be, or (zip) is not deleted per entry. */
    fun delete(): Boolean
}

interface ImportFiles {
    fun entries(): Sequence<ImportEntry>
    /**
     * After the walk: [moved] and [allTaken] (every entry imported or identical) let a zip delete itself. Returns false
     * only when the zip should have been deleted and could not be.
     */
    fun finish(moved: Boolean, allTaken: Boolean): Boolean
}

/**
 * A zip read once, in order: an entry's [ImportEntry.open] streams it straight from the zip (nothing is held in memory)
 * and is valid only until the walk moves on to the next entry. Move deletes the whole zip in [finish] when [allTaken].
 */
class ZipImportFiles(private val open: () -> InputStream, private val deleteZip: () -> Boolean) : ImportFiles {
    override fun entries(): Sequence<ImportEntry> = sequence {
        ZipInputStream(open()).use { z ->
            var current = 0
            while (true) {
                val e = z.nextEntry ?: break
                current++
                if (e.isDirectory) continue
                val mine = current
                yield(object : ImportEntry {
                    override val path = e.name
                    // The extended UTC timestamp when the zip has one (SaveExport writes it), else the local DOS time.
                    override val mtime = (e.lastModifiedTime?.toMillis() ?: e.time).takeIf { it >= 0 }
                    override val size = e.size.takeIf { it >= 0 }
                    override fun open(): InputStream = object : FilterInputStream(z) {
                        private fun ensureCurrent() { if (current != mine) throw IOException("${e.name}: the zip has moved past it") }
                        override fun read(): Int { ensureCurrent(); return super.read() }
                        override fun read(b: ByteArray, off: Int, len: Int): Int { ensureCurrent(); return super.read(b, off, len) }
                        override fun skip(n: Long): Long { ensureCurrent(); return super.skip(n) }
                        override fun close() = Unit   // the zip stays open for the next entry
                    }
                    override fun delete() = false
                })
            }
        }
    }
    override fun finish(moved: Boolean, allTaken: Boolean): Boolean = !(moved && allTaken) || deleteZip()
}

/**
 * A local folder in the card layout ([SaveArchive.ROOTS] under [root]); a Move removes the folders it emptied, `Saves`
 * and `.userdata/shared` included, but never [root] or `.userdata`.
 */
class DirImportFiles(private val root: File) : ImportFiles {
    private val emptiedCandidates = LinkedHashSet<File>()

    override fun entries(): Sequence<ImportEntry> = SaveArchive.ROOTS.asSequence()
        .map { File(root, it) }.filter { it.isDirectory }
        .flatMap { it.walkTopDown().filter { f -> f.isFile } }
        .map { f ->
            object : ImportEntry {
                override val path = f.relativeTo(root).invariantSeparatorsPath
                override val mtime = f.lastModified()
                override val size = f.length()
                override fun open(): InputStream = f.inputStream()
                override fun delete() = f.delete().also { if (it) emptiedCandidates.add(f.parentFile!!) }
            }
        }
    /** Removes only the folders this run's Move emptied, walking up but never removing [root], `.userdata` or anything above. */
    override fun finish(moved: Boolean, allTaken: Boolean): Boolean {
        if (!moved) return true
        val kept = setOf(root, File(root, ".userdata"))
        for (start in emptiedCandidates) {
            var dir: File? = start
            while (dir != null && dir !in kept && dir.startsWith(root) && dir.list()?.isEmpty() == true && dir.delete()) dir = dir.parentFile
        }
        return true
    }
}

/**
 * Settings → Emulators → Saves → Import saves into private storage ([root] = `filesDir/sd`), per [ImportRules]: new
 * files are copied in, identical ones skipped, newer ones replace the private file (kept as a `.bak`,
 * [SaveCopyRules.freeBackupName]), older ones are skipped. Each file lands through a `.tmp` and gets the source's
 * modified time (or the import's, when it has none). The copy is verified against the source's size when that is known
 * (a zip's own CRC check covers its entries). With [move], a source is deleted only after its copy is verified; a zip
 * only when every entry it holds was taken. Each entry is read to its end before the next ([ZipImportFiles] streams).
 * No logging: JVM-tested.
 *
 * Every handheld emulator save folder of a console (`GBA`, `MGBA`) merges into the console's phone folder, a game's
 * save matching across the four Save formats; the winner is written in [format]. A save in a folder several consoles
 * share (`Saves/GPGX`) goes to its game's console by the library's ROM names ([games]).
 */
class SaveImporter(
    private val root: File,
    private val now: () -> Long = System::currentTimeMillis,
    /** Moves the verified temp copy onto its target; injectable so a failing place can be tested. */
    private val rename: (File, File) -> Boolean = File::renameTo,
    /** The phone's Save format: console-folder saves are named and encoded as it says. */
    private val format: SaveFormat = SaveFormat.DEFAULT,
    /** Console tag → the library's ROM names of it ([SharedSaves.gamesByConsole]): where a shared `Saves/GPGX` save goes. */
    private val games: Map<String, Set<String>> = emptyMap(),
) {
    fun run(source: ImportFiles, move: Boolean, onFile: (String) -> Unit = {}): ImportSummary {
        var s = ImportSummary()
        var allTaken = true
        var taken = 0
        for (e in source.entries()) {
            val path = SaveArchive.safePath(e.path)
            val area = path?.let(SaveArchive::areaOf)
            // An ignored entry keeps a zip too: Move deletes it only when every entry it holds was taken.
            if (path == null || area == null) { s = s.copy(ignored = s.ignored + 1); allTaken = false; continue }
            val result = runCatching { take(path, e) }
            val action = result.getOrNull()
            if (action == null) { s = s.copy(failed = s.failed + 1); allTaken = false; continue }
            s = when (action) {
                ImportAction.NEW, ImportAction.REPLACE -> when (area) {
                    SaveArea.SAVES -> s.copy(saves = s.saves + 1)
                    SaveArea.STATES -> s.copy(states = s.states + 1)
                    SaveArea.MARKERS -> s.copy(markers = s.markers + 1)
                }.let { if (action == ImportAction.REPLACE) it.copy(replaced = it.replaced + 1) else it }
                ImportAction.IDENTICAL -> s.copy(identical = s.identical + 1)
                ImportAction.SKIP_OLDER -> { allTaken = false; s.copy(skippedOlder = s.skippedOlder + 1) }
            }
            if (action != ImportAction.SKIP_OLDER) taken++
            if (move && action != ImportAction.SKIP_OLDER) e.delete()
            onFile(path)
        }
        if (!source.finish(move, allTaken && taken > 0)) s = s.copy(zipNotDeleted = true)
        return s
    }

    /**
     * Lands [e] at [path] (its emulator folder mapped onto the console's, [targetPath]) and returns what happened; throws
     * when it could not be copied (the source is then kept). A battery save or clock file at the top of a console folder
     * is matched to the phone's copies of the same game under any Save format's name and written under [format]'s name;
     * a name that needs the ROM file name (MinUI, clock) takes it from the incoming name, else from the newest phone copy
     * that carries one, else keeps the incoming name, as does an incoming `X.gb.sav` (ROM `X.gb` or `X.gb.zip`) they do
     * not settle.
     */
    private fun take(path: String, e: ImportEntry): ImportAction {
        val mapped = targetPath(path)
        val dir = File(root, mapped.substringBeforeLast('/'))
        val incomingName = mapped.substringAfterLast('/')
        val key = SaveKeys.of(incomingName)?.takeIf { isConsoleFolderFile(mapped) }
        // The phone's copies of this game, whatever their name (newest first).
        val mine = if (key == null) emptyList() else dir.listFiles().orEmpty()
            .filter { it.isFile && SaveKeys.of(it.name) == key }.sortedByDescending { it.lastModified() }
        // A Generic `<stem>.sav` under the MinUI format borrows the ROM name of a phone copy (`<rom.ext>.sav`, one this
        // import just wrote from another tag folder included) rather than keeping a name the format never uses. The
        // library's Roms folders are out of this importer's reach (private storage only), so they are not asked. An
        // incoming `X.gb.sav` may be ROM `X.gb.zip`'s Generic name, so it names nothing by itself: unless the phone's
        // copies settle the ROM it keeps its name ([SaveKeys.romOf], [SaveKeys.romAmbiguous]).
        val rom = SaveKeys.romOf(incomingName, mine.map { it.name })
        val target = File(dir, key?.let { if (rom == null && SaveKeys.romAmbiguous(incomingName)) null else SaveKeys.fileName(it, format, rom) } ?: incomingName)
        val tmp = File(dir, ".${target.name}.import.tmp")
        dir.mkdirs()
        try {
            var written = e.open().use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(e.size == null || written == e.size) { "short copy of ${target.name}: $written of ${e.size} bytes" }
            // The phone's copies of this game (the target alone for a file with no game key).
            val same = if (key == null) listOfNotNull(target.takeIf { it.isFile }) else mine
            // A malformed RZIP fails here, before any phone save is touched.
            val raw = if (key?.kind == SaveKind.BATTERY) Rzip.decode(tmp.readBytes()) ?: error("$incomingName: malformed RZIP") else null
            if (raw != null) {
                val bytes = if (format.compressed) Rzip.encode(raw) else raw
                tmp.writeBytes(bytes)
                written = bytes.size.toLong()
            }
            val newest = same.maxByOrNull { it.lastModified() }
            val action = ImportRules.decide(newest?.lastModified(), e.mtime) {
                if (raw == null) sameContent(tmp, newest!!) else Rzip.decode(newest!!.readBytes())?.contentEquals(raw) == true
            }
            val mtime = e.mtime ?: now()   // no source time: the import's, never 1970
            when (action) {
                ImportAction.NEW -> place(tmp, target, mtime, written)
                ImportAction.REPLACE -> {
                    check(tmp.length() == written) { "short copy of ${target.name}" }
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(now()))
                    val kept = mutableListOf<Pair<File, File>>()   // (phone file, its backup)
                    try {
                        for (old in same) {
                            val bak = File(dir, SaveCopyRules.freeBackupName(old.name, stamp) { File(dir, it).exists() })
                            check(old.renameTo(bak)) { "could not keep ${old.name} aside" }
                            kept += old to bak
                        }
                        place(tmp, target, mtime, written)
                    } catch (t: Throwable) {
                        kept.forEach { (old, bak) -> bak.renameTo(old) }
                        throw t
                    }
                }
                ImportAction.IDENTICAL, ImportAction.SKIP_OLDER -> Unit
            }
            return action
        } finally {
            tmp.delete()
        }
    }

    /**
     * [path] with a known console's `Saves/<emulator tag>/` folder turned into `Saves/<console tag>/`; anything else
     * unchanged. A save at the top of a folder several consoles share (`Saves/GPGX`) goes to the console whose library
     * games hold its game ([SharedSaves.consoleOf]), else to the tag's own console (Sega Genesis).
     */
    private fun targetPath(path: String): String {
        val parts = path.split('/', limit = 3)
        if (parts.size < 3 || parts[0] != "Saves") return path
        if ('/' !in parts[2]) SharedSaves.consoleOf(parts[1], parts[2], games)?.let { return "Saves/$it/${parts[2]}" }
        val console = CoreRegistry.consoleOf(parts[1]) ?: return path
        return "Saves/${console.defaultTag}/${parts[2]}"
    }

    /** True for a file directly inside a known console's `Saves/<tag>/` folder. */
    private fun isConsoleFolderFile(path: String): Boolean {
        val parts = path.split('/')
        return parts.size == 3 && parts[0] == "Saves" && CoreRegistry.consoleOf(parts[1]) != null
    }

    private fun place(tmp: File, target: File, mtime: Long, written: Long) {
        check(tmp.length() == written) { "short copy of ${target.name}" }
        check(rename(tmp, target)) { "could not place ${target.name}" }
        target.setLastModified(mtime)
    }

    private fun sameContent(a: File, b: File): Boolean {
        if (a.length() != b.length()) return false
        val bufA = ByteArray(BLOCK)
        val bufB = ByteArray(BLOCK)
        a.inputStream().use { x -> b.inputStream().use { y ->
            while (true) {
                val n = fill(x, bufA)
                if (n != fill(y, bufB)) return false
                if (n == 0) return true
                for (i in 0 until n) if (bufA[i] != bufB[i]) return false
            }
        } }
    }

    /** Reads until [buf] is full or the stream ends (java.io only: no API 33 readNBytes). */
    private fun fill(input: InputStream, buf: ByteArray): Int {
        var n = 0
        while (n < buf.size) {
            val r = input.read(buf, n, buf.size - n)
            if (r < 0) break
            n += r
        }
        return n
    }

    private companion object { const val BLOCK = 64 * 1024 }
}
