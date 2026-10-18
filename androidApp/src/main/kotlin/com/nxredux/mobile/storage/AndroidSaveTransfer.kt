package com.nxredux.mobile.storage

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import com.nxredux.mobile.library.LibraryIndexCodec
import com.nxredux.mobile.runner.ContentRef
import com.nxredux.mobile.runner.CoreRegistry
import com.nxredux.mobile.runner.NxStorage
import com.nxredux.mobile.settings.AndroidAppSettings
import com.nxredux.mobile.settings.ImportSource
import com.nxredux.mobile.settings.SaveTransfer
import com.nxredux.mobile.settings.SaveTransferRules
import com.nxredux.mobile.settings.TransferReport
import com.swordfish.libretrodroid.NxLog
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Settings → Emulators → Saves → Export / Import on Android: the pickers come from [pickers] (MainActivity's
 * activity-result launchers); the work runs on one background thread (one transfer at a time) and the report is called
 * on the main thread, with "Working…" (not done) when the work starts and then, done, the summary or the error.
 *
 * An import refuses while a game screen is alive ([gameScreenAlive]: a suspended game would write its save in memory
 * over the imported file), and [importing] stays true from its start to its result so no game starts meanwhile
 * (AppState checks it before the runner).
 */
class AndroidSaveTransfer(
    context: Context,
    private val pickers: Pickers,
    /** Main thread only: some GameActivity holds the session ([com.nxredux.mobile.runner.LibretroRunner.gameScreenAlive]). */
    private val gameScreenAlive: () -> Boolean,
    /** A Device Sync is running: it writes the same saves, so an import waits for it. */
    private val syncing: () -> Boolean = { false },
) : SaveTransfer {
    /** Each answers a SAF document/tree as a content ref, or with All files access a file ref ([FileRefs]); null: cancelled. */
    interface Pickers {
        fun createZip(suggestedName: String, onRef: (ContentRef?) -> Unit)
        fun openZip(onRef: (ContentRef?) -> Unit)
        fun openFolder(onRef: (ContentRef?) -> Unit)
    }

    private val app = context.applicationContext
    private val resolver = app.contentResolver
    private val main = Handler(Looper.getMainLooper())

    override val importing: Boolean get() = importsRunning.get() > 0

    override fun exportSaves(report: TransferReport) {
        pickers.createZip(SaveExport.suggestedName(System.currentTimeMillis())) { ref ->
            ref ?: return@createZip
            work(report) {
                // The list-style picker's target (the user agreed to replace an existing one): written through a part.
                val exportTag = exportTagMapper()
                if (FileRefs.isFile(ref)) return@work SaveExport.writeFile(NxStorage.root(app), File(FileRefs.path(ref)), exportTag).text()
                val uri = Uri.parse(ref.value)
                val summary = try {
                    val out = resolver.openOutputStream(uri, "wt") ?: error("cannot write the zip")
                    out.use { SaveExport.write(NxStorage.root(app), it, exportTag) }
                } catch (e: Throwable) {
                    deleteDocument(uri)   // never leave a half zip
                    throw e
                }
                if (summary.saves == 0 && summary.states == 0) deleteDocument(uri)   // "Nothing to export": no empty zip left
                summary.text()
            }
        }
    }

    override fun importSaves(source: ImportSource, move: Boolean, report: TransferReport) {
        SaveTransferRules.importRefusal(gameScreenAlive(), syncing())?.let { return report(it, true) }   // before any picker
        when (source) {
            ImportSource.HOME -> {
                val home = AndroidAppSettings(app).treeRef ?: return report("No home folder", true)
                // Revoked access walks a file home as empty: say why instead.
                if (FileRefs.accessOff(home, AndroidAllFilesAccess.isGranted())) return report(FileRefs.ACCESS_OFF_MESSAGE, true)
                importWork(report) { importTree(home, move) }
            }
            ImportSource.FOLDER -> pickers.openFolder { ref ->
                ref ?: return@openFolder
                importWork(report) { importTree(ref, move) }   // runs now, under the picker's temporary grant (SAF)
            }
            ImportSource.ZIP -> pickers.openZip { ref ->
                ref ?: return@openZip
                importWork(report) {
                    val file = if (FileRefs.isFile(ref)) File(FileRefs.path(ref)) else null
                    val open: () -> InputStream? = { if (file != null) FileInputStream(file) else resolver.openInputStream(Uri.parse(ref.value)) }
                    // The picker shows every file type (a zip filter hid everything on Samsung's picker): only a zip goes on.
                    if (!isZip(open)) return@importWork "That file is not a zip"
                    val zip = ZipImportFiles({ open() ?: error("cannot read the zip") }) { file?.delete() ?: deleteDocument(Uri.parse(ref.value)) }
                    importer().run(zip, move).text(move)
                }
            }
        }
    }

    /** [work] for an import: asked again after a picker (a game may have started meanwhile); [importing] until the result. */
    private fun importWork(report: TransferReport, block: () -> String) {
        SaveTransferRules.importRefusal(gameScreenAlive(), syncing())?.let { return report(it, true) }
        importsRunning.incrementAndGet()
        work({ text, done -> if (done) importsRunning.decrementAndGet(); report(text, done) }, block)
    }

    /** A SAF pick is a tree URI: the walk starts at its root document. A file ref (All files access) is the folder itself. */
    private fun importTree(root: ContentRef, move: Boolean): String {
        val start = if (FileRefs.isFile(root)) root else Uri.parse(root.value).let {
            ContentRef(DocumentsContract.buildDocumentUriUsingTree(it, DocumentsContract.getTreeDocumentId(it)).toString())
        }
        return importer().run(TreeImportFiles(ImportTree.of(resolver, root), start), move).text(move)
    }

    /**
     * Console folder → the handheld folder of the console's default emulator (Settings → Default emulators; GPGX_SMS's is
     * GPGX, [CoreRegistry.handheldTag]); other folders as they are.
     */
    private fun exportTagMapper(): (String) -> String {
        val defaults = AndroidAppSettings(app).defaultTags
        return { x -> CoreRegistry.consoleOf(x)?.let { CoreRegistry.handheldTag(it.defaultTagFor(defaults[it.id])) } ?: x }
    }

    /** An importer writing console-folder saves in the phone's Save format. */
    private fun importer() = SaveImporter(NxStorage.root(app), format = AndroidAppSettings(app).saveNaming.saveFormat, games = libraryGames())

    /**
     * The library's ROM names per console ([SharedSaves.gamesByConsole]), read off the main thread from its index; empty
     * when there is none.
     */
    private fun libraryGames(): Map<String, Set<String>> = runCatching {
        File(NxStorage.root(app), ".userdata/android/library.json").takeIf { it.isFile }?.readText()?.let(LibraryIndexCodec::decode)
    }.getOrNull().let(SharedSaves::gamesByConsole)

    /** True when the stream starts like a zip (`PK\3\4`, or `PK\5\6` for an empty one). */
    private fun isZip(open: () -> InputStream?): Boolean = runCatching {
        open()?.use { s ->
            val head = ByteArray(4)
            var n = 0
            while (n < head.size) { val r = s.read(head, n, head.size - n); if (r < 0) break; n += r }
            n == 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte() &&
                ((head[2] == 3.toByte() && head[3] == 4.toByte()) || (head[2] == 5.toByte() && head[3] == 6.toByte()))
        } ?: false
    }.getOrDefault(false)

    private fun deleteDocument(uri: Uri): Boolean =
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }.onFailure { NxLog.w(TAG, "could not delete $uri", it) }.getOrDefault(false)

    /** Runs [block] on the transfer thread; any throwable (a corrupt zip, a CRC error, out of memory) becomes "Failed: …". */
    private fun work(report: TransferReport, block: () -> String) {
        main.post { report("Working…", false) }
        executor.execute {
            val text = try {
                block()
            } catch (e: Throwable) {
                NxLog.e(TAG, "save transfer failed", e)
                "Failed: ${e.message ?: e::class.simpleName}"
            }
            main.post { report(text, true) }
        }
    }

    companion object {
        private const val TAG = "SaveTransfer"
        private val executor = Executors.newSingleThreadExecutor { Thread(it, "nx-save-transfer") }
        /** Imports started and not yet reported, process-wide (a recreated MainActivity makes a new instance). */
        private val importsRunning = AtomicInteger()

        /** Some import (from any instance) is still writing saves; Device Sync refuses meanwhile. */
        internal val anyImporting: Boolean get() = importsRunning.get() > 0
    }
}
